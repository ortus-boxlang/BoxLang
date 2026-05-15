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
import ortus.boxlang.runtime.types.Struct;

class StructListenerTest {

	@Test
	@DisplayName( "onEvent invokes matching per-kind handler" )
	void testOnEventInvokesPerKindHandler() {
		ortus.boxlang.runtime.types.Function	onCreate	= mock( ortus.boxlang.runtime.types.Function.class );
		IBoxContext								baseCtx		= mock( IBoxContext.class );
		IBoxContext								threadCtx	= mock( IBoxContext.class );
		StructListener							listener	= new StructListener( Struct.ofNonConcurrent( Key.onCreate, onCreate ) );
		WatcherContext							ctx			= new WatcherContext( baseCtx, null );
		WatcherEvent							event		= new WatcherEvent(
		    WatcherEvent.Kind.CREATED,
		    Path.of( "/tmp/watched/file.txt" ),
		    Path.of( "file.txt" ),
		    Path.of( "/tmp/watched" ),
		    Instant.parse( "2026-04-07T12:00:00Z" )
		);

		when( threadCtx.invokeFunction( eq( onCreate ), any( Object[].class ) ) ).thenReturn( null );

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
		verify( threadCtx, times( 1 ) ).invokeFunction( eq( onCreate ), argsCaptor.capture() );
		Object[] args = argsCaptor.getValue();
		assertThat( args.length ).isEqualTo( 1 );
		assertThat( args[ 0 ] ).isInstanceOf( IStruct.class );
		assertThat( ( ( IStruct ) args[ 0 ] ).getAsString( Key.kind ) ).isEqualTo( "created" );
	}

	@Test
	@DisplayName( "onEvent invokes both per-kind and global onEvent handlers when both exist" )
	void testOnEventInvokesBothSpecificAndGlobalHandlers() {
		ortus.boxlang.runtime.types.Function	onDelete	= mock( ortus.boxlang.runtime.types.Function.class );
		ortus.boxlang.runtime.types.Function	onEvent		= mock( ortus.boxlang.runtime.types.Function.class );
		IBoxContext								baseCtx		= mock( IBoxContext.class );
		IBoxContext								threadCtx	= mock( IBoxContext.class );
		StructListener							listener	= new StructListener( Struct.ofNonConcurrent( Key.onDelete, onDelete, Key.onEvent, onEvent ) );
		WatcherContext							ctx			= new WatcherContext( baseCtx, null );
		WatcherEvent							event		= new WatcherEvent(
		    WatcherEvent.Kind.DELETED,
		    Path.of( "/tmp/watched/file.txt" ),
		    Path.of( "file.txt" ),
		    Path.of( "/tmp/watched" ),
		    Instant.parse( "2026-04-07T12:00:00Z" )
		);

		when( threadCtx.invokeFunction( eq( onDelete ), any( Object[].class ) ) ).thenReturn( null );
		when( threadCtx.invokeFunction( eq( onEvent ), any( Object[].class ) ) ).thenReturn( null );

		try ( MockedStatic<ThreadBoxContext> mockedStatic = Mockito.mockStatic( ThreadBoxContext.class ) ) {
			mockedStatic.when( () -> ThreadBoxContext.runInContext( eq( baseCtx ), eq( true ), any() ) )
			    .thenAnswer( invocation -> {
				    Function<IBoxContext, Object> runnable = invocation.getArgument( 2 );
				    return runnable.apply( threadCtx );
			    } );

			listener.onEvent( event, ctx );

			mockedStatic.verify( () -> ThreadBoxContext.runInContext( eq( baseCtx ), eq( true ), any() ), times( 2 ) );
		}

		verify( threadCtx, times( 1 ) ).invokeFunction( eq( onDelete ), any( Object[].class ) );
		verify( threadCtx, times( 1 ) ).invokeFunction( eq( onEvent ), any( Object[].class ) );
	}

