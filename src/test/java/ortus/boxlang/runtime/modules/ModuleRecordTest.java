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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.IStruct;

class ModuleRecordTest {

	static BoxRuntime runtime;

	@BeforeAll
	public static void setupBeforeAll() {
		runtime = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void tearDownAfterAll() {
		runtime.shutdown();
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
		String			physicalPath	= Paths.get( "src/main/resources/modules/test" ).toAbsolutePath().toString();
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
		String			physicalPath	= Paths.get( "src/main/resources/modules/test" ).toAbsolutePath().toString();
		ModuleRecord	moduleRecord	= new ModuleRecord( moduleName, physicalPath );
		IBoxContext		context			= new ScriptingRequestBoxContext();

		// When
		moduleRecord.loadDescriptor( context );
		moduleRecord.configure( context );

		// Then

		// Verify mapping was registered
		// System.out.println( Arrays.toString( runtime.getConfiguration().runtime.getRegisteredMappings() ) );
		assertThat(
		    runtime.getConfiguration().runtime.hasMapping( "/bxModules/test" )
		).isTrue();

		// Verify interceptor points were registered
		System.out.println( runtime.getInterceptorService().getInterceptionPoints() );
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
}
