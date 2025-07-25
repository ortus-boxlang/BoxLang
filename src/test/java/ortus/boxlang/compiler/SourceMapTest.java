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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

public class SourceMapTest {

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
	public void testVariableLineNumber() {
		if ( RunnableLoader.getInstance().getBoxpiler().getName().equals( Key.java ) ) {
			return;
		}
		Exception		e		= assertThrows( KeyNotFoundException.class, () -> {
									instance.executeSource(
									    """
									     new src.test.java.ortus.boxlang.compiler.sourcemaptests.MissingVar();
									    """,
									    context );
								} );

		StringWriter	out		= new StringWriter();
		PrintWriter		printer	= new PrintWriter( out );
		e.printStackTrace( printer );

		assertThat( out.toString() ).contains( "MissingVar.bx:3" );
	}

	@Test
	public void testTemmplateMissingVariable() {
		if ( RunnableLoader.getInstance().getBoxpiler().getName().equals( Key.java ) ) {
			return;
		}

		Exception		e		= assertThrows( KeyNotFoundException.class, () -> {
									instance.executeSource(
									    """
									     include template="src/test/java/ortus/boxlang/compiler/sourcemaptests/MissingVarTemplate.bxm";
									    """,
									    context );
								} );

		StringWriter	out		= new StringWriter();
		PrintWriter		printer	= new PrintWriter( out );
		e.printStackTrace( printer );

		assertThat( out.toString() ).contains( "MissingVarTemplate.bxm:14" );
	}

	@Test
	public void testTemmplateMissingVariableInComponent() {
		if ( RunnableLoader.getInstance().getBoxpiler().getName().equals( Key.java ) ) {
			return;
		}
		Exception		e		= assertThrows( KeyNotFoundException.class, () -> {
									instance.executeSource(
									    """
									     include template="src/test/java/ortus/boxlang/compiler/sourcemaptests/ComponentInTemplate.bxm";
									    """,
									    context );
								} );

		StringWriter	out		= new StringWriter();
		PrintWriter		printer	= new PrintWriter( out );
		e.printStackTrace( printer );

		String errorOutput = out.toString();

		assertThat( errorOutput ).contains( "ComponentInTemplate.bxm:5" );
		assertThat( errorOutput ).contains( "ComponentInTemplate.bxm:4" );
		assertThat( errorOutput ).contains( "ComponentInTemplate.bxm:1" );
	}

}
