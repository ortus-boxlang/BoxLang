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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

@Disabled( "Unimplemented" )
public class IsInstanceOfTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}


	@DisplayName( "True conditions" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    aJavaString           = isInstanceOf( "boxlang", "java.lang.String" );
		    aDateObject           = isInstanceOf( now(), "java.util.Date" );
		    aStruct               = isInstanceOf( {}, "java.util.Map" );
		    anArray               = isInstanceOf( [], "Array" );
			aBoxLangClass         = isInstanceOf( new tests.resources.BoxLang.KitchenSink(), "KitchenSink" );
			aBoxLangClassFullPath = isInstanceOf( new tests.resources.BoxLang.KitchenSink(), "tests.resources.BoxLang.KitchenSink" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.dereference( Key.of( "aJavaString" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aDateObject" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aStruct" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "anArray" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aJavaString" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aBoxLangClass" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aBoxLangClassFullPath" ), false ) ).isTrue();
	}
	@DisplayName( "False conditions" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    aJavaString           = isInstanceOf( "gibberish", "aJavaString" );
		    aDateObject           = isInstanceOf( now(), "java.bad.path.to.Date" );
		    anArray               = isInstanceOf( {}, "java.util.Map" );
			aBoxLangClass         = isInstanceOf( new tests.resources.BoxLang.Auto(), "KitchenSink" );
			aBoxLangClassFullPath = isInstanceOf( new tests.resources.BoxLang.KitchenSink(), "tests.nowhere.BoxLang.KitchenSink" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.dereference( Key.of( "aJavaString" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aDateObject" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "anArray" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aBoxLangClass" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aBoxLangClassFullPath" ), false ) ).isFalse();
	}

}
