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
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.jdbc.qoq.QoQScalarFunctionDef;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.QueryColumnType;

public class Power extends QoQScalarFunctionDef {

	private static final Key					name		= Key.of( "power" );

	public static final QoQScalarFunctionDef	INSTANCE	= new Power();

	@Override
	public Key getName() {
		return name;
	}

	@Override
	public QueryColumnType getReturnType( QoQSelectExecution QoQExec, List<SQLExpression> expressions ) {
		return QueryColumnType.DOUBLE;
	}

	@Override
	public int getMinArgs() {
		return 2;
	}

	@Override
	public Object apply( List<Object> args, List<SQLExpression> expressions ) {
		Object	left	= args.get( 0 );
		Object	right	= args.get( 1 );
		if ( left == null || right == null ) {
			return null;
		}
		// empty strings are treated as 0 in sql math
		if ( left instanceof String s && s.isEmpty() ) {
			left = 0;
		}
		if ( right instanceof String s && s.isEmpty() ) {
			right = 0;
		}
		return Math.pow( NumberCaster.cast( left ).doubleValue(), NumberCaster.cast( right ).doubleValue() );
	}

}
