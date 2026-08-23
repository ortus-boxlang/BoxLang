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
package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.ModuleConfig;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.modules.BoxModuleConfig;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Tests for module inception: modules nested inside another module's own {@code modules} folder.
 *
 * <p>
 * Covers discovery and recursion, the parent/child links, registration and activation ordering,
 * the class loader chain that mirrors the module hierarchy, and the settings precedence
 * (child defaults, then parent overrides, then global app config).
 * </p>
 */
class ModuleInceptionTest {

	static BoxRuntime		runtime;
	ModuleService			service;

	/** Fixture tree: inceptionParent > inceptionChild > inceptionGrandchild. */
	static final Path		INCEPTION_MODULES			= Paths.get( "src/test/resources/inception-modules" ).toAbsolutePath();

	/** Fixture tree: disablingParent > disabledChild, where the parent switches the child off. */
	static final Path		INCEPTION_MODULES_DISABLED	= Paths.get( "src/test/resources/inception-modules-disabled" ).toAbsolutePath();

	/** Holds javaJarModule.jar, built by the javaTestModule Gradle subproject. */
	static final Path		JAR_MODULES					= Paths.get( "src/test/jar-modules" ).toAbsolutePath();

	static final Key		PARENT						= Key.of( "inceptionParent" );
	static final Key		CHILD						= Key.of( "inceptionChild" );
	static final Key		GRANDCHILD					= Key.of( "inceptionGrandchild" );
	static final Key		DISABLING_PARENT			= Key.of( "disablingParent" );
	static final Key		DISABLED_CHILD				= Key.of( "disabledChild" );
	static final Key		JAR_MODULE					= Key.of( "javaJarModule" );
	static final Key		JAR_HOST					= Key.of( "jarHostModule" );

	static final List<Key>	ALL_FIXTURES				= List.of(
	    PARENT, CHILD, GRANDCHILD, DISABLING_PARENT, DISABLED_CHILD, JAR_MODULE, JAR_HOST
	);

	@BeforeEach
	void setup() {
		runtime	= BoxRuntime.getInstance( true );
		service	= runtime.getModuleService();
		cleanupFixtures();
	}

	@AfterEach
	void teardown() {
		cleanupFixtures();
	}

	// -------------------------------------------------------------------------
	// Discovery
	// -------------------------------------------------------------------------

	@DisplayName( "A module's own modules folder is discovered recursively, to arbitrary depth" )
	@Test
	void testNestedModulesAreDiscoveredRecursively() {
		service.buildRegistryFromPath( INCEPTION_MODULES );

		assertThat( service.hasModule( PARENT ) ).isTrue();
		assertThat( service.hasModule( CHILD ) ).isTrue();
		assertThat( service.hasModule( GRANDCHILD ) ).isTrue();
	}

	@DisplayName( "Discovery links parent and child records both ways" )
	@Test
	void testParentAndChildAreLinkedBothWays() {
		service.buildRegistryFromPath( INCEPTION_MODULES );

		ModuleRecord	parent		= service.getModuleRecord( PARENT );
		ModuleRecord	child		= service.getModuleRecord( CHILD );
		ModuleRecord	grandchild	= service.getModuleRecord( GRANDCHILD );

		// Downward: a parent knows its children and can target one directly
		assertThat( parent.nestedModules ).contains( CHILD.getName() );
		assertThat( parent.hasNestedModule( CHILD ) ).isTrue();
		assertThat( parent.getNestedModule( CHILD ) ).isSameInstanceAs( child );
		assertThat( child.nestedModules ).contains( GRANDCHILD.getName() );

		// Upward: a child knows its parent
		assertThat( child.parentModule ).isEqualTo( PARENT );
		assertThat( grandchild.parentModule ).isEqualTo( CHILD );

		// A top-level module has no parent, and a leaf has no children
		assertThat( parent.parentModule ).isNull();
		assertThat( grandchild.nestedModules ).isEmpty();
	}

	@DisplayName( "getNestedModule only resolves this module's own children, not arbitrary modules" )
	@Test
	void testGetNestedModuleIsScopedToDirectChildren() {
		service.buildRegistryFromPath( INCEPTION_MODULES );

		ModuleRecord parent = service.getModuleRecord( PARENT );

		// The grandchild is in the registry, but it is not a DIRECT child of the parent
		assertThat( service.hasModule( GRANDCHILD ) ).isTrue();
		assertThat( parent.hasNestedModule( GRANDCHILD ) ).isFalse();
		assertThat( parent.getNestedModule( GRANDCHILD ) ).isNull();
	}

