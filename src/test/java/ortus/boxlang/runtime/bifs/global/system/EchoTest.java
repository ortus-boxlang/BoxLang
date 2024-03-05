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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

@Execution( ExecutionMode.SAME_THREAD )
public class EchoTest {

	static BoxRuntime				instance;
	IBoxContext						context;
	IScope							variables;
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

	@DisplayName( "It can echo to the console" )
	@Test
	public void testEcho() {
		instance.executeSource(
		    """
		    echo( "Hello World" )
		    """,
		    context );
		assertThat( new String( outContent.toByteArray() ) ).contains( "Hello World" );
	}

	@DisplayName( "It can echo multiple to the console" )
	@Test
	public void testEcho2() {
		instance.executeSource(
		    """
		    echo( "Hello World" )
		    echo( "Hello World" )
		    echo( "Hello World" )
		    echo( "Hello World" )
		    echo( "Hello World" )
		    """,
		    context );
		assertThat( new String( outContent.toByteArray() ) ).contains( "Hello WorldHello WorldHello WorldHello WorldHello World" );
	}

	@DisplayName( "It can echo from function" )
	@Test
	public void testEchoFunction() {
		instance.executeSource(
		    """

		         echo( "pre func" )
		      // output defaults to false in BoxLang
		       function foo() {
		           echo( "Hello World" )
		       }
		    foo()
		         echo( "post func" )
		         """,
		    context );
		assertThat( new String( outContent.toByteArray() ) ).contains( "pre funcpost func" );
	}

	@DisplayName( "It can echo from function output=true" )
	@Test
	public void testEchoFunctionOutput() {
		instance.executeSource(
		    """

		         echo( "pre func" )
		      @output true
		       function foo() output=true {
		           echo( "Hello World" )
		       }
		    foo()
		         echo( "post func" )
		         """,
		    context );
		assertThat( new String( outContent.toByteArray() ) ).contains( "pre funcHello Worldpost func" );
	}

	protected void assertEqualsNoWhiteSpaces( String expected, String actual ) {
		assertEquals( expected.replaceAll( "[ \\t\\r\\n]", "" ), actual.replaceAll( "[ \\t\\r\\n]", "" ) );
	}

}
