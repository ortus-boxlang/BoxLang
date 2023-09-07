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

package ortus.boxlang.runtime.context;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Function.Argument;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.UDF;

@DisplayName( "FunctionBoxContextTest Tests" )
public class FunctionBoxContextTest {

	@Test
	@DisplayName( "Test constructors" )
	void testConstructor() {
		assertThrows( Throwable.class, () -> new FunctionBoxContext( null, null ) );
		IBoxContext			parentContext	= new TemplateBoxContext();
		FunctionBoxContext	context			= new FunctionBoxContext( parentContext, null );
		assertThat( context.getParent() ).isNotNull();
		assertThat( context.getFunction() ).isNull();

		context = new FunctionBoxContext( parentContext, null, new ArgumentsScope() );
		assertThat( context.getScopeNearby( ArgumentsScope.name ).getName() ).isEqualTo( ArgumentsScope.name );
	}

	@Test
	@DisplayName( "Test scope lookup" )
	void testScopeLookup() {
		IBoxContext		parentContext	= new TemplateBoxContext();
		ArgumentsScope	argumentsScope	= new ArgumentsScope();
		IBoxContext		context			= new FunctionBoxContext( parentContext, null, argumentsScope );
		IScope			localScope		= context.getScopeNearby( LocalScope.name );
		IScope			variablesScope	= context.getScopeNearby( VariablesScope.name );
		Key				ambiguous		= Key.of( "ambiguous" );
		Key				localOnly		= Key.of( "localOnly" );
		Key				variablesOnly	= Key.of( "variablesOnly" );
		Key				argsOnly		= Key.of( "argsOnly" );

		// The variable scope that the function "sees" is the same one from the parent template context
		assertThat( variablesScope ).isEqualTo( parentContext.getScopeNearby( VariablesScope.name ) );

		variablesScope.put( ambiguous, "variables scope ambiguous" );
		argumentsScope.put( ambiguous, "arguments scope ambiguous" );
		localScope.put( ambiguous, "local scope ambiguous" );

		localScope.put( localOnly, "local scope only" );
		argumentsScope.put( argsOnly, "arguments scope only" );
		variablesScope.put( variablesOnly, "variables scope only" );

		// ambiguous finds local scope
		assertThat( context.scopeFindNearby( ambiguous, null ).value() ).isEqualTo( "local scope ambiguous" );

		// local.ambiguous works
		assertThat( context.getScopeNearby( LocalScope.name ).get( ambiguous ) ).isEqualTo( "local scope ambiguous" );
		// variables.ambiguous works
		assertThat( context.getScopeNearby( VariablesScope.name ).get( ambiguous ) ).isEqualTo( "variables scope ambiguous" );
		// arguments.ambiguous works
		assertThat( context.getScopeNearby( ArgumentsScope.name ).get( ambiguous ) ).isEqualTo( "arguments scope ambiguous" );

		// find var in local
		assertThat( context.scopeFindNearby( localOnly, null ).value() ).isEqualTo( "local scope only" );
		// find var in arguments
		assertThat( context.scopeFindNearby( argsOnly, null ).value() ).isEqualTo( "arguments scope only" );
		// find var in variables
		assertThat( context.scopeFindNearby( variablesOnly, null ).value() ).isEqualTo( "variables scope only" );
	}

	@Test
	@DisplayName( "Can find closest function" )
	void testCanFindClosestFunction() {
		// We call a function
		Key					funcName		= Key.of( "MyFunc$" );
		IBoxContext			parentContext	= new TemplateBoxContext();
		UDF					udf				= new SampleUDF( UDF.Access.PUBLIC, funcName, "String", new Argument[] {}, "", false, null );
		FunctionBoxContext	context			= new FunctionBoxContext( parentContext, udf );

		assertThat( context.findClosestFunction() ).isNotNull();
		assertThat( context.findClosestFunction().getName() ).isEqualTo( funcName );

		// Our function includes a template
		IBoxContext childContext = new TemplateBoxContext( null, context );

		assertThat( childContext.findClosestFunction() ).isNotNull();
		assertThat( childContext.findClosestFunction().getName() ).isEqualTo( funcName );

		// which includes another template
		IBoxContext childChildContext = new TemplateBoxContext( null, childContext );

		assertThat( childChildContext.findClosestFunction() ).isNotNull();
		assertThat( childChildContext.findClosestFunction().getName() ).isEqualTo( funcName );

		// which includes ANOTHER template
		IBoxContext childChildChildContext = new TemplateBoxContext( null, childChildContext );

		assertThat( childChildChildContext.findClosestFunction() ).isNotNull();
		assertThat( childChildChildContext.findClosestFunction().getName() ).isEqualTo( funcName );

		// which calls another function
		Key					funcName2	= Key.of( "another_function_here" );
		UDF					udf2		= new SampleUDF( UDF.Access.PUBLIC, funcName2, "String", new Argument[] {}, "", false, null );
		FunctionBoxContext	context2	= new FunctionBoxContext( childChildChildContext, udf2 );

		assertThat( context2.findClosestFunction() ).isNotNull();
		assertThat( context2.findClosestFunction().getName() ).isEqualTo( funcName2 );
	}
}