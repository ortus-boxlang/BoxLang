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
package ortus.boxlang.runtime.jdbc.qoq.functions.scalar;

import java.util.List;

import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.jdbc.qoq.QoQScalarFunctionDef;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.QueryColumnType;

public class Right extends QoQScalarFunctionDef {

	private static final Key					name		= Key.of( "right" );

	public static final QoQScalarFunctionDef	INSTANCE	= new Right();

	@Override
	public Key getName() {
		return name;
	}

	@Override
	public QueryColumnType getReturnType( QoQSelectExecution QoQExec, List<SQLExpression> expressions ) {
		return QueryColumnType.VARCHAR;
	}

	@Override
	public int getMinArgs() {
		return 2;
	}

	@Override
	public Object apply( List<Object> args, List<SQLExpression> expressions ) {
		String	value	= StringCaster.cast( args.get( 0 ) );
		int		length	= IntegerCaster.cast( args.get( 1 ) );
		if ( length > value.length() ) {
			length = value.length();
		}
		return value.substring( value.length() - length );
	}

}
