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
package TestCases.asm;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

public class BasicTest {

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
		instance.useASMBoxPiler();
	}

	@AfterEach
	public void teardownEach() {
		instance.useJavaBoxpiler();
	}

	@DisplayName( "ASM Easy Difficulty Source Test" )
	@Test
	@Disabled
	public void testEasySource() {
		instance.executeStatement(
		    """
		    result = 2;

		    result += 2;
		        """,
		    context );

		assertThat( variables.getAsDouble( result ) ).isEqualTo( 4.0 );
	}

	@DisplayName( "ASM Medium Difficulty Source Test" )
	@Test
	@Disabled
	public void testMediumSource() {
// @formatter:off
		var output = instance.executeStatement(
		    """
		    		    colors = [
		    		    	"red",
		    		    	"orange",
		    		    	"yellow",
		    		    	"green",
		    		    	"blue",
		    		    	"purple"
		    		    ];

		    		    function getCircle( required numeric radius ){
		    		    	return {
		    		    		radius: radius,
		    		    		circumference: Pi() * radius * 2,
		    		    		color: colors[ 2 ]
		    		    	};
		    		    }

		    		    aCircle = getCircle( 5 );

		    		    echo( "Generated a circle:
" );
		    		    echo( "  radius:        #aCircle.radius#
" );
		    		    echo( "  circumference: #aCircle.circumference#
" );
		    		    echo( "  color:         #aCircle.color#
" );

		    		    getBoxContext().getBuffer().toString();
		    		          """,
		    context );


		assertThat( output ).isEqualTo( """
Generated a circle:
  radius:        5
  circumference: 31.4159265359
  color:         orange
""" );
		// @formatter:on
	}

	@DisplayName( "ASM Hard Difficulty Source Test" )
	@Test
	// @Disabled
	public void testHardSource() {
		var output = instance.executeStatement(
		    """
		    operator = new src.test.java.TestCases.asm.Operator();

		    operator.setOperation( ( x ) -> x * 2 );

		    echo( operator.run( 5 ) );

		    getBoxContext().getBuffer().toString();

		    // expected output
		    // 10.0
		          """,
		    context );

		assertThat( output ).isEqualTo( "10" );
	}

}
