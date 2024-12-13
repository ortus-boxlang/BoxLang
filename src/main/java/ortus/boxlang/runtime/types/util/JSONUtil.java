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
package ortus.boxlang.runtime.types.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.extension.javatime.JacksonJrJavaTimeExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JacksonJrExtension;
import com.fasterxml.jackson.jr.ob.api.ExtensionContext;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.conversion.BoxJsonProvider;

/**
 * Utility class for JSON operations based on our library of choice.
 */
public class JSONUtil {

	/**
	 * The JSON builder library we use
	 */
	@SuppressWarnings( "deprecation" )
	private static final JSON JSON_BUILDER = JSON.builder(
	    // Use a custom factory with enabled parsing features
	    new JsonFactory()
	        .enable( JsonParser.Feature.ALLOW_COMMENTS )
	        .enable( JsonParser.Feature.ALLOW_YAML_COMMENTS )
	        // TODO: This whole block needs to be converted over to use the JsonFactory.builder() as the following feature is deprecated
	        .enable( JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER )
	)
	    // Enable JSON features
	    // https://fasterxml.github.io/jackson-jr/javadoc/jr-objects/2.8/com/fasterxml/jackson/jr/ob/JSON.Feature.html
	    .enable(
	        JSON.Feature.PRETTY_PRINT_OUTPUT,
	        JSON.Feature.USE_BIG_DECIMAL_FOR_FLOATS,
	        JSON.Feature.USE_FIELDS,
	        JSON.Feature.WRITE_NULL_PROPERTIES
	    )
	    // Add Jackson annotation support
	    .register( JacksonAnnotationExtension.std )
	    // Add JavaTime Extension
	    .register( new JacksonJrJavaTimeExtension() )
	    // Add Custom Serializers/ Deserializers
	    .register( new JacksonJrExtension() {

		    @Override
		    protected void register( ExtensionContext extensionContext ) {
			    extensionContext.insertProvider( new BoxJsonProvider() );
		    }

	    } )
	    // Yeaaaahaaa!
	    .build();

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the JSON builder library we use
	 *
	 * @see com.fasterxml.jackson.jr.ob.JSON
	 *
	 * @return The JSON builder
	 */
	public static JSON getJSONBuilder() {
		return JSON_BUILDER;
	}

	/**
	 * Read method that will take given JSON Source (of one of supported types),
	 * read contents and map it to one of simple mappings ({@link java.util.Map}
	 * for JSON Objects, {@link java.util.List} for JSON Arrays,
	 * {@link java.lang.String}
	 * for JSON Strings, null for JSON null, {@link java.lang.Boolean} for JSON
	 * booleans
	 * and {@link java.lang.Number} for JSON numbers.
	 *
	 * Supported source types include:
	 * <ul>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.File}</li>
	 * <li>{@link java.net.URL}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li><code>byte[]</code></li>
	 * <li><code>char[]</code></li>
	 * </ul>
	 *
	 * @param json The JSON to parse
	 *
	 * @return The parsed JSON in raw Java format
	 */
	public static Object fromJSON( Object json, boolean toBLTypes ) {
		try {
			Object parsed = JSON_BUILDER.anyFrom( json );
			// Now parse the JSON
			return toBLTypes ? mapToBLTypes( parsed, true ) : parsed;
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to parse JSON " + json.toString(), e );
		}
	}

	/**
	 * Read method that will take given JSON Source (of one of supported types),
	 * read contents and map it to one of simple mappings ({@link java.util.Map}
	 * for JSON Objects, {@link java.util.List} for JSON Arrays,
	 * {@link java.lang.String}
	 * for JSON Strings, null for JSON null, {@link java.lang.Boolean} for JSON
	 * booleans
	 * and {@link java.lang.Number} for JSON numbers.
	 *
	 * Supported source types include:
	 * <ul>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.File}</li>
	 * <li>{@link java.net.URL}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li><code>byte[]</code></li>
	 * <li><code>char[]</code></li>
	 * </ul>
	 *
	 * @param json The JSON to parse
	 *
	 * @return The parsed JSON in raw Java format
	 */
	public static Object fromJSON( Object json ) {
		return fromJSON( json, false );
	}

