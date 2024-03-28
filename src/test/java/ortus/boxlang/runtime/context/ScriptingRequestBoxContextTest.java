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

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

@DisplayName( "ScriptingRequestBoxContext Tests" )
public class ScriptingRequestBoxContextTest {

	@Test
	@DisplayName( "Test default constructor" )
	void testDefaultConstructor() {
		ScriptingRequestBoxContext context = new ScriptingRequestBoxContext();
		assertThat( context.hasTemplates() ).isFalse();
		assertThat( context.findClosestFunctionName() ).isNull();
	}

	@Test
	@DisplayName( "Test template path" )
	void testTemplatePath() {
		ScriptingRequestBoxContext context = new ScriptingRequestBoxContext();
		context.pushTemplate( new BoxTemplate() {

			@Override
			public void _invoke( IBoxContext context ) {
			}

			@Override
			public long getRunnableCompileVersion() {
				return 0;
			}

			@Override
			public LocalDateTime getRunnableCompiledOn() {
				return null;
			}

			@Override
			public Object getRunnableAST() {
				return null;
			}

			@Override
			public Path getRunnablePath() {
				return Path.of( "test/file.cfm" );
			}

			public BoxSourceType getSourceType() {
				return BoxSourceType.BOXSCRIPT;
			}

			public List<ImportDefinition> getImports() {
				return null;
			}

		} );
		assertThat( context.findClosestTemplate() ).isNotNull();
		assertThat( context.hasTemplates() ).isTrue();
	}

	@Test
	@DisplayName( "Test scopeFind with existing key" )
	void testScopeFindExistingKey() {
		ScriptingRequestBoxContext	context			= new ScriptingRequestBoxContext();
		Key							key				= Key.of( "testIt" );
		IScope						variablesScope	= context.getScopeNearby( Key.of( "variables" ) );
		variablesScope.put( key, "value" );
		ScopeSearchResult result = context.scopeFindNearby( key, null );
		assertThat( result.value() ).isEqualTo( "value" );
		assertThat( result.scope() ).isEqualTo( variablesScope );
	}

	@Test
	@DisplayName( "Test scopeFind default scope" )
	void testScopeFindDefaultScope() {
		ScriptingRequestBoxContext	context			= new ScriptingRequestBoxContext();
		Key							key				= Key.of( "testIt" );
		IScope						variablesScope	= context.getScopeNearby( Key.of( "variables" ) );
		ScopeSearchResult			result			= context.scopeFindNearby( key, variablesScope );
		assertThat( result.value() ).isEqualTo( null );
		assertThat( result.scope() ).isEqualTo( variablesScope );
	}

	@Test
	@DisplayName( "Test scopeFind with missing key" )
	void testScopeFindMissingKey() {
		ScriptingRequestBoxContext context = new ScriptingRequestBoxContext();
		assertThrows( KeyNotFoundException.class, () -> context.scopeFindNearby( new Key( "nonExistentKey" ), null ) );
	}

	@Test
	@DisplayName( "Test default assignment scope" )
	void testDefaultAssignmentScope() {
		ScriptingRequestBoxContext context = new ScriptingRequestBoxContext();
		assertThat( context.getDefaultAssignmentScope().getName().getName() ).isEqualTo( "variables" );
	}
}
