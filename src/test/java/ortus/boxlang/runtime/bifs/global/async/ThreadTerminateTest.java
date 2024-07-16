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

public class ThreadTerminateTest {

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

	@DisplayName( "Can stop a thread" )
	@Test
	public void testCanStopThread() {
		// @formatter:off
		instance.executeSource(
		    """
				start = getTickCount()
		    	thread name="myThread" {
		    		sleep( 2000 )
		    	}
				threadTerminate( "myThread" )
				thread name="myThread" action="join" timeout=1000;
		    	result = myThread;
				println( result )
		    	totalTime = getTickCount() - start
		    """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( variables.getAsStruct( result ).get( Key.status ) ).isEqualTo( "TERMINATED" );
		assertThat( variables.getAsDouble( Key.of( "totalTime" ) ) < 1000 ).isTrue();
	}

}
