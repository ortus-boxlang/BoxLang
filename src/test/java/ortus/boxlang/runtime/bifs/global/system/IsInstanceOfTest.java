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

public class IsInstanceOfTest {

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

	@DisplayName( "It can can check Java types" )
	@Test
	public void testCheckJavaTypes() {
		instance.executeSource(
		    """
		    result = isInstanceOf( [], "ortus.boxlang.runtime.types.Array" );
		    result2 = isInstanceOf( [], "java.util.List" );
		    result3 = isInstanceOf( [], "java.util.Collection" );
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( true );
	}

	@DisplayName( "It can can check not Java types" )
	@Test
	public void testCheckNotJavaTypes() {
		instance.executeSource(
		    """
		    result = isInstanceOf( {}, "ortus.boxlang.runtime.types.Array" );
		    result2 = isInstanceOf( {}, "java.util.List" );
		    result3 = isInstanceOf( "", "java.util.Collection" );
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( false );
	}

	@DisplayName( "It can can check BL Class types" )
	@Test
	public void testCheckBLClassTypes() {
		instance.executeSource(
		    """
		    cfc = new src.test.java.TestCases.phase3.Chihuahua();
		       result = isInstanceOf( cfc, "ortus.boxlang.runtime.runnables.IClassRunnable" );
		       result2 = isInstanceOf( cfc, "src.test.java.TestCases.phase3.Chihuahua" );
		       result3 = isInstanceOf( cfc, "Dog" );
		       result4 = isInstanceOf( cfc, "Animal" );
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( true );
	}

	@DisplayName( "It can can check BL Interface types" )
	@Test
	public void testCheckBLInterfaceTypes() {
		instance.executeSource(
		    """
		    cfc = new src.test.java.TestCases.phase3.Moped();
		       result = isInstanceOf( cfc, "src.test.java.TestCases.phase3.Moped" );
		       result2 = isInstanceOf( cfc, "Moped" );
		       result3 = isInstanceOf( cfc, "src.test.java.TestCases.phase3.IBicycle" );
		       result4 = isInstanceOf( cfc, "src.test.java.TestCases.phase3.IMotorcycle" );
		       result5 = isInstanceOf( cfc, "IBicycle" );
		       result6 = isInstanceOf( cfc, "IMotorcycle" );
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( true );
	}

	@DisplayName( "It can can check BL inherited Interface types" )
	@Test
	public void testCheckBLInheritedInterfaceTypes() {
		instance.executeSource(
		    """
		    cfc = new src.test.java.TestCases.phase3.SeniorVespa();
		       result = isInstanceOf( cfc, "src.test.java.TestCases.phase3.Moped" );
		       result2 = isInstanceOf( cfc, "Moped" );
		       result3 = isInstanceOf( cfc, "src.test.java.TestCases.phase3.IBicycle" );
		       result4 = isInstanceOf( cfc, "src.test.java.TestCases.phase3.IMotorcycle" );
		       result5 = isInstanceOf( cfc, "IBicycle" );
		       result6 = isInstanceOf( cfc, "IMotorcycle" );
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( true );
	}

}
