package ortus.boxlang.runtime.application;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.services.MockInterceptor;

public class SessionTest {

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
	@DisplayName( "It tests that an error during session end does not prevent cleanup" )
	public void testSessionEndErrorHandling() {
		String				sessionID			= UUID.randomUUID().toString();
		Key					sessionKey			= Key.of( sessionID );
		String				applicationId		= UUID.randomUUID().toString();
		Key					applicationKey		= Key.of( applicationId );
		Application			testApplication		= new Application( applicationKey );
		InterceptorService	interceptorService	= instance.getInterceptorService();
		testApplication.start( context );
		Session session = new Session( sessionKey, testApplication, Duration.ofSeconds( 5 ) );
		assertThat( session.isShutdown() ).isFalse();
		// register an interception which will throw an error during shutdown
		DynamicObject	mockInterceptor	= DynamicObject.of( new MockInterceptor() );
		Key				stateKey		= Key.of( "onSessionEnd" );

		interceptorService.register(
		    mockInterceptor,
		    stateKey
		);

		assertThat( interceptorService.getState( stateKey ).size() ).isEqualTo( 1 );

		session.shutdown( testApplication.getStartingListener() );

		assertThat( session.isShutdown() ).isTrue();
	}

}
