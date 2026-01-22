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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class LargeMethodTest {

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

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@Test
	public void testShouldNotThrowOnLargeMethod() {
		assertDoesNotThrow( () -> {
			ResolvedFilePath	resolvedPath	= ResolvedFilePath.of( Path.of( "src/test/java/ortus/boxlang/compiler/LargeMethod.cfc" ) );
			var					x				= RunnableLoader.getInstance()
			    .getBoxpiler()
			    .compileClass( resolvedPath );

		} );

	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@Test
	public void testShouldNotThrowOnLargeMethod2() {
		assertDoesNotThrow( () -> {
			ResolvedFilePath	resolvedPath	= ResolvedFilePath.of( Path.of( "src/test/java/ortus/boxlang/compiler/LargeMethod2.cfc" ) );
			var					x				= RunnableLoader.getInstance()
			    .getBoxpiler()
			    .compileClass( resolvedPath );

		} );

	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@Test
	public void testShouldBeAbleToInstantiate() {
		assertDoesNotThrow( () -> {
			instance.executeSource( """
			                        	x = new src.test.java.ortus.boxlang.compiler.LargeMethod2();

			                        """, context );

		} );

	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@Test
	public void testShouldNotThrowOnLargeMethod3() {
		assertDoesNotThrow( () -> {
			instance.executeSource( """
			                        	x = new src.test.java.ortus.boxlang.compiler.LargeMethod3();
			                        	x.test( "while2");

			                        """, context );

		} );

	}
}
