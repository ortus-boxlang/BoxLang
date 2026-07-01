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

import java.lang.reflect.Field;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;

/**
 * Tests for pure-Java module config support via IModuleConfig + ServiceLoader,
 * and for the BxModuleConfig proxy that unifies BX and Java dispatch.
 */
class ModuleRecordJavaConfigTest {

	static BoxRuntime	runtime;
	IBoxContext			context;

	/** Physical path of the pre-built Java test module (populated by the javaTestModule Gradle subproject build). */
	static final String	JAVA_MODULE_PATH	= Paths.get( "./modules/javaTestModule" ).toAbsolutePath().toString();

	@BeforeEach
	void setup() {
		runtime	= BoxRuntime.getInstance( true );
		context	= new ScriptingRequestBoxContext();
	}

	@AfterEach
	void tearDown() {
		// Clean up any registered mappings so tests don't bleed into each other
		try {
			runtime.getConfiguration().unregisterMapping( Key.of( ModuleService.MODULE_MAPPING_PREFIX + "javaTestModule" ) );
		} catch ( Exception ignored ) {
		}
	}

	// -------------------------------------------------------------------------
	// Discovery
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "A module directory with only box.json (no ModuleConfig.bx) is discovered by buildRegistryFromPath" )
	void testJavaOnlyModuleDiscoveredViaBoxJson() {
		// The javaTestModule directory has box.json but NO ModuleConfig.bx
		java.nio.file.Path	modulesDir	= Paths.get( "./modules" ).toAbsolutePath();
		ModuleService		service		= runtime.getModuleService();

		// Build registry from the parent modules directory
		service.buildRegistryFromPath( modulesDir );

		assertThat( service.hasModule( Key.of( "javaTestModule" ) ) ).isTrue();

		// Clean up the registry entry
		service.getRegistry().remove( Key.of( "javaTestModule" ) );
	}

	// -------------------------------------------------------------------------
	// Registration / loadDescriptor + register
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "Java IModuleConfig is detected via ServiceLoader; moduleConfig is the Java impl (not BxModuleConfig)" )
	void testJavaModuleConfigDetectedViaServiceLoader() {
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );

		record.loadDescriptor( context );
		record.register( context );

		assertThat( record.moduleConfig ).isNotNull();
		assertThat( record.moduleConfig ).isNotInstanceOf( BxModuleConfig.class );

		cleanup( record );
	}

	@Test
	@DisplayName( "Public field metadata is extracted from the Java IModuleConfig implementation" )
	void testMetadataExtractedFromPublicFields() {
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );

		record.loadDescriptor( context );
		record.register( context );

		assertThat( record.version ).isEqualTo( "2.0.0" );
		assertThat( record.author ).isEqualTo( "Ortus Solutions" );
		assertThat( record.description ).isEqualTo( "A pure-Java test module" );
		assertThat( record.webURL ).isEqualTo( "https://www.ortussolutions.com" );
		assertThat( record.enabled ).isTrue();

		cleanup( record );
	}

	@Test
	@DisplayName( "configure(IBoxContext, ModuleRecord) is called during register(); settings are mutated" )
	void testJavaConfigureCalledAndSettingsMutated() {
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );

		record.loadDescriptor( context );
		record.register( context );

		// configure() should have put "javaKey" -> "javaValue" into settings
		assertThat( record.settings.containsKey( Key.of( "javaKey" ) ) ).isTrue();
		assertThat( record.settings.getAsString( Key.of( "javaKey" ) ) ).isEqualTo( "javaValue" );

		// Confirm via the static tracking flag (accessed through the module classloader's class)
		assertThat( getStaticBooleanFlag( record, "configureCalled" ) ).isTrue();

		cleanup( record );
	}

	// -------------------------------------------------------------------------
	// Activation
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "onLoad(IBoxContext, ModuleRecord) is called during activate()" )
	void testJavaOnLoadCalledOnActivate() {
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );

		record.loadDescriptor( context ).register( context ).activate( context );

		assertThat( getStaticBooleanFlag( record, "onLoadCalled" ) ).isTrue();

		cleanup( record );
	}

	@Test
	@DisplayName( "Java config is registered as an interceptor after activate() without throwing" )
	void testJavaModuleRegisteredAsInterceptorWithoutError() {
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );

		// activate() internally calls moduleConfig.registerInterceptor(service, settings).
		// If it throws, the Java config was not accepted as a valid IInterceptor.
		org.junit.jupiter.api.Assertions.assertDoesNotThrow( () -> {
			record.loadDescriptor( context ).register( context ).activate( context );
		} );
		assertThat( record.activated ).isTrue();

		cleanup( record );
	}

	// -------------------------------------------------------------------------
	// Unload
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "onUnload(IBoxContext, ModuleRecord) is called during unload()" )
	void testJavaOnUnloadCalledOnUnload() {
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );

		record.loadDescriptor( context ).register( context ).activate( context );
		record.unload( context );

		assertThat( getStaticBooleanFlag( record, "onUnloadCalled" ) ).isTrue();
	}

	// -------------------------------------------------------------------------
	// Java wins over BX
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "Java IModuleConfig wins when both Java config and ModuleConfig.bx exist" )
	void testJavaConfigWinsOverBxDescriptor() {
		// The javaTestModule has no ModuleConfig.bx, so loadDescriptor() leaves moduleConfig null.
		// After register(), ServiceLoader finds the Java impl and sets moduleConfig to the Java class.
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );

		record.loadDescriptor( context );

		// loadDescriptor() must NOT have loaded a BX config (there is no ModuleConfig.bx)
		assertThat( record.moduleConfig ).isNull();

		record.register( context );

		// register() must have set the Java config (not a BxModuleConfig proxy)
		assertThat( record.moduleConfig ).isNotNull();
		assertThat( record.moduleConfig ).isNotInstanceOf( BxModuleConfig.class );

		cleanup( record );
	}

	// -------------------------------------------------------------------------
	// BX regression
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "BX-based modules still work correctly (regression)" )
	void testBxModuleStillWorksWithJavaConfigFeatureInPlace() {
		String			bxModulePath	= Paths.get( "./modules/test" ).toAbsolutePath().toString();
		ModuleRecord	record			= new ModuleRecord( bxModulePath );

		record.loadDescriptor( context );

		// BX descriptor must be wrapped in a BxModuleConfig proxy; no Java config
		assertThat( record.moduleConfig ).isNotNull();
		assertThat( record.moduleConfig ).isInstanceOf( BxModuleConfig.class );

		record.register( context );

		assertThat( record.version ).isEqualTo( "2.0.0" );
		assertThat( record.author ).isEqualTo( "Luis Majano" );

		cleanup( record );
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Reads a static boolean field from the IModuleConfig implementation class
	 * loaded by the module's isolated classloader.
	 */
	private static boolean getStaticBooleanFlag( ModuleRecord record, String fieldName ) {
		try {
			Field field = record.moduleConfig.getClass().getField( fieldName );
			return ( boolean ) field.get( null );
		} catch ( Exception e ) {
			throw new RuntimeException( "Could not read static flag [" + fieldName + "]: " + e.getMessage(), e );
		}
	}

	private void cleanup( ModuleRecord record ) {
		try {
			runtime.getConfiguration().unregisterMapping( record.mapping );
			runtime.getConfiguration().unregisterMapping( record.publicMapping );
		} catch ( Exception ignored ) {
		}
		if ( record.getModuleClassLoader() != null ) {
			try {
				record.getModuleClassLoader().close();
			} catch ( Exception ignored ) {
			}
		}
	}

}
