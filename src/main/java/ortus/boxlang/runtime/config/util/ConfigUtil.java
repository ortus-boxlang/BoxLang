/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.config.util;

import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * A utility for getting configuration from structs in an orderly manner that reduces boilerplate and allows for easy
 * validation and defaulting.
 */
public class ConfigUtil {

	private ConfigUtil() {
		// Static Utility Class
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation
	 * 
	 * @param clazz             The expected Java return type
	 * @param castType          The BoxLang type to cast to
	 * @param key               The key to get
	 * @param config            The struct to get the config from
	 * @param required          Whether this config item is required
	 * @param defaultExpression The default expression to evaluate if the config item is not present
	 * @param description       A description of the config item, used in error messages
	 * @param context           The BoxLang context
	 * 
	 * @return The configuration value cast to the expected type, or as Object if clazz is null
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T getAs(
	    Class<T> clazz,
	    String castType,
	    Key key,
	    IStruct config,
	    boolean required,
	    DefaultExpression defaultExpression,
	    String description,
	    IBoxContext context ) {

		Object value = config.get( key );

		// If key doesn't exist, or is null, handle defaulting / required
		if ( value == null ) {
			if ( defaultExpression != null ) {
				value = defaultExpression.evaluate( context );
			} else if ( required ) {
				throw new BoxValidationException( buildDescription( key, description ) + " is required." );
			}
		}

		// Don't try to cast nulls
		if ( value != null ) {
			// Pass through generic caster
			try {
				value = GenericCaster.cast( context, value, castType );
			} catch ( BoxCastException bce ) {
				// Handle cast failure ourselves so we can control the error message
				throw new BoxValidationException( String.format( "Could not cast " + buildDescription( key, description ) + " of type [%s] to type [%s]",
				    TypeUtil.getObjectName( value ), castType ), bce );
			}
		}

		// Type erasure will make this the same as casting to Object
		if ( clazz == null ) {
			return ( T ) value;
		}

		// Cast to expected type
		return clazz.cast( DynamicObject.unWrap( value ) );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation wrapped in an Attempt
	 * 
	 * @param clazz             The expected Java return type
	 * @param castType          The BoxLang type to cast to
	 * @param key               The key to get
	 * @param config            The struct to get the config from
	 * @param required          Whether this config item is required
	 * @param defaultExpression The default expression to evaluate if the config item is not present
	 * @param description       A description of the config item, used in error messages
	 * @param context           The BoxLang context
	 * 
	 * @return The configuration value cast to the expected type, or as Object if clazz is null wrapped in an Attempt
	 */
	public static <T> Attempt<T> getAsAttempt(
	    Class<T> clazz,
	    String castType,
	    Key key,
	    IStruct config,
	    boolean required,
	    DefaultExpression defaultExpression,
	    String description,
	    IBoxContext context ) {
		return Attempt.of( getAs( clazz, castType, key, config, required, defaultExpression, description, context ) );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation
	 * A string BL cast type is provided and no specific java return type is expected (Object)
	 * 
	 * @param castType The BoxLang type to cast to
	 * @param key      The key to get
	 * @param config   The struct to get the config from
	 * @param context  The BoxLang context
	 * 
	 * @return The configuration value cast to the expected type, or as Object if clazz is null
	 */
	public static Object getAs( String castType, Key key, IStruct config, IBoxContext context ) {
		return getAs( null, castType, key, config, false, null, null, context );
	}

	/**
	 * Get one-off configuration value from a struct with casting wrapped in an Attempt
	 * A string BL cast type is provided and no specific java return type is expected (Object)
	 * 
	 * @param castType The BoxLang type to cast to
	 * @param key      The key to get
	 * @param config   The struct to get the config from
	 * @param context  The BoxLang context
	 * 
	 * @return The configuration value wrapped in an Attempt
	 */
	public static Attempt<Object> getAsAttempt( String castType, Key key, IStruct config, IBoxContext context ) {
		return Attempt.of( getAs( castType, key, config, context ) );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation
	 * A string BL cast type is provided and no specific java return type is expected (Object)
	 * 
	 * @param castType          The BoxLang type to cast to
	 * @param key               The key to get
	 * @param config            The struct to get the config from
	 * @param required          Whether this config item is required
	 * @param defaultExpression The default expression to evaluate if the config item is not present
	 * @param description       A description of the config item, used in error messages
	 * @param context           The BoxLang context
	 * 
	 * @return The configuration value cast to the expected type, or as Object if clazz is null
	 */
	public static Object getAs( String castType, Key key, IStruct config, boolean required, DefaultExpression defaultExpression, String description,
	    IBoxContext context ) {
		return getAs( null, castType, key, config, required, defaultExpression, description, context );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation wrapped in an Attempt
	 * A string BL cast type is provided and no specific java return type is expected (Object)
	 * 
	 * @param castType          The BoxLang type to cast to
	 * @param key               The key to get
	 * @param config            The struct to get the config from
	 * @param required          Whether this config item is required
	 * @param defaultExpression The default expression to evaluate if the config item is not present
	 * @param description       A description of the config item, used in error messages
	 * @param context           The BoxLang context
	 * 
	 * @return The configuration value wrapped in an Attempt
	 */
	public static Attempt<Object> getAsAttempt( String castType, Key key, IStruct config, boolean required, DefaultExpression defaultExpression,
	    String description,
	    IBoxContext context ) {
		return Attempt.of( getAs( castType, key, config, required, defaultExpression, description, context ) );
	}

	// Ommiting all the other combination of overlaoded methods starting with a string cast type for simplicity and not having 8,000 overloaded methods

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation
	 * A Java return type is provided and the BoxLang cast type is guessed from it
	 * 
	 * @param clazz   The expected Java return type
	 * @param key     The key to get
	 * @param config  The struct to get the config from
	 * @param context The BoxLang context
	 * 
	 * @return The configuration value cast to the expected type, or as Object if clazz is null
	 */
	public static <T> T getAs( Class<T> clazz, Key key, IStruct config, IBoxContext context ) {
		return getAs( clazz, guessCastType( clazz ), key, config, false, null, null, context );
	}

	/**
	 * Get one-off configuration value from a struct with casting wrapped in an Attempt
	 * A Java return type is provided and the BoxLang cast type is guessed from it
	 * 
	 * @param clazz   The expected Java return type
	 * @param key     The key to get
	 * @param config  The struct to get the config from
	 * @param context The BoxLang context
	 * 
	 * @return The configuration value wrapped in an Attempt
	 */
	public static <T> Attempt<T> getAsAttempt( Class<T> clazz, Key key, IStruct config, IBoxContext context ) {
		return Attempt.of( getAs( clazz, key, config, context ) );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation
	 * A Java return type is provided and the BoxLang cast type is guessed from it
	 * 
	 * @param clazz             The expected Java return type
	 * @param key               The key to get
	 * @param config            The struct to get the config from
	 * @param required          Whether this config item is required
	 * @param defaultExpression The default expression to evaluate if the config item is not present
	 * @param description       A description of the config item, used in error messages
	 * @param context           The BoxLang context
	 * 
	 * @return The configuration value cast to the expected type, or as Object if clazz is null
	 */
	public static <T> T getAs( Class<T> clazz, Key key, IStruct config, boolean required, DefaultExpression defaultExpression, String description,
	    IBoxContext context ) {
		return getAs( clazz, guessCastType( clazz ), key, config, required, defaultExpression, description, context );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation wrapped in an Attempt
	 * A Java return type is provided and the BoxLang cast type is guessed from it
	 * 
	 * @param clazz             The expected Java return type
	 * @param key               The key to get
	 * @param config            The struct to get the config from
	 * @param required          Whether this config item is required
	 * @param defaultExpression The default expression to evaluate if the config item is not present
	 * @param description       A description of the config item, used in error messages
	 * @param context           The BoxLang context
	 * 
	 * @return The configuration value wrapped in an Attempt
	 */
	public static <T> Attempt<T> getAsAttempt( Class<T> clazz, Key key, IStruct config, boolean required, DefaultExpression defaultExpression,
	    String description,
	    IBoxContext context ) {
		return Attempt.of( getAs( clazz, key, config, required, defaultExpression, description, context ) );
	}

	// Ommiting all the other combination of overlaoded methods starting with a Java class return type for simplicity and not having 8,000 overloaded methods

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation
	 * 
	 * @param clazz      The expected Java return type
	 * @param configItem The config item to get
	 * @param config     The struct to get the config from
	 * @param context    The BoxLang context
	 * 
	 * @return The configuration value cast to the expected type, or as Object if clazz is null
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T getAs( Class<T> clazz, ConfigItem configItem, IStruct config, IBoxContext context ) {
		Object value = config.get( configItem.name() );
		// If key doesn't exist, or is null, handle defaulting / required
		if ( value == null ) {
			if ( configItem.hasDefaultValue() ) {
				value = configItem.getDefaultValue( context );
				config.put( configItem.name(), value );
			} else if ( configItem.required() ) {
				throw new BoxValidationException( configItem.buildDescription() + " is required." );
			}
		}

		// Pass through generic caster
		if ( configItem.type() != null && value != null ) {
			try {
				value = GenericCaster.cast( context, value, configItem.type() );
				config.put( configItem.name(), value );
			} catch ( BoxCastException bce ) {
				// Handle cast failure ourselves so we can control the error message
				throw new BoxValidationException( String.format( "Could not cast " + configItem.buildDescription() + " of type [%s] to type [%s]",
				    TypeUtil.getObjectName( value ), configItem.type() ), bce );
			}

		}

		// Apply validators
		configItem.validate( context, configItem.description() == null ? null : Key.of( configItem.description() ), config );
		value = config.get( configItem.name() );

		// Type erasure will make this the same as casting to Object
		if ( clazz == null ) {
			return ( T ) value;
		}

		// Cast to expected type
		return clazz.cast( DynamicObject.unWrap( value ) );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation wrapped in an Attempt
	 * 
	 * @param clazz      The expected Java return type
	 * @param configItem The config item to get
	 * @param config     The struct to get the config from
	 * @param context    The BoxLang context
	 * 
	 * @return The configuration value wrapped in an Attempt
	 */
	public static <T> Attempt<T> getAsAttempt( Class<T> clazz, ConfigItem configItem, IStruct config, IBoxContext context ) {
		return Attempt.of( getAs( clazz, configItem, config, context ) );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation
	 * 
	 * @param configItem The config item to get
	 * @param config     The struct to get the config from
	 * @param context    The BoxLang context
	 * 
	 * @return The configuration value
	 */
	public static Object getAs( ConfigItem configItem, IStruct config, IBoxContext context ) {
		return getAs( null, configItem, config, context );
	}

	/**
	 * Get one-off configuration value from a struct with casting, defaulting, and required validation wrapped in an Attempt
	 * 
	 * @param configItem The config item to get
	 * @param config     The struct to get the config from
	 * @param context    The BoxLang context
	 * 
	 * @return The configuration value wrapped in an Attempt
	 */
	public static Attempt<Object> getAsAttempt( ConfigItem configItem, IStruct config, IBoxContext context ) {
		return Attempt.of( getAs( configItem, config, context ) );
	}

	/**
	 * Get a config set from a struct based on a set of config items
	 * 
	 * @param configItems The set of config items to retrieve
	 * @param config      The struct containing the configuration
	 * @param context     The BoxLang context
	 * 
	 * @return A struct containing the retrieved configuration values
	 */
	public static IStruct getConfigSet( Set<ConfigItem> configItems, IStruct config, IBoxContext context ) {
		for ( ConfigItem configItem : configItems ) {
			config.put( configItem.name(), ConfigUtil.getAs( configItem, config, context ) );
		}

		return config;
	}

	/**
	 * Build description for error messages
	 * 
	 * @param key         The config key
	 * @param description The description
	 * 
	 * @return The built description
	 */
	private static String buildDescription( Key key, String description ) {
		if ( description != null && !description.isBlank() ) {
			return String.format( "Configuration item [%s] in %s", key.toString(), description );
		} else {
			return String.format( "Configuration item [%s]", key.toString() );
		}
	}

	/**
	 * Guess the BoxLang cast type based on a Java class
	 * 
	 * @param clazz The Java class to guess the cast type for
	 * 
	 * @return The BoxLang cast type string
	 */
	private static String guessCastType( Class<?> clazz ) {
		return switch ( clazz ) {
			case null -> "any";

			// String
			case Class<?> c when c == String.class -> "string";

			// Boolean
			case Class<?> c when c == Boolean.class || c == boolean.class -> "boolean";

			// Numeric types
			case Class<?> c when c == Double.class || c == double.class -> "double";
			case Class<?> c when c == Float.class || c == float.class -> "float";
			case Class<?> c when c == Integer.class || c == int.class -> "integer";
			case Class<?> c when c == Long.class || c == long.class -> "long";
			case Class<?> c when c == Short.class || c == short.class -> "short";
			case Class<?> c when c == Byte.class || c == byte.class -> "byte";
			case Class<?> c when c == Character.class || c == char.class -> "char";
			case Class<?> c when c == Number.class -> "numeric";
			case Class<?> c when c == java.math.BigDecimal.class -> "bigdecimal";
			case Class<?> c when c == java.math.BigInteger.class -> "biginteger";

			// Date/Time
			case Class<?> c when c == ortus.boxlang.runtime.types.DateTime.class -> "datetime";
			case Class<?> c when c == java.time.LocalDate.class || c == java.time.LocalDateTime.class || c == java.time.ZonedDateTime.class
			    || c == java.util.Date.class || c == java.sql.Date.class || c == java.sql.Timestamp.class -> "datetime";
			case Class<?> c when c == java.time.LocalTime.class || c == java.sql.Time.class -> "time";

			// Collections
			case Class<?> c when c == ortus.boxlang.runtime.types.Array.class -> "array";
			case Class<?> c when c == ortus.boxlang.runtime.types.Struct.class || c == ortus.boxlang.runtime.types.IStruct.class -> "struct";
			case Class<?> c when c == ortus.boxlang.runtime.types.Query.class -> "query";

			// XML
			case Class<?> c when c == ortus.boxlang.runtime.types.XML.class -> "xml";

			// Functions
			case Class<?> c when c == ortus.boxlang.runtime.types.Function.class || c == ortus.boxlang.runtime.types.UDF.class
			    || c == ortus.boxlang.runtime.types.Closure.class || c == ortus.boxlang.runtime.types.Lambda.class -> "function";

			// Key
			case Class<?> c when c == ortus.boxlang.runtime.scopes.Key.class -> "key";

			// UUID
			case Class<?> c when c == java.util.UUID.class -> "uuid";

			// Binary
			case Class<?> c when c == byte[].class -> "binary";

			// Throwable
			case Class<?> c when Throwable.class.isAssignableFrom( c ) -> "throwable";

			// Stream
			case Class<?> c when c == java.util.stream.Stream.class -> "stream";

			// Default to the name of the class, which will basically do Java type checking
			default -> clazz.getName();
		};
	}

}
