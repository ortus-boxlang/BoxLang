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
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.SampleLambda;

@DisplayName( "LambdaBoxContextTest Tests" )
public class LambdaBoxContextTest {

	@Test
	@DisplayName( "Test constructors" )
	void testConstructor() {
		assertThrows( Throwable.class, () -> new LambdaBoxContext( null, null ) );
		IBoxContext			parentContext	= new ScriptingRequestBoxContext();
		Lambda				Lambda			= new SampleLambda( new Argument[] {}, "Brad" );
		LambdaBoxContext	context			= new LambdaBoxContext( parentContext, Lambda );
		assertThat( context.getParent() ).isNotNull();
		assertThat( context.getFunction() ).isNotNull();

		context = new LambdaBoxContext( parentContext, Lambda, new ArgumentsScope() );
		assertThat( context.getScopeNearby( ArgumentsScope.name ).getName() ).isEqualTo( ArgumentsScope.name );
	}

	@Test
	@DisplayName( "Test scope lookup" )
	void testScopeLookup() {
		Lambda			Lambda			= new SampleLambda( new Argument[] {}, "Brad" );
		IBoxContext		parentContext	= new ScriptingRequestBoxContext();
		ArgumentsScope	argumentsScope	= new ArgumentsScope();
		IBoxContext		context			= new LambdaBoxContext( parentContext, Lambda, argumentsScope );
		IScope			localScope		= context.getScopeNearby( LocalScope.name );

		// Lambda has no visiblity to this scope
		assertThrows( Throwable.class, () -> context.getScopeNearby( VariablesScope.name ) );
		IScope	variablesScope	= parentContext.getScopeNearby( VariablesScope.name );
		Key		ambiguous		= Key.of( "ambiguous" );
		Key		localOnly		= Key.of( "localOnly" );
		Key		variablesOnly	= Key.of( "variablesOnly" );
		Key		argsOnly		= Key.of( "argsOnly" );

		// The variable scope that the function "sees" is the same one from the parent template context
		assertThat( variablesScope ).isEqualTo( parentContext.getScopeNearby( VariablesScope.name ) );

		localScope.put( ambiguous, "local scope ambiguous" );
		argumentsScope.put( ambiguous, "arguments scope ambiguous" );
		variablesScope.put( ambiguous, "variables scope ambiguous" );

		localScope.put( localOnly, "local scope only" );
		argumentsScope.put( argsOnly, "arguments scope only" );
		variablesScope.put( variablesOnly, "variables scope only" );

		// ambiguous finds local scope
		assertThat( context.scopeFindNearby( ambiguous, null ).value() ).isEqualTo( "local scope ambiguous" );

		// local.ambiguous works
		assertThat( context.getScopeNearby( LocalScope.name ).get( ambiguous ) ).isEqualTo( "local scope ambiguous" );
		// Lambda has no visiblity to variables.ambiguous
		assertThrows( Throwable.class, () -> context.getScopeNearby( VariablesScope.name ).get( ambiguous ) );
		// arguments.ambiguous works
		assertThat( context.getScopeNearby( ArgumentsScope.name ).get( ambiguous ) ).isEqualTo( "arguments scope ambiguous" );

		// find var in local
		assertThat( context.scopeFindNearby( localOnly, null ).value() ).isEqualTo( "local scope only" );
		// find var in arguments
		assertThat( context.scopeFindNearby( argsOnly, null ).value() ).isEqualTo( "arguments scope only" );

		// Lambda has no visiblity to variables scope
		assertThrows( Throwable.class, () -> context.getScopeNearby( VariablesScope.name ).get( ambiguous ) );
	}

	@Test
	@DisplayName( "Can find closest function" )
	void testCanfindClosestFunctionName() {
		// We call a function
		Key			funcName		= Key.of( "lambda" );
		IBoxContext	parentContext	= new ScriptingRequestBoxContext();
		Lambda		Lambda			= new SampleLambda( new Argument[] {}, "Brad" );
		IBoxContext	context			= new LambdaBoxContext( parentContext, Lambda );

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
		Lambda		Lambda2		= new SampleLambda( new Argument[] {}, "Brad" );
		IBoxContext	context2	= new LambdaBoxContext( parentContext, Lambda2 );

		assertThat( context2.findClosestFunctionName() ).isNotNull();
		assertThat( context2.findClosestFunctionName() ).isEqualTo( funcName );
	}

	@Test
	@DisplayName( "Test default assignment scope" )
	void testDefaultAssignmentScope() {
		IBoxContext	parentContext	= new ScriptingRequestBoxContext();
		Lambda		Lambda			= new SampleLambda( new Argument[] {}, "Brad" );
		IBoxContext	context			= new LambdaBoxContext( parentContext, Lambda );
		assertThat( context.getDefaultAssignmentScope().getName().getName() ).isEqualTo( "local" );
	}
}