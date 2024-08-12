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

package ortus.boxlang.runtime.components.async;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class ThreadTest {

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

	@DisplayName( "It can thread tag" )
	@Test
	public void testCanthreadTag() {

		// @formatter:off
		instance.executeSource(
		    """
				<cfthread name="myThread" foo="bar">
					<cfset printLn( "thread is done!" )>
					<cfset sleep( 1000 )>
					</cfthread>
				<cfset printLn( "thread tag done" )>
				<cfset sleep( 2000 ) >
				<cfset printLn( "test is done done" )>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
	}

	@DisplayName( "It can thread BL tag" )
	@Test
	public void testCanthreadBLTag() {
		// @formatter:off
		instance.executeSource(
		    """
				<bx:thread name="myThread" foo="bar">
					<bx:set printLn( "thread is done!" )>
					<bx:set sleep( 1000 )>
				</bx:thread>
				<bx:set printLn( "thread tag done" )>
				<bx:set sleep( 2000 ) >
				<bx:set printLn( "test is done done" )>
			""",
		    context, BoxSourceType.BOXTEMPLATE );
			// @formatter:on
	}

	@DisplayName( "It can thread script" )
	@Test
	public void testCanthreadScript() {
		// @formatter:off
		instance.executeSource(
		    """
		    thread name="myThread" foo="bar"{
		    	printLn( "thread is done!" )
		    	sleep( 1000 )
		    }
		    printLn( "thread tag done" )
		    sleep( 2000 )
		    printLn( "test is done done" )
		            """,
		    context );
		// @formatter:on
	}

	@DisplayName( "It can thread ACF script" )
	@Test
	public void testCanthreadACFScript() {
		// @formatter:off
		instance.executeSource(
		    """
		    cfthread( name="myThread", foo="bar" ){
		    	printLn( "thread is done!" )
		    	sleep( 1000 )
		    }
		    printLn( "thread tag done" )
		    sleep( 2000 )
		    printLn( "test is done done" )
		         """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
	}

	@Test
	public void testHasTheadScope() {
		// @formatter:off
		instance.executeSource(
		    """
		    thread name="myThread" foo="bar"{
		    	thread.insideThread = "yup";
		    	myThread.insideThread2 = "yeah";
		    	bxthread.myThread.insideThread3 = "yep";
		    	sleep( 1000 )
		    }
		    sleep( 500 )
		    result1 = bxthread;
		    result2 = myThread;
		    result3 = bxthread.myThread;
		                 """,
		    context );
		// @formatter:on

		assertThat( variables.get( Key.of( "result1" ) ) ).isInstanceOf( IStruct.class );
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( IStruct.class );
		assertThat( variables.get( Key.of( "result3" ) ) ).isInstanceOf( IStruct.class );

		IStruct	result1	= variables.getAsStruct( Key.of( "result1" ) );
		IStruct	result2	= variables.getAsStruct( Key.of( "result2" ) );
		IStruct	result3	= variables.getAsStruct( Key.of( "result3" ) );

		assertThat( result1.get( Key.of( "myThread" ) ) ).isInstanceOf( IStruct.class );
		assertThat( result1.getAsStruct( Key.of( "myThread" ) ).get( Key.of( "insideThread" ) ) ).isEqualTo( "yup" );
		assertThat( result1.getAsStruct( Key.of( "myThread" ) ).get( Key.of( "insideThread2" ) ) ).isEqualTo( "yeah" );
		assertThat( result1.getAsStruct( Key.of( "myThread" ) ).get( Key.of( "insideThread3" ) ) ).isEqualTo( "yep" );

		assertThat( result2.get( Key.of( "insideThread" ) ) ).isEqualTo( "yup" );
		assertThat( result2.get( Key.of( "insideThread2" ) ) ).isEqualTo( "yeah" );
		assertThat( result2.get( Key.of( "insideThread3" ) ) ).isEqualTo( "yep" );

		assertThat( result3.get( Key.of( "insideThread" ) ) ).isEqualTo( "yup" );
		assertThat( result3.get( Key.of( "insideThread2" ) ) ).isEqualTo( "yeah" );
		assertThat( result3.get( Key.of( "insideThread3" ) ) ).isEqualTo( "yep" );
	}

	@DisplayName( "It can join thread no timeout" )
	@Test
	public void testCanJoinThreadNoTimeout() {
		// @formatter:off
		instance.executeSource(
		    """
		       thread name="myThread" {
		    	   sleep( 2000 )
		       }
		    thread name="myThread" action="join";
		       result = myThread;
		    		""",
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( variables.getAsStruct( result ).get( Key.status ) ).isEqualTo( "COMPLETED" );
	}

	@DisplayName( "It can join thread zero timeout" )
	@Test
	public void testCanJoinThreadZeroTimeout() {
		// @formatter:off
		instance.executeSource(
		    """
		       thread name="myThread" {
		    	   sleep( 2000 )
		       }
		    thread name="myThread" action="join" timeout=0;
		       result = myThread;
		    		""",
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( variables.getAsStruct( result ).get( Key.status ) ).isEqualTo( "COMPLETED" );
	}

	@DisplayName( "It can join thread postive timeout" )
	@Test
	public void testCanJoinThreadPositiveTimeout() {
		// @formatter:off
		instance.executeSource(
		    """
		    start = getTickCount()
		    	 thread name="myThread" {
		    		 sleep( 2000 )
		    	 }
		    	 thread name="myThread2" {
		    		 sleep( 2000 )
		    	 }
		    	 thread name="myThread3" {
		    		 sleep( 2000 )
		    	 }
		      thread name="myThread,myThread2,myThread3" action="join" timeout=1000;
		    	 result = myThread;
		      totalTime = getTickCount() - start
		    		  """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( variables.getAsStruct( result ).get( Key.status ) ).isEqualTo( "WAITING" );
		assertThat( variables.getAsNumber( Key.of( "totalTime" ) ).doubleValue() > 1000 ).isTrue();
		assertThat( variables.getAsNumber( Key.of( "totalTime" ) ).doubleValue() < 2000 ).isTrue();
	}

	@DisplayName( "It can stop thread" )
	@Test
	@Disabled
	public void testCanStopThread() {
		// @formatter:off
		instance.executeSource(
		    """
		    	start = getTickCount()
		    	thread name="myThread" {
		    		sleep( 2000 )
		    	}
		    	thread name="myThread" action="terminate";
		      	thread name="myThread" action="join" timeout=1000;
		    	result = myThread;
		    	totalTime = getTickCount() - start
		    """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on

		assertThat( variables.getAsStruct( result ).get( Key.status ) ).isEqualTo( "TERMINATED" );
		assertThat( variables.getAsNumber( Key.of( "totalTime" ) ).doubleValue() < 1000 ).isTrue();
	}

	@DisplayName( "It can access the this scope inside a thread created by a class" )
	@Test
	public void testThreadThisScope() {
		// @formatter:off
		instance.executeSource(
			"""
				myThreadingClass = new src.test.bx.MyThreadingClass()
				result = myThreadingClass.execute();
			""",
			context, BoxSourceType.CFSCRIPT );
		IStruct asStruct = variables.getAsStruct( result );
		assertThat( asStruct.get( Key.error ) ).isNull();
	}

}
