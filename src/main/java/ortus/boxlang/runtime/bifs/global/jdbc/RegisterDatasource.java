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
package ortus.boxlang.runtime.bifs.global.jdbc;

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class RegisterDatasource extends BIF {

	/**
	 * Constructor
	 */
	public RegisterDatasource() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._name, Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Argument( false, Argument.STRUCT, Key.config, Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Argument( false, Argument.STRUCT, Key.options, new Struct() )
		};
	}

	/**
	 * Register a new JDBC datasource into the Boxlang runtime.
	 * 
	 * Also clears the context config cache for immediate usage within the same request.
	 * 
	 * @argument.name The name of the datasource to register. This must be unique, i.e. cannot already exist within the current context's datasource configuration list.
	 * 
	 * @argument.config The configuration properties for the datasource. See {@link DatasourceConfig} for the list of supported properties.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String				datasourceName		= arguments.getAsString( Key._name );
		IStruct				configProps			= arguments.getAsStruct( Key.config );
		DatasourceConfig	datasourceConfig	= new DatasourceConfig( datasourceName ).process( configProps );

		IStruct				datasourceSet		= runtime.getConfiguration().datasources;
		if ( datasourceSet.containsKey( datasourceName ) ) {
			throw new DatabaseException( String.format( "Datasource name '%s' already exists within the current context", datasourceName ) );
		}
		datasourceSet.put( datasourceName, datasourceConfig );
		context.clearConfigCache();

		return null;
	}
}
