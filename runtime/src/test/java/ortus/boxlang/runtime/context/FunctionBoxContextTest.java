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

@DisplayName( "FunctionBoxContextTest Tests" )
public class FunctionBoxContextTest {

	@Test
	@DisplayName( "Test constructors" )
	void testConstructor() {
		assertThrows( Throwable.class, () -> new FunctionBoxContext( null ) );
		IBoxContext			parentContext	= new TemplateBoxContext();
		FunctionBoxContext	context			= new FunctionBoxContext( parentContext );
		assertThat( context.getParent() ).isNotNull();

		context = new FunctionBoxContext( parentContext, new ArgumentsScope() );
		assertThat( context.getScopeNearby( ArgumentsScope.name ).getName() ).isEqualTo( ArgumentsScope.name );
	}

	@Test
	@DisplayName( "Test scope lookup" )
	void testScopeLookup() {
		IBoxContext		parentContext	= new TemplateBoxContext();
		ArgumentsScope	argumentsScope	= new ArgumentsScope();
		IBoxContext		context			= new FunctionBoxContext( parentContext, argumentsScope );
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

}