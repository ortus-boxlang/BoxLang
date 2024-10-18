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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class BoxResolverTest extends AbstractResolverTest {

	public static BoxResolver boxResolver;

	@BeforeAll
	public static void setUp() {
		boxResolver = runtime.getClassLocator().getBoxResolver();
		// Create a mapping to the `resources` directory as `/tests`
		Path resourcesDirectory = Paths.get( "src/test/resources" ).toAbsolutePath();
		runtime.getConfiguration().registerMapping( "/tests", resourcesDirectory.toString() );
		// System.out.println( "mappings: " + Arrays.toString( runtime.getConfiguration().getRegisteredMappings() ) );
	}

	@DisplayName( "It can be created correctly with the rigth prefix" )
	@Test
	void testItCanBeCreated() {
		assertThat( boxResolver.getName() ).isEqualTo( "BoxResolver" );
		assertThat( boxResolver.getPrefix() ).isEqualTo( "bx" );
	}

	@DisplayName( "It can find classes from local disk using a mapping" )
	@Test
	void testFindFromLocal() {
		// You can find this in src/test/resources/tests/components/User.cfc
		String					testComponent	= "tests.components.User";
		Optional<ClassLocation>	classLocation	= boxResolver.findFromLocal( context, testComponent, new ArrayList<>() );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().name() ).isEqualTo( "User" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "tests.components" );

		IClassRunnable targetClass = ( IClassRunnable ) DynamicObject.of( classLocation.get().clazz() )
		    .invokeConstructor( context )
		    .getTargetInstance();
		assertThat( targetClass ).isNotNull();
		assertThat( targetClass.getThisScope().getAsString( Key.of( "name" ) ) ).isEqualTo( "test" );
		assertThat( targetClass.getThisScope().get( Key.of( "created" ) ) ).isInstanceOf( DateTime.class );
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
		assertThat( boxResolver.resolve( context, className ).get().path() ).contains( "Person" );
	}

	@DisplayName( "It can resolve classes using relative paths from a template executing it" )
	@Test
	void testResolveRelativeFromTemplate() {
		// Push a mock template
		String mockPath = Path.of( "src/test/bx/index.bxm" ).toAbsolutePath().toString();
		context.pushTemplate( new IBoxRunnable() {

			@Override
			public List<ImportDefinition> getImports() {
				return List.of();
			}

			@Override
			public long getRunnableCompileVersion() {
				return 0L;
			}

			@Override
			public LocalDateTime getRunnableCompiledOn() {
				return null;
			}

			@Override
			public Object getRunnableAST() {
				return null;
			}

			@Override
			public ResolvedFilePath getRunnablePath() {
				return ResolvedFilePath.of( mockPath );
			}

			@Override
			public BoxSourceType getSourceType() {
				return BoxSourceType.BOXTEMPLATE;
			}

		} );

		try {
			// Relative to the template above
			String					target			= "Person";
			Optional<ClassLocation>	classLocation	= boxResolver.findFromLocal( context, target, new ArrayList<>() );
			assertThat( classLocation.isPresent() ).isTrue();
			assertThat( classLocation.get().name() ).isEqualTo( "Person" );

			// Relative to the template above but embedded
			target			= "models.TestClass";
			classLocation	= boxResolver.findFromLocal( context, target, new ArrayList<>() );
			assertThat( classLocation.isPresent() ).isTrue();
			assertThat( classLocation.get().name() ).isEqualTo( "TestClass" );
		} finally {
			context.popTemplate();
		}
	}

	@DisplayName( "It can resolve classes using direct imports" )
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

	@DisplayName( "It can resolve classes using wildcard imports" )
	@Test
	void testResolveWithWildcardImports() {
		String					className		= "TestClass";
		List<ImportDefinition>	imports			= Arrays.asList(
		    ImportDefinition.parse( "src.test.bx.models.*" )
		);
		Optional<ClassLocation>	classLocation	= boxResolver.resolve( context, className, imports );
		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().path() ).contains( "TestClass" );
	}

	@DisplayName( "It can find classes from modules" )
	@Test
	void testFindFromModules() {
		loadTestModule();
		String					className		= "models.Hello@test";
		Optional<ClassLocation>	classLocation	= boxResolver.resolve( context, className, new ArrayList<>() );
		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().name() ).isEqualTo( "Hello" );
	}

	@DisplayName( "It can resolve classes using imports for modules" )
	@Test
	void testResolveWithImportsForModules() {
		loadTestModule();
		String					className		= "Hello";
		List<ImportDefinition>	imports			= Arrays.asList(
		    ImportDefinition.parse( "models.Hello@test" ),
		    ImportDefinition.parse( "src.test.bx.models.TestClass" )
		);
		Optional<ClassLocation>	classLocation	= boxResolver.resolve( context, className, imports );
		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().path().replace( "\\", "/" ) ).contains( "test/models/Hello" );
	}

}
