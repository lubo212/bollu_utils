package com.bollu.goosefs.config;

import com.bollu.goosefs.common.exception.ExceptionMessage;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration property keys. This class provides a set of pre-defined property keys.
 */
@ThreadSafe
public final class PropertyKey implements Comparable<PropertyKey> {
  private static final Logger LOG = LoggerFactory.getLogger(PropertyKey.class);

  // The following two maps must be the first to initialize within this file.
  /**
   * A map from default property key's string name to the key.
   */
  private static final Map<String, PropertyKey> DEFAULT_KEYS_MAP = new ConcurrentHashMap<>();
  /**
   * A map from default property key's alias to the key.
   */
  private static final Map<String, PropertyKey> DEFAULT_ALIAS_MAP = new ConcurrentHashMap<>();
  /**
   * A cache storing result for template regexp matching results.
   */
  private static final Cache<String, Boolean> REGEXP_CACHE = CacheBuilder.newBuilder()
      .maximumSize(1024)
      .build();

  /**
   * Builder to create {@link PropertyKey} instances. Note that, <code>Builder.build()</code> will
   * throw exception if there is an existing property built with the same name.
   */
  public static final class Builder {
    private String[] mAlias;
    private DefaultSupplier mDefaultSupplier;
    private Object mDefaultValue;
    private String mDescription;
    private String mName;
    private boolean mIgnoredSiteProperty;
    private boolean mIsBuiltIn = true;
    private boolean mIsHidden;
    private ConsistencyCheckLevel mConsistencyCheckLevel = ConsistencyCheckLevel.IGNORE;
    private Scope mScope = Scope.ALL;
    private DisplayType mDisplayType = DisplayType.DEFAULT;

    /**
     * @param name name of the property
     */
    public Builder(String name) {
      mName = name;
    }

    /**
     * @param template template for the property name
     * @param params   parameters of the template
     */
    public Builder(PropertyKey.Template template, Object... params) {
      mName = String.format(template.mFormat, params);
    }

    /**
     * @param aliases aliases for the property
     * @return the updated builder instance
     */
    public Builder setAlias(String... aliases) {
      mAlias = Arrays.copyOf(aliases, aliases.length);
      return this;
    }

    /**
     * @param name name for the property
     * @return the updated builder instance
     */
    public Builder setName(String name) {
      mName = name;
      return this;
    }

    public Builder setDefaultSupplier(DefaultSupplier defaultSupplier) {
      mDefaultSupplier = defaultSupplier;
      return this;
    }

    public Builder setDefaultSupplier(Supplier<Object> supplier, String description) {
      mDefaultSupplier = new DefaultSupplier(supplier, description);
      return this;
    }

    /**
     * @param defaultValue the property's default value
     * @return the updated builder instance
     */
    public Builder setDefaultValue(Object defaultValue) {
      mDefaultValue = defaultValue;
      return this;
    }

    /**
     * @param description of the property
     * @return the updated builder instance
     */
    public Builder setDescription(String description) {
      mDescription = description;
      return this;
    }

    /**
     * @param isBuiltIn whether to the property is a built-in GooseFS property
     * @return the updated builder instance
     */
    public Builder setIsBuiltIn(boolean isBuiltIn) {
      mIsBuiltIn = isBuiltIn;
      return this;
    }

    /**
     * @param isHidden whether to hide the property when generating property documentation
     * @return the updated builder instance
     */
    public Builder setIsHidden(boolean isHidden) {
      mIsHidden = isHidden;
      return this;
    }

    /**
     * @param ignoredSiteProperty whether the property should be ignored in goosefs-site.properties
     * @return the updated builder instance
     */
    public Builder setIgnoredSiteProperty(boolean ignoredSiteProperty) {
      mIgnoredSiteProperty = ignoredSiteProperty;
      return this;
    }

    /**
     * @param consistencyCheckLevel the consistency level that applies to this property
     * @return the updated builder instance
     */
    public Builder setConsistencyCheckLevel(ConsistencyCheckLevel consistencyCheckLevel) {
      mConsistencyCheckLevel = consistencyCheckLevel;
      return this;
    }

