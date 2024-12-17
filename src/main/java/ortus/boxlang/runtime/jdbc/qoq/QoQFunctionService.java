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
package ortus.boxlang.runtime.jdbc.qoq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.jdbc.qoq.functions.aggregate.Avg;
import ortus.boxlang.runtime.jdbc.qoq.functions.aggregate.GroupConcat;
import ortus.boxlang.runtime.jdbc.qoq.functions.aggregate.Max;
import ortus.boxlang.runtime.jdbc.qoq.functions.aggregate.Min;
import ortus.boxlang.runtime.jdbc.qoq.functions.aggregate.StringAgg;
import ortus.boxlang.runtime.jdbc.qoq.functions.aggregate.Sum;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Abs;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Acos;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Asin;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Atan;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Cast;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Ceiling;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Coalesce;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Concat;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Convert;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Cos;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Exp;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Floor;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.IsNull;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Length;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Lower;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Ltrim;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Mod;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Power;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Rtrim;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Sin;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Sqrt;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Tan;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Trim;
import ortus.boxlang.runtime.jdbc.qoq.functions.scalar.Upper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I handle executing functions in query of queries
 */
public class QoQFunctionService {

	private static Map<Key, QoQFunction> functions = new HashMap<Key, QoQFunction>();

	static {

		// Scalar
		register( Upper.INSTANCE );
		register( Lower.INSTANCE );
		register( Abs.INSTANCE );
		register( Acos.INSTANCE );
		register( Asin.INSTANCE );
		register( Atan.INSTANCE );
		register( Ceiling.INSTANCE );
		register( Coalesce.INSTANCE );
		register( Concat.INSTANCE );
		register( Cos.INSTANCE );
		register( Exp.INSTANCE );
		register( Floor.INSTANCE );
		register( IsNull.INSTANCE );
		register( Length.INSTANCE );
		register( Lower.INSTANCE );
		register( Ltrim.INSTANCE );
		register( Mod.INSTANCE );
		register( Power.INSTANCE );
		register( Rtrim.INSTANCE );
		register( Sin.INSTANCE );
		register( Sqrt.INSTANCE );
		register( Tan.INSTANCE );
		register( Trim.INSTANCE );
		register( Upper.INSTANCE );

		// Aggregate
		register( Max.INSTANCE );
		register( Min.INSTANCE );
		register( Cast.INSTANCE );
		register( Convert.INSTANCE );
		register( Sum.INSTANCE );
		register( Avg.INSTANCE );
		register( GroupConcat.INSTANCE );
		register( StringAgg.INSTANCE );
	}

	private QoQFunctionService() {
	}

	public static void register( Key name, java.util.function.BiFunction<List<Object>, List<SQLExpression>, Object> function, IQoQFunctionDef functionDef,
	    QueryColumnType returnType, int requiredParams ) {
		functions.put( name, QoQFunction.of( function, functionDef, returnType, requiredParams ) );
	}

	public static void registerCustom( Key name, ortus.boxlang.runtime.types.Function function, QueryColumnType returnType, int requiredParams,
	    IBoxContext context ) {
		functions.put( name, QoQFunction.of(
		    // TODO: do we pass the expressions here?
		    ( List<Object> arguments, List<SQLExpression> expressions ) -> context.invokeFunction( function, arguments.toArray() ),
		    null,
		    returnType,
		    requiredParams
		) );
	}

	public static void registerAggregate( Key name, java.util.function.BiFunction<List<Object[]>, List<SQLExpression>, Object> function,
	    IQoQFunctionDef functionDef,
	    QueryColumnType returnType,
	    int requiredParams ) {
		functions.put( name, QoQFunction.ofAggregate( function, functionDef, returnType, requiredParams ) );
	}

	public static void register( QoQScalarFunctionDef functionDef ) {
		register( functionDef.getName(), functionDef, functionDef, null, functionDef.getMinArgs() );
	}

	public static void register( QoQAggregateFunctionDef functionDef ) {
		registerAggregate( functionDef.getName(), functionDef, functionDef, null, functionDef.getMinArgs() );
	}

	public static void unregister( Key name ) {
		functions.remove( name );
	}

	public static QoQFunction getFunction( Key name ) {
		if ( !functions.containsKey( name ) ) {
			throw new BoxRuntimeException( "Function [" + name + "] not found" );
		}
		return functions.get( name );
	}

	public record QoQFunction(
	    java.util.function.BiFunction<List<Object>, List<SQLExpression>, Object> callable,
	    java.util.function.BiFunction<List<Object[]>, List<SQLExpression>, Object> aggregateCallable,
	    IQoQFunctionDef functionDef,
	    QueryColumnType returnType,
	    int requiredParams ) {

		static QoQFunction of( java.util.function.BiFunction<List<Object>, List<SQLExpression>, Object> callable, IQoQFunctionDef functionDef,
		    QueryColumnType returnType,
		    int requiredParams ) {
			return new QoQFunction( callable, null, functionDef, returnType, requiredParams );
		}

		static QoQFunction ofAggregate( java.util.function.BiFunction<List<Object[]>, List<SQLExpression>, Object> callable, IQoQFunctionDef functionDef,
		    QueryColumnType returnType,
		    int requiredParams ) {
			return new QoQFunction( null, callable, functionDef, returnType, requiredParams );
		}

		public Object invoke( List<Object> arguments, List<SQLExpression> expressions ) {
			return callable.apply( arguments, expressions );
		}

		public Object invokeAggregate( List<Object[]> arguments, List<SQLExpression> expressions ) {
			return aggregateCallable.apply( arguments, expressions );
		}

		public boolean isAggregate() {
			return aggregateCallable != null;
		}

		public QueryColumnType returnType( QoQSelectExecution QoQExec, List<SQLExpression> expressions ) {
			if ( functionDef != null ) {
				return functionDef.getReturnType( QoQExec, expressions );
			}
			return returnType;
		}
	}

}
