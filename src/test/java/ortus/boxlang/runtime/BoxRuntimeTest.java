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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;

public class BoxRuntimeTest {

	@DisplayName( "It can startup" )
	@Test
	public void testItCanStartUp() {

		BoxRuntime instance2 = BoxRuntime.getInstance( true );
		assertThat( BoxRuntime.getInstance() ).isSameInstanceAs( instance2 );
		assertThat( instance2.inDebugMode() ).isTrue();
		assertThat( instance2.getStartTime() ).isNotNull();
	}

	@DisplayName( "It can shutdown" )
	@Test
	public void testItCanShutdown() {
		BoxRuntime instance = BoxRuntime.getInstance( true );
		// Ensure shutdown sets instance to null
		instance.shutdown();
	}

	@DisplayName( "It can execute a template" )
	@Test
	public void testItCanExecuteATemplate() {
		String testTemplate;
		try {
			testTemplate = ( new File( getClass().getResource( "/test-templates/BoxRuntime.cfm" ).toURI() ) ).getPath();
		} catch ( URISyntaxException e ) {
			throw new MissingIncludeException( "Invalid template path to execute.", "", getClass().getResource( "/test-templates/BoxRuntime.bx" ).toString(),
			    e );
		}
		assertDoesNotThrow( () -> {
			BoxRuntime instance = BoxRuntime.getInstance( true );
			instance.executeTemplate( testTemplate );
			instance.shutdown();
		} );
	}

	@DisplayName( "It can execute a template URL" )
	@Test
	public void testItCanExecuteATemplateURL() {
		URL testTemplate = getClass().getResource( "/test-templates/BoxRuntime.cfm" );

		assertDoesNotThrow( () -> {
			BoxRuntime instance = BoxRuntime.getInstance( true );
			instance.executeTemplate( testTemplate );
			instance.shutdown();
		} );
	}

}
