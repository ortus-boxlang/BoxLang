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
package ortus.boxlang.runtime.bifs.global.conversion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.JSONUtil;

@BoxBIF
public class JSONDeserialize extends BIF {

	/**
	 * Constructor
	 */
	public JSONDeserialize() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.json ),
		    new Argument( false, "boolean", Key.strictMapping, true ),
		    new Argument( false, "string", Key.useCustomSerializer )
		};
	}

	/**
	 * Converts a JSON (JavaScript Object Notation) string data representation into CFML data, such as a CFML structure or array.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.json The JSON string to convert to CFML data.
	 * 
	 * @argument.strictMapping A Boolean value that specifies whether to convert the JSON strictly. If true, everything becomes structures.
	 * 
	 * @argument.useCustomSerializer A string that specifies the name of a custom serializer to use. (Not used)
	 * 
	 * @return The CFML data representation of the JSON string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	json			= arguments.getAsString( Key.json );
		Boolean	strictMapping	= arguments.getAsBoolean( Key.strictMapping );
		Object	result			= JSONUtil.fromJSON( json );
		return mapToBLTypes( result, strictMapping );
	}

	/**
	 * Maps the JSON result to BoxLang types.
	 * 
	 * @param result The JSON result to map.
	 * 
	 * @return The result mapped to BoxLang types.
	 */
	@SuppressWarnings( "unchecked" )
	private Object mapToBLTypes( Object result, Boolean strictMapping ) {
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
