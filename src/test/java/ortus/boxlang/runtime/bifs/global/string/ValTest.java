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
package ortus.boxlang.runtime.bifs.global.string;

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

public class ValTest {

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

	@DisplayName( "It extracts number from string" )
	@Test
	public void testItExtractsNumber() {
		instance.executeSource(
		    """
		       result = val("abcdef");
		       result2 = val("abcdef");
		       result3 = val("abc123");
		       result4 = val("123abcdef");
		       result5 = val("123");
		       result6 = val(".123");
		       result7 = val("123.456");
		       result8 = val(".");
		       result9 = val(".sdf");
		    result10 = val(null);
		         """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result2" ) ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result3" ) ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result4" ) ).doubleValue() ).isEqualTo( 123 );
		assertThat( variables.getAsNumber( Key.of( "result5" ) ).doubleValue() ).isEqualTo( 123 );
		assertThat( variables.getAsNumber( Key.of( "result6" ) ).doubleValue() ).isEqualTo( .123 );
		assertThat( variables.getAsNumber( Key.of( "result7" ) ).doubleValue() ).isEqualTo( 123.456 );
		assertThat( variables.getAsNumber( Key.of( "result8" ) ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result9" ) ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result10" ) ).doubleValue() ).isEqualTo( 0 );
	}

	@DisplayName( "It extracts number from string Member" )
	@Test
	public void testItExtractsNumberMember() {
		instance.executeSource(
		    """
		       result = "abcdef".val();
		       result2 = "abcdef".val();
		       result3 = "abc123".val();
		       result4 = "123abcdef".val();
		       result5 = "123".val();
		       result6 = ".123".val();
		       result7 = "123.456".val();
		       result8 = ".".val();
		       result9 = ".sdf".val();
		    result10 = "-50".val();
		    result11 = "-45-60".val();
		         """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result2" ) ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result3" ) ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result4" ) ).doubleValue() ).isEqualTo( 123 );
		assertThat( variables.getAsNumber( Key.of( "result5" ) ).doubleValue() ).isEqualTo( 123 );
		assertThat( variables.getAsNumber( Key.of( "result6" ) ).doubleValue() ).isEqualTo( .123 );
		assertThat( variables.getAsNumber( Key.of( "result7" ) ).doubleValue() ).isEqualTo( 123.456 );
		assertThat( variables.getAsNumber( Key.of( "result8" ) ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result9" ) ).doubleValue() ).isEqualTo( 0 );
		assertThat( variables.getAsNumber( Key.of( "result10" ) ).doubleValue() ).isEqualTo( -50 );
		assertThat( variables.getAsNumber( Key.of( "result11" ) ).doubleValue() ).isEqualTo( -45 );
	}

	@DisplayName( "It avoids sci notation" )
	@Test
	public void testItAvoidsSciNotation() {
		instance.executeSource(
		    """
		    result = val(15852073);
		           """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15852073 );
	}

}
