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
package ortus.boxlang.runtime.loader;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.BoxFQN;
import ortus.boxlang.runtime.util.FQN;

/**
 * Verifies the single, consolidated {@link IClassLoaderFactory} seam that governs BOTH the
 * runtime root loader and each module's isolated loader. The default reproduces the standard
 * JVM {@link DynamicClassLoader} behavior; swapping the factory (installed at boot via
 * {@link BoxRuntime#setClassLoaderFactory}) lets a target change all class-loader construction
 * in one place — without forking {@code BoxRuntime}/{@code ModuleService}/{@code ModuleRecord}.
 */
class ClassLoaderFactoryTest {

	static BoxRuntime	runtime;
	IClassLoaderFactory	original;

	@BeforeAll
	static void setUpClass() {
		runtime = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	void saveFactory() {
		original = runtime.getClassLoaderFactory();
	}

	@AfterEach
	void restoreFactory() {
		BoxRuntime.setClassLoaderFactory( original );
	}

	@DisplayName( "The default class loader factory is the standard JVM factory" )
	@Test
	void testDefaultFactory() {
		assertThat( original ).isInstanceOf( DynamicClassLoaderFactory.class );
	}

	@DisplayName( "The booted runtime loader was built by the factory (a DynamicClassLoader)" )
	@Test
	void testBootedRuntimeLoader() {
		assertThat( runtime.getRuntimeLoader() ).isInstanceOf( DynamicClassLoader.class );
	}

	@DisplayName( "The factory builds a runtime loader parented to the app loader" )
	@Test
	void testCreateRuntimeClassLoader() {
		ClassLoader created = new DynamicClassLoaderFactory().createRuntimeClassLoader( runtime, getClass().getClassLoader() );
		assertThat( created ).isInstanceOf( DynamicClassLoader.class );
	}

	@DisplayName( "A swapped factory yields a non-DynamicClassLoader runtime loader" )
	@Test
	void testSwapRuntimeFactory() {
		ClassLoader appParent = getClass().getClassLoader();
		// A target like Android returns the app loader directly (no URLClassLoader).
		BoxRuntime.setClassLoaderFactory( new DynamicClassLoaderFactory() {

			@Override
			public ClassLoader createRuntimeClassLoader( BoxRuntime rt, ClassLoader parent ) {
				return parent;
			}
		} );

		ClassLoader created = runtime.getClassLoaderFactory().createRuntimeClassLoader( runtime, appParent );
		assertThat( created ).isSameInstanceAs( appParent );
		assertThat( created ).isNotInstanceOf( DynamicClassLoader.class );
	}

	@DisplayName( "ModuleRecord.register builds the module loader through the factory" )
	@Test
	void testModuleLoaderUsesFactory() {
		AtomicReference<String>	seenModule	= new AtomicReference<>();

		// Delegating factory: record the module loader request, then defer to default behavior.
		IClassLoaderFactory		original	= runtime.getClassLoaderFactory();
		BoxRuntime.setClassLoaderFactory( new DynamicClassLoaderFactory() {

			@Override
			public IModuleClassLoader createModuleClassLoader( ModuleRecord record, ClassLoader parent ) {
				seenModule.set( record.name.getName() );
				return original.createModuleClassLoader( record, parent );
			}
		} );

		IBoxContext		context			= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		String			physicalPath	= Paths.get( "./modules/test" ).toAbsolutePath().toString();

		ModuleRecord	moduleRecord	= null;
		try {
			moduleRecord = new ModuleRecord( physicalPath )
			    .loadDescriptor( context )
			    .register( context );

			assertThat( seenModule.get() ).isEqualTo( moduleRecord.name.getName() );
			assertThat( moduleRecord.getModuleClassLoader() ).isNotNull();
			assertThat( moduleRecord.getModuleClassLoader().toClassLoader() ).isNotNull();
		} finally {
			if ( moduleRecord != null ) {
				moduleRecord.unload( context );
			}
		}
	}

	@DisplayName( "The default factory builds a DiskClassLoader for generated classes" )
	@Test
	void testDefaultGeneratedClassLoader() {
		IBoxpiler	boxpiler	= RunnableLoader.getInstance().getBoxpiler();
		ClassLoader	created		= new DynamicClassLoaderFactory().createGeneratedClassLoader( runtime, boxpiler, "__ad_hoc_source__" );
		assertThat( created ).isInstanceOf( DiskClassLoader.class );
	}

	@DisplayName( "ClassInfo.getClassLoader() is built through the factory (the AOT seam)" )
	@Test
	void testClassInfoUsesFactoryForGeneratedLoader() {
		ClassLoader				sentinel	= new ClassLoader( getClass().getClassLoader() ) {
											};
		AtomicReference<String>	seenPool	= new AtomicReference<>();

		// A target like Android returns a resolve-only loader instead of a DiskClassLoader.
		BoxRuntime.setClassLoaderFactory( new DynamicClassLoaderFactory() {

			@Override
			public ClassLoader createGeneratedClassLoader( BoxRuntime rt, IBoxpiler boxpiler, String classPoolName ) {
				assertThat( boxpiler ).isNotNull();
				seenPool.set( classPoolName );
				return sentinel;
			}
		} );

		ClassInfo classInfo = ClassInfo.forScript( "x = 1", BoxSourceType.BOXSCRIPT, RunnableLoader.getInstance().getBoxpiler() );
		assertThat( classInfo.getClassLoader() ).isSameInstanceAs( sentinel );
		assertThat( seenPool.get() ).isEqualTo( "__ad_hoc_source__" );
	}

	@DisplayName( "getDiskClass() resolves a generated class by parent delegation — no defineClass" )
	@Test
	void testGeneratedClassResolvesByDelegation() {
		ClassLoader appLoader = getClass().getClassLoader();
		// Resolve-only loader: never defines, only delegates to a parent that already has the class.
		BoxRuntime.setClassLoaderFactory( new DynamicClassLoaderFactory() {

			@Override
			public ClassLoader createGeneratedClassLoader( BoxRuntime rt, IBoxpiler boxpiler, String classPoolName ) {
				return new ClassLoader( appLoader ) {
				};
			}
		} );

		// A ClassInfo whose FQN points at a class already present on the parent loader, simulating
		// an AOT-dexed class. getDiskClass() must resolve it via delegation, loaded by the parent.
		ClassInfo	classInfo	= classInfoFor( "ortus.boxlang.runtime.types.Array" );
		Class<?>	resolved	= classInfo.getDiskClass();
		assertThat( resolved.getName() ).isEqualTo( "ortus.boxlang.runtime.types.Array" );
		assertThat( resolved.getClassLoader() ).isSameInstanceAs( appLoader );

		// A name no loader can resolve fails loudly (never an illegal define).
		assertThrows( BoxRuntimeException.class, () -> classInfoFor( "boxgenerated.scripts.DoesNotExist_zzz" ).getDiskClass() );
	}

	/**
	 * Build a minimal ad-hoc {@link ClassInfo} with an explicit FQN (no file on disk), so the test
	 * controls exactly which class name {@code getDiskClass()} will ask the loader to resolve.
	 */
	private ClassInfo classInfoFor( String fqn ) {
		return new ClassInfo(
		    FQN.of( fqn ),
		    BoxFQN.of( "" ),
		    "BoxScript",
		    "Object",
		    BoxSourceType.BOXSCRIPT,
		    "x = 1",
		    0L,
		    new ClassLoader[ 1 ],
		    null,
		    RunnableLoader.getInstance().getBoxpiler(),
		    null,
		    fqn.hashCode(),
		    new boolean[] { false }
		);
	}

}
