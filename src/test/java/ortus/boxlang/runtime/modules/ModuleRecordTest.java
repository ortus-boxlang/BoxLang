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

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

class ModuleRecordTest {

	@Test
	@DisplayName( "ModuleRecord Initialization" )
	void testModuleRecordInitialization() {
		// Arrange
		Key				moduleName		= new Key( "TestModule" );
		String			physicalPath	= "/path/to/module";

		// Act
		ModuleRecord	moduleRecord	= new ModuleRecord( moduleName, physicalPath );

		// Assert
		assertThat( moduleRecord.name ).isEqualTo( moduleName );
		assertThat( moduleRecord.physicalPath ).isEqualTo( physicalPath );
		assertThat( moduleRecord.mapping ).isEqualTo( "/bxModules/TestModule" );
		assertThat( moduleRecord.invocationPath ).isEqualTo( "bxModules.TestModule" );
		assertThat( moduleRecord.registrationTime ).isNotNull();
	}

	@Test
	@DisplayName( "ModuleRecord Activation" )
	void testModuleRecordActivation() {
		// Arrange
		Key				moduleName		= new Key( "TestModule" );
		String			physicalPath	= "/path/to/module";
		ModuleRecord	moduleRecord	= new ModuleRecord( moduleName, physicalPath );

		// Act
		moduleRecord.activated		= true;
		moduleRecord.activationTime	= Instant.now();

		// Assert
		assertThat( moduleRecord.isActivated() ).isTrue();
		assertThat( moduleRecord.activationTime ).isNotNull();
	}

	@Test
	@DisplayName( "ModuleRecord As Struct" )
	void testModuleRecordAsStruct() {
		// Arrange
		Key				moduleName				= new Key( "TestModule" );
		String			physicalPath			= "/path/to/module";
		ModuleRecord	moduleRecord			= new ModuleRecord( moduleName, physicalPath );

		// Act
		IStruct			structRepresentation	= moduleRecord.asStruct();

		// Assert
		assertThat( structRepresentation ).isInstanceOf( IStruct.class );
		assertThat( structRepresentation.getAsBoolean( Key.of( "activated" ) ) ).isFalse();
		assertThat( structRepresentation.get( "activationTime" ) ).isNull();
		assertThat( structRepresentation.get( "author" ) ).isEqualTo( "" );
		assertThat( structRepresentation.get( "description" ) ).isEqualTo( "" );
		assertThat( structRepresentation.getAsBoolean( Key.of( "disabled" ) ) ).isFalse();
		assertThat( structRepresentation.get( "id" ) ).isNotNull();
		assertThat( structRepresentation.getAsArray( Key.of( "interceptors" ) ).size() ).isEqualTo( 0 );
		assertThat( structRepresentation.get( "invocationPath" ) ).isEqualTo( "bxModules.TestModule" );
		assertThat( structRepresentation.get( "mapping" ) ).isEqualTo( "/bxModules/TestModule" );
		assertThat( structRepresentation.get( "name" ) ).isEqualTo( moduleName );
		assertThat( structRepresentation.get( "physicalPath" ) ).isEqualTo( physicalPath );
		assertThat( structRepresentation.get( "registrationTime" ) ).isNotNull();

	}
}
