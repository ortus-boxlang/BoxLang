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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

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

	@DisplayName( "It gets current template path in include" )
	@Test
	public void testCurrentTemplateCatch() {

		instance.executeSource(
		    """
		    include "src/test/java/ortus/boxlang/runtime/bifs/global/system/CurrentTemplateCatch.cfs";
		     """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "CurrentTemplateCatchInclude.cfs" );

	}

	@Test
	public void testInjectedUDFCCurrentTemplate() {
		// @formatter:off
		instance.executeSource(
		    """
				injectedUDFCCurrentTemplate = new src.test.java.ortus.boxlang.runtime.bifs.global.system.InjectedUDFCCurrentTemplate();
				injectedUDFCCurrentTemplate2 = new src.test.java.ortus.boxlang.runtime.bifs.global.system.InjectedUDFCCurrentTemplate2();
				injectedUDFCCurrentTemplate.test = injectedUDFCCurrentTemplate2.test;

				{ argCurrentTemplate : result1a, funCurrentTemplate : result1b } = injectedUDFCCurrentTemplate2.test();

				{ argCurrentTemplate : result2a, funCurrentTemplate : result2b } = injectedUDFCCurrentTemplate.test();

		      """,
		    context );
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "result1a" ) ) ).contains( "InjectedUDFCCurrentTemplate2.bx" );
		assertThat( variables.getAsString( Key.of( "result1b" ) ) ).contains( "InjectedUDFCCurrentTemplate2.bx" );
		assertThat( variables.getAsString( Key.of( "result2a" ) ) ).contains( "InjectedUDFCCurrentTemplate.bx" );
		assertThat( variables.getAsString( Key.of( "result2b" ) ) ).contains( "InjectedUDFCCurrentTemplate.bx" );
	}

	@Test
	public void testIncludedInClassUDFCCurrentTemplate() {
		// @formatter:off
		instance.executeSource(
		    """
				includedInClassUDFCCurrentTemplate = new src.test.java.ortus.boxlang.runtime.bifs.global.system.IncludedInClassUDFCCurrentTemplate();

				{ argCurrentTemplate : result1, funCurrentTemplate : result2 } = includedInClassUDFCCurrentTemplate.test();

				println( result1 );
				println( result2 );
		      """,
		    context );
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "result1" ) ) ).contains( "IncludedInClassUDFCCurrentTemplate.bxs" );
		assertThat( variables.getAsString( Key.of( "result2" ) ) ).contains( "IncludedInClassUDFCCurrentTemplate.bxs" );
	}

}
