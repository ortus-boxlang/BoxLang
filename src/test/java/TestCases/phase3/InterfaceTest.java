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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class InterfaceTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			foo		= new Key( "foo" );

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

	@DisplayName( "basic CF interface" )
	@Test
	public void testBasicCFInterface() {

		BoxInterface inter = ( BoxInterface ) DynamicObject.of( RunnableLoader.getInstance().loadClass(
		    """
		    /**
		    * This is my interface description
		    *
		    * @brad wood
		    * @luis
		    */
		    interface singleton gavin="pickin" inject foo="bar" {

		    	function init();

		    	function foo();

		    	private function bar();

		    	default function myDefaultMethod() {
		    		return this.foo();
		    	}

		    }

		       """, context, BoxSourceType.CFSCRIPT ), context ).unWrapBoxLangClass();

		assertThat( inter.getMetaData().get( Key.of( "type" ) ) ).isEqualTo( "Interface" );
		assertThat( inter.getMetaData().getAsArray( Key.of( "functions" ) ) ).hasSize( 4 );

	}

	@DisplayName( "basic interface file CF" )
	@Test
	public void testBasicClassFileCF() {

		instance.executeStatement(
		    """
		    	result = createObject( "component", "src.test.java.TestCases.phase3.MyInterfaceCF" );
		    """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxInterface.class );
		BoxInterface inter = variables.getAsBoxInterface( result );
		assertThat( inter.getMetaData().get( Key.of( "type" ) ) ).isEqualTo( "Interface" );
		assertThat( inter.getMetaData().getAsArray( Key.of( "functions" ) ) ).hasSize( 4 );

	}

	@DisplayName( "basic interface file BL" )
	@Test
	public void testBasicClassFileBL() {

		instance.executeStatement(
		    """
		    	result = createObject( "component", "src.test.java.TestCases.phase3.MyInterfaceBL" );
		    """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxInterface.class );
		BoxInterface inter = variables.getAsBoxInterface( result );
		assertThat( inter.getMetaData().get( Key.of( "type" ) ) ).isEqualTo( "Interface" );
		assertThat( inter.getMetaData().getAsArray( Key.of( "functions" ) ) ).hasSize( 4 );

	}

	@DisplayName( "basic interface file CF Tag" )
	@Test
	public void testBasicClassFileCFTag() {

		instance.executeStatement(
		    """
		    	result = createObject( "component", "src.test.java.TestCases.phase3.MyInterfaceCFTag" );
		    """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxInterface.class );
		BoxInterface inter = variables.getAsBoxInterface( result );
		assertThat( inter.getMetaData().get( Key.of( "type" ) ) ).isEqualTo( "Interface" );
		assertThat( inter.getMetaData().getAsArray( Key.of( "functions" ) ) ).hasSize( 4 );

	}

	@DisplayName( "moped example" )
	@Test
	public void testMopedExample() {

		instance.executeStatement(
		    """
		    boxClass = new src.test.java.TestCases.phase3.Moped();

		    // implemented method from IBicycle
		    result1 = boxClass.pedal( 3 )
		    // impelemented method from IMotorcycle
		    result2 = boxClass.shift( 4 )
		    // Default method from IBicycle
		    result3 = boxClass.hasInnerTube()
		    // Default method from IMotorcycle but overridden by moped
		    result4 = boxClass.needsFuel()
		                  """, context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "Pedal speed 3" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "Shift to gear 4" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( true );

	}

	@DisplayName( "onMissingMethod example" )
	@Test
	public void testOnMissingMethod() {

		instance.executeStatement(
		    """
		    boxClass = new src.test.java.TestCases.phase3.WheeledThing();

		    // implemented method from IBicycle
		    result1 = boxClass.pedal( 3 )
		    // impelemented method from IMotorcycle
		    result2 = boxClass.shift( 4 )
		    // Default method from IBicycle
		    result3 = boxClass.hasInnerTube()
		    // Default method from IMotorcycle but overridden by moped
		    result4 = boxClass.needsFuel()
		                  """, context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "Pedal speed 3" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "Shift to gear 4" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( false );

	}

	@DisplayName( "interface inheritence example" )
	@Test
	public void testInterfaceInheritence() {

		instance.executeStatement(
		    """
		    boxClass = new src.test.java.TestCases.phase3.InterfaceInheritenceTest();
		    result1 = boxClass.childMethod()
		    result2 = boxClass.parentMethod()
		    result3 = boxClass.childDefaultMethod()
		    result4 = boxClass.parentDefaultMethod()
		    result5 = boxClass.defaultOverrideMe()
		    assert boxClass instanceof "InterfaceInheritenceTest";
		    assert boxClass instanceof "IChildInterface";
		    assert boxClass instanceof "IParentInterface";
		                            """, context );

		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "childMethod" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "parentMethod" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "childDefaultMethod" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "parentDefaultMethod" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "defaultOverrideMeChild" );

	}

	@DisplayName( "interface inheritence static" )
	@Test
	public void testInterfaceStatic() {

		instance.executeStatement(
		    """
		       import src.test.java.TestCases.phase3.InterfaceStatic as is;
		    result1 = is.foo()
		    result2 = is.myVar;
		    result3 = is.yourVar;
		    result4 = is.callStatic();
		       """, context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "luis" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );

	}

}
