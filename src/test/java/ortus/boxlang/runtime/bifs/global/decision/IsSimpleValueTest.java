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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class IsSimpleValueTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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

	@DisplayName( "It detects simple values" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    aBool   = isSimpleValue( true );
		    aString = isSimpleValue( "Michael" );
		    anInt   = isSimpleValue( 12345 );
		    aFloat  = isSimpleValue( 3.45 );
		    aDate   = isSimpleValue( value = now() );
		    aUUID   = isSimpleValue( value = createUUID() );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aBool" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aString" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "anInt" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aFloat" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aDate" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aUUID" ) ) ).isTrue();
	}

	@DisplayName( "It returns false for non-simple values" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    aStruct     = isSimpleValue( {} );
		    anArray     = isSimpleValue( [] );
		    aNull       = isSimpleValue( value = nullValue() );
		    aJavaClass  = isSimpleValue( value = createObject( 'java', "java.lang.String" ) );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aStruct" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "anArray" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aNull" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aJavaClass" ) ) ).isFalse();
	}

	@Disabled( "QueryNew is not yet implemented." )
	@DisplayName( "It returns false for queries" )
	@Test
	public void testQuery() {
		assertThat( ( Boolean ) instance.executeStatement( "isSimpleValue( queryNew( 'id' ) )" ) ).isFalse();
	}

}
