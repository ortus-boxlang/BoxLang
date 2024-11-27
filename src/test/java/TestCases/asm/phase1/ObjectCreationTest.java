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
package TestCases.asm.phase1;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ObjectCreationTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			resultKey	= new Key( "result" );

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
		instance.useASMBoxPiler();
	}

	@AfterEach
	public void teardownEach() {
		// instance.useJavaBoxpiler();
	}

	@DisplayName( "new keyword prefix" )
	@Test
	public void testNewKeywordPrefix() {
		Object result = instance.executeStatement( "new java:java.lang.String( 'My String' )", context );
		assertThat( result instanceof DynamicObject ).isEqualTo( true );
		assertThat( ( ( DynamicObject ) result ).getTargetInstance() ).isEqualTo( "My String" );
	}

	@DisplayName( "new keyword no prefix" )
	@Test
	public void testNewKeywordNoPrefix() {
		Object result = instance.executeStatement( "new java.lang.String( 'My String' )", context );
		assertThat( result instanceof DynamicObject ).isEqualTo( true );
		assertThat( ( ( DynamicObject ) result ).getTargetInstance() ).isEqualTo( "My String" );
	}

	@DisplayName( "new keyword no prefix2" )
	@Test
	public void testNewKeywordNoPrefix2() {
		Object result = instance.executeStatement( "new ortus.boxlang.runtime.types.Array()", context );
		assertThat( result instanceof DynamicObject ).isEqualTo( true );
	}

	@DisplayName( "new keyword quoted" )
	@Test
	public void testNewKeywordQuoted() {
		Object result = instance.executeStatement( "new 'java:java.lang.String'( 'My String' );", context );
		assertThat( result instanceof DynamicObject ).isEqualTo( true );
		assertThat( ( ( DynamicObject ) result ).getTargetInstance() ).isEqualTo( "My String" );

		instance.executeSource(
		    """
		    classNameToCreate = 'java:java.lang.String';
		    result = new "#classNameToCreate#"( 'My String' );
		    """,
		    context );
		assertThat( variables.get( resultKey ) instanceof DynamicObject ).isEqualTo( true );
		assertThat( ( ( DynamicObject ) result ).getTargetInstance() ).isEqualTo( "My String" );

	}
	/*
	 * @DisplayName( "create keyword prefix" )
	 *
	 * @Test
	 * public void testCreateKeywordPrefix() {
	 * Object result = instance.executeStatement( "create java:java.lang.System", context );
	 * assertThat( result instanceof DynamicObject ).isEqualTo( true );
	 * assertThat( ( ( DynamicObject ) result ).getTargetClass().getName() ).isEqualTo( "java.lang.System" );
	 * }
	 *
	 * @DisplayName( "create keyword no prefix" )
	 *
	 * @Test
	 * public void testCreateKeywordNoPrefix() {
	 * Object result = instance.executeStatement( "create java.lang.System", context );
	 * assertThat( result instanceof DynamicObject ).isEqualTo( true );
	 * assertThat( ( ( DynamicObject ) result ).getTargetClass().getName() ).isEqualTo( "java.lang.System" );
	 * }
	 *
	 * @DisplayName( "create keyword quoted" )
	 *
	 * @Test
	 * public void testCreateKeywordQuoted() {
	 * Object result = instance.executeStatement( "create 'java:java.lang.System';", context );
	 * assertThat( result instanceof DynamicObject ).isEqualTo( true );
	 * assertThat( ( ( DynamicObject ) result ).getTargetClass().getName() ).isEqualTo( "java.lang.System" );
	 *
	 * instance.executeSource(
	 * """
	 * classNameToCreate = 'java:java.lang.System';
	 * result = create "#classNameToCreate#";
	 * """,
	 * context );
	 * assertThat( variables.get( resultKey ) instanceof DynamicObject ).isEqualTo( true );
	 * assertThat( ( ( DynamicObject ) variables.get( resultKey ) ).getTargetClass().getName() ).isEqualTo( "java.lang.System" );
	 * }
	 *
	 * @DisplayName( "create keyword static method call one-liner" )
	 *
	 * @Test
	 * public void testCreateKeywordstaticMethodCallOneLiner() {
	 * instance.executeStatement( "(create java.lang.System).out.println( 2+3 )", context );
	 * instance.executeSource(
	 * """
	 * (create java.lang.System).out.println( 2+3 )
	 * """,
	 * context );
	 *
	 * }
	 */

	@DisplayName( "imports prefix" )
	@Test
	public void testImportsPrefix() {
		instance.executeSource(
		    """
		    import java:java.lang.String;
		    result = new java:String( 'My String' );
		    """,
		    context );

		assertThat( variables.get( resultKey ) instanceof DynamicObject ).isEqualTo( true );
		assertThat( ( ( DynamicObject ) variables.get( resultKey ) ).getTargetInstance() ).isEqualTo( "My String" );
	}

	@DisplayName( "imports no prefix" )
	@Test
	public void testImportsNoPrefix() {
		instance.executeSource(
		    """
		    import java.lang.String;
		    result = new String( 'My String' );
		    """,
		    context );

		assertThat( variables.get( resultKey ) instanceof DynamicObject ).isEqualTo( true );
		assertThat( ( ( DynamicObject ) variables.get( resultKey ) ).getTargetInstance() ).isEqualTo( "My String" );
	}

	@DisplayName( "imports as" )
	@Test
	public void testImportsAs() {
		instance.executeSource(
		    """
		    import java.lang.String as jString;
		    result = new jString( 'My String' );
		    """,
		    context );

		assertThat( variables.get( resultKey ) instanceof DynamicObject ).isEqualTo( true );
		assertThat( ( ( DynamicObject ) variables.get( resultKey ) ).getTargetInstance() ).isEqualTo( "My String" );

	}

}
