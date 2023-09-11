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

import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

@DisplayName( "ScriptingBoxContext Tests" )
public class ScriptingBoxContextTest {

	@Test
	@DisplayName( "Test default constructor" )
	void testDefaultConstructor() {
		ScriptingBoxContext context = new ScriptingBoxContext();
		assertThat( context.getParent() ).isNull();
		assertThat( context.hasTemplates() ).isFalse();
		assertThat( context.findClosestFunction() ).isNull();
	}

	@Test
	@DisplayName( "Test template path" )
	void testTemplatePath() {
		ScriptingBoxContext context = new ScriptingBoxContext();
		context.pushTemplate( new BaseTemplate() );
		assertThat( context.findClosestTemplate() ).isNotNull();
		assertThat( context.hasTemplates() ).isTrue();
	}

	@Test
	@DisplayName( "Test scopeFind with existing key" )
	void testScopeFindExistingKey() {
		ScriptingBoxContext	context			= new ScriptingBoxContext();
		Key					key				= Key.of( "testIt" );
		IScope				variablesScope	= context.getScopeNearby( Key.of( "variables" ) );
		variablesScope.put( key, "value" );
		ScopeSearchResult result = context.scopeFindNearby( key, null );
		assertThat( result.value() ).isEqualTo( "value" );
		assertThat( result.scope() ).isEqualTo( variablesScope );
	}

	@Test
	@DisplayName( "Test scopeFind default scope" )
	void testScopeFindDefaultScope() {
		ScriptingBoxContext	context			= new ScriptingBoxContext();
		Key					key				= Key.of( "testIt" );
		IScope				variablesScope	= context.getScopeNearby( Key.of( "variables" ) );
		ScopeSearchResult	result			= context.scopeFindNearby( key, variablesScope );
		assertThat( result.value() ).isEqualTo( null );
		assertThat( result.scope() ).isEqualTo( variablesScope );
	}

	@Test
	@DisplayName( "Test scopeFind with missing key" )
	void testScopeFindMissingKey() {
		ScriptingBoxContext context = new ScriptingBoxContext();
		assertThrows( KeyNotFoundException.class, () -> context.scopeFindNearby( new Key( "nonExistentKey" ), null ) );
	}

	@Test
	@DisplayName( "Test default assignment scope" )
	void testDefaultAssignmentScope() {
		ScriptingBoxContext context = new ScriptingBoxContext();
		assertThat( context.getDefaultAssignmentScope().getName().getName() ).isEqualTo( "variables" );
	}
}
