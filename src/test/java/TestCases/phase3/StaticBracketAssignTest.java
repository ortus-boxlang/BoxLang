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
package TestCases.phase3;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

/**
 * BL-2750: {@code static[ expr ] = value} inside a function must assign into the static scope,
 * while {@code static [ a, b ] = data} keeps working as a static destructuring declaration.
 */
public class StaticBracketAssignTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "static[ name ] = value in a static function assigns into the static scope" )
	@Test
	public void testStaticBracketAssignInStaticFunction() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticBracketAssign;
		    result1 = StaticBracketAssign::setDyn( "x", 7 );
		    result2 = StaticBracketAssign::x;
		    result3 = StaticBracketAssign::readDyn( "x" );
		    result4 = StaticBracketAssign::counter;
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 7 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 7 );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( 7 );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "static[ name ] = value in an instance function assigns into the static scope" )
	@Test
	public void testStaticBracketAssignInInstanceFunction() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticBracketAssign;
		    result1 = new StaticBracketAssign().setDynInstance( "fromInstance", "yes" );
		    result2 = StaticBracketAssign::fromInstance;
		    result3 = StaticBracketAssign::readDyn( "fromInstance" );
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "yes" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "yes" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "yes" );
	}

	@DisplayName( "static [ name ] = value with a space, literal keys and nested brackets are scope access" )
	@Test
	public void testStaticBracketAssignVariants() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticBracketAssign;
		    result1 = StaticBracketAssign::setDynSpaced( "spaced", 1 );
		    result2 = StaticBracketAssign::spaced;
		    result3 = StaticBracketAssign::setDynLiteral( "lit" );
		    result4 = StaticBracketAssign::setDynNested( "nested", 3 );
		    result5 = StaticBracketAssign::nested;
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "lit" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( 3 );
	}

	@DisplayName( "static[ k ] += 1 is a compound assignment into the static scope" )
	@Test
	public void testStaticBracketCompoundAssign() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticBracketAssign;
		    StaticBracketAssign::setDyn( "hits", 10 );
		    result1 = StaticBracketAssign::increment( "hits" );
		    result2 = StaticBracketAssign::hits;
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 11 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 11 );
	}

	@DisplayName( "static array destructuring declarations still assign into the static scope" )
	@Test
	public void testStaticArrayDestructuringDeclarations() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticBracketAssign;
		    result1 = StaticBracketAssign::destructurePair( [ 1, 2 ] );
		    result2 = StaticBracketAssign::first;
		    result3 = StaticBracketAssign::destructureSingle( [ "solo" ] );
		    result4 = StaticBracketAssign::only;
		    result5 = StaticBracketAssign::destructureRest( [ 1, 2, 3 ] );
		    result6 = StaticBracketAssign::destructureDefault( [] );
		    result7 = StaticBracketAssign::destructureNested( [ [ "deep" ] ] );
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( ( ( Array ) variables.get( Key.of( "result1" ) ) ).toArray() ).asList().containsExactly( 1, 2 ).inOrder();
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "solo" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "solo" );
		Array rest = ( Array ) variables.get( Key.of( "result5" ) );
		assertThat( rest.get( 0 ) ).isEqualTo( 1 );
		assertThat( ( ( Array ) rest.get( 1 ) ).toArray() ).asList().containsExactly( 2, 3 ).inOrder();
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( 99 );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( "deep" );
	}

	@DisplayName( "static[ name ] = value and static destructuring in a CFML component" )
	@Test
	public void testStaticBracketAssignInCFComponent() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticBracketAssignCF;
		    result1 = StaticBracketAssignCF::setDyn( "cfKey", 5 );
		    result2 = StaticBracketAssignCF::cfKey;
		    result3 = StaticBracketAssignCF::increment( "cfKey" );
		    result4 = StaticBracketAssignCF::destructurePair( [ 1, 2 ] );
		    result5 = StaticBracketAssignCF::second;
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 5 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 5 );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( 6 );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( 2 );
	}

}
