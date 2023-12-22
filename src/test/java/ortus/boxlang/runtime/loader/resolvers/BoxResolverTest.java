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

package ortus.boxlang.runtime.loader.resolvers;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;

public class BoxResolverTest {

	@DisplayName( "It can find be created" )
	@Test
	public void testItCanBeCreated() {
		BoxResolver boxResolver = BoxResolver.getInstance();
		assertThat( boxResolver.getName() ).isEqualTo( "BoxResolver" );
		assertThat( boxResolver.getPrefix() ).isEqualTo( "bx" );
	}

	@DisplayName( "It can find classes from modules" )
	@Test
	public void testFindFromModules() {
		BoxResolver	boxResolver	= BoxResolver.getInstance();
		String		className	= "apppath.models.User"; // Example class name
		assertThat( boxResolver.findFromModules( null, className, new ArrayList<>() ).isPresent() ).isFalse();
	}

	@DisplayName( "It can find classes from local disk" )
	@Test
	public void testFindFromLocal() {
		BoxResolver	boxResolver	= BoxResolver.getInstance();
		String		className	= "apppath.models.User"; // Example class name
		assertThat( boxResolver.findFromLocal( null, className, new ArrayList<>() ).isPresent() ).isFalse();
	}

	@DisplayName( "It can resolve classes" )
	@Test
	public void testResolve() {
		BoxResolver	boxResolver	= BoxResolver.getInstance();

		IBoxContext	context		= new ScriptingBoxContext();
		String		className	= "apppath.models.User"; // Example class name

		assertThat( boxResolver.resolve( context, className ).isPresent() ).isFalse();
	}

}
