
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

package ortus.boxlang.runtime.components.debug;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class TimerTest {

	static BoxRuntime			instance;
	ScriptingRequestBoxContext	context;
	IScope						variables;
	ByteArrayOutputStream		baos;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.setOut( new PrintStream( ( baos = new ByteArrayOutputStream() ), true ) );
		variables = context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF Timer as a comment" )
	@Test
	public void testComponentCF() {
		instance.executeSource(
		    """
		    <cftimer type="comment" label="TimeIt"><cfscript>sleep(1)</cfscript></cftimer>
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertTrue( baos.toString() instanceof String );
		assertTrue( baos.toString().length() > 0 );
		assertTrue( baos.toString().trim().contains( "<!-- TimeIt :" ) );
		System.out.println( baos.toString().trim() );
		assertTrue( baos.toString().trim().contains( "ms -->" ) );
	}

	@DisplayName( "It tests the BIF Timer as a comment in BL" )
	@Test
	public void testComponentBXM() {
		instance.executeSource(
		    """
		    <bx:timer type="comment" label="TimeIt"><bx:script>sleep(1)</bx:script></bx:timer>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( baos.toString().length() > 0 );
		assertTrue( baos.toString().trim().contains( "<!-- TimeIt :" ) );
		assertTrue( baos.toString().trim().contains( "ms -->" ) );
	}

	@DisplayName( "It tests the BIF Timer as a comment in BL Script" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    bx:timer type="comment" label="TimeIt"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( baos.toString().length() > 0 );
		assertTrue( baos.toString().trim().contains( "<!-- TimeIt :" ) );
		assertTrue( baos.toString().trim().contains( "ms -->" ) );
	}

	@DisplayName( "It tests the BIF Timer as a variable" )
	@Test
	public void testComponentVariable() {
		instance.executeSource(
		    """
		    bx:timer variable="result"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof Long );
		assertTrue( variables.getAsLong( result ) >= 1 );
	}

	@DisplayName( "It tests the BIF Timer as a variable with nanoseconds" )
	@Test
	public void testComponentVariableNS() {
		instance.executeSource(
		    """
		    bx:timer variable="result" unit="nano"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof Long );
		assertTrue( variables.getAsLong( result ) > 100 );
	}

	@DisplayName( "It tests the BIF Timer as a variable with microseconds" )
	@Test
	public void testComponentVariableMicroSec() {
		instance.executeSource(
		    """
		    bx:timer variable="result" unit="micro"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof Long );
		assertTrue( variables.getAsLong( result ) > 1000 );
	}

	@DisplayName( "It tests the BIF Timer as a variable with seconds" )
	@Test
	public void testComponentVariableSec() {
		instance.executeSource(
		    """
		    bx:timer variable="result" unit="second"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof Long );
		assertTrue( variables.getAsLong( result ) < 1 );
	}

	@DisplayName( "It tests the BIF Timer with only a label" )
	@Test
	public void testComponentLabelOnly() {
		instance.executeSource(
		    """
		    bx:timer label="TimeIt"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( baos.toString() instanceof String );
		assertTrue( baos.toString().length() > 0 );
		assertTrue( baos.toString().trim().contains( "TimeIt" ) );
		assertTrue( baos.toString().trim().contains( "ms" ) );
	}

	@DisplayName( "It tests the BIF Timer as a debug append" )
	@Test
	public void testComponentVariableDebug() {
		instance.executeSource(
		    """
		    bx:timer type="debug" label="TimeIt"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( ExpressionInterpreter.getVariable( context, "request.debugInfo", true ) instanceof IStruct );
		IStruct debugInfo = StructCaster.cast( ExpressionInterpreter.getVariable( context, "request.debugInfo", false ) );
		assertTrue( debugInfo.getAsString( Key.of( "TimeIt" ) ).contains( "ms" ) );
	}

	@DisplayName( "It tests the BIF Timer as inline in BL" )
	@Test
	public void testComponentInline() {
		instance.executeSource(
		    """
		    <bx:timer type="inline" label="TimeIt"><bx:script>sleep(1)</bx:script></bx:timer>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( baos.toString() instanceof String );
		assertTrue( baos.toString().length() > 0 );
		assertTrue( baos.toString().trim().contains( "TimeIt :" ) );
		assertTrue( baos.toString().trim().contains( "ms" ) );
	}

	@DisplayName( "It tests the BIF Timer as outline in BL" )
	@Test
	public void testComponentOutline() {
		instance.executeSource(
		    """
		    <bx:timer type="outline" label="TimeIt"><bx:script>sleep(1)</bx:script></bx:timer>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		System.out.println( baos.toString() );
		assertThat( baos.toString().length() > 0 ).isTrue();
		assertThat( baos.toString().trim().contains( "<fieldset" ) ).isTrue();
		assertThat( baos.toString().trim().contains( "<legend" ) ).isTrue();
		assertThat( baos.toString().trim().contains( "TimeIt:" ) ).isTrue();
		assertThat( baos.toString().trim().contains( "ms" ) ).isTrue();
	}

	@DisplayName( "It tests the Component StopWatch as a variable" )
	@Test
	public void testStopwatchVariable() {
		instance.executeSource(
		    """
		    bx:stopwatch variable="result"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof Long );
		assertTrue( variables.getAsLong( result ) >= 1 );
	}

	@DisplayName( "It tests the Component StopWatch with only a label" )
	@Test
	public void testStopWatchLabelOnly() {
		instance.executeSource(
		    """
		    bx:stopwatch label="TimeIt"{
		      	sleep(1);
		      }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( baos.toString().length() > 0 );
		assertTrue( baos.toString().trim().contains( "TimeIt" ) );
		assertTrue( baos.toString().trim().contains( "ms" ) );
	}

}
