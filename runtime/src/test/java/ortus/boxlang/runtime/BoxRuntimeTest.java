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
package ortus.boxlang.runtime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URL;

public class BoxRuntimeTest {

	@DisplayName( "It can startup" )
	@Test
	public void testItCanStartUp() {
		// Ensure getInstance() returns the same instance as startup()
		BoxRuntime instance1 = BoxRuntime.getInstance();
		assertThat( instance1 ).isNull();

		BoxRuntime instance2 = BoxRuntime.startup();
		assertThat( BoxRuntime.getInstance() ).isSameInstanceAs( instance2 );
		assertThat( BoxRuntime.hasStarted() ).isTrue();
		assertThat( BoxRuntime.getStartTime().isPresent() ).isTrue();
	}

	@DisplayName( "It can shutdown" )
	@Test
	public void testItCanShutdown() {
		BoxRuntime.startup();
		// Ensure shutdown sets instance to null
		BoxRuntime.shutdown();
		assertThat( BoxRuntime.getInstance() ).isNull();
		assertThat( BoxRuntime.hasStarted() ).isFalse();
		assertThat( BoxRuntime.getStartTime().isPresent() ).isFalse();
	}

	@DisplayName( "It can execute a template" )
	@Test
	public void testItCanExecuteATemplate() throws Throwable {
		String testTemplate = getClass().getResource( "/test-templates/BoxRuntime.bx" ).getPath();

		assertDoesNotThrow( () -> {
			BoxRuntime.startup();
			BoxRuntime.executeTemplate( testTemplate );
			BoxRuntime.shutdown();
		} );
	}

	@DisplayName( "It can execute a template URL" )
	@Test
	public void testItCanExecuteATemplateURL() throws Throwable {
		URL testTemplate = getClass().getResource( "/test-templates/BoxRuntime.bx" );

		assertDoesNotThrow( () -> {
			BoxRuntime.startup();
			BoxRuntime.executeTemplate( testTemplate );
			BoxRuntime.shutdown();
		} );
	}

}
