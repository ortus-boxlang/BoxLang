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
package ortus.boxlang.runtime.context;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName( "Context Shutdown Tests" )
public class ContextShutdownTest {

	@DisplayName( "A failing shutdown listener does not prevent the remaining listeners from running" )
	@Test
	void testShutdownListenerIsolation() {
		ScriptingRequestBoxContext	context	= new ScriptingRequestBoxContext();
		AtomicBoolean				ran		= new AtomicBoolean( false );

		// Listeners are invoked in reverse registration order, so this listener (registered first) runs LAST
		context.registerShutdownListener( c -> ran.set( true ) );
		// This listener (registered last) runs FIRST and blows up
		context.registerShutdownListener( c -> {
			throw new RuntimeException( "Simulated listener failure" );
		} );

		context.shutdown();

		// The failing listener must not prevent the other listener - which may be releasing critical resources, such as JDBC connections - from running
		assertThat( ran.get() ).isTrue();
	}
}
