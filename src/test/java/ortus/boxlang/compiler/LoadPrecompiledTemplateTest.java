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
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

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
		    instance,
		    Path.of( "" ),
		    "/"
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
		    instance,
		    Path.of( "" ),
		    "/"
		);
		instance.executeSource(
		    """
		       foo = new src.test.java.ortus.boxlang.compiler.Precompiled();
		    result = foo.bar()
		          """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "brad" );

	}
}