	// -------------------------------------------------------------------------
	// Ordering
	// -------------------------------------------------------------------------

	@DisplayName( "Nested modules register and activate before the module that carries them" )
	@Test
	void testNestedModulesLoadBeforeTheirParent() {
		service.buildRegistryFromPath( INCEPTION_MODULES );

		service.register( PARENT );
		service.activate( PARENT );

		ModuleRecord	parent		= service.getModuleRecord( PARENT );
		ModuleRecord	child		= service.getModuleRecord( CHILD );
		ModuleRecord	grandchild	= service.getModuleRecord( GRANDCHILD );

		// Registering/activating only the parent must cascade down the whole tree
		assertThat( child.registeredOn ).isNotNull();
		assertThat( grandchild.registeredOn ).isNotNull();
		assertThat( child.isActivated() ).isTrue();
		assertThat( grandchild.isActivated() ).isTrue();
		assertThat( parent.isActivated() ).isTrue();

		// Deepest first, on the way both in and up
		assertThat( grandchild.registeredOn ).isAtMost( child.registeredOn );
		assertThat( child.registeredOn ).isAtMost( parent.registeredOn );
		assertThat( grandchild.activatedOn ).isAtMost( child.activatedOn );
		assertThat( child.activatedOn ).isAtMost( parent.activatedOn );
	}

	@DisplayName( "Unloading a module unloads the modules nested inside it first" )
	@Test
	void testNestedModulesUnloadBeforeTheirParent() {
		service.buildRegistryFromPath( INCEPTION_MODULES );
		service.register( PARENT );
		service.activate( PARENT );

		service.unload( PARENT, true );

		// The whole tree comes down with the parent, not just the parent itself
		assertThat( service.hasModule( PARENT ) ).isFalse();
		assertThat( service.hasModule( CHILD ) ).isFalse();
		assertThat( service.hasModule( GRANDCHILD ) ).isFalse();
	}

	// -------------------------------------------------------------------------
	// Class loader hierarchy
	// -------------------------------------------------------------------------

	@DisplayName( "A nested module's class loader is parented to its parent module's loader, up to the runtime loader" )
	@Test
	void testClassLoaderHierarchyMirrorsModuleHierarchy() {
		service.buildRegistryFromPath( INCEPTION_MODULES );
		service.register( PARENT );

		// Module loaders are built with loadParentFirst=false, so they deliberately pass null to
		// URLClassLoader's parent to create an isolation boundary and track the real parent
		// themselves. getDynamicParent() is therefore the chain to assert on, not getParent().
		DynamicClassLoader	parentLoader		= ( DynamicClassLoader ) service.getModuleRecord( PARENT ).getModuleClassLoader();
		DynamicClassLoader	childLoader			= ( DynamicClassLoader ) service.getModuleRecord( CHILD ).getModuleClassLoader();
		DynamicClassLoader	grandchildLoader	= ( DynamicClassLoader ) service.getModuleRecord( GRANDCHILD ).getModuleClassLoader();

		// Each level chains to the one above it...
		assertThat( grandchildLoader.getDynamicParent() ).isSameInstanceAs( childLoader );
		assertThat( childLoader.getDynamicParent() ).isSameInstanceAs( parentLoader );
		// ...and the top-level module chains to the runtime loader
		assertThat( parentLoader.getDynamicParent() ).isSameInstanceAs( runtime.getRuntimeLoader() );
	}

	@DisplayName( "The class loader is built once and reused" )
	@Test
	void testClassLoaderCreationIsIdempotent() {
		service.buildRegistryFromPath( INCEPTION_MODULES );

		ModuleRecord child = service.getModuleRecord( CHILD );

		assertThat( child.getOrCreateModuleClassLoader() ).isSameInstanceAs( child.getOrCreateModuleClassLoader() );
	}

	// -------------------------------------------------------------------------
	// Settings precedence
	// -------------------------------------------------------------------------

