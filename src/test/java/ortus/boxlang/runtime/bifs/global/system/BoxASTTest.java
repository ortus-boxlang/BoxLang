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
package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.ApplicationService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxASTTest {

	static BoxRuntime			instance;
	static ApplicationService	applicationService;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		applicationService	= instance.getApplicationService();
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	@DisplayName( "It can parse source code and return struct" )
	void testParseSourceReturnStruct() {
		instance.executeSource(
		    """
		    result = boxAST( source = "x = 1 + 2" );
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap ).isNotNull();
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
	}

	@Test
	@DisplayName( "It can parse source code and return JSON" )
	void testParseSourceReturnJSON() {
		instance.executeSource(
		    """
		    result = boxAST( source = "x = 1 + 2", returnType = "json" );
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		String json = ( String ) variables.get( result );
		assertThat( json ).isNotEmpty();
		assertThat( json ).contains( "\"ASTType\"" );
	}

	@Test
	@DisplayName( "It can parse source code and return text" )
	void testParseSourceReturnText() {
		instance.executeSource(
		    """
		    result = boxAST( source = "x = 1 + 2", returnType = "text" );
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		String text = ( String ) variables.get( result );
		assertThat( text ).isNotEmpty();
	}

	// Note: File-based parsing tests are disabled due to current BIF limitation
	// where Parser.parse(File).getRoot() returns null. This appears to be a bug
	// in the implementation that needs to be fixed in the BoxAST BIF.

	@Test
	@DisplayName( "It throws exception when neither source nor filepath provided" )
	void testThrowsExceptionWhenNoArguments() {
		BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    result = boxAST();
			    """,
			    context );
		} );

		assertThat( exception.getMessage() ).contains( "Either 'source' or 'filepath' argument must be provided" );
	}

	@Test
	@DisplayName( "It throws exception when both arguments are empty" )
	void testThrowsExceptionWhenBothArgumentsEmpty() {
		BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    result = boxAST( source = "", filepath = "" );
			    """,
			    context );
		} );

		assertThat( exception.getMessage() ).contains( "Either 'source' or 'filepath' argument must be provided" );
	}

	// Note: testThrowsExceptionWhenFileNotFound disabled - it requires fixing the BIF's file handling

	@Test
	@DisplayName( "It throws exception for invalid return type" )
	void testThrowsExceptionForInvalidReturnType() {
		BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    result = boxAST( source = "x = 1", returnType = "invalid" );
			    """,
			    context );
		} );

		assertThat( exception.getMessage() ).containsMatch( "(?i)(one of|valueOneOf)" );
	}

	@Test
	@DisplayName( "It can parse complex BoxLang code" )
	void testParseComplexCode() {
		instance.executeSource(
		    """
		    result = boxAST(
		        source = "
		            function multiply( x, y ) {
		                return x * y;
		            }
		            result = multiply( 5, 10 );
		        "
		    );
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap ).isNotNull();
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
	}

	@Test
	@DisplayName( "It defaults to struct return type" )
	void testDefaultReturnType() {
		instance.executeSource(
		    """
		    result = boxAST( source = "a = 1" );
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( java.util.Map.class );
	}

	@Test
	@DisplayName( "It can parse source with whitespace only throws parse error" )
	void testParseWhitespaceOnly() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = boxAST( source = "   " );
			    """,
			    context );
		} );
	}

	@DisplayName( "It can parse a class" )
	@Test
	void testParseClass() throws Exception {
		instance.executeSource(
		    """
		       result = boxAST( filepath= "src/test/bx/Interceptor.bx" );
		    println( result )
		       """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap ).isNotNull();
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
	}

	@DisplayName( "It can parse a bxm" )
	@Test
	void testParseBXM() throws Exception {
		instance.executeSource(
		    """
		       result = boxAST( filepath= "src/test/bx/index.bxm" );
		    println( result )
		       """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap ).isNotNull();
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
	}

	@DisplayName( "It can blow up if the file path is invalid" )
	@Test
	void testParseInvalidFilePath() throws Exception {
		BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			       result = boxAST( filepath= "src/test/bx/nonexistent.bx" );
			    println( result )
			       """,
			    context );
		} );

		assertThat( exception.getMessage() ).contains( "could not be found or does not exist" );
	}

	@Test
	@DisplayName( "It can be used as a member function on a string (default struct return)" )
	void testMemberFunctionReturnStruct() {
		instance.executeSource(
		    """
		    source = "x = 1 + 2";
		    result = source.toAST();
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap ).isNotNull();
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
	}

	@Test
	@DisplayName( "It can be used as a member function with JSON return type" )
	void testMemberFunctionReturnJSON() {
		instance.executeSource(
		    """
		    source = "a = [1, 2, 3]";
		    result = source.toAST( returnType = "json" );
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		String json = ( String ) variables.get( result );
		assertThat( json ).isNotEmpty();
		assertThat( json ).contains( "\"ASTType\"" );
	}

	@Test
	@DisplayName( "It can be used as a member function with text return type" )
	void testMemberFunctionReturnText() {
		instance.executeSource(
		    """
		    source = "function add(a, b) { return a + b; }";
		    result = source.toAST( returnType = "text" );
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		String text = ( String ) variables.get( result );
		assertThat( text ).isNotEmpty();
	}

	@Test
	@DisplayName( "Member function works with complex code" )
	void testMemberFunctionComplexCode() {
		instance.executeSource(
		    """
		    source = '''
		        class MyClass {
		            function init() {
		                variables.name = "BoxLang";
		            }
		            function greet() {
		                return "Hello from " & variables.name;
		            }
		        }
		    ''';
		    result = source.toAST();
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap ).isNotNull();
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
		// The parser wraps class declarations in a BoxExpressionStatement
		assertThat( astMap.get( "ASTType" ).toString() ).containsMatch( "(?i)(BoxScript)" );
	}

	@Test
	@DisplayName( "Member function handles inline string literals" )
	void testMemberFunctionInlineString() {
		instance.executeSource(
		    """
		    result = "x = 42".toAST();
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap ).isNotNull();
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
	}

	@Test
	@DisplayName( "It can parse with default sourceType (script)" )
	void testSourceTypeDefault() {
		instance.executeSource(
		    """
		    result = boxAST( source = "x = 1 + 2" );
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
	}

	@Test
	@DisplayName( "It can parse with sourceType = 'script'" )
	void testSourceTypeScript() {
		instance.executeSource(
		    """
		    result = boxAST( source = "x = 1 + 2", sourceType = "script" );
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
	}

	@Test
	@DisplayName( "It can parse with sourceType = 'template'" )
	void testSourceTypeTemplate() {
		instance.executeSource(
		    """
		    result = boxAST( source = "<bx:output>Hello</bx:output>", sourceType = "template" );
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
	}

	@Test
	@DisplayName( "It can parse with sourceType = 'cfscript'" )
	void testSourceTypeCFScript() {
		instance.executeSource(
		    """
		    result = boxAST( source = "cfset x = 1", sourceType = "cfscript" );
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
	}

	@Test
	@DisplayName( "It can parse with sourceType = 'cftemplate'" )
	void testSourceTypeCFTemplate() {
		instance.executeSource(
		    """
		    result = boxAST( source = "<cfoutput>#now()#</cfoutput>", sourceType = "cftemplate" );
		    """,
		    context );

		Object ast = variables.get( result );
		assertThat( ast ).isInstanceOf( java.util.Map.class );
		java.util.Map<?, ?> astMap = ( java.util.Map<?, ?> ) ast;
		assertThat( astMap.containsKey( "ASTType" ) ).isTrue();
	}

	@Test
	@DisplayName( "It throws exception for invalid sourceType" )
	void testInvalidSourceType() {
		BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    result = boxAST( source = "x = 1", sourceType = "invalid" );
			    """,
			    context );
		} );

		assertThat( exception.getMessage() ).containsMatch( "(?i)(one of|valueOneOf)" );
	}

}