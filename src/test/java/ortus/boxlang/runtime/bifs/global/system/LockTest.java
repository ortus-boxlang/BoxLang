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
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.LockException;

public class LockTest {

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

	@DisplayName( "It can get named lock" )
	@Test
	public void testLockNamed() {
		instance.executeSource(
		    """
		    lock name="mylock" timeout=10 {
		    	result = "bar";
		    }
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can get named lock" )
	@Test
	public void testLockNamedTag() {
		instance.executeSource(
		    """
		    <cflock name="mylock" timeout=10>
		    	<cfset result = "bar">
		    </cflock>
		    """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can get named lock" )
	@Test
	public void testLockNamedACFScript() {
		instance.executeSource(
		    """
		    cflock( name="mylock", timeout=10 ) {
		    	result = "bar";
		    }
		    """,
		    context, BoxScriptType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can get named lock readonly" )
	@Test
	public void testLockNamedReadonly() {
		instance.executeSource(
		    """
		    lock name="mylock" timeout=10 type="readonly" {
		    	result = "bar";
		    }
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can get named lock exclusive" )
	@Test
	public void testLockNamedExclusive() {
		instance.executeSource(
		    """
		    lock name="mylock" timeout=10 type="exclusive" {
		    	result = "bar";
		    }
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can get scope lock" )
	@Test
	public void testLockScope() {
		instance.executeSource(
		    """
		    lock scope="request" timeout=10 {
		    	result = "bar";
		    }
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can timeout" )
	@Test
	public void testTimeout() {
		assertThrows( LockException.class, () -> instance.executeSource(
		    """
		    thread {
		    	lock name="mylock" timeout=1 {
		    		sleep( 5000 );
		    	}
		    }
		    sleep( 500 );
		    lock name="mylock" timeout=1 {
		    	result = "bar";
		       }
		       """,
		    context ) );
	}

	@DisplayName( "It can timeout no error" )
	@Test
	@Disabled( "Timing issue Brad to fix" )
	public void testTimeoutNoError() {
		// @formatter:off
		instance.executeSource(
		    """
				thread {
					lock name="mylock" timeout=4 {
						sleep( 4000 );
					}
				}
				result="default"
				lock name="mylock" timeout=1 throwOnTimeout=false {
					result = "bar";
				}
		    """,
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( "default" );
	}

	@DisplayName( "It can readonly" )
	@Test
	public void testReadonly() {
		instance.executeSource(
		    """
		    variables.result = "";
		    thread {
		    	lock name="mylock" timeout=1 type="readonly" {
		       		sleep( 1000 );
		    		variables.result &= "inthread"
		       		sleep( 1000 );
		    	}
		    }
		       lock name="mylock" timeout=1 type="readonly" {
		    		variables.result &= "afterlock"
		       }
		       	sleep( 3000 );
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "afterlockinthread" );
	}

	@DisplayName( "It can exclusive" )
	@Test
	@Disabled( "Timing issue Brad to fix" )
	public void testExclusive() {
		// @formatter:off
		instance.executeSource(
		    """
				variables.result = "";
				thread {
					lock name="mylock" timeout=1 type="exclusive" {
						sleep( 1000 );
						variables.result &= "inthread"
						sleep( 1000 );
					}
				}
				// Give the thread a chance to start
				sleep( 1000 );
				lock name="mylock" timeout=10 type="exclusive" {
						variables.result &= "afterlock"
				}
				sleep( 3000 );
		    """,
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( "inthreadafterlock" );
	}

}