	@DisplayName( "A parent module's overrides beat the child's own configure() defaults" )
	@Test
	void testParentOverridesBeatChildDefaults() {
		service.buildRegistryFromPath( INCEPTION_MODULES );
		service.register( PARENT );

		ModuleRecord child = service.getModuleRecord( CHILD );

		// Overridden by the parent
		assertThat( child.settings.getAsString( Key.of( "overriddenByParent" ) ) ).isEqualTo( "fromParent" );
		// Contributed only by the parent
		assertThat( child.settings.getAsString( Key.of( "parentOnlySetting" ) ) ).isEqualTo( "addedByParent" );
		// Untouched by the parent: the merge is additive, not wholesale replacement
		assertThat( child.settings.getAsString( Key.of( "ownSetting" ) ) ).isEqualTo( "fromChild" );
	}

	@DisplayName( "The global app config beats a parent module's overrides" )
	@Test
	void testGlobalConfigBeatsParentOverrides() {
		ModuleConfig globalConfig = new ModuleConfig( CHILD.getName() );
		globalConfig.settings.put( Key.of( "overriddenByParent" ), "fromGlobalConfig" );
		runtime.getConfiguration().modules.put( CHILD, globalConfig );

		try {
			service.buildRegistryFromPath( INCEPTION_MODULES );
			service.register( PARENT );

			ModuleRecord child = service.getModuleRecord( CHILD );

			// Global app config is applied last and wins over the parent
			assertThat( child.settings.getAsString( Key.of( "overriddenByParent" ) ) ).isEqualTo( "fromGlobalConfig" );
			// The parent's other contribution still stands where global config is silent
			assertThat( child.settings.getAsString( Key.of( "parentOnlySetting" ) ) ).isEqualTo( "addedByParent" );
		} finally {
			runtime.getConfiguration().modules.remove( CHILD );
		}
	}

	@DisplayName( "A parent module can disable a module nested inside it" )
	@Test
	void testParentCanDisableNestedModule() {
		service.buildRegistryFromPath( INCEPTION_MODULES_DISABLED );
		service.register( DISABLING_PARENT );

		ModuleRecord disabledChild = service.getModuleRecord( DISABLED_CHILD );

		// The child declares itself enabled; only the parent's override switches it off
		assertThat( disabledChild.isEnabled() ).isFalse();

		service.activate( DISABLING_PARENT );
		assertThat( disabledChild.isActivated() ).isFalse();
	}

	@DisplayName( "Disabling a module also skips the modules nested inside it" )
	@Test
	void testDisablingAModuleSkipsItsNestedModules() {
		ModuleConfig globalConfig = new ModuleConfig( PARENT.getName() );
		globalConfig.enabled = false;
		runtime.getConfiguration().modules.put( PARENT, globalConfig );

		try {
			service.buildRegistryFromPath( INCEPTION_MODULES );
			service.register( PARENT );
			service.activate( PARENT );

			// A nested module's class loader chains to its parent's, so it cannot load without it
			assertThat( service.getModuleRecord( PARENT ).registeredOn ).isNull();
			assertThat( service.getModuleRecord( CHILD ).registeredOn ).isNull();
			assertThat( service.getModuleRecord( GRANDCHILD ).registeredOn ).isNull();
			assertThat( service.getModuleRecord( CHILD ).isActivated() ).isFalse();
		} finally {
			runtime.getConfiguration().modules.remove( PARENT );
		}
	}

	// -------------------------------------------------------------------------
	// Jar modules
	// -------------------------------------------------------------------------

	@DisplayName( "A bare jar sitting in a modules folder is discovered as a module in its own right" )
	@Test
	void testJarInModulesFolderIsAModule() {
		service.buildRegistryFromPath( JAR_MODULES );

		assertThat( service.hasModule( JAR_MODULE ) ).isTrue();

		ModuleRecord record = service.getModuleRecord( JAR_MODULE );
		// The jar's base name becomes the module name, with the extension stripped
		assertThat( record.name ).isEqualTo( JAR_MODULE );
		assertThat( record.isJarModule() ).isTrue();
	}

