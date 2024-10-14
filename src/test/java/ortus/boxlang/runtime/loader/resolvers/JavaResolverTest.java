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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.ConsoleHandler;

import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.IStruct;

public class JavaResolverTest {

	static BoxRuntime	runtime;
	static JavaResolver	javaResolver;
	static IBoxContext	context;

	@BeforeAll
	public static void setUp() {
		runtime			= BoxRuntime.getInstance( true );
		javaResolver	= runtime.getClassLocator().getJavaResolver();
	}

	@BeforeEach
	public void setup() {
		context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		javaResolver.clearJdkImportCache();
	}

	@DisplayName( "It can find be created" )
	@Test
	public void testItCanBeCreated() {
		assertThat( javaResolver.getName() ).isEqualTo( "JavaResolver" );
		assertThat( javaResolver.getPrefix() ).isEqualTo( "java" );
	}

	@DisplayName( "It can find inner classes using the $ separator" )
	@Test
	public void testFindInnerClasses() {
		String					className		= "java.util.Map$Entry"; // Example class name
		Optional<ClassLocation>	classLocation	= javaResolver.findFromSystem( className, new ArrayList<>(), context );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().clazz() ).isEqualTo( Map.Entry.class );
		assertThat( classLocation.get().name() ).isEqualTo( "Entry" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "java.util" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
		assertThat( classLocation.get().module() ).isNull();
	}

	@DisplayName( "It can find inner class enums using the $ separator" )
	@Test
	public void testFindInnerClassEnums() {
		String					className		= "ortus.boxlang.runtime.types.IStruct$TYPES";
		Optional<ClassLocation>	classLocation	= javaResolver.findFromSystem( className, new ArrayList<>(), context );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().clazz() ).isEqualTo( IStruct.TYPES.class );
		assertThat( classLocation.get().name() ).isEqualTo( "TYPES" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "ortus.boxlang.runtime.types" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
		assertThat( classLocation.get().module() ).isNull();
	}

	@DisplayName( "It can find classes from the system" )
	@Test
	public void testFindFromSystem() {
		String					className		= "java.util.logging.ConsoleHandler";
		Optional<ClassLocation>	classLocation	= javaResolver.findFromSystem( className, new ArrayList<>(), context );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().clazz() ).isEqualTo( ConsoleHandler.class );
		assertThat( classLocation.get().name() ).isEqualTo( "ConsoleHandler" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "java.util.logging" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
		assertThat( classLocation.get().module() ).isNull();
	}

	@DisplayName( "It can find classes from dependent libraries" )
	@Test
	public void testFindFromDependentLibraries() {
		String					className		= "org.apache.commons.lang3.ClassUtils";
		Optional<ClassLocation>	classLocation	= javaResolver.findFromSystem( className, new ArrayList<>(), context );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().clazz() ).isEqualTo( ClassUtils.class );
		assertThat( classLocation.get().name() ).isEqualTo( "ClassUtils" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "org.apache.commons.lang3" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
		assertThat( classLocation.get().module() ).isNull();
	}

	@DisplayName( "It can resolve classes" )
	@Test
	public void testResolveClasses() {
		String					className		= "java.util.HashSet";
		Optional<ClassLocation>	classLocation	= javaResolver.findFromSystem( className, new ArrayList<>(), context );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().clazz() ).isEqualTo( HashSet.class );
		assertThat( classLocation.get().name() ).isEqualTo( "HashSet" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "java.util" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
		assertThat( classLocation.get().module() ).isNull();
	}

	@DisplayName( "It can resolve classes using aliases" )
	@Test
	public void testResolveWithAliases() {
		String					className		= "MySet";
		List<ImportDefinition>	imports			= Arrays.asList(
		    ImportDefinition.parse( "java:java.util.HashSet as MySet" )
		);
		Optional<ClassLocation>	classLocation	= javaResolver.resolve( context, className, imports );

		System.out.println( classLocation );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().clazz() ).isEqualTo( HashSet.class );
		assertThat( classLocation.get().name() ).isEqualTo( "HashSet" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "java.util" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
	}

	@DisplayName( "It can resolve a module class by resolution and not explicitly" )
	@Test
	public void testResolveModuleClass() {
		loadTestModule();
		String					className		= "com.ortussolutions.bifs.Hola";
		List<ImportDefinition>	imports			= Arrays.asList(
		    ImportDefinition.parse( "java:java.util.HashSet as MySet" )
		);
		Optional<ClassLocation>	classLocation	= javaResolver.resolve( context, className, imports );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().name() ).isEqualTo( "Hola" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "com.ortussolutions.bifs" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
		assertThat( classLocation.get().module() ).isEqualTo( "test" );
	}

	@DisplayName( "It can resolve a module class explicitly via an import" )
	@Test
	public void testResolveModuleClassExplicitly() {
		loadTestModule();
		String					className		= "Hola";
		List<ImportDefinition>	imports			= Arrays.asList(
		    ImportDefinition.parse( "java:com.ortussolutions.bifs.Hola@test" )
		);
		Optional<ClassLocation>	classLocation	= javaResolver.resolve( context, className, imports );

		System.out.println( classLocation );

		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().name() ).isEqualTo( "Hola" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "com.ortussolutions.bifs" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
		assertThat( classLocation.get().module() ).isEqualTo( "test" );
	}

	@DisplayName( "It can resolve wildcard imports from the JDK itself" )
	@Test
	void testItCanResolveWildcardImports() throws Exception {
		List<ImportDefinition> imports = Arrays.asList(
		    ImportDefinition.parse( "java:java.lang.*" ),
		    ImportDefinition.parse( "java:java.util.*" )
		);

		assertThat( javaResolver.getJdkImportCacheSize() ).isEqualTo( 0 );

		String fqn = javaResolver.expandFromImport( new ScriptingRequestBoxContext(), "String", imports );
		assertThat( fqn ).isEqualTo( "java.lang.String" );

		fqn = javaResolver.expandFromImport( new ScriptingRequestBoxContext(), "Integer", imports );
		assertThat( fqn ).isEqualTo( "java.lang.Integer" );

		fqn = javaResolver.expandFromImport( new ScriptingRequestBoxContext(), "List", imports );
		assertThat( fqn ).isEqualTo( "java.util.List" );
	}

	@DisplayName( "It can load libs from the 'home/libs' convention" )
	@Test
	void testItCanLoadLibsFromHomeLibs() throws IOException {
		IBoxContext	ctx			= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		Path		homeLibs	= Path.of( "src/test/resources/libs" ).toAbsolutePath();

		runtime.getRuntimeLoader().addURLs(
		    DynamicClassLoader.getJarURLs( homeLibs )
		);

		System.out.println( Arrays.toString( runtime.getRuntimeLoader().getURLs() ) );

		String					targetClass	= "com.github.benmanes.caffeine.cache.Caffeine";
		Optional<ClassLocation>	location	= javaResolver.resolve( ctx, targetClass );

		assertThat( location.isPresent() ).isTrue();
		assertThat( location.get().clazz().getName() ).isEqualTo( targetClass );
	}

	private void loadTestModule() {
		Key				moduleName		= new Key( "test" );
		String			physicalPath	= Paths.get( "./modules/test" ).toAbsolutePath().toString();
		ModuleRecord	moduleRecord	= new ModuleRecord( physicalPath );
		ModuleService	moduleService	= runtime.getModuleService();

		moduleRecord
		    .loadDescriptor( context )
		    .register( context )
		    .activate( context );

		moduleService.getRegistry().put( moduleName, moduleRecord );
	}

}
