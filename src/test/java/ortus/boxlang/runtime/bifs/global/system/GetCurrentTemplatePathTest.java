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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class GetCurrentTemplatePathTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It gets current template path" )
	@Test
	public void testCurrentTemplate() {
		context.pushTemplate( new BoxTemplate() {

			@Override
			public List<ImportDefinition> getImports() {
				return null;
			}

			@Override
			public void _invoke( IBoxContext context ) {
			}

			@Override
			public long getRunnableCompileVersion() {
				return 1;
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
			public ResolvedFilePath getRunnablePath() {
				return ResolvedFilePath.of( Path.of( "/tmp/test.bxs" ) );
			}

			public BoxSourceType getSourceType() {
				return BoxSourceType.BOXSCRIPT;
			}

		} );

		instance.executeSource(
		    """
		    result = getCurrentTemplatePath();
		     """,
		    context );
		assertThat( variables.getAsString( result ).contains( "test.bxs" ) ).isTrue();

		context.popTemplate();
	}

	@DisplayName( "It gets current template path in include" )
	@Test
	public void testCurrentTemplateInclude() {

		instance.executeSource(
		    """
		    include "src/test/java/ortus/boxlang/runtime/bifs/global/system/IncludeTest.cfs";
		     """,
		    context );
		assertThat( variables.getAsString( result ).contains( "IncludeTest.cfs" ) ).isTrue();

	}

}