    /**
     * @param scope which components this property applies to
     * @return the updated builder instance
     */
    public Builder setScope(Scope scope) {
      mScope = scope;
      return this;
    }

    /**
     * @param displayType the displayType that indicates how the property value should be displayed
     * @return the updated builder instance
     */
    public Builder setDisplayType(DisplayType displayType) {
      mDisplayType = displayType;
      return this;
    }

    /**
     * Creates and registers the property key.
     *
     * @return the created property key instance
     */
    public PropertyKey build() {
      PropertyKey key = buildUnregistered();
      Preconditions.checkState(PropertyKey.register(key), "Cannot register existing key \"%s\"",
          mName);
      return key;
    }

    private PropertyKey buildUnregistered() {
      DefaultSupplier defaultSupplier = mDefaultSupplier;
      if (defaultSupplier == null) {
        String defaultString = String.valueOf(mDefaultValue);
        defaultSupplier = (mDefaultValue == null)
            ? new DefaultSupplier(() -> null, "null")
            : new DefaultSupplier(() -> defaultString, defaultString);
      }

      PropertyKey key = new PropertyKey(mName, mDescription, defaultSupplier, mAlias,
          mIgnoredSiteProperty, mIsHidden, mConsistencyCheckLevel, mScope, mDisplayType,
          mIsBuiltIn);
      return key;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("alias", mAlias)
          .add("defaultValue", mDefaultValue)
          .add("description", mDescription)
          .add("name", mName).toString();
    }
  }

  public static final PropertyKey CONF_DIR =
      new Builder(Name.CONF_DIR)
          .setDefaultValue(String.format("${%s}/conf", "/bollu"))
          .setDescription("The directory containing files used to configure GooseFS.")
          .setIgnoredSiteProperty(true)
          .setConsistencyCheckLevel(ConsistencyCheckLevel.WARN)
          .setScope(Scope.ALL)
          .build();

  public static final PropertyKey SITE_CONF_DIR =
      new Builder(Name.SITE_CONF_DIR)
          .setDefaultSupplier(
              () -> String.format("${%s}/,%s/.goosefs/,/etc/goosefs/",
                  Name.CONF_DIR, System.getProperty("user.home")),
              String.format("${%s}/,${user.home}/.goosefs/,/etc/goosefs/", Name.CONF_DIR))
          .setDescription(
              String.format("Comma-separated search path for goosefs.properties."))
          .setIgnoredSiteProperty(true)
          .setConsistencyCheckLevel(ConsistencyCheckLevel.WARN)
          .setScope(Scope.ALL)
          .build();

  public static final PropertyKey CONF_VALIDATION_ENABLED =
      new Builder(Name.CONF_VALIDATION_ENABLED)
          .setDefaultValue(true)
          .setDescription("Whether to validate the configuration properties when initializing "
              + "GooseFS clients or server process.")
          .setIsHidden(true)
          .setConsistencyCheckLevel(ConsistencyCheckLevel.WARN)
          .setScope(Scope.ALL)
          .build();

  public static final PropertyKey NETWORK_IP_ADDRESS_USED =
      new Builder(Name.NETWORK_IP_ADDRESS_USED)
          .setDefaultValue("false")
          .setDescription("If true, when goosefs.<service_name>.hostname and "
              + "goosefs.<service_name>.bind.host of a service not specified, "
              + "use IP as the connect host of the service.")
          .setConsistencyCheckLevel(ConsistencyCheckLevel.WARN)
          .setScope(Scope.ALL)
          .build();
  public static final PropertyKey NETWORK_HOST_RESOLUTION_TIMEOUT_MS =
      new Builder(Name.NETWORK_HOST_RESOLUTION_TIMEOUT_MS)
          .setAlias("goosefs.network.host.resolution.timeout.ms")
          .setDefaultValue("5sec")
          .setDescription("During startup of the Master and Worker processes GooseFS needs to "
              + "ensure that they are listening on externally resolvable and reachable host "
              + "names. To do this, GooseFS will automatically attempt to select an "
              + "appropriate host name if one was not explicitly specified. This represents "
              + "the maximum amount of time spent waiting to determine if a candidate host "
              + "name is resolvable over the network.")
          .setConsistencyCheckLevel(ConsistencyCheckLevel.WARN)
          .setScope(Scope.ALL)
          .build();
  public static final PropertyKey USER_HOSTNAME = new Builder(Name.USER_HOSTNAME)
      .setDescription("The hostname to use for an GooseFS client.")
      .setConsistencyCheckLevel(ConsistencyCheckLevel.WARN)
      .setScope(Scope.CLIENT)
      .build();

