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
package ortus.boxlang.runtime.dynamic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

public class ExpressionInterpreterTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "It can get a scope" )
	@Test
	void testItCanGetAScope() {
		Object result = ExpressionInterpreter.getVariable( context, "variables", false );
		assertThat( result ).isInstanceOf( VariablesScope.class );
	}

	@DisplayName( "It can get a scoped variable" )
	@Test
	void testItCanGetAScopedVar() {
		context.getScopeNearby( VariablesScope.name ).put( "brad", "Wood" );
		Object result = ExpressionInterpreter.getVariable( context, "variables.brad", false );
		assertThat( result ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can get a scoped variable2" )
	@Test
	void testItCanGetAScopedVar2() {
		IBoxContext fContext = new FunctionBoxContext( context, new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", new Argument[] {}, null ) );
		fContext.getScopeNearby( LocalScope.name ).put( "brad", "Wood" );
		Object result = ExpressionInterpreter.getVariable( fContext, "local.brad", false );
		assertThat( result ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can get an unscoped variable" )
	@Test
	void testItCanGetAnUncopedVar() {
		context.getScopeNearby( VariablesScope.name ).put( "brad", "Wood" );
		Object result = ExpressionInterpreter.getVariable( context, "brad", false );
		assertThat( result ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can get a deep variable" )
	@Test
	void testItCanGetADeepVar() {
		context.getScopeNearby( VariablesScope.name ).put( "foo", Struct.of(
		    "bar", Struct.of(
		        "baz", "bum"
		    )
		) );
		Object result = ExpressionInterpreter.getVariable( context, "foo.bar.baz", false );
		assertThat( result ).isEqualTo( "bum" );
		result = ExpressionInterpreter.getVariable( context, "variables.foo.bar.baz", false );
		assertThat( result ).isEqualTo( "bum" );
	}

	@DisplayName( "It can error on invalid Expression" )
	@Test
	void testItCanErrorOnInvalidExpression() {
		assertThrows( ExpressionException.class, () -> ExpressionInterpreter.getVariable( context, "", false ) );
		assertThrows( ExpressionException.class, () -> ExpressionInterpreter.getVariable( context, ".", false ) );
		assertThrows( ExpressionException.class, () -> ExpressionInterpreter.getVariable( context, ".foo", false ) );
		assertThrows( ExpressionException.class, () -> ExpressionInterpreter.getVariable( context, "foo.", false ) );
		assertThrows( KeyNotFoundException.class, () -> ExpressionInterpreter.getVariable( context, "does.not.exist", false ) );
	}

	@DisplayName( "It can set a scoped variable" )
	@Test
	void testItCanSetAScopedVar() {
		Object result = ExpressionInterpreter.setVariable( context, "variables.brad", "Wood" );
		assertThat( result ).isEqualTo( "Wood" );
		assertThat( context.getScopeNearby( VariablesScope.name ).get( "brad" ) ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can get a scoped variable2" )
	@Test
	void testItCanSetAScopedVar2() {
		IBoxContext	fContext	= new FunctionBoxContext( context, new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", new Argument[] {}, null ) );
		Object		result		= ExpressionInterpreter.setVariable( fContext, "local.brad", "Wood" );
		assertThat( result ).isEqualTo( "Wood" );
		assertThat( fContext.getScopeNearby( LocalScope.name ).get( "brad" ) ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can set an unscoped variable" )
	@Test
	void testItCanSetAnUnscopedVar() {
		Object result = ExpressionInterpreter.setVariable( context, "brad", "Wood" );
		assertThat( result ).isEqualTo( "Wood" );
		assertThat( context.getScopeNearby( VariablesScope.name ).get( "brad" ) ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can get a deep variable" )
	@Test
	void testItCanSetADeepVar() {
		Object result = ExpressionInterpreter.setVariable( context, "foo.bar.baz", "bum" );
		assertThat( result ).isEqualTo( "bum" );
		IScope variables = context.getScopeNearby( VariablesScope.name );
		assertThat( variables.get( "foo" ) ).isInstanceOf( IStruct.class );
		assertThat( variables.getAsStruct( Key.of( "foo" ) ).get( "bar" ) ).isInstanceOf( IStruct.class );
		assertThat( variables.getAsStruct( Key.of( "foo" ) ).getAsStruct( Key.of( "bar" ) ) ).isInstanceOf( IStruct.class );
		assertThat( variables.getAsStruct( Key.of( "foo" ) ).getAsStruct( Key.of( "bar" ) ).get( "baz" ) ).isEqualTo( "bum" );
	}

}
