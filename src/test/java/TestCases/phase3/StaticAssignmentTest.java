/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
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

/**
 * Tests for assigning static variables from outside a class with {@code Class::name = value} (BL-2749)
 */
public class StaticAssignmentTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Assign an existing static with an import alias" )
	@Test
	public void testAssignExistingStatic() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticAssign;
		    assigned = StaticAssign::counter = 42;
		    result = StaticAssign::counter;
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "assigned" ) ) ).isEqualTo( 42 );
		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

	@DisplayName( "Assign an existing static with a fully qualified name" )
	@Test
	public void testAssignStaticFQN() {
		instance.executeSource(
		    """
		    src.test.java.TestCases.phase3.StaticAssign::counter = 7;
		    result = src.test.java.TestCases.phase3.StaticAssign::counter;
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
	}

	@DisplayName( "Assign a static key that does not exist yet" )
	@Test
	public void testAssignNewStaticKey() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticAssign;
		    StaticAssign::brandNew = "hello";
		    result = StaticAssign::brandNew;
		    result2 = StaticAssign::getStaticKey( "brandNew" );
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "hello" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "hello" );
	}

	@DisplayName( "Compound assignment and increment on a static" )
	@Test
	public void testCompoundAssignment() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticAssign;
		    StaticAssign::counter = 10;
		    StaticAssign::counter += 5;
		    afterPlus = StaticAssign::counter;
		    StaticAssign::counter -= 3;
		    afterMinus = StaticAssign::counter;
		    StaticAssign::counter *= 2;
		    afterMult = StaticAssign::counter;
		    StaticAssign::counter++;
		    afterPostInc = StaticAssign::counter;
		    preInc = ++StaticAssign::counter;
		    StaticAssign::counter--;
		    afterPostDec = StaticAssign::counter;
		    StaticAssign::label = "a";
		    StaticAssign::label &= "b";
		    label = StaticAssign::label;
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsNumber( Key.of( "afterPlus" ) ).intValue() ).isEqualTo( 15 );
		assertThat( variables.getAsNumber( Key.of( "afterMinus" ) ).intValue() ).isEqualTo( 12 );
		assertThat( variables.getAsNumber( Key.of( "afterMult" ) ).intValue() ).isEqualTo( 24 );
		assertThat( variables.getAsNumber( Key.of( "afterPostInc" ) ).intValue() ).isEqualTo( 25 );
		assertThat( variables.getAsNumber( Key.of( "preInc" ) ).intValue() ).isEqualTo( 26 );
		assertThat( variables.getAsNumber( Key.of( "afterPostDec" ) ).intValue() ).isEqualTo( 25 );
		assertThat( variables.get( Key.of( "label" ) ) ).isEqualTo( "ab" );
	}

	@DisplayName( "Static assignment is visible to static functions of the class" )
	@Test
	public void testVisibleToStaticFunctions() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticAssign;
		    StaticAssign::counter = 99;
		    result = StaticAssign::getCounter();
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( 99 );
	}

	@DisplayName( "Static assignment through a variable holding the class" )
	@Test
	public void testAssignStaticOnVariable() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticAssign;
		    myClass = StaticAssign;
		    myClass::counter = 123;
		    result = StaticAssign::getCounter();
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( 123 );
	}

}
