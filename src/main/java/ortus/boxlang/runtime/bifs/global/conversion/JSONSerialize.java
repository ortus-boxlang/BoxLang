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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
@BoxMember( type = BoxLangType.CUSTOM, customType = java.lang.Boolean.class, name = "toJSON" )
@BoxMember( type = BoxLangType.CUSTOM2, customType = java.lang.Number.class, name = "toJSON" )
@BoxMember( type = BoxLangType.ARRAY, name = "toJSON" )
@BoxMember( type = BoxLangType.CLASS, name = "toJSON" )
@BoxMember( type = BoxLangType.QUERY, name = "toJSON" )
@BoxMember( type = BoxLangType.STRUCT, name = "toJSON" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listToJSON" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "toJSON" )
public class JSONSerialize extends BIF {

	/**
	 * Constructor
	 */
	public JSONSerialize() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.data ),
		    // NOT A BOOLEAN! Can be true, false, row, column, or struct
		    new Argument( false, Argument.STRING, Key.queryFormat, "row", Set.of(
		        Validator.REQUIRED,
		        Validator.valueOneOf( "true", "false", "row", "column", "struct" )
		    ) ),
		    // Don't set this to a boolean, Lucee accepts a charset here which ColdBox passes
		    new Argument( false, Argument.STRING, Key.useSecureJSONPrefix, false ),
		    new Argument( false, Argument.BOOLEAN, Key.useCustomSerializer ),
		    new Argument( false, Argument.BOOLEAN, Key.pretty, false )
		};
	}

	/**
	 * Converts a BoxLang variable into a JSON (JavaScript Object Notation) string according to the specified options.
	 *
	 * <h2>Query Format Options</h2>
	 * The <code>queryFormat</code> argument determines how queries are serialized:
	 * <ul>
	 * <li><code>row</code> or <code>false</code>: Serializes the query as a top-level struct with two keys:
	 * <code>columns</code> (an array of column names) and <code>data</code> (an array of arrays representing
	 * each row's data).</li>
	 * <li><code>column</code> or <code>true</code>: Serializes the query as a top-level struct with three keys:
	 * <code>rowCount</code> (the number of rows), <code>columns</code> (an array of column names), and
	 * <code>data</code> (a struct where each key is a column name and the value is an array of values for that column).</li>
	 * <li><code>struct</code>: Serializes the query as an array of structs, where each struct represents a row of data.</li>
	 * </ul>
	 *
	 * <h2>Usage</h2>
	 * 
	 * <pre>
	 * // Convert a query to JSON
	 * myQuery = ...;
	 * json = jsonSerialize( myQuery, queryFormat="row" );
	 * // Convert a list to JSON
	 * myList = "foo,bar,baz";
	 * jsonList = jsonSerialize( myList );
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.data The variable to convert to a JSON string.
	 *
	 * @argument.queryFormat If the variable is a query, specifies whether to serialize the query by rows or by columns. Valid values are:
	 *                       <code>row</code> same as <code>false</code>, <code>column</code> same as <code>true</code>, or <code>struct</code>. Defaults to <code>row</code>.
	 *
	 * @argument.useSecureJSONPrefix If true, the JSON string is prefixed with a secure JSON prefix. (Not implemented yet)
	 *
	 * @argument.useCustomSerializer If true, the JSON string is serialized using a custom serializer. (Not implemented yet)
	 *
	 * @argument.pretty If true, the JSON string is formatted with indentation and line breaks for readability. Defaults to false.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// TODO useSecureJSONPrefix - Don't assume this is a boolean, Lucee accepts a charset here which ColdBox passes

		Object	obj			= arguments.get( Key.data );
		String	queryFormat	= arguments.getAsString( Key.queryFormat ).toLowerCase();

		// Normalize Params
		if ( queryFormat.equals( "yes" ) ) {
			queryFormat = "true";
		}
		if ( queryFormat.equals( "no" ) ) {
			queryFormat = "false";
		}

		// Query serialization is manual, due to the different formats in the arguments
		if ( obj instanceof Query qry ) {
			// "row" is the same as "false". Top level struct with columns (array of strings), data (array of arrays)
			if ( queryFormat.equals( "row" ) || queryFormat.equals( "false" ) ) {
				obj = Struct.linkedOf(
				    "columns", qry.getColumns().keySet().stream().map( c -> c.getName() ).toArray( String[]::new ),
				    "data", qry.getData()
				);
				// "column" is the same as "true". Top level struct with rowcount, columns (array of strings), data (struct with column name as key and array of
				// values as value)
			} else if ( queryFormat.equals( "column" ) || queryFormat.equals( "true" ) ) {
				var						data	= new Struct( IStruct.TYPES.LINKED );
				Map<Key, QueryColumn>	cols	= qry.getColumns();
				for ( var col : cols.keySet() ) {
					data.put( col, cols.get( col ).getColumnData() );
				}
				obj = Struct.linkedOf(
				    "rowCount", qry.size(),
				    "columns", qry.getColumns().keySet().stream().map( c -> c.getName() ).toArray( String[]::new ),
				    "data", data
				);
				// "struct" is what we get by default (array of structs)
			} else if ( queryFormat.equals( "struct" ) ) {
				obj = qry;
			} else {
				throw new BoxRuntimeException( "Invalid queryFormat: " + queryFormat );
			}
		}

		// If we called "foo,bar".listToJSON(), then we need to convert the string to a list
		if ( arguments.get( BIF.__functionName ).equals( Key.listToJSON ) ) {
			obj = ListUtil.asList( StringCaster.cast( arguments.get( Key.data ) ), "," );
		}

		// Serialize the object to JSON
		try {
			return JSONUtil.getJSONBuilder( arguments.getAsBoolean( Key.pretty ) ).asString( obj );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error serializing to JSON", e );
		}
	}
}
