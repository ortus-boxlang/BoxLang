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

package ortus.boxlang.runtime.components.system;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ExecuteComponentTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		testTextFile	= "src/test/resources/tmp/executeCTest/output.txt";
	static String		tmpDirectory	= "src/test/resources/tmp/executeCTest";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() throws IOException {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}
	}

	@Test
	public void testExecuteCF() throws IOException {
		instance.executeSource(
		    """
		    <cfexecute variable="result" exitCode="exitCode" name="java" arguments="--version" />
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertEquals( 0, variables.get( Key.exitCode ) );
		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
	}

	@Test
	public void testExecuteCFFailure() throws IOException {
		instance.executeSource(
		    """
		    <cfexecute variable="blah" exitCode="exitCode" errorVariable="result" name="java" arguments="--blah" />
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertNotEquals( 0, variables.get( Key.exitCode ) );
		String errorOutput = variables.getAsString( result );
		assertNotNull( errorOutput );
		assertTrue( variables.get( result ) instanceof String );
		assertTrue( errorOutput.length() > 0 );
	}

	@Test
	public void testExecuteBX() throws IOException {
		instance.executeSource(
		    """
		    <bx:execute variable="result" exitCode="exitCode" name="java" arguments="--version" />
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertEquals( 0, variables.get( Key.exitCode ) );
		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
	}

	@Test
	public void testExecuteBXFailure() throws IOException {
		instance.executeSource(
		    """
		    <bx:execute variable="blah" exitCode="exitCode" errorVariable="result" name="java" arguments="--blah" />
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertNotEquals( 0, variables.get( Key.exitCode ) );
		String errorOutput = variables.getAsString( result );
		assertNotNull( errorOutput );
		assertTrue( variables.get( result ) instanceof String );
		assertTrue( errorOutput.length() > 0 );

	}

}