  public static final PropertyKey MASTER_HOSTNAME =
      new Builder(Name.MASTER_HOSTNAME)
          .setDescription("The hostname of GooseFS master.")
          .setScope(Scope.ALL)
          .build();
  public static final PropertyKey MASTER_BIND_HOST =
      new Builder(Name.MASTER_BIND_HOST)
          .setDefaultValue("0.0.0.0")
          .setDescription("The hostname that GooseFS master binds to.")
          .setScope(Scope.MASTER)
          .build();
  public static final PropertyKey MASTER_RPC_PORT =
      new Builder(Name.MASTER_RPC_PORT)
          .setAlias("goosefs.master.port")
          .setDefaultValue(9200)
          .setDescription("The port for GooseFS master's RPC service.")
          .setConsistencyCheckLevel(ConsistencyCheckLevel.WARN)
          .setScope(Scope.ALL)
          .build();
  public static final PropertyKey WORKER_HOSTNAME = new Builder(Name.WORKER_HOSTNAME)
      .setDescription("The hostname of GooseFS worker.")
      .setScope(Scope.WORKER)
      .build();
  public static final PropertyKey ZOOKEEPER_ENABLED =
      new Builder(Name.ZOOKEEPER_ENABLED)
          .setDefaultValue(false)
          .setDescription("If true, setup master fault tolerant mode using ZooKeeper.")
          .setConsistencyCheckLevel(ConsistencyCheckLevel.ENFORCE)
          .setScope(Scope.ALL)
          .build();
  public static final PropertyKey MASTER_RPC_ADDRESSES =
      new Builder(Name.MASTER_RPC_ADDRESSES).setDescription(
              "A list of comma-separated host:port RPC addresses where the client should look for "
                  + "masters when using multiple masters without Zookeeper. This property is not "
                  + "used when Zookeeper is enabled, since Zookeeper already stores the master "
                  + "addresses.")
          .setScope(Scope.ALL)
          .build();
  public static final PropertyKey MASTER_EMBEDDED_JOURNAL_PORT =
      new Builder(Name.MASTER_EMBEDDED_JOURNAL_PORT)
          .setDescription("The port to use for embedded journal communication with other masters.")
          .setDefaultValue(9202)
          .setScope(Scope.ALL)
          .build();

  public static final PropertyKey TEST_DEPRECATED_KEY =
      new Builder("goosefs.test.deprecated.key")
          .build();

  @ThreadSafe
  public static final class Name {
    public static final String CONF_DIR = "goosefs.conf.dir";
    public static final String SITE_CONF_DIR = "goosefs.site.conf.dir";
    public static final String CONF_VALIDATION_ENABLED = "goosefs.conf.validation.enabled";
    public static final String NETWORK_IP_ADDRESS_USED = "goosefs.network.ip.address.used";
    public static final String NETWORK_HOST_RESOLUTION_TIMEOUT_MS = "goosefs.network.host.resolution.timeout";
    public static final String USER_HOSTNAME = "goosefs.user.hostname";
    public static final String MASTER_HOSTNAME = "goosefs.master.hostname";
    public static final String WORKER_HOSTNAME = "goosefs.worker.hostname";
    public static final String ZOOKEEPER_ENABLED = "goosefs.zookeeper.enabled";
    public static final String MASTER_RPC_ADDRESSES = "goosefs.master.rpc.addresses";
    public static final String MASTER_BIND_HOST = "goosefs.master.bind.host";
    public static final String MASTER_RPC_PORT = "goosefs.master.rpc.port";
    public static final String MASTER_EMBEDDED_JOURNAL_PORT = "goosefs.master.embedded.journal.port";