	/**
	 * Read method that will take given JSON Source (of one of supported types),
	 * read contents and map it to a Java Bean of given type.
	 *
	 * <a href=
	 * "https://github.com/FasterXML/jackson-jr#readingwriting-simple-objects-beans-listarrays-thereof">Read
	 * more here:</a>
	 *
	 * Supported source types include:
	 * <ul>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.File}</li>
	 * <li>{@link java.net.URL}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li><code>byte[]</code></li>
	 * <li><code>char[]</code></li>
	 * </ul>
	 *
	 * @param clazz The POJO Java Beans class to parse the JSON into
	 * @param json  The JSON to parse
	 *
	 * @return The parsed JSON into the given class passsed
	 */
	public static <T> T fromJSON( Class<T> clazz, Object json ) {
		try {
			return JSON_BUILDER.beanFrom( clazz, json );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to parse JSON into " + clazz.getSimpleName(), e );
		}
	}

	/**
	 * Maps a deserialized JSON result to BoxLang types.
	 *
	 * @param result        The JSON result to map.
	 * @param strictMapping A Boolean value that specifies whether to convert the JSON strictly.
	 *
	 * @return The result mapped to BoxLang types.
	 */
	@SuppressWarnings( "unchecked" )
	public static Object mapToBLTypes( Object result, Boolean strictMapping ) {

		if ( result == null ) {
			return null;
		}

		if ( result instanceof java.util.Map map ) {

			// Check to see if this struct is really a query
			// There will be a column, data and possibly a recordCount key
			if ( !strictMapping && ( map.size() == 2 || map.size() == 3 ) ) {
				var		castedmap	= ( Map<String, Object> ) map;
				String	columnKey	= null;
				String	dataKey		= null;
				String	rowCountKey	= null;

				// Do a case insensitive check for "columns" and "data" in this map
				for ( var col : castedmap.keySet() ) {
					if ( col.equalsIgnoreCase( "columns" ) ) {
						columnKey = col;
					}
					if ( col.equalsIgnoreCase( "data" ) ) {
						dataKey = col;
					}
					if ( col.equalsIgnoreCase( "rowCount" ) ) {
						rowCountKey = col;
					}
				}

				// If we found columns and data keys and columns is a list, let's proceed further
				if ( columnKey != null && dataKey != null && castedmap.get( columnKey ) instanceof java.util.List colList ) {
					// Setup query with columns in place (default type to object)
					Query		qry				= new Query();
					List<Key>	validColumns	= new ArrayList<>();
					colList.forEach( c -> {
						Key thisCol = Key.of( c.toString() );
						validColumns.add( thisCol );
						qry.addColumn( thisCol, QueryColumnType.OBJECT );
					} );

					// If data is a list, check for array of arrays of data
					Boolean golden = true;
					if ( map.get( dataKey ) instanceof java.util.List listData ) {
						for ( Object row : listData ) {
							if ( row instanceof java.util.List listRow && listRow.size() == colList.size() ) {
								qry.addRow( listRow.toArray() );
							} else {
								// give up on the query and continue as a normal map
								golden = false;
								break;
							}
						}
						if ( golden ) {
							return qry;
						}
						// If data is a map with as many keys as column and we found a rowCount key in the outer map.
					} else if ( map.get( dataKey ) instanceof java.util.Map tmp && tmp.keySet().size() == colList.size() && rowCountKey != null ) {
						CastAttempt<Integer>	rowCountAttempt	= IntegerCaster.attempt( map.get( rowCountKey ) );
						Map<Object, Object>		mapData			= ( Map<Object, Object> ) map.get( dataKey );
						if ( rowCountAttempt.wasSuccessful() ) {
							int rowCount = rowCountAttempt.get();
							for ( var col : mapData.entrySet() ) {
								Key thisColKey = Key.of( col.getKey().toString() );
								if ( validColumns.contains( thisColKey ) ) {
									if ( mapData.get( thisColKey.getName() ) instanceof List listData && listData.size() == rowCount ) {
										qry.addColumn( thisColKey, QueryColumnType.OBJECT, listData.toArray() );

									} else {
										// give up on the query and continue as a normal map
										golden = false;
										break;
									}
								} else {
									// give up on the query and continue as a normal map
									golden = false;
									break;
								}
							}
							if ( golden ) {
								return qry;
							}
						}

					}
				}
			}

			IStruct str = Struct.fromMap( IStruct.TYPES.LINKED, map );
			// Loop over struct keys and map values
			for ( Key key : str.keySet() ) {
				str.put( key, mapToBLTypes( str.get( key ), strictMapping ) );
			}
			return str;
		}

		if ( result instanceof java.util.List list ) {
			Array arr = Array.fromList( list );
			// loop over array and map values
			for ( int i = 0; i < arr.size(); i++ ) {
				arr.set( i, mapToBLTypes( arr.get( i ), strictMapping ) );
			}
			return arr;
		}

		return result;
	}

}
