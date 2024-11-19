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
package ortus.boxlang.runtime.config.segments;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SecurityConfigTest {

	private SecurityConfig securityConfig;

	@BeforeEach
	public void setUp() {
		securityConfig = new SecurityConfig();
	}

	@Test
	public void testIsBIFDisallowed_AllowedBIF() {
		String bifName = "allowedBIF";
		assertTrue( securityConfig.isBIFAllowed( bifName ) );
		// Verify it's in the allowedBIFSLookup
		assertTrue( securityConfig.allowedBIFsLookup.containsKey( bifName ) );
	}

	@Test
	public void testisBIFAllowed_DisallowedBIF() {
		String bifName = "disallowedBIF";
		securityConfig.disallowedBIFs.add( bifName );
		assertThrows( SecurityException.class, () -> securityConfig.isBIFAllowed( bifName ) );
	}

	@Test
	public void testisComponentAllowed_AllowedComponent() {
		String componentName = "allowedComponent";
		assertTrue( securityConfig.isComponentAllowed( componentName ) );
		// Verify it's in the allowedComponentsLookup
		assertTrue( securityConfig.allowedComponentsLookup.containsKey( componentName ) );
	}

	@Test
	public void testisComponentAllowed_DisallowedComponent() {
		String componentName = "disallowedComponent";
		securityConfig.disallowedComponents.add( componentName );
		assertThrows( SecurityException.class, () -> securityConfig.isComponentAllowed( componentName ) );
	}

	@DisplayName( "Check if a class import is allowed" )
	@Test
	public void testIsImportDisallowed_AllowedImport() {
		String importName = "java.lang.Runnable";
		assertTrue( securityConfig.isClassAllowed( importName ) );
		// Verify it's in the allowedImportsLookup
		assertTrue( securityConfig.allowedImportsLookup.containsKey( importName ) );
	}

	@DisplayName( "Check if a class import is disallowed" )
	@Test
	public void testIsImportDisallowed_DisallowedImport() {
		String importName = "java\\.lang\\.String";
		securityConfig.disallowedImports.add( importName );
		assertThrows( SecurityException.class, () -> securityConfig.isClassAllowed( "java.lang.String" ) );
	}

	@DisplayName( "Check if an exe extension is disallowed" )
	@Test
	public void testIsExeDisalloweed() {
		String fileName = "test.exe";
		securityConfig.disallowedFileOperationExtensions.add( "exe" );
		assertFalse( securityConfig.isFileOperationAllowed( fileName ) );
	}

}
