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
package ortus.boxlang.runtime.components.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.validators.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.jdbc.ExecutedQuery;
import ortus.boxlang.runtime.jdbc.PendingQuery;
import ortus.boxlang.runtime.jdbc.QueryOptions;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import javax.annotation.Nonnull;
import java.util.Set;

@BoxComponent( allowsBody = false )
public class QueryParam extends Component {

	/**
	 * Constructor
	 */
	public QueryParam() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.value, "any" ),
		    new Attribute( Key.cfsqltype, "string" ),
		    new Attribute( Key.maxLength, "numeric" ),
		    new Attribute( Key.scale, "numeric" ),
		    new Attribute( Key.nulls, "boolean" ),
		    new Attribute( Key.list, "boolean" ),
		    new Attribute( Key.separator, "string" )
		};

	}

	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IStruct parentState = context.findClosestComponent( Key.query );
		if ( parentState == null ) {
			throw new RuntimeException( "QueryParam must be nested in the body of a Query component" );
		}
		// Set our data into the Query component for it to use
		parentState.getAsArray( Key.queryParams ).add( attributes );
		context.writeToBuffer( "?" );
		return DEFAULT_RETURN;
	}

}
