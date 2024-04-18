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
package ortus.boxlang.runtime.modules;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.resolvers.JavaResolver;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.IStruct;

class ModuleRecordTest {

	static BoxRuntime runtime;

	@BeforeEach
	public void setup() {
		runtime = BoxRuntime.getInstance( true );
	}

	@AfterEach
	public void tearDown() {
	}

	@Test
	@DisplayName( "ModuleRecord Initialization" )
	void testModuleRecordInitialization() {
		// Given
		Key				moduleName		= new Key( "TestModule" );
		String			physicalPath	= "/path/to/module";

		// When
		ModuleRecord	moduleRecord	= new ModuleRecord( moduleName, physicalPath );

		// Then
		assertThat( moduleRecord.name ).isEqualTo( moduleName );
		assertThat( moduleRecord.mapping ).isEqualTo( "/bxModules/TestModule" );
		Path modulePath = moduleRecord.physicalPath;
		assertThat( modulePath.getFileName().toString() ).isEqualTo( "module" );
		assertThat( moduleRecord.invocationPath ).isEqualTo( "bxModules.TestModule" );
		assertThat( moduleRecord.registeredOn ).isNull();
	}

	@Test
	@DisplayName( "ModuleRecord Activation" )
	void testModuleRecordActivation() {
		// Given
		Key				moduleName		= new Key( "TestModule" );
		String			physicalPath	= "/path/to/module";
		ModuleRecord	moduleRecord	= new ModuleRecord( moduleName, physicalPath );

		// When
		moduleRecord.activated		= true;
		moduleRecord.activatedOn	= Instant.now();

		// Then
		assertThat( moduleRecord.isActivated() ).isTrue();
		assertThat( moduleRecord.activatedOn ).isNotNull();
	}

	@Test
	@DisplayName( "ModuleRecord As Struct" )
	void testModuleRecordAsStruct() {
		// Given
		Key				moduleName				= new Key( "TestModule" );
		String			physicalPath			= "/path/to/module";
		ModuleRecord	moduleRecord			= new ModuleRecord( moduleName, physicalPath );

		// When
		IStruct			structRepresentation	= moduleRecord.asStruct();

		// Then
		assertThat( structRepresentation ).isInstanceOf( IStruct.class );
		assertThat( structRepresentation.getAsBoolean( Key.of( "activated" ) ) ).isFalse();
		assertThat( structRepresentation.get( "activatedOn" ) ).isNull();
		assertThat( structRepresentation.get( "author" ) ).isEqualTo( "" );
		assertThat( structRepresentation.get( "description" ) ).isEqualTo( "" );
		assertThat( structRepresentation.getAsBoolean( Key.of( "disabled" ) ) ).isFalse();
		assertThat( structRepresentation.get( "id" ) ).isNotNull();
		assertThat( structRepresentation.getAsArray( Key.of( "interceptors" ) ).size() ).isEqualTo( 0 );
		assertThat( structRepresentation.get( "invocationPath" ) ).isEqualTo( "bxModules.TestModule" );
		assertThat( structRepresentation.get( "mapping" ) ).isEqualTo( "/bxModules/TestModule" );
		assertThat( structRepresentation.get( "name" ) ).isEqualTo( moduleName );
	}

	@DisplayName( "Can load a module descriptor" )
	@Test
	void testCanLoadModuleDescriptor() {
		// Given
		Key				moduleName		= new Key( "test" );
		String			physicalPath	= Paths.get( "./modules/test" ).toAbsolutePath().toString();
		ModuleRecord	moduleRecord	= new ModuleRecord( moduleName, physicalPath );
		IBoxContext		context			= new ScriptingRequestBoxContext();

		// When
		moduleRecord.loadDescriptor( context );

		// Then
		assertThat( moduleRecord.version ).isEqualTo( "2.0.0" );
		assertThat( moduleRecord.author ).isEqualTo( "Luis Majano" );
		assertThat( moduleRecord.description ).isEqualTo( "This module does amazing things" );
		assertThat( moduleRecord.webURL ).isEqualTo( "https://www.ortussolutions.com" );
		assertThat( moduleRecord.disabled ).isEqualTo( false );
		assertThat( moduleRecord.mapping ).isEqualTo( ModuleService.MODULE_MAPPING_PREFIX + "test" );
		assertThat( moduleRecord.invocationPath ).isEqualTo( ModuleService.MODULE_MAPPING_INVOCATION_PREFIX + moduleRecord.name.getName() );
	}

