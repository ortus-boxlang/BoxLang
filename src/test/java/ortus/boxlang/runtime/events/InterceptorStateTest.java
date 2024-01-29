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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

class InterceptorStateTest {

	private InterceptorState	interceptorState;
	private DynamicObject		observer1;
	private DynamicObject		observer2;

	@BeforeEach
	void setUp() {
		interceptorState	= new InterceptorState( Key.of( "onTests" ) );
		observer1			= DynamicObject.of( this );
		observer2			= DynamicObject.of( this );
		assertThat( interceptorState.getName().getName() ).isEqualTo( "onTests" );
	}

	@DisplayName( "It can register and unregister observers" )
	@Test
	void testItCanRegisterObservers() {
		interceptorState.register( observer1 );
		assertThat( interceptorState.exists( observer1 ) ).isTrue();
		interceptorState.unregister( observer1 );
		assertThat( interceptorState.exists( observer1 ) ).isFalse();
	}

	@DisplayName( "It can process observers" )
	@Test
	void testItCanProcessObservers() {
		interceptorState.register( observer1 );
		interceptorState.register( observer2 );

		Key		counterKey	= Key.of( "counter" );
		IStruct	data		= new Struct();
		data.put( counterKey, 0 );

		interceptorState.announce( data, new ScriptingRequestBoxContext() );

		assertThat( data.get( counterKey ) ).isEqualTo( 2 );
	}

	public void onTests( IStruct data ) {
		Key	counterKey	= Key.of( "counter" );
		int	counter		= ( int ) data.get( counterKey );
		data.put( counterKey, counter + 1 );
	}

}