    private Name() {
    } // prevent instantiation
  }

  @ThreadSafe
  public enum Template {
    LOCALITY_TIER("goosefs.locality.%s", "goosefs\\.locality\\.(\\w+)"),
    MASTER_IMPERSONATION_GROUPS_OPTION("goosefs.master.security.impersonation.%s.groups",
        "goosefs\\.master\\.security\\.impersonation\\.([a-zA-Z_0-9-\\.@]+)\\.groups"),
    MASTER_MOUNT_TABLE_ROOT_OPTION_PROPERTY("goosefs.master.mount.table.root.option.%s",
        "goosefs\\.master\\.mount\\.table\\.root\\.option\\.(?<nested>(\\w+\\.)*+\\w+)",
        PropertyCreators.NESTED_UFS_PROPERTY_CREATOR);

    // puts property creators in a nested class to avoid NPE in enum static initialization
    private static class PropertyCreators {
      private static final BiFunction<String, PropertyKey, PropertyKey> DEFAULT_PROPERTY_CREATOR =
          fromBuilder(new Builder(""));
      private static final BiFunction<String, PropertyKey, PropertyKey>
          NESTED_UFS_PROPERTY_CREATOR =
          createNestedPropertyCreator(Scope.SERVER, ConsistencyCheckLevel.ENFORCE);

      private static BiFunction<String, PropertyKey, PropertyKey> fromBuilder(Builder builder) {
        return (name, baseProperty) -> builder.setName(name).buildUnregistered();
      }

      private static BiFunction<String, PropertyKey, PropertyKey> createNestedPropertyCreator(
          Scope scope, ConsistencyCheckLevel consistencyCheckLevel) {
        return (name, baseProperty) -> {
          Builder builder = new Builder(name)
              .setScope(scope)
              .setConsistencyCheckLevel(consistencyCheckLevel);
          if (baseProperty != null) {
            builder.setDisplayType(baseProperty.getDisplayType());
          }
          return builder.buildUnregistered();
        };
      }
    }

    private static final String NESTED_GROUP = "nested";
    private final String mFormat;
    private final Pattern mPattern;
    private BiFunction<String, PropertyKey, PropertyKey> mPropertyCreator =
        PropertyCreators.DEFAULT_PROPERTY_CREATOR;

    /**
     * Constructs a property key format.
     *
     * @param format String of this property as formatted string
     * @param re     String of this property as regexp
     */
    Template(String format, String re) {
      mFormat = format;
      mPattern = Pattern.compile(re);
    }

    /**
     * Constructs a nested property key format with a function to construct property key given
     * base property key.
     *
     * @param format          String of this property as formatted string
     * @param re              String of this property as regexp
     * @param propertyCreator a function that creates property key given name and base property key
     *                        (for nested properties only, will be null otherwise)
     */
    Template(String format, String re,
             BiFunction<String, PropertyKey, PropertyKey> propertyCreator) {
      this(format, re);
      mPropertyCreator = propertyCreator;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("format", mFormat).add("pattern", mPattern)
          .toString();
    }

    /**
     * @param params ordinal
     * @return corresponding property
     */
    public PropertyKey format(Object... params) {
      return new PropertyKey(String.format(mFormat, params));
    }

    /**
     * @param input the input property key string
     * @return whether the input string matches this template
     */
    public boolean matches(String input) {
      Matcher matcher = mPattern.matcher(input);
      return matcher.matches();
    }

    /**
     * @param input the input property key string
     * @return the matcher matching the template to the string
     */
    public Matcher match(String input) {
      return mPattern.matcher(input);
    }

