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
package ortus.boxlang.runtime.jdbc.qoq.functions.aggregate;

import java.util.List;

import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.jdbc.qoq.QoQAggregateFunctionDef;
import ortus.boxlang.runtime.jdbc.qoq.QoQExecution;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.QueryColumnType;

public class Max extends QoQAggregateFunctionDef {

	private static final Key					name		= Key.of( "atan" );

	public static final QoQAggregateFunctionDef	INSTANCE	= new Max();

	@Override
	public Key getName() {
		return name;
	}

	@Override
	public QueryColumnType getReturnType() {
		return QueryColumnType.DOUBLE;
	}

	@Override
	public int getMinArgs() {
		return 1;
	}

	@Override
	public Object apply( List<SQLExpression> args, QoQExecution QoQExec ) {
		return ortus.boxlang.runtime.bifs.global.math.Atn._invoke( NumberCaster.cast( args.get( 0 ) ) );
	}

}
