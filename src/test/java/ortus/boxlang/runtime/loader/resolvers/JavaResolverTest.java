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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.loader.ClassLocation;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.types.IStruct;

public class JavaResolverTest extends AbstractResolverTest {

	public static JavaResolver javaResolver;

	@BeforeAll
	public static void beforeAll() {
		javaResolver = runtime.getClassLocator().getJavaResolver();
	}

	@BeforeEach
	@Override
	public void beforeEach() {
		super.beforeEach();
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
		assertThat( classLocation.get().clazz( context ) ).isEqualTo( Map.Entry.class );
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
		assertThat( classLocation.get().clazz( context ) ).isEqualTo( IStruct.TYPES.class );
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
		assertThat( classLocation.get().clazz( context ) ).isEqualTo( ConsoleHandler.class );
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
		assertThat( classLocation.get().clazz( context ) ).isEqualTo( ClassUtils.class );
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
		assertThat( classLocation.get().clazz( context ) ).isEqualTo( HashSet.class );
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
		assertThat( classLocation.get().clazz( context ) ).isEqualTo( HashSet.class );
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

		// System.out.println( classLocation );
		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().name() ).isEqualTo( "Hola" );
		assertThat( classLocation.get().packageName() ).isEqualTo( "com.ortussolutions.bifs" );
		assertThat( classLocation.get().type() ).isEqualTo( ClassLocator.TYPE_JAVA );
		assertThat( classLocation.get().module() ).isEqualTo( "test" );
	}

	@DisplayName( "It can resolve wildcard imports from the JDK itself" )
	@Test
	void testItCanResolveWildcardImports() {
		List<ImportDefinition> imports = Arrays.asList(
		    ImportDefinition.parse( "java:java.lang.*" ),
		    ImportDefinition.parse( "java:java.util.*" )
		);

		assertThat( javaResolver.getJdkImportCacheSize() ).isEqualTo( 0 );

		String fqn = javaResolver.expandFromImport( context, "String", imports );
		assertThat( fqn ).isEqualTo( "java.lang.String" );

		fqn = javaResolver.expandFromImport( context, "Integer", imports );
		assertThat( fqn ).isEqualTo( "java.lang.Integer" );

		fqn = javaResolver.expandFromImport( context, "List", imports );
		assertThat( fqn ).isEqualTo( "java.util.List" );
	}

	@DisplayName( "It can resolve wildcard imports from NON JDK classes on disk path" )
	@Test
	void testItCanResolveWildcardImportsFromNonJDK() {
		List<ImportDefinition>	imports	= Arrays.asList(
		    ImportDefinition.parse( "java:ortus.boxlang.runtime.util.*" )
		);

		String					fqn		= javaResolver.expandFromImport( context, "DumpUtil", imports );
		assertThat( fqn ).isEqualTo( "ortus.boxlang.runtime.util.DumpUtil" );
	}

	@DisplayName( "It can resolve wildcard imports from NON JDK classes in a JAR" )
	@Test
	void testItCanResolveWildcardImportsFromNonJDKInAJar() {
		List<ImportDefinition>	imports	= Arrays.asList(
		    ImportDefinition.parse( "java:com.zaxxer.hikari.util.*" )
		);

		String					fqn		= javaResolver.expandFromImport( context, "ConcurrentBag", imports );
		assertThat( fqn ).isEqualTo( "com.zaxxer.hikari.util.ConcurrentBag" );
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

}