    /**
     * Gets the property key if the property name matches the template.
     *
     * @param propertyName name of the property
     * @return the property key, or null if the property name does not match the template
     */
    @Nullable
    private PropertyKey getPropertyKey(String propertyName) {
      Matcher matcher = match(propertyName);
      if (!matcher.matches()) {
        return null;
      }
      // if the template can extract a nested property, build the new property from the nested one
      String nestedKeyName = null;
      try {
        nestedKeyName = matcher.group(NESTED_GROUP);
      } catch (IllegalArgumentException e) {
        // ignore if group is not found
      }
      PropertyKey nestedProperty = null;
      if (nestedKeyName != null && isValid(nestedKeyName)) {
        nestedProperty = fromString(nestedKeyName);
      }
      return mPropertyCreator.apply(propertyName, nestedProperty);
    }
  }

  /**
   * @param input string of property key
   * @return whether the input is a valid property name
   */
  public static boolean isValid(String input) {
    // Check if input matches any default keys or aliases
    if (DEFAULT_KEYS_MAP.containsKey(input) || DEFAULT_ALIAS_MAP.containsKey(input)) {
      return true;
    }
    // Regex matching for templates can be expensive when checking properties frequently.
    // Use a cache to store regexp matching results to reduce CPU overhead.
    Boolean result = REGEXP_CACHE.getIfPresent(input);
    if (result != null) {
      return result;
    }
    // Check if input matches any parameterized keys
    result = false;
    for (Template template : Template.values()) {
      if (template.matches(input)) {
        result = true;
        break;
      }
    }
    REGEXP_CACHE.put(input, result);
    return result;
  }

  /**
   * Parses a string and return its corresponding {@link PropertyKey}, throwing exception if no such
   * a property can be found.
   *
   * @param input string of property key
   * @return corresponding property
   */
  public static PropertyKey fromString(String input) {
    // First try to parse it as default key
    PropertyKey key = DEFAULT_KEYS_MAP.get(input);
    if (key != null) {
      return key;
    }
    // Try to match input with alias
    key = DEFAULT_ALIAS_MAP.get(input);
    if (key != null) {
      return key;
    }
    // Try different templates and see if any template matches
    for (Template template : Template.values()) {
      key = template.getPropertyKey(input);
      if (key != null) {
        return key;
      }
    }

    throw new IllegalArgumentException(
        ExceptionMessage.INVALID_CONFIGURATION_KEY.getMessage(input));
  }

  public static PropertyKey getOrBuildCustom(String name) {
    return DEFAULT_KEYS_MAP.computeIfAbsent(name,
        (key) -> {
          final Builder propertyKeyBuilder = new Builder(key).setIsBuiltIn(false);
          return propertyKeyBuilder.buildUnregistered();
        });
  }

  @Nullable
  public String getDefaultValue() {
    Object defaultValue = mDefaultSupplier.get();
    return defaultValue == null ? null : defaultValue.toString();
  }

  public DefaultSupplier getDefaultSupplier() {
    return mDefaultSupplier;
  }

  /**
   * @return all pre-defined property keys
   */
  public static Collection<? extends PropertyKey> defaultKeys() {
    return Sets.newHashSet(DEFAULT_KEYS_MAP.values());
  }

  /**
   * Property name.
   */
  private final String mName;

  /**
   * Property Key description.
   */
  private final String mDescription;

  private final DefaultSupplier mDefaultSupplier;

  /**
   * Property Key alias.
   */
  private final String[] mAliases;

  /**
   * Whether to ignore as a site property.
   */
  private final boolean mIgnoredSiteProperty;

  /**
   * Whether the property is an GooseFS built-in property.
   */
  private final boolean mIsBuiltIn;

  /**
   * Whether to hide in document.
   */
  private final boolean mIsHidden;

  /**
   * Whether property should be consistent within the cluster.
   */
  private final ConsistencyCheckLevel mConsistencyCheckLevel;

  /**
   * The scope this property applies to.
   */
  private final Scope mScope;

  /**
   * The displayType which indicates how the property value should be displayed.
   **/
  private final DisplayType mDisplayType;

