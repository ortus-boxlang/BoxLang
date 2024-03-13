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

package ortus.boxlang.runtime.components.threading;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxScriptType;
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
		    context, BoxScriptType.CFMARKUP );
	}

	@DisplayName( "It can thread script" )
	@Test
	public void testCanthreadScript() {

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
	}

	@DisplayName( "It can thread ACF script" )
	@Test
	public void testCanthreadACFScript() {

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
		    context );
	}

	@Test
	public void testHasTheadScope() {

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

}
