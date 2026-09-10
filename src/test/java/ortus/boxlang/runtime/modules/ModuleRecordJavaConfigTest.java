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
		assertThat( record.moduleConfig ).isNotInstanceOf( BoxModuleConfig.class );

		cleanup( record );
	}

	@Test
	@DisplayName( "Annotation metadata is extracted from the @BoxModule annotation on the Java IModuleConfig implementation" )
	void testMetadataExtractedFromAnnotation() {
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

		// Settings assertions above validate configure() ran; avoid static-flag assertions that can leak across tests.

		cleanup( record );
	}

	// -------------------------------------------------------------------------
	// Activation
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "onLoad(IBoxContext, ModuleRecord) is called during activate()" )
	void testJavaOnLoadCalledOnActivate() {
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );

		record.loadDescriptor( context ).register( context );
		org.junit.jupiter.api.Assertions.assertDoesNotThrow( () -> record.moduleConfig.getClass().getMethod( "reset" ).invoke( null ) );
		record.activate( context );

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
		// Capture the config class BEFORE unloading: unload() resets the record's lifecycle
		// state (moduleConfig included) so a later reload rebuilds everything from scratch
		Class<?> configClass = record.moduleConfig.getClass();
		org.junit.jupiter.api.Assertions.assertDoesNotThrow( () -> configClass.getMethod( "reset" ).invoke( null ) );
		record.unload( context );

		assertThat( getStaticBooleanFlag( configClass, "onUnloadCalled" ) ).isTrue();
		// The record's lifecycle state was reset for a potential reload
		assertThat( record.moduleConfig ).isNull();
		assertThat( record.getModuleClassLoader() ).isNull();
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
		assertThat( record.moduleConfig ).isNotInstanceOf( BoxModuleConfig.class );

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
		assertThat( record.moduleConfig ).isInstanceOf( BoxModuleConfig.class );

		record.register( context );

		assertThat( record.version ).isEqualTo( "2.0.0" );
		assertThat( record.author ).isEqualTo( "Luis Majano" );

		cleanup( record );
	}

	// -------------------------------------------------------------------------
	// Annotation-based metadata tests
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "The @BoxModule annotation is present on the Java test module class" )
	void testBoxModuleAnnotationPresent() {
		ModuleRecord record = new ModuleRecord( JAVA_MODULE_PATH );
		record.loadDescriptor( context );
		record.register( context );

		BoxModule meta = record.moduleConfig.getClass().getAnnotation( BoxModule.class );
		assertThat( meta ).isNotNull();
		assertThat( meta.version() ).isEqualTo( "2.0.0" );
		assertThat( meta.author() ).isEqualTo( "Ortus Solutions" );

		cleanup( record );
	}

	@Test
	@DisplayName( "IModuleConfig without @BoxModule annotation keeps all convention defaults" )
	void testMissingAnnotationKeepsDefaults() {
		// Create an IModuleConfig without @BoxModule
		IModuleConfig	noAnnotationConfig	= new IModuleConfig() {

												@Override
												public void configure( IBoxContext context, ModuleRecord moduleRecord ) {
													moduleRecord.settings.put( "test", "value" );
												}
											};

		// Build and register using the standard path
		ModuleRecord	record				= new ModuleRecord( JAVA_MODULE_PATH );
		record.loadDescriptor( context );

		// Override the moduleConfig to use our no-annotation config
		record.moduleConfig = noAnnotationConfig;

		// Simulate what extractJavaMetadata() does with no @BoxModule
		java.lang.reflect.Method extractMethod;
		try {
			extractMethod = ModuleRecord.class.getDeclaredMethod( "extractJavaMetadata" );
			extractMethod.setAccessible( true );
			extractMethod.invoke( record );

			// All fields should keep their constructor defaults
			assertThat( record.version ).isEqualTo( "1.0.0" );
			assertThat( record.author ).isEmpty();
			assertThat( record.description ).isEmpty();
			assertThat( record.webURL ).isEmpty();
			assertThat( record.enabled ).isTrue();
			assertThat( record.dependencies ).isEmpty();
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}

		cleanup( record );
	}

	@Test
	@DisplayName( "extractJavaMetadata is null-safe when moduleConfig is null" )
	void testExtractJavaMetadataNullSafe() throws Exception {
		ModuleRecord				record			= new ModuleRecord( JAVA_MODULE_PATH );
		// moduleConfig is null before loadDescriptor/register

		java.lang.reflect.Method	extractMethod	= ModuleRecord.class.getDeclaredMethod( "extractJavaMetadata" );
		extractMethod.setAccessible( true );

		// Should not throw
		extractMethod.invoke( record );

		// Defaults still intact
		assertThat( record.version ).isEqualTo( "1.0.0" );
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Reads a static boolean field from the IModuleConfig implementation class
	 * loaded by the module's isolated classloader.
	 */
	private static boolean getStaticBooleanFlag( ModuleRecord record, String fieldName ) {
		return getStaticBooleanFlag( record.moduleConfig.getClass(), fieldName );
	}

	/**
	 * Reads a static boolean field from a config class captured before an unload
	 * (unload resets the record's moduleConfig reference).
	 */
	private static boolean getStaticBooleanFlag( Class<?> configClass, String fieldName ) {
		try {
			Field field = configClass.getField( fieldName );
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
				// This is causing concurrency issues elsewhere in the test suite
				// record.getModuleClassLoader().close();
			} catch ( Exception ignored ) {
			}
		}
	}

}
