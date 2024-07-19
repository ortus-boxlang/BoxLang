package ortus.boxlang.runtime.bifs.global.async;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ThreadJoinTest {

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

	@DisplayName( "It can join all threads" )
	@Test
	public void testCanJOinAllThreads() {
		// @formatter:off
		instance.executeSource(
		    """
				thread name="myThread" {
					sleep( 2000 )
				}
				thread name="myThread2" {
					sleep( 2000 )
				}
		    	threadJoin()
				result = myThread;
				result2 = myThread2;
		    """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( variables.getAsStruct( result ).get( Key.status ) ).isEqualTo( "COMPLETED" );
		assertThat( variables.getAsStruct( Key.of( "result2" ) ).get( Key.status ) ).isEqualTo( "COMPLETED" );
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
		       	threadJoin( "myThread" )
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
				threadJoin( "myThread", 0 )
		       	result = myThread;
		    """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( variables.getAsStruct( result ).get( Key.status ) ).isEqualTo( "COMPLETED" );
	}

}
