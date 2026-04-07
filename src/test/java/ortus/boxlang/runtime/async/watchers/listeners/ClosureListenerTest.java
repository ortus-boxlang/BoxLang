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
package ortus.boxlang.runtime.async.watchers.listeners;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import ortus.boxlang.runtime.async.watchers.WatcherContext;
import ortus.boxlang.runtime.async.watchers.WatcherEvent;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

class ClosureListenerTest {

	@Test
	@DisplayName( "onEvent delegates to onEvent function with event struct" )
	void testOnEventInvokesHandlerWithStruct() {
		ortus.boxlang.runtime.types.Function	onEventFn	= mock( ortus.boxlang.runtime.types.Function.class );
		IBoxContext								baseCtx		= mock( IBoxContext.class );
		IBoxContext								threadCtx	= mock( IBoxContext.class );
		ClosureListener							listener	= new ClosureListener( onEventFn );
		WatcherContext							ctx			= new WatcherContext( baseCtx, null );
		WatcherEvent							event		= new WatcherEvent(
		    WatcherEvent.Kind.CREATED,
		    Path.of( "/tmp/watched/file.txt" ),
		    Path.of( "file.txt" ),
		    Path.of( "/tmp/watched" ),
		    Instant.parse( "2026-04-07T12:00:00Z" )
		);

		when( threadCtx.invokeFunction( eq( onEventFn ), any( Object[].class ) ) ).thenReturn( null );

		try ( MockedStatic<ThreadBoxContext> mockedStatic = Mockito.mockStatic( ThreadBoxContext.class ) ) {
			mockedStatic.when( () -> ThreadBoxContext.runInContext( eq( baseCtx ), eq( true ), any() ) )
			    .thenAnswer( invocation -> {
				    Function<IBoxContext, Object> runnable = invocation.getArgument( 2 );
				    return runnable.apply( threadCtx );
			    } );

			listener.onEvent( event, ctx );

			mockedStatic.verify( () -> ThreadBoxContext.runInContext( eq( baseCtx ), eq( true ), any() ), times( 1 ) );
		}

		ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass( Object[].class );
		verify( threadCtx, times( 1 ) ).invokeFunction( eq( onEventFn ), argsCaptor.capture() );

		Object[] args = argsCaptor.getValue();
		assertThat( args.length ).isEqualTo( 1 );
		assertThat( args[ 0 ] ).isInstanceOf( IStruct.class );
		IStruct eventStruct = ( IStruct ) args[ 0 ];
		assertThat( eventStruct.getAsString( Key.kind ) ).isEqualTo( "created" );
		assertThat( eventStruct.getAsString( Key.path ) ).isEqualTo( "/tmp/watched/file.txt" );
	}

	@Test
	@DisplayName( "onError does nothing when no error handler is configured" )
	void testOnErrorNoHandler() {
		ortus.boxlang.runtime.types.Function	onEventFn	= mock( ortus.boxlang.runtime.types.Function.class );
		IBoxContext								baseCtx		= mock( IBoxContext.class );
		ClosureListener							listener	= new ClosureListener( onEventFn );
		WatcherContext							ctx			= new WatcherContext( baseCtx, null );
		Exception								error		= new RuntimeException( "boom" );

		try ( MockedStatic<ThreadBoxContext> mockedStatic = Mockito.mockStatic( ThreadBoxContext.class ) ) {
			listener.onError( error, ctx );
			mockedStatic.verifyNoInteractions();
		}

		verifyNoInteractions( onEventFn );
	}

	@Test
	@DisplayName( "onError delegates to onError function with exception" )
	void testOnErrorInvokesErrorHandler() {
		ortus.boxlang.runtime.types.Function	onEventFn	= mock( ortus.boxlang.runtime.types.Function.class );
		ortus.boxlang.runtime.types.Function	onErrorFn	= mock( ortus.boxlang.runtime.types.Function.class );
		IBoxContext								baseCtx		= mock( IBoxContext.class );
		IBoxContext								threadCtx	= mock( IBoxContext.class );
		ClosureListener							listener	= new ClosureListener( onEventFn, onErrorFn );
		WatcherContext							ctx			= new WatcherContext( baseCtx, null );
		Exception								error		= new RuntimeException( "boom" );

		when( threadCtx.invokeFunction( eq( onErrorFn ), any( Object[].class ) ) ).thenReturn( null );

		try ( MockedStatic<ThreadBoxContext> mockedStatic = Mockito.mockStatic( ThreadBoxContext.class ) ) {
			mockedStatic.when( () -> ThreadBoxContext.runInContext( eq( baseCtx ), eq( true ), any() ) )
			    .thenAnswer( invocation -> {
				    Function<IBoxContext, Object> runnable = invocation.getArgument( 2 );
				    return runnable.apply( threadCtx );
			    } );

			listener.onError( error, ctx );

			mockedStatic.verify( () -> ThreadBoxContext.runInContext( eq( baseCtx ), eq( true ), any() ), times( 1 ) );
		}

		ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass( Object[].class );
		verify( threadCtx, times( 1 ) ).invokeFunction( eq( onErrorFn ), argsCaptor.capture() );
		Object[] args = argsCaptor.getValue();
		assertThat( args.length ).isEqualTo( 1 );
		assertThat( args[ 0 ] ).isSameInstanceAs( error );
	}
}
