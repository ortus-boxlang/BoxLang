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

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@BoxComponent( description = "Define parameters for parameterized queries", allowsBody = false )
public class QueryParam extends Component {

	/**
	 * Constructor
	 */
	public QueryParam() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.value, "any" ),
		    new Attribute( Key.sqltype, "string" ),
		    new Attribute( Key.maxLength, "integer" ),
		    new Attribute( Key.scale, "integer" ),
		    new Attribute( Key.nulls, "boolean" ),
		    new Attribute( Key.list, "boolean" ),
		    new Attribute( Key.separator, "string" )
		};
	}

	/**
	 * Used to verify or strongly type a query parameter to a valid SQL Type.
	 * <p>
	 * <strong>We recommend you ALWAYS use query params on any bind variables</strong>
	 * <p>
	 * This component is used to define a parameter to a query. It is used in conjunction with the
	 * Query component. It is not used directly.
	 * <p>
	 * Valid SQL Types are
	 * <ul>
	 * <li>bigint</li>
	 * <li>bit</li>
	 * <li>blob</li>
	 * <li>boolean</li>
	 * <li>char</li>
	 * <li>clob</li>
	 * <li>date</li>
	 * <li>decimal</li>
	 * <li>double</li>
	 * <li>float</li>
	 * <li>int</li>
	 * <li>integer</li>
	 * <li>idstamp</li>
	 * <li>longvarchar</li>
	 * <li>money</li>
	 * <li>numeric</li>
	 * <li>real</li>
	 * <li>smallint</li>
	 * <li>string</li>
	 * <li>time</li>
	 * <li>timestamp</li>
	 * <li>tinyint</li>
	 * <li>varbinary</li>
	 * <li>varchar</li>
	 * </ul>
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.value The value of the parameter
	 * 
	 * @attribute.maxLength The maximum length of the parameter
	 * 
	 * @attribute.scale The scale of the parameter, used only on `double` and `decimal` types. Defaults to `null`
	 * 
	 * @attribute.null Whether the parameter can be null or not.
	 * 
	 * @attribute.list Whether the parameter is a list or not
	 * 
	 * @attribute.separator The separator to use for the parameter. Defaults to a comma.
	 * 
	 * @attribute.sqltype The SQL type of the parameter. One of: `bigint`, `bit`, `blob`, `boolean`, `char`, `clob`, `date`, `decimal`, `double`, `float`, `int`, `integer`, `idstamp`, `longvarchar`, `money`, `numeric`, `real`, `smallint`, `string`,
	 *                    `time`, `timestamp`, `tinyint`, `varbinary`, or `varchar`.
	 *
	 */
	@Override
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IStruct parentState = context.findClosestComponent( Key.query );
		if ( parentState == null ) {
			throw new RuntimeException( "QueryParam must be nested in the body of a Query component" );
		}
		// Set our data into the Query component for it to use
		parentState.getAsArray( Key.queryParams ).add( attributes );
		String tokenReplacement = "?";
		context.writeToBuffer( tokenReplacement, true );
		return DEFAULT_RETURN;
	}

}