	@DisplayName( "Can configure a module descriptor" )
	@Test
	void testCanConfigureModuleDescriptor() {
		// Given
		Key				moduleName		= new Key( "test" );
		String			physicalPath	= Paths.get( "./modules/test" ).toAbsolutePath().toString();
		ModuleRecord	moduleRecord	= new ModuleRecord( moduleName, physicalPath );
		IBoxContext		context			= new ScriptingRequestBoxContext();

		// When
		moduleRecord.loadDescriptor( context );
		moduleRecord.register( context );

		// Then

		// Verify mapping was registered
		// System.out.println( Arrays.toString( runtime.getConfiguration().runtime.getRegisteredMappings() ) );
		assertThat(
		    runtime.getConfiguration().runtime.hasMapping( "/bxModules/test" )
		).isTrue();

		// Verify interceptor points were registered
		assertThat(
		    runtime.getInterceptorService().hasInterceptionPoint( Key.of( "onBxTestModule" ) )
		).isTrue();

		assertThat( moduleRecord.registrationTime ).isNotNull();
		assertThat( moduleRecord.version ).isEqualTo( "2.0.0" );
		assertThat( moduleRecord.author ).isEqualTo( "Luis Majano" );
		assertThat( moduleRecord.description ).isEqualTo( "This module does amazing things" );
		assertThat( moduleRecord.webURL ).isEqualTo( "https://www.ortussolutions.com" );
		assertThat( moduleRecord.disabled ).isEqualTo( false );
		assertThat( moduleRecord.mapping ).isEqualTo( ModuleService.MODULE_MAPPING_PREFIX + "test" );
		assertThat( moduleRecord.invocationPath ).isEqualTo( ModuleService.MODULE_MAPPING_INVOCATION_PREFIX + moduleRecord.name.getName() );
	}

	@DisplayName( "Can activate a module descriptor" )
	@Test
	void testItCanActivateAModule() throws ClassNotFoundException {
		// Given
		Key				moduleName		= new Key( "test" );
		String			physicalPath	= Paths.get( "./modules/test" ).toAbsolutePath().toString();
		ModuleRecord	moduleRecord	= new ModuleRecord( moduleName, physicalPath );
		IBoxContext		context			= new ScriptingRequestBoxContext();
		ModuleService	moduleService	= runtime.getModuleService();

		// When
		moduleRecord
		    .loadDescriptor( context )
		    .register( context )
		    .activate( context );

		moduleService.getRegistry().put( moduleName, moduleRecord );

		// Then

		// It should register global functions
		FunctionService functionService = runtime.getFunctionService();
		assertThat( moduleRecord.bifs.size() ).isEqualTo( 2 );
		assertThat( functionService.hasGlobalFunction( Key.of( "moduleHelloWorld" ) ) ).isTrue();
		assertThat( functionService.hasGlobalFunction( Key.of( "moduleNow" ) ) ).isTrue();

		// Register a class loader
		Class<?> clazz = moduleRecord.findModuleClass( "HelloWorld", false );
		assertThat( clazz ).isNotNull();
		assertThat( clazz.getName() ).isEqualTo( "HelloWorld" );

		// JavaResolver can find the class explicitly
		Optional<ClassLocation> classLocation = JavaResolver.getInstance().findFromModules( "HelloWorld@test", List.of() );
		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().clazz().getName() ).isEqualTo( "HelloWorld" );
		// JavaResolver can find the class by discovery, it should interrogate all modules for it.
		classLocation = JavaResolver.getInstance().findFromModules( "HelloWorld", List.of() );
		assertThat( classLocation.isPresent() ).isTrue();
		assertThat( classLocation.get().clazz().getName() ).isEqualTo( "HelloWorld" );

		// Test the bif
		// @formatter:off
		runtime.executeSource("""
				// Test BoxLang Bifs
		       result = moduleHelloWorld( 'boxlang' );
		    	result2 = moduleNow();

				// Test Member Methods
				result3 = "boxlang".foo();

			//result4 = hola();
		    """, context );
		// @formatter:on

		IScope variables = context.getScopeNearby( VariablesScope.name );
		assertThat( variables.getAsString( Key.result ) )
		    .isEqualTo( "Hello World, my name is boxlang and I am 0 years old" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "result3" ) ) ).isEqualTo(
		    "Hello World, my name is boxlang and I am 0 years old"
		);
		// assertThat( variables.getAsString( Key.of( "result4" ) ) ).isEqualTo( "Hola Mundo!" );
	}
}
