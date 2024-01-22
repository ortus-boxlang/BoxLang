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
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.SampleClosure;

@DisplayName( "ClosureBoxContextTest Tests" )
public class ClosureBoxContextTest {

	@Test
	@DisplayName( "Test constructors" )
	void testConstructor() {
		assertThrows( Throwable.class, () -> new ClosureBoxContext( null, null ) );
		IBoxContext			parentContext	= new ScriptingRequestBoxContext();
		Closure				closure			= new SampleClosure( new Argument[] {}, new ScriptingRequestBoxContext(), "Brad" );
		ClosureBoxContext	context			= new ClosureBoxContext( parentContext, closure );
		assertThat( context.getParent() ).isNotNull();
		assertThat( context.getFunction() ).isNotNull();

		context = new ClosureBoxContext( parentContext, closure, new ArgumentsScope() );
		assertThat( context.getScopeNearby( ArgumentsScope.name ).getName() ).isEqualTo( ArgumentsScope.name );
	}

	@Test
	@DisplayName( "Test scope lookup" )
	void testScopeLookup() {

		IBoxContext		declaringDeclaringContext	= new ScriptingRequestBoxContext();
		IBoxContext		dummyParentContext			= new ScriptingRequestBoxContext();
		Closure			declaringclosure			= new SampleClosure( new Argument[] {}, declaringDeclaringContext, "Brad" );

		IBoxContext		declaringContext			= new ClosureBoxContext( dummyParentContext, declaringclosure );
		Closure			closure						= new SampleClosure( new Argument[] {}, declaringContext, "Brad" );
		IBoxContext		parentContext				= new ScriptingRequestBoxContext();
		ArgumentsScope	argumentsScope				= new ArgumentsScope();
		IBoxContext		context						= new ClosureBoxContext( parentContext, closure, argumentsScope );
		IScope			localScope					= context.getScopeNearby( LocalScope.name );
		IScope			variablesScope				= context.getScopeNearby( VariablesScope.name );
		Key				ambiguous					= Key.of( "ambiguous" );
		Key				localOnly					= Key.of( "localOnly" );
		Key				variablesOnly				= Key.of( "variablesOnly" );
		Key				argsOnly					= Key.of( "argsOnly" );
		Key				declaringOnly				= Key.of( "declaringOnly" );
		Key				declaringDeclaringOnly		= Key.of( "declaringDeclaringOnly" );

		localScope.put( ambiguous, "local scope ambiguous" );
		argumentsScope.put( ambiguous, "arguments scope ambiguous" );
		declaringContext.getScopeNearby( LocalScope.name ).put( ambiguous, "declaring scope ambiguous" );
		declaringDeclaringContext.getScopeNearby( VariablesScope.name ).put( ambiguous, "declaring declaring scope ambiguous" );

		localScope.put( localOnly, "local scope only" );
		argumentsScope.put( argsOnly, "arguments scope only" );
		variablesScope.put( variablesOnly, "declaring declaring variables scope only" );
		declaringContext.getScopeNearby( LocalScope.name ).put( declaringOnly, "declaring scope only" );
		declaringDeclaringContext.getScopeNearby( VariablesScope.name ).put( declaringDeclaringOnly, "declaring declaring scope only" );

		// The variable scope that the function "sees" is the same one from the parent template context
		assertThat( variablesScope ).isEqualTo( declaringDeclaringContext.getScopeNearby( VariablesScope.name ) );

		// ambiguous finds local scope
		assertThat( context.scopeFindNearby( ambiguous, null ).value() ).isEqualTo( "local scope ambiguous" );

		// local.ambiguous works
		assertThat( context.getScopeNearby( LocalScope.name ).get( ambiguous ) ).isEqualTo( "local scope ambiguous" );
		// variables.ambiguous works
		assertThat( context.getScopeNearby( VariablesScope.name ).get( ambiguous ) ).isEqualTo( "declaring declaring scope ambiguous" );
		// arguments.ambiguous works
		assertThat( context.getScopeNearby( ArgumentsScope.name ).get( ambiguous ) ).isEqualTo( "arguments scope ambiguous" );

		// find var in local
		assertThat( context.scopeFindNearby( localOnly, null ).value() ).isEqualTo( "local scope only" );
		// find var in arguments
		assertThat( context.scopeFindNearby( argsOnly, null ).value() ).isEqualTo( "arguments scope only" );
		// find var in variables
		assertThat( context.scopeFindNearby( variablesOnly, null ).value() ).isEqualTo( "declaring declaring variables scope only" );
		// find var in declaring scope
		assertThat( context.scopeFindNearby( declaringOnly, null ).value() ).isEqualTo( "declaring scope only" );
		// find var in declaring closure's declaring scope
		assertThat( context.scopeFindNearby( declaringDeclaringOnly, null ).value() ).isEqualTo( "declaring declaring scope only" );
	}

	@Test
	@DisplayName( "Can find closest function" )
	void testCanfindClosestFunctionName() {
		// We call a function
		IBoxContext	declaringContext	= new ScriptingRequestBoxContext();
		Key			funcName			= Key.of( "closure" );
		IBoxContext	parentContext		= new ScriptingRequestBoxContext();
		Closure		closure				= new SampleClosure( new Argument[] {}, declaringContext, "Brad" );
		IBoxContext	context				= new ClosureBoxContext( parentContext, closure );

		assertThat( context.findClosestFunctionName() ).isNotNull();
		assertThat( context.findClosestFunctionName() ).isEqualTo( funcName );

		// Our function includes a template
		IBoxContext childContext = new ScriptingRequestBoxContext( context );

		assertThat( childContext.findClosestFunctionName() ).isNotNull();
		assertThat( childContext.findClosestFunctionName() ).isEqualTo( funcName );

		// which includes another template
		IBoxContext childChildContext = new ScriptingRequestBoxContext( childContext );

		assertThat( childChildContext.findClosestFunctionName() ).isNotNull();
		assertThat( childChildContext.findClosestFunctionName() ).isEqualTo( funcName );

		// which includes ANOTHER template
		IBoxContext childChildChildContext = new ScriptingRequestBoxContext( childChildContext );

		assertThat( childChildChildContext.findClosestFunctionName() ).isNotNull();
		assertThat( childChildChildContext.findClosestFunctionName() ).isEqualTo( funcName );

		// which calls another function
		Closure		closure2	= new SampleClosure( new Argument[] {}, declaringContext, "Brad" );
		IBoxContext	context2	= new ClosureBoxContext( parentContext, closure2 );

		assertThat( context2.findClosestFunctionName() ).isNotNull();
		assertThat( context2.findClosestFunctionName() ).isEqualTo( funcName );
	}

	@Test
	@DisplayName( "Test default assignment scope" )
	void testDefaultAssignmentScope() {
		IBoxContext	declaringContext	= new ScriptingRequestBoxContext();
		IBoxContext	parentContext		= new ScriptingRequestBoxContext();
		Closure		closure				= new SampleClosure( new Argument[] {}, declaringContext, "Brad" );
		IBoxContext	context				= new ClosureBoxContext( parentContext, closure );
		assertThat( context.getDefaultAssignmentScope().getName().getName() ).isEqualTo( "local" );
	}
}