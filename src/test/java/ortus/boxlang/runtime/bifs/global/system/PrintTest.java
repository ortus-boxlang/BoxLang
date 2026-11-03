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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class PrintTest {

	static BoxRuntime				instance;
	static IBoxContext				context;
	static IScope					variables;
	static Key						result		= new Key( "result" );
	static ByteArrayOutputStream	outContent;
	static PrintStream				originalOut	= System.out;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		outContent	= new ByteArrayOutputStream();
		System.setOut( new PrintStream( outContent ) );
	}

	@AfterAll
	public static void teardown() {
		System.setOut( originalOut );

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		outContent.reset();
	}

	@DisplayName( "It can print to the console" )
	@Test
	public void testPrint() {
		instance.executeSource(
		    """
		    print( "Hello World" )
		    """,
		    context );
		assertThat( new String( outContent.toByteArray() ) ).contains( "Hello World" );
	}

	@DisplayName( "It can print an array to the console" )
	@Test
	public void testPrintArray() {
		instance.executeSource(
		    """
		    a = [1,2,3]
		       print( a )
		       """,
		    context );
		assertEqualsNoWhiteSpaces( new String( outContent.toByteArray() ), "[1, 2, 3]" );
	}

	@DisplayName( "It can print a struct to the console" )
	@Test
	public void testPrintStruct() {
		instance.executeSource(
		    """
		    s = {a:1,b:2,c:3}
		       print( s )
		       """,
		    context );
		assertEqualsNoWhiteSpaces( new String( outContent.toByteArray() ), "{a:1, b:2, c:3}" );
	}

	protected void assertEqualsNoWhiteSpaces( String expected, String actual ) {
		assertThat( expected.replaceAll( "[ \\t\\r\\n]", "" ) ).contains( actual.replaceAll( "[ \\t\\r\\n]", "" ) );
	}

	@DisplayName( "It can print Java native array to the console" )
	@Test
	public void testPrintJavaArray() {
		instance.executeSource(
		    """
		    print( "abc".getBytes() )
		    """,
		    context );
		assertThat( new String( outContent.toByteArray() ).replaceAll( "[ \\t]", "" ) ).contains( "[97,98,99]" );
	}

	@DisplayName( "It can print Java double without scientific notation" )
	@Test
	public void testPrintJavaDouble() {
		instance.executeSource(
		    """
		       import java.lang.Double;
		       myDoubleInstance = Double.valueOf( 15852073 );
		    print( myDoubleInstance );
		    """,
		    context );
		assertThat( new String( outContent.toByteArray() ) ).contains( "15852073" );
	}

	@Test
	public void testMultipleArguments() {
		context.getRequestContext().setOut( new PrintStream( outContent ) );
		instance.executeSource( """
		                        print("Name:", "Peter", 21, "Peter");
		                        """, context );
		assertThat( new String( outContent.toByteArray() ) ).isEqualTo( String.join( " ", "Name:", "Peter", "21", "Peter" ) + "" );
	}

	@Test
	public void testEmptyArguments() {
		context.getRequestContext().setOut( new PrintStream( outContent ) );
		instance.executeSource( """
		                        print("", "A", "");
		                        """, context );
		assertThat( new String( outContent.toByteArray() ) ).isEqualTo( " " + "A" + " " + "" );
	}

	@Test
	public void testNamedMessage() {
		context.getRequestContext().setOut( new PrintStream( outContent ) );
		instance.executeSource( """
		                        print(message="Hello");
		                        """, context );
		assertThat( new String( outContent.toByteArray() ) ).isEqualTo( "Hello" + "" );
	}

	@Test
	public void testJavaArrayAmongArguments() {
		context.getRequestContext().setOut( new PrintStream( outContent ) );
		instance.executeSource( """
		                        print("bytes:", "abc".getBytes(), "end");
		                        """, context );
		assertThat( new String( outContent.toByteArray() ) ).isEqualTo( String.join( " ", "bytes:", "[97, 98, 99]", "end" ) + "" );
	}

	@Test
	public void testArgumentCollection() {
		context.getRequestContext().setOut( new PrintStream( outContent ) );
		instance.executeSource( """
		                        print(argumentCollection=["A", "B", 3]);
		                        """, context );
		assertThat( new String( outContent.toByteArray() ) ).isEqualTo( String.join( " ", "A", "B", "3" ) + "" );
	}

	@Test
	public void testConsecutiveCalls() {
		context.getRequestContext().setOut( new PrintStream( outContent ) );
		instance.executeSource( """
		                        print("A", "B"); print("C");
		                        """, context );
		assertThat( new String( outContent.toByteArray() ) ).isEqualTo( "A BC" );
	}

	@Test
	public void testMessageStillRequired() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource( "print();", context ) );
	}

}
