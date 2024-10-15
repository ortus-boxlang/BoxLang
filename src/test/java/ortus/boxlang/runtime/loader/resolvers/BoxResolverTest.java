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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;

public class BoxResolverTest {

	static BoxRuntime	runtime;
	static IBoxContext	context;
	static BoxResolver	boxResolver;

	@BeforeAll
	public static void setUp() {
		runtime		= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		boxResolver	= runtime.getClassLocator().getBoxResolver();

		// Create a mapping to the `resources` directory
		Path resourcesDirectory = Paths.get( "src/test/resources" ).toAbsolutePath();
		runtime.getConfiguration().registerMapping( "/tests", resourcesDirectory.toString() );
		// System.out.println( "mappings: " + Arrays.toString( runtime.getConfiguration().getRegisteredMappings() ) );
	}

	@AfterAll
	public static void teardown() {
	}

	@DisplayName( "It can find be created" )
	@Test
	void testItCanBeCreated() {
		assertThat( boxResolver.getName() ).isEqualTo( "BoxResolver" );
		assertThat( boxResolver.getPrefix() ).isEqualTo( "bx" );
	}

	@DisplayName( "It can find classes from modules" )
	@Test
	@Disabled
	void testFindFromModules() {
		String className = "apppath.models.User"; // Example class name
		assertThat( boxResolver.findFromModules( new ScriptingRequestBoxContext(), className, new ArrayList<>() ).isPresent() ).isFalse();
	}

	@DisplayName( "It can find classes from local disk using a mapping" )
	@Test
	void testFindFromLocal() throws URISyntaxException {
		// You can find this in src/test/resources/tests/components/User.cfc
		String					testComponent	= "tests.components.User";

		// System.out.println( "mappings: " + Arrays.toString( runtime.getConfiguration().getRegisteredMappings() ) );

		Optional<ClassLocation>	classLocation	= boxResolver.findFromLocal( context, testComponent, new ArrayList<>() );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().name() ).isEqualTo( "User" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "tests.components" );

		IClassRunnable cfc = ( IClassRunnable ) DynamicObject.of( classLocation.get().clazz() )
		    .invokeConstructor( context )
		    .getTargetInstance();
		assertThat( cfc ).isNotNull();
		assertThat( cfc.getThisScope().getAsString( Key.of( "name" ) ) ).isEqualTo( "test" );
		assertThat( cfc.getThisScope().get( Key.of( "created" ) ) ).isInstanceOf( DateTime.class );
	}

	@DisplayName( "It can resolve classes using direct class names" )
	@Test
	void testResolve() {
		// Invalid Class
		String className = "apppath.models.User";
		assertThat( boxResolver.resolve( context, className ).isPresent() ).isFalse();

		// Now a class that exists
		className = "src.test.bx.Person";
		assertThat( boxResolver.resolve( context, className ).isPresent() ).isTrue();
	}

	@DisplayName( "It can resolve classes using imports" )
	@Test
	void testResolveWithImports() {
		String					className		= "TestClass";

		List<ImportDefinition>	imports			= Arrays.asList(
		    ImportDefinition.parse( "src.test.bx.models.Validation" ),
		    ImportDefinition.parse( "src.test.bx.models.TestClass" )
		);
		Optional<ClassLocation>	classLocation	= boxResolver.resolve( context, className, imports );
		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().path() ).contains( "TestClass" );
	}

	@DisplayName( "It can resolve classes using imports for modules" )
	@Disabled
	@Test
	void testResolveWithImportsForModules() {
		String					className		= "Hello";
		List<ImportDefinition>	imports			= Arrays.asList(
		    ImportDefinition.parse( "models.Hello@test" ),
		    ImportDefinition.parse( "src.test.bx.models.TestClass" )
		);
		Optional<ClassLocation>	classLocation	= boxResolver.resolve( context, className, imports );
		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().path() ).contains( "test/models/Hello" );
	}

}
