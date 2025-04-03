package ortus.boxlang.runtime.bifs.global.scheduler;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class SchedulersTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	public void testBasicEmptyScheduling() {
		// @formatter:off
		instance.executeSource(
		    """
		   		scheduler = schedulerStart( "src.test.bx.Scheduler" )

				list = schedulerList()
				println( list )

				all = schedulerGetAll()
				println( all )

				result = schedulerGet( "My-Scheduler" )

				schedulerRestart( "My-Scheduler" )

				allStats = schedulerStats()
				println( allStats )

				targetStats = schedulerStats( "My-Scheduler" )
				println( targetStats )

				schedulerShutdown( "My-Scheduler", true )
		    """,
		    context );
		// @formatter:on

		IScheduler	scheduler	= ( IScheduler ) variables.get( Key.of( "scheduler" ) );
		Array		list		= variables.getAsArray( Key.of( "list" ) );
		assertThat( list ).hasSize( 1 );
		IStruct all = ( IStruct ) variables.get( Key.of( "all" ) );
		assertThat( all ).hasSize( 1 );
		assertThat( scheduler ).isEqualTo( ( IScheduler ) variables.get( result ) );
	}

}
