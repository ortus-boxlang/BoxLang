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
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.*;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.jdbc.ExecutedQuery;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.*;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;

import javax.annotation.Nullable;
import javax.swing.*;

@BoxBIF
public class QueryExecute extends BIF {

	/**
	 * Constructor
	 */
	public QueryExecute() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "String", Key.sql ),
		    new Argument( false, "any", Key.params, new Array() ),
		    new Argument( false, "struct", Key.options )
		};
	}

	/**
	 * Executes a query and returns the results.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.sql The SQL to execute
	 *
	 * @argument.params An array of binding parameters or a struct of named binding parameters
	 *
	 * @argument.options A struct of queryExecute options
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		DataSourceManager		manager			= DataSourceManager.getInstance();

		CastAttempt<IStruct>	optionsAsStruct	= StructCaster.attempt( arguments.get( Key.options ) );
		IStruct					options			= optionsAsStruct.getOrDefault( new Struct() );

		DataSource				datasource;
		if ( options.containsKey( "datasource" ) ) {
			String datasourceName = options.getAsString( Key.datasource );
			datasource = manager.getDatasource( Key.of( datasourceName ) );
			if ( datasource == null ) {
				throw new BoxRuntimeException( "No [" + datasourceName + "] datasource defined." );
			}
		} else {
			datasource = manager.getDefaultDatasource();
			if ( datasource == null ) {
				throw new BoxRuntimeException(
				    "No default datasource has been defined. Either register a default datasource or provide a datasource name in the query options." );
			}
		}

		String			sql			= arguments.getAsString( Key.sql );

		ExecutedQuery	query		= null;

		Object			bindings	= arguments.get( Key.params );
		if ( bindings == null ) {
			query = datasource.execute( sql, options );
		}

		if ( query == null ) {
			CastAttempt<Array> castAsArray = ArrayCaster.attempt( bindings );
			if ( castAsArray.wasSuccessful() ) {
				query = datasource.execute( sql, castAsArray.getOrFail(), options );
			}
		}

		if ( query == null ) {
			CastAttempt<IStruct> castAsStruct = StructCaster.attempt( bindings );
			if ( castAsStruct.wasSuccessful() ) {
				query = datasource.execute( sql, castAsStruct.getOrFail(), options );
			}
		}

		if ( query == null ) {
			String className = "null";
			if ( bindings != null ) {
				className = bindings.getClass().getName();
			}
			throw new BoxRuntimeException( "Invalid type for params. Expected array or struct. Received: " + className );
		}

		Object				returnTypeObject	= options.getOrDefault( Key.returnType, "query" );
		CastAttempt<String>	returnTypeAsString	= StringCaster.attempt( returnTypeObject );
		String				returnType			= returnTypeAsString.getOrDefault( "query" );

		String				resultsVariableName	= options.getAsString( Key.result );
		if ( resultsVariableName != null ) {
			ExpressionInterpreter.setVariable( context, resultsVariableName, query.getResultStruct() );
		}

        switch (returnType) {
            case "query" -> {
                return query.getResults();
            }
            case "array" -> {
                return query.getResultsAsArray();
            }
            case "struct" -> {
                String columnKey = options.getAsString(Key.columnKey);
                if (columnKey == null) {
                    throw new BoxRuntimeException("You must defined a `columnKey` option when using `returnType: struct`.");
                }
                return query.getResultsAsStruct(columnKey);
            }
            default -> throw new BoxRuntimeException("Unknown return type: " + returnType);
        }
	}

}
