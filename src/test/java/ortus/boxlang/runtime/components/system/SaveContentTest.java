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

package ortus.boxlang.runtime.components.system;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class SaveContentTest {

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

	@DisplayName( "It can capture content script" )
	@Test
	public void testCanCaptureContentScript() {

		instance.executeSource(
		    """
		       	echo( "before" );
		          bx:saveContent variable="result" {
		       	echo( "Hello World" );
		       }
		    echo( "after" );
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content ACF script" )
	@Test
	public void testCanCaptureContentACFScript() {

		instance.executeSource(
		    """
		       	echo( "before" );
		          cfsaveContent( variable="result" ) {
		       	echo( "Hello World" );
		       }
		    echo( "after" );
		          """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content tag" )
	@Test
	public void testCanCaptureContentTag() {

		instance.executeSource(
		    """
		    before
		       <cfsaveContent variable="result" >
		       	Hello World
		      </cfsaveContent>
		    after
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content BL tag" )
	@Test
	public void testCanCaptureContentBLTag() {

		instance.executeSource(
		    """
		    before
		       <bx:saveContent variable="result" >
		       	Hello World
		      </bx:saveContent>
		    after
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture trimmed content tag" )
	@Test
	public void testCanCaptureTrimmedContentTag() {

		instance.executeSource(
		    """
		    before
		       <cfsaveContent variable="result" trim=true >
		       	Hello World
		       </cfsaveContent>
		    after
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture appended content tag" )
	@Test
	public void testCanCaptureAppendedContentTag() {

		instance.executeSource(
		    """
		    <cfset result = "before-">
		       <cfsaveContent variable="variables.result" trim=true append="true" >
		       	Hello World
		       </cfsaveContent>

		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "before-Hello World" );
	}

	@DisplayName( "It can capture content with the ACF savecontent expression syntax" )
	@Test
	public void testCanCaptureContentExpressionSyntax() {
		instance.executeSource(
		    """
		    result = savecontent {
		    	echo( "Hello World" );
		    }
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content with the expression syntax into bracket notation" )
	@Test
	public void testCanCaptureContentExpressionSyntaxBracket() {
		instance.executeSource(
		    """
		    data = {};
		    data[ "html" ] = savecontent {
		    	echo( "Hello World" );
		    }
		    result = data.html;
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture trimmed content with the expression syntax" )
	@Test
	public void testCanCaptureTrimmedContentExpressionSyntax() {
		instance.executeSource(
		    """
		    result = savecontent trim=true {
		    	echo( "   Hello World   " );
		    }
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content with the expression syntax into a var scoped variable" )
	@Test
	public void testCanCaptureContentExpressionSyntaxVar() {
		instance.executeSource(
		    """
		    function getContent() {
		    	var html = savecontent {
		    		echo( "Hello World" );
		    	}
		    	return html;
		    }
		    result = getContent();
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
		// var keeps it local to the function, so it must not leak into variables
		assertThat( variables.containsKey( new Key( "html" ) ) ).isFalse();
	}

	@DisplayName( "It still supports the regular savecontent statement in script" )
	@Test
	public void testRegularSaveContentStatementStillWorks() {
		instance.executeSource(
		    """
		    savecontent variable="result" {
		    	echo( "Hello World" );
		    }
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It errors when final is used with the expression syntax" )
	@Test
	public void testExpressionSyntaxRejectsFinal() {
		org.junit.jupiter.api.Assertions.assertThrows( ortus.boxlang.runtime.types.exceptions.ParseException.class,
		    () -> instance.executeSource(
		        """
		        function getContent() {
		        	final var html = savecontent { echo( "Hello World" ); }
		        	return html;
		        }
		        """,
		        context, BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "It errors when static is used with the expression syntax" )
	@Test
	public void testExpressionSyntaxRejectsStatic() {
		org.junit.jupiter.api.Assertions.assertThrows( ortus.boxlang.runtime.types.exceptions.ParseException.class,
		    () -> instance.executeSource(
		        """
		        static html = savecontent { echo( "Hello World" ); }
		        """,
		        context, BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "It errors when the variable attribute is also passed to the expression syntax" )
	@Test
	public void testExpressionSyntaxRejectsVariableAttribute() {
		org.junit.jupiter.api.Assertions.assertThrows( ortus.boxlang.runtime.types.exceptions.ParseException.class,
		    () -> instance.executeSource(
		        """
		        result = savecontent variable="other" { echo( "Hello World" ); }
		        """,
		        context, BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "It errors when the expression syntax is not a plain assignment statement" )
	@Test
	public void testExpressionSyntaxRejectsNestedAssignment() {
		org.junit.jupiter.api.Assertions.assertThrows( ortus.boxlang.runtime.types.exceptions.ParseException.class,
		    () -> instance.executeSource(
		        """
		        a = b = savecontent { echo( "Hello World" ); }
		        """,
		        context, BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "It does not write the captured content to the page with the expression syntax" )
	@Test
	public void testExpressionSyntaxDoesNotWriteToPage() {
		instance.executeSource(
		    """
		    echo( "before" );
		    result = savecontent {
		    	echo( "Hello World" );
		    }
		    echo( "after" );
		    output = getBoxContext().getBuffer().toString();
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsString( result ) ).isEqualTo( "Hello World" );
		// The captured content is not written to the page
		assertThat( variables.getAsString( Key.of( "output" ) ) ).isEqualTo( "beforeafter" );
	}

	@DisplayName( "It can capture content into a scoped variable with the expression syntax" )
	@Test
	public void testCanCaptureContentExpressionSyntaxScoped() {
		instance.executeSource(
		    """
		    variables.result = saveContent {
		    	writeOutput( "Hello World" );
		    };
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsString( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content with the expression syntax inside cfscript in a template" )
	@Test
	public void testCanCaptureContentExpressionSyntaxInTemplate() {
		instance.executeSource(
		    """
		    <cfscript>
		    	result = savecontent {
		    		writeOutput( "Hello World" );
		    	}
		    </cfscript>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content with the expression syntax into an existing local variable" )
	@Test
	public void testCanCaptureContentExpressionSyntaxExistingLocal() {
		instance.executeSource(
		    """
		    function getContent() {
		    	var out = "";
		    	out = savecontent {
		    		echo( "Hello World" );
		    	}
		    	return out;
		    }
		    result = getContent();
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
		assertThat( variables.containsKey( Key.of( "out" ) ) ).isFalse();
	}

	@DisplayName( "A variable named savecontent can still be assigned" )
	@Test
	public void testVariableNamedSaveContent() {
		instance.executeSource(
		    """
		    savecontent = "Hello World";
		    result = savecontent;
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

}
