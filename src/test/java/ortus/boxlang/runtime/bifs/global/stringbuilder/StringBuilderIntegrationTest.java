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
package ortus.boxlang.runtime.bifs.global.stringbuilder;

import static com.google.common.truth.Truth.assertThat;

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
import ortus.boxlang.runtime.types.BoxStringBuilder;

public class StringBuilderIntegrationTest {

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

	// -------------------------------------------------------------------------
	// &= in-place mutation
	// -------------------------------------------------------------------------

	@DisplayName( "&= mutates a BoxStringBuilder in-place" )
	@Test
	public void testConcatAssignMutatesInPlace() {
		instance.executeSource( """
		                        sb = sb'Hello';
		                        ref = sb;
		                        sb &= ' World';
		                        result = sb.toString();
		                        sameInstance = ( sb === ref );
		                        """, context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
		// The same BoxStringBuilder instance should have been mutated, not replaced
		assertThat( variables.get( Key.of( "sameInstance" ) ) ).isEqualTo( true );
	}

	// -------------------------------------------------------------------------
	// sb"" and sb'' literal syntax
	// -------------------------------------------------------------------------

	@DisplayName( "sb\"\" literal creates a BoxStringBuilder" )
	@Test
	public void testSBLiteralDoubleQuoteEmpty() {
		instance.executeSource( """
		                        result = sb\"\";
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		assertThat( ( ( BoxStringBuilder ) variables.get( result ) ).toString() ).isEqualTo( "" );
	}

	@DisplayName( "sb'' literal creates a BoxStringBuilder" )
	@Test
	public void testSBLiteralSingleQuoteEmpty() {
		instance.executeSource( """
		                        result = sb'';
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		assertThat( ( ( BoxStringBuilder ) variables.get( result ) ).toString() ).isEqualTo( "" );
	}

	@DisplayName( "sb\"hello\" literal creates a BoxStringBuilder with the initial value" )
	@Test
	public void testSBLiteralDoubleQuoteSeeded() {
		instance.executeSource( """
		                        result = sb\"Hello\";
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		assertThat( ( ( BoxStringBuilder ) variables.get( result ) ).toString() ).isEqualTo( "Hello" );
	}

	@DisplayName( "sb'hello' literal creates a BoxStringBuilder with the initial value" )
	@Test
	public void testSBLiteralSingleQuoteSeeded() {
		instance.executeSource( """
		                        result = sb'Hello';
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		assertThat( ( ( BoxStringBuilder ) variables.get( result ) ).toString() ).isEqualTo( "Hello" );
	}

	@DisplayName( "sb\"...\" literal supports #...# interpolation" )
	@Test
	public void testSBLiteralDoubleQuoteInterpolation() {
		instance.executeSource( """
		                        name = 'World';
		                        result = sb"Hello #name#";
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		assertThat( ( ( BoxStringBuilder ) variables.get( result ) ).toString() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "sb'' literal DOES support #...# interpolation (same as double quotes)" )
	@Test
	public void testSBLiteralSingleQuoteInterpolation() {
		instance.executeSource( """
		                        name = 'World';
		                        result = sb'Hello #name#';
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		assertThat( ( ( BoxStringBuilder ) variables.get( result ) ).toString() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "sb literal can be chained with member methods" )
	@Test
	public void testSBLiteralChaining() {
		instance.executeSource( """
		                        result = sb"Hello".append( ' World' ).toString();
		                        """, context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "A Java StringBuilder can be wrapped and then used with BIFs" )
	@Test
	public void testJavaStringBuilderIntoBIF() {
		instance.executeSource( """
		                        javaSB = createObject( "java", "java.lang.StringBuilder" ).init( "Hello" );
		                        boxSB = stringBuilderNew( javaSB );
		                        returned = stringBuilderAppend( boxSB, " World" );
		                        javaResult = javaSB.toString();
		                        returnedResult = returned.toString();
		                        """, context );
		assertThat( variables.get( Key.of( "javaResult" ) ) ).isEqualTo( "Hello World" );
		assertThat( variables.get( Key.of( "returnedResult" ) ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "BoxLang member methods do not intercept raw Java StringBuilder calls" )
	@Test
	public void testJavaStringBuilderDeleteMemberCollision() {
		instance.executeSource( """
		                        javaSB = createObject( "java", "java.lang.StringBuilder" ).init( "abcde" );
		                        javaSB.delete( 2, 2 );
		                        result = javaSB.toString();
		                        """, context );
		assertThat( variables.get( result ) ).isEqualTo( "abcde" );
	}

	@DisplayName( "variable named 'sb' still works as an identifier" )
	@Test
	public void testSBAsIdentifier() {
		instance.executeSource( """
		                        sb = 'not a stringbuilder';
		                        result = sb;
		                        """, context );
		assertThat( variables.get( result ) ).isEqualTo( "not a stringbuilder" );
	}

	@DisplayName( "benchmark append performance for String vs StringBuilder over 1000 iterations" )
	@Test
	@Disabled
	public void testStringVsStringBuilderAppendTiming1000Iterations() {
		// @formatter:off
		instance.executeSource( """
			iterations = 250_000;

			str = "";
			stringStart = getTickCount();
			for ( i = 1; i <= iterations; i++ ) {
				str &= "x";
			}
			stringElapsedMs = getTickCount() - stringStart;

			strExplicitAssign = "";
			stringExplicitAssignStart = getTickCount();
			for ( i = 1; i <= iterations; i++ ) {
				strExplicitAssign = strExplicitAssign & "x";
			}
			stringExplicitAssignElapsedMs = getTickCount() - stringExplicitAssignStart;

			sbAppend = sb"";
			sbAppendStart = getTickCount();
			for ( i = 1; i <= iterations; i++ ) {
		     	sbAppend.append( "x" );
			}
			sbAppendElapsedMs = getTickCount() - sbAppendStart;

			sbConcatAssign = sb"";
			sbConcatAssignStart = getTickCount();
			for ( i = 1; i <= iterations; i++ ) {
			  	sbConcatAssign &= "x";
			}
			sbConcatAssignElapsedMs = getTickCount() - sbConcatAssignStart;

			sbExplicitAssign = sb"";
			sbExplicitAssignStart = getTickCount();
			for ( i = 1; i <= iterations; i++ ) {
		    	variables.sbExplicitAssign = variables.sbExplicitAssign & "x";
			}
			sbExplicitAssignElapsedMs = getTickCount() - sbExplicitAssignStart;

		""", context );
		// @formatter:on

		long	stringElapsedMs					= Long.parseLong( variables.get( Key.of( "stringElapsedMs" ) ).toString() );
		long	stringExplicitAssignElapsedMs	= Long.parseLong( variables.get( Key.of( "stringExplicitAssignElapsedMs" ) ).toString() );
		long	sbAppendElapsedMs				= Long.parseLong( variables.get( Key.of( "sbAppendElapsedMs" ) ).toString() );
		long	sbConcatAssignElapsedMs			= Long.parseLong( variables.get( Key.of( "sbConcatAssignElapsedMs" ) ).toString() );
		long	sbExplicitAssignElapsedMs		= Long.parseLong( variables.get( Key.of( "sbExplicitAssignElapsedMs" ) ).toString() );

		System.out.println( String.format( "String &= elapsed: %,d ms", stringElapsedMs ) );
		System.out.println( String.format( "String foo = foo & \"x\" elapsed: %,d ms", stringExplicitAssignElapsedMs ) );
		System.out.println( String.format( "StringBuilder .append() elapsed: %,d ms", sbAppendElapsedMs ) );
		System.out.println( String.format( "StringBuilder &= elapsed: %,d ms", sbConcatAssignElapsedMs ) );
		System.out.println( String.format( "StringBuilder foo = foo & \"x\" elapsed: %,d ms", sbExplicitAssignElapsedMs ) );
	}

	@DisplayName( "benchmark append performance for String vs StringBuilder over 1000 iterations" )
	@Test
	@Disabled
	public void FindBreakingPointOfStringConcatPerformance() {

		// @formatter:off
		instance.executeSource( """
			iterations = 100_000;

			str = "";
			x = "x";
			stringStart = getTickCount();
			for ( i = 1; i <= iterations; i++ ) {
				str = str & "x" & "x" & "x" & "x" & "x" & "x" & "x";
			}
			stringElapsedMs = getTickCount() - stringStart;

		""", context );
		// @formatter:on

		long stringElapsedMs = Long.parseLong( variables.get( Key.of( "stringElapsedMs" ) ).toString() );

		System.out.println( String.format( "String &= elapsed: %,d ms", stringElapsedMs ) );
	}

}
