/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.query;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
public class QueryNew extends BIF {

	/**
	 * Constructor
	 */
	public QueryNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "String", Key.columnList ),
		    new Argument( false, "string", Key.columnTypeList, "" ),
		    new Argument( false, "any", Key.rowData )
		};
	}

	/**
	 * Return new query
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.columnList The column list to be used in the query. Delimited list of column names, or an empty string.
	 *
	 * @argument.columnTypeList Comma-delimited list specifying column data types.
	 *
	 * @argument.rowData Data to populate the query. Can be a struct (with keys matching column names), an array of structs, or an array of arrays (in
	 *                   same order as columnList)
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	columnNames	= ListUtil.asList( arguments.getAsString( Key.columnList ), "," );
		Array	columnTypes	= ListUtil.asList( arguments.getAsString( Key.columnTypeList ), "," );
		Object	rowData		= arguments.get( Key.rowData );

		if ( columnNames.size() != columnTypes.size() ) {
			throw new BoxRuntimeException( "columnList and columnTypeList must have the same number of elements" );
		}

		return new Query( columnNames, columnTypes, rowData );
	}

}