	@DisplayName( "A jar module's descriptor is found via ServiceLoader on its own class loader" )
	@Test
	void testJarModuleResolvesItsJavaConfig() {
		service.buildRegistryFromPath( JAR_MODULES );
		service.register( JAR_MODULE );
		service.activate( JAR_MODULE );

		ModuleRecord record = service.getModuleRecord( JAR_MODULE );

		// The IModuleConfig comes from the jar itself, not from any surrounding folder
		assertThat( record.moduleConfig ).isNotNull();
		assertThat( record.moduleConfig ).isNotInstanceOf( BoxModuleConfig.class );
		assertThat( record.isActivated() ).isTrue();

		// @BoxModule annotation metadata is applied
		assertThat( record.version ).isEqualTo( "2.0.0" );
		assertThat( record.author ).isEqualTo( "Ortus Solutions" );

		// configure() ran against this record
		assertThat( record.settings.getAsString( Key.of( "javaKey" ) ) ).isEqualTo( "javaValue" );
	}

	@DisplayName( "A jar module nested inside another module loads before it, chained to its loader" )
	@Test
	void testJarModuleNestedInsideAnotherModule( @TempDir Path tempDir ) throws IOException {
		// Build a module folder on the fly that carries the jar module inside its own modules folder
		Path hostModule = tempDir.resolve( "jarHostModule" );
		Files.createDirectories( hostModule.resolve( ModuleService.MODULE_PACKAGE_PREFIX ) );
		Files.writeString(
		    hostModule.resolve( ModuleService.MODULE_DESCRIPTOR ),
		    """
		    class {
		    	this.version = "1.0.0";
		    	this.mapping = "jarHostModule";
		    	this.enabled = true;

		    	function configure(){
		    		settings					= {};
		    		interceptors				= [];
		    		customInterceptionPoints	= [];
		    	}
		    }
		    """
		);
		Files.copy(
		    JAR_MODULES.resolve( "javaJarModule.jar" ),
		    hostModule.resolve( ModuleService.MODULE_PACKAGE_PREFIX ).resolve( "javaJarModule.jar" )
		);

		try {
			service.loadModule( hostModule );

			ModuleRecord	host	= service.getModuleRecord( JAR_HOST );
			ModuleRecord	jar		= service.getModuleRecord( JAR_MODULE );

			// The jar is a full child module of the host
			assertThat( host.hasNestedModule( JAR_MODULE ) ).isTrue();
			assertThat( jar.parentModule ).isEqualTo( JAR_HOST );
			assertThat( jar.isActivated() ).isTrue();
			assertThat( jar.activatedOn ).isAtMost( host.activatedOn );

			// ...and its class loader chains to the host's, like any other nested module
			DynamicClassLoader	hostLoader	= ( DynamicClassLoader ) host.getModuleClassLoader();
			DynamicClassLoader	jarLoader	= ( DynamicClassLoader ) jar.getModuleClassLoader();
			assertThat( jarLoader.getDynamicParent() ).isSameInstanceAs( hostLoader );
		} finally {
			cleanupModule( JAR_HOST );
		}
	}

	// -------------------------------------------------------------------------
	// loadModule
	// -------------------------------------------------------------------------

	@DisplayName( "loadModule on a single module also loads the modules nested inside it" )
	@Test
	void testLoadModulePicksUpNestedModules() {
		service.loadModule( INCEPTION_MODULES.resolve( "inceptionParent" ) );

		assertThat( service.getModuleRecord( PARENT ).isActivated() ).isTrue();
		assertThat( service.getModuleRecord( CHILD ).isActivated() ).isTrue();
		assertThat( service.getModuleRecord( GRANDCHILD ).isActivated() ).isTrue();
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Removes the fixture modules and their mappings so each test starts from a clean registry
	 * and tests cannot bleed into one another.
	 */
	private void cleanupFixtures() {
		ALL_FIXTURES.forEach( this::cleanupModule );
	}

	/**
	 * Unloads one fixture module and drops it from the registry, ignoring the failures expected
	 * when a module never got far enough to register mappings or activate.
	 *
	 * @param name The module to clean up
	 */
	private void cleanupModule( Key name ) {
		ModuleRecord record = service.getModuleRecord( name );
		if ( record == null ) {
			return;
		}

		try {
			service.unload( name, true );
		} catch ( Exception ignored ) {
			// Best effort: an un-activated record has nothing to unload
		}

		try {
			runtime.getConfiguration().unregisterMapping( record.mapping );
			runtime.getConfiguration().unregisterMapping( record.publicMapping );
		} catch ( Exception ignored ) {
			// Mappings are only registered once a module registers successfully
		}

		service.getRegistry().remove( name );
	}

}
