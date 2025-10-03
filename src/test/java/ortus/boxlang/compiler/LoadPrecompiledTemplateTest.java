/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class LoadPrecompiledTemplateTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	public void testPrecompiledTemplate() {
		Path	source	= Path.of( "src/test/java/ortus/boxlang/compiler/Precompiled-source.bxs" );
		Path	target	= Path.of( "src/test/java/ortus/boxlang/compiler/Precompiled.bxs" );
		// delete target, if exists
		if ( target.toFile().exists() ) {
			target.toFile().delete();
		}
		// copy source over target on disk
		try {
			Files.copy( source, target );
		} catch ( Exception e ) {
			e.printStackTrace();
		}

		BXCompiler.compileFile(
		    target,
		    target,
		    true,
		    instance
		);
		instance.executeSource(
		    """
		    include 'src/test/java/ortus/boxlang/compiler/Precompiled.bxs'
		       """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( 4 );
	}

	@Test
	public void testPrecompiledClass() {
		Path	source	= Path.of( "src/test/java/ortus/boxlang/compiler/Precompiled-source.bx" );
		Path	target	= Path.of( "src/test/java/ortus/boxlang/compiler/Precompiled.bx" );
		// delete target, if exists
		if ( target.toFile().exists() ) {
			target.toFile().delete();
		}
		// copy source over target on disk
		try {
			Files.copy( source, target );
		} catch ( Exception e ) {
			e.printStackTrace();
		}

		BXCompiler.compileFile(
		    target,
		    target,
		    true,
		    instance
		);

		// Create some alternative ways to create the same physical class on disk. These should have different names in their
		// bytecode which represents how they were referenced when they were created.
		instance.getConfiguration().registerMapping( "/precompiled/path/to/", source.getParent().toString() );
		instance.getConfiguration().registerMapping( "/another/precompiled/path/whee/", source.getParent().toString() );

		instance.executeSource(
		    """
		      	foo = new src.test.java.ortus.boxlang.compiler.Precompiled();
		      	result = foo.bar()
		    resultName = foo.$bx.meta.name;
		      	foo2 = new precompiled.path.to.Precompiled();
		      	result2 = foo2.bar()
		    resultName2 = foo2.$bx.meta.name;
		      	foo3 = new another.precompiled.path.whee.Precompiled();
		      	result3 = foo3.bar()
		    resultName3 = foo3.$bx.meta.name;
		      """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "brad" );
		assertThat( variables.get( new Key( "resultName" ) ) ).isEqualTo( "src.test.java.ortus.boxlang.compiler.Precompiled" );
		assertThat( variables.get( new Key( "result2" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( new Key( "resultName2" ) ) ).isEqualTo( "precompiled.path.to.Precompiled" );
		assertThat( variables.get( new Key( "result3" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( new Key( "resultName3" ) ) ).isEqualTo( "another.precompiled.path.whee.Precompiled" );

	}

	@Test
	@DisplayName( "Load a pre-compiled class and execute" )
	public void testPrecompiledModuleConfig() {
		Path			target			= Path.of( "src/test/resources/compiledCode/ModuleConfig.bx" );

		// Load the ModuleConfig.bx, Construct it and store it
		IClassRunnable	moduleConfig	= ( IClassRunnable ) DynamicObject.of(
		    RunnableLoader.getInstance().loadClass(
		        ResolvedFilePath.of(
		            null,
		            null,
		            "",
		            target.toString()
		        ),
		        context
		    )
		)
		    .invokeConstructor( context )
		    .getTargetInstance();

	}
}