	@Test
	@DisplayName( "onEvent invokes global onEvent handler when no per-kind handler exists" )
	void testOnEventUsesGlobalHandlerAsFallback() {
		ortus.boxlang.runtime.types.Function	onEvent		= mock( ortus.boxlang.runtime.types.Function.class );
		IBoxContext								baseCtx		= mock( IBoxContext.class );
		IBoxContext								threadCtx	= mock( IBoxContext.class );
		StructListener							listener	= new StructListener( Struct.ofNonConcurrent( Key.onEvent, onEvent ) );
		WatcherContext							ctx			= new WatcherContext( baseCtx, null );
		WatcherEvent							event		= new WatcherEvent( Instant.parse( "2026-04-07T12:00:00Z" ) );

		when( threadCtx.invokeFunction( eq( onEvent ), any( Object[].class ) ) ).thenReturn( null );

		try ( MockedStatic<ThreadBoxContext> mockedStatic = Mockito.mockStatic( ThreadBoxContext.class ) ) {
			mockedStatic.when( () -> ThreadBoxContext.runInContext( eq( baseCtx ), eq( true ), any() ) )
			    .thenAnswer( invocation -> {
				    Function<IBoxContext, Object> runnable = invocation.getArgument( 2 );
				    return runnable.apply( threadCtx );
			    } );

			listener.onEvent( event, ctx );

			mockedStatic.verify( () -> ThreadBoxContext.runInContext( eq( baseCtx ), eq( true ), any() ), times( 1 ) );
		}

		verify( threadCtx, times( 1 ) ).invokeFunction( eq( onEvent ), any( Object[].class ) );
	}

	@Test
	@DisplayName( "onError invokes onError handler when present" )
	void testOnErrorInvokesHandlerWhenPresent() {
		ortus.boxlang.runtime.types.Function	onError		= mock( ortus.boxlang.runtime.types.Function.class );
		IBoxContext								baseCtx		= mock( IBoxContext.class );
		IBoxContext								threadCtx	= mock( IBoxContext.class );
		StructListener							listener	= new StructListener( Struct.ofNonConcurrent( Key.onError, onError ) );
		WatcherContext							ctx			= new WatcherContext( baseCtx, null );
		Exception								error		= new RuntimeException( "boom" );

		when( threadCtx.invokeFunction( eq( onError ), any( Object[].class ) ) ).thenReturn( null );

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
		verify( threadCtx, times( 1 ) ).invokeFunction( eq( onError ), argsCaptor.capture() );
		Object[] args = argsCaptor.getValue();
		assertThat( args.length ).isEqualTo( 1 );
		assertThat( args[ 0 ] ).isSameInstanceAs( error );
	}

	@Test
	@DisplayName( "onError does nothing when handler is missing" )
	void testOnErrorNoHandler() {
		IBoxContext		baseCtx		= mock( IBoxContext.class );
		StructListener	listener	= new StructListener( Struct.ofNonConcurrent() );
		WatcherContext	ctx			= new WatcherContext( baseCtx, null );
		Exception		error		= new RuntimeException( "boom" );

		try ( MockedStatic<ThreadBoxContext> mockedStatic = Mockito.mockStatic( ThreadBoxContext.class ) ) {
			listener.onError( error, ctx );
			mockedStatic.verifyNoInteractions();
		}
	}

	@Test
	@DisplayName( "onEvent ignores non-function handler values" )
	void testOnEventIgnoresNonFunctionHandlers() {
		IBoxContext		baseCtx		= mock( IBoxContext.class );
		StructListener	listener	= new StructListener( Struct.ofNonConcurrent( Key.onModify, "not-a-function" ) );
		WatcherContext	ctx			= new WatcherContext( baseCtx, null );
		WatcherEvent	event		= new WatcherEvent(
		    WatcherEvent.Kind.MODIFIED,
		    Path.of( "/tmp/watched/file.txt" ),
		    Path.of( "file.txt" ),
		    Path.of( "/tmp/watched" ),
		    Instant.parse( "2026-04-07T12:00:00Z" )
		);

		try ( MockedStatic<ThreadBoxContext> mockedStatic = Mockito.mockStatic( ThreadBoxContext.class ) ) {
			listener.onEvent( event, ctx );
			mockedStatic.verifyNoInteractions();
		}
	}
}
