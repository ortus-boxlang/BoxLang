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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
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

public class PrintlnTest {

	static BoxRuntime				instance;
	static IBoxContext				context;
	static IScope					variables;
	static Key						result		= new Key( "result" );
	static ByteArrayOutputStream	outContent;
	static PrintStream				originalOut	= System.out;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		outContent	= new ByteArrayOutputStream();
		System.setOut( new PrintStream( outContent ) );
	}

	@AfterAll
	public static void teardown() {
		System.setOut( originalOut );
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
		outContent.reset();
	}

	@DisplayName( "It can print to the console" )
	@Test
	public void testPrint() {
		instance.executeSource(
		    """
		    println( "Hello World" )
		    """,
		    context );
		assertThat( new String( outContent.toByteArray() ) ).isEqualTo( "Hello World" + System.lineSeparator() );
	}

	@DisplayName( "It can print an array to the console" )
	@Test
	public void testPrintArray() {
		instance.executeSource(
		    """
		    a = [1,2,3]
		       println( a )
		       """,
		    context );
		assertEqualsNoWhiteSpaces( new String( outContent.toByteArray() ), "[1, 2, 3]" );
		// check the bytearray ends with System.lineSeparator()
		assertEquals( System.lineSeparator(),
		    new String( outContent.toByteArray() ).substring( new String( outContent.toByteArray() ).length() - System.lineSeparator().length() ) );
	}

	@DisplayName( "It can print a struct to the console" )
	@Test
	public void testPrintStruct() {
		instance.executeSource(
		    """
		    s = {a:1,b:2,c:3}
		       println( s )
		       """,
		    context );
		assertEqualsNoWhiteSpaces( new String( outContent.toByteArray() ), "{a:1, b:2, c:3}" );
		// check the bytearray ends with System.lineSeparator()
		assertEquals( System.lineSeparator(),
		    new String( outContent.toByteArray() ).substring( new String( outContent.toByteArray() ).length() - System.lineSeparator().length() ) );
	}

	protected void assertEqualsNoWhiteSpaces( String expected, String actual ) {
		assertEquals( expected.replaceAll( "[ \\t\\r\\n]", "" ), actual.replaceAll( "[ \\t\\r\\n]", "" ) );
	}

}