  /**
   * @param name                  String of this property
   * @param description           String description of this property key
   * @param aliases               alias of this property key
   * @param ignoredSiteProperty   true if GooseFS ignores user-specified value for this property in
   *                              site properties file
   * @param isHidden              whether to hide in document
   * @param consistencyCheckLevel the consistency check level to apply to this property
   * @param scope                 the scope this property applies to
   * @param displayType           how the property value should be displayed
   * @param isBuiltIn             whether this is an GooseFS built-in property
   */
  private PropertyKey(String name, String description, DefaultSupplier defaultSupplier,
                      String[] aliases, boolean ignoredSiteProperty, boolean isHidden,
                      ConsistencyCheckLevel consistencyCheckLevel, Scope scope, DisplayType displayType,
                      boolean isBuiltIn) {
    mName = Preconditions.checkNotNull(name, "name");
    // TODO(binfan): null check after we add description for each property key
    mDescription = Strings.isNullOrEmpty(description) ? "N/A" : description;
    mDefaultSupplier = defaultSupplier;
    mAliases = aliases;
    mIgnoredSiteProperty = ignoredSiteProperty;
    mIsHidden = isHidden;
    mConsistencyCheckLevel = consistencyCheckLevel;
    mScope = scope;
    mDisplayType = displayType;
    mIsBuiltIn = isBuiltIn;
  }

  /**
   * @param name String of this property
   */
  private PropertyKey(String name) {
    this(name, null, new DefaultSupplier(() -> null, "null"), null, false, false,
        ConsistencyCheckLevel.IGNORE, Scope.ALL, DisplayType.DEFAULT, true);
  }

  /**
   * Registers the given key to the global key map.
   *
   * @param key th property
   * @return whether the property key is successfully registered
   */
  @VisibleForTesting
  public static boolean register(PropertyKey key) {
    String name = key.getName();
    String[] aliases = key.getAliases();
    if (DEFAULT_KEYS_MAP.containsKey(name)) {
      if (DEFAULT_KEYS_MAP.get(name).isBuiltIn() || !key.isBuiltIn()) {
        return false;
      }
    }

    DEFAULT_KEYS_MAP.put(name, key);
    if (aliases != null) {
      for (String alias : aliases) {
        DEFAULT_ALIAS_MAP.put(alias, key);
      }
    }
    return true;
  }

  /**
   * Unregisters the given key from the global key map.
   *
   * @param key the property to unregister
   */
  @VisibleForTesting
  public static void unregister(PropertyKey key) {
    String name = key.getName();
    DEFAULT_KEYS_MAP.remove(name);
    DEFAULT_ALIAS_MAP.remove(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PropertyKey)) {
      return false;
    }
    PropertyKey that = (PropertyKey) o;
    return Objects.equal(mName, that.mName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mName);
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public int compareTo(PropertyKey o) {
    return mName.compareTo(o.mName);
  }

  /**
   * @return length of this property key
   */
  public int length() {
    return mName.length();
  }

  /**
   * @param key the name of input key
   * @return if this key is nested inside the given key
   */
  public boolean isNested(String key) {
    return key.length() > length() + 1 && key.startsWith(mName) && key.charAt(length()) == '.';
  }

  /**
   * @return the name of the property
   */
  public String getName() {
    return mName;
  }

  /**
   * @return the alias of a property
   */
  public String[] getAliases() {
    return mAliases;
  }

  /**
   * @return the description of a property
   */
  public String getDescription() {
    return mDescription;
  }


  /**
   * @return true if this property should be ignored as a site property
   */
  public boolean isIgnoredSiteProperty() {
    return mIgnoredSiteProperty;
  }

  /**
   * @return true if this property is built-in
   */
  public boolean isBuiltIn() {
    return mIsBuiltIn;
  }

  /**
   * @return true if this property should not show up in the document
   */
  public boolean isHidden() {
    return mIsHidden;
  }

  /**
   * @return the consistency check level to apply to this property
   */
  public ConsistencyCheckLevel getConsistencyLevel() {
    return mConsistencyCheckLevel;
  }

  /**
   * @return the scope which this property applies to
   */
  public Scope getScope() {
    return mScope;
  }

  /**
   * @return the displayType which indicates how the property value should be displayed
   */
  public DisplayType getDisplayType() {
    return mDisplayType;
  }
}




