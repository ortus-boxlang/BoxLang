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
package ortus.boxlang.runtime.events;

import static com.google.common.truth.Truth.assertThat;

import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * BL-2759: announcements made without a context run on the current request when there is one,
 * and on the runtime context otherwise.
 */
public class InterceptorRequestContextTest {

	static BoxRuntime			instance;
	static InterceptorService	interceptorService;
	static final Key			STATE	= Key.of( "onBL2759" );
	IBoxContext					context;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		interceptorService	= instance.getInterceptorService();
		interceptorService.registerInterceptionPoint( STATE );
		instance.executeSource(
		    "boxRegisterInterceptor( new src.test.bx.BL2759Interceptor(), \"onBL2759\" )",
		    new ScriptingRequestBoxContext( instance.getRuntimeContext() )
		);
	}

	@AfterAll
	public static void teardown() {
		interceptorService.removeState( STATE );
	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
	}

	@DisplayName( "A context-less announce inside a request runs the listener on that request" )
	@Test
	public void testAnnounceUsesCurrentRequest() {
		ZoneId tokyo = ZoneId.of( "Asia/Tokyo" );
		context.getRequestContext().setTimezone( tokyo );

		IStruct data = new Struct();
		RequestBoxContext.setCurrent( context );
		try {
			interceptorService.announce( STATE, data );
		} finally {
			RequestBoxContext.removeCurrent();
		}

		assertThat( data.get( Key.of( "hasRequest" ) ) ).isEqualTo( true );
		assertThat( data.get( Key.of( "zone" ) ) ).isEqualTo( tokyo.getId() );
	}

	@DisplayName( "A context-less announce with a data supplier inside a request runs the listener on that request" )
	@Test
	public void testAnnounceSupplierUsesCurrentRequest() {
		IStruct data = new Struct();
		RequestBoxContext.setCurrent( context );
		try {
			interceptorService.announce( STATE, () -> data );
		} finally {
			RequestBoxContext.removeCurrent();
		}

		assertThat( data.get( Key.of( "hasRequest" ) ) ).isEqualTo( true );
	}

	@DisplayName( "A context-less announce with no request falls back to the runtime context" )
	@Test
	public void testAnnounceWithoutRequestUsesRuntime() throws InterruptedException {
		AtomicReference<IStruct>	result	= new AtomicReference<>();
		// A fresh thread has no current request
		Thread						thread	= new Thread( () -> {
												IStruct data = new Struct();
												interceptorService.announce( STATE, data );
												result.set( data );
											} );
		thread.start();
		thread.join();

		IStruct data = result.get();
		assertThat( data.get( Key.of( "hasRequest" ) ) ).isEqualTo( false );
		assertThat( data.get( Key.of( "zone" ) ) ).isEqualTo( instance.getConfiguration().timezone.getId() );
	}

	@DisplayName( "An announce with an explicit context keeps using that context" )
	@Test
	public void testExplicitContextIsKept() {
		IStruct data = new Struct();
		RequestBoxContext.setCurrent( context );
		try {
			interceptorService.announce( STATE, data, instance.getRuntimeContext() );
		} finally {
			RequestBoxContext.removeCurrent();
		}

		assertThat( data.get( Key.of( "hasRequest" ) ) ).isEqualTo( false );
	}
}
