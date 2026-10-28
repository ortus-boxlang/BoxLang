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
package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ContainerBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.InterceptorPool;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

class TransactionEventsTest {

	private static BoxRuntime			runtime;
	private static final Key			SAVEPOINT	= Key.of( "eventSavepoint" );
	private ScriptingRequestBoxContext	requestContext;
	private IBoxContext					context;
	private Transaction					transaction;
	private BoxConnection				connection;

	@BeforeAll
	static void initialize() {
		runtime = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	void setup() throws SQLException {
		this.requestContext	= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		// A nested context distinguishes the caller from both the request and runtime contexts.
		this.context		= new ContainerBoxContext( this.requestContext );
		this.connection		= mock( BoxConnection.class );
		DataSource datasource = mock( DataSource.class );
		when( datasource.getBoxConnection() ).thenReturn( this.connection );
		when( this.connection.setSavepoint( any( String.class ) ) ).thenReturn( mock( Savepoint.class ) );
		this.transaction = new Transaction( this.context, datasource );
	}

	@ParameterizedTest
	@EnumSource( value = BoxEvent.class, names = {
	    "ON_TRANSACTION_BEGIN", "ON_TRANSACTION_ACQUIRE", "ON_TRANSACTION_COMMIT", "ON_TRANSACTION_ROLLBACK",
	    "ON_TRANSACTION_SET_SAVEPOINT", "ON_TRANSACTION_RELEASE_SAVEPOINT", "ON_TRANSACTION_END", "ON_TRANSACTION_RELEASE"
	} )
	void announcesWithLocalContextInBothPools( BoxEvent event ) {
		if ( event != BoxEvent.ON_TRANSACTION_ACQUIRE ) {
			this.transaction.getBoxConnection();
		}
		this.transaction.setSavepoint( SAVEPOINT );

		InterceptorPool	applicationPool	= this.requestContext.getApplicationListener().getInterceptorPool();
		InterceptorPool	runtimePool		= runtime.getInterceptorService();
		boolean			hadRuntimeState	= runtimePool.hasState( event.key() );
		IReferenceable	listener		= mock( IReferenceable.class );
		DynamicObject	observer		= DynamicObject.of( listener );
		applicationPool.register( observer, event.key() );
		runtimePool.register( observer, event.key() );
		try {
			switch ( event ) {
				case ON_TRANSACTION_BEGIN -> this.transaction.begin();
				case ON_TRANSACTION_ACQUIRE -> this.transaction.getBoxConnection();
				case ON_TRANSACTION_COMMIT -> this.transaction.commit();
				case ON_TRANSACTION_ROLLBACK -> this.transaction.rollback( SAVEPOINT );
				case ON_TRANSACTION_SET_SAVEPOINT -> this.transaction.setSavepoint( Key.of( "second" ) );
				case ON_TRANSACTION_RELEASE_SAVEPOINT -> this.transaction.releaseSavepoint( SAVEPOINT );
				case ON_TRANSACTION_END, ON_TRANSACTION_RELEASE -> this.transaction.end();
				default -> throw new IllegalArgumentException( "Unexpected event: " + event );
			}

			ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass( Object[].class );
			verify( listener, times( 2 ) ).dereferenceAndInvoke( same( this.context ), eq( event.key() ), arguments.capture(), eq( false ) );
			for ( Object[] invocation : arguments.getAllValues() ) {
				IStruct data = ( IStruct ) invocation[ 0 ];
				assertThat( invocation[ 1 ] ).isSameInstanceAs( this.context );
				assertThat( data.get( Key.context ) ).isSameInstanceAs( this.context );
				assertThat( data.get( "transaction" ) ).isSameInstanceAs( this.transaction );
				if ( event != BoxEvent.ON_TRANSACTION_BEGIN ) {
					assertThat( data.get( "connection" ) ).isSameInstanceAs( this.connection );
				}
				if ( event == BoxEvent.ON_TRANSACTION_SET_SAVEPOINT ) {
					assertThat( data.getAsString( Key.savepoint ) ).isEqualTo( "second" );
				} else if ( event == BoxEvent.ON_TRANSACTION_ROLLBACK || event == BoxEvent.ON_TRANSACTION_RELEASE_SAVEPOINT ) {
					assertThat( data.getAsString( Key.savepoint ) ).isEqualTo( SAVEPOINT.getName() );
				}
			}
		} finally {
			applicationPool.unregister( observer, event.key() );
			runtimePool.unregister( observer, event.key() );
			if ( !hadRuntimeState ) {
				runtimePool.removeState( event.key() );
			}
		}
	}

	@Test
	void skipsReleasePayloadWithoutListeners() {
		AtomicInteger	conversions	= new AtomicInteger();

		Key				savepoint	= new Key( "lazySavepoint" ) {

										@Override
										public String toString() {
											conversions.incrementAndGet();
											return super.toString();
										}
									};
		assertThat( runtime.getInterceptorService().hasState( BoxEvent.ON_TRANSACTION_RELEASE_SAVEPOINT.key() ) ).isFalse();
		this.transaction.setSavepoint( savepoint );
		conversions.set( 0 );
		this.transaction.releaseSavepoint( savepoint );
		assertThat( conversions.get() ).isEqualTo( 0 );

		// An application-only listener must still receive the lazily produced payload.
		this.requestContext.getApplicationListener().getInterceptorPool().register( data -> {
			assertThat( data.getAsString( Key.savepoint ) ).isEqualTo( "lazySavepoint" );
			assertThat( data.get( "connection" ) ).isNull();
			return false;
		}, BoxEvent.ON_TRANSACTION_RELEASE_SAVEPOINT.key() );
		this.transaction.setSavepoint( savepoint );
		conversions.set( 0 );
		this.transaction.releaseSavepoint( savepoint );
		assertThat( conversions.get() ).isEqualTo( 1 );
	}

	@Test
	void announcesWithoutARequestContext() {
		IBoxContext		localContext		= new ContainerBoxContext( runtime.getRuntimeContext() );
		Transaction		localTransaction	= new Transaction( localContext, null );
		IReferenceable	listener			= mock( IReferenceable.class );
		DynamicObject	observer			= DynamicObject.of( listener );
		InterceptorPool	pool				= runtime.getInterceptorService();
		Key				event				= BoxEvent.ON_TRANSACTION_BEGIN.key();
		boolean			hadState			= pool.hasState( event );
		pool.register( observer, event );
		try {
			localTransaction.begin();
			verify( listener ).dereferenceAndInvoke( same( localContext ), eq( event ), any( Object[].class ), eq( false ) );
		} finally {
			pool.unregister( observer, event );
			if ( !hadState ) {
				pool.removeState( event );
			}
		}
	}
}
