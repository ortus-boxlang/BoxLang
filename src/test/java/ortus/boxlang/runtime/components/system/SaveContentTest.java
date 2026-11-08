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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;

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

	@DisplayName( "It concatenates captured content using compound assignment" )
	@Test
	public void testCanCaptureContentExpressionSyntaxConcatAssignment() {
		instance.executeSource(
		    """
		    result = "before-";
		    result &= savecontent {
		        writeOutput( "after" );
		    }
		    """,
		    this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "before-after" );
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

	@DisplayName( "It rejects attributes on the assignment form" )
	@ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource( strings = { "trim=true", "append=true", "variable=\"other\"" } )
	public void testExpressionSyntaxRejectsAttributes( String attribute ) {
		assertThrows( ParseException.class, () -> instance.executeSource(
		    "result = savecontent " + attribute + " { writeOutput( 'test' ); }", this.context, BoxSourceType.CFSCRIPT ) );
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

	@DisplayName( "It supports final local capture assignments" )
	@Test
	public void testExpressionSyntaxSupportsFinal() {
		instance.executeSource(
		    """
		    function getContent() {
		        final var html = savecontent { writeOutput( "Hello World" ); }
		        return html;
		    }
		    result = getContent();
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "Hello World" );
		var error = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    function changeContent() {
		        final var html = savecontent { writeOutput( "first" ); }
		        html = "second";
		    }
		    changeContent();
		    """, this.context, BoxSourceType.CFSCRIPT ) );
		assertThat( error.getMessage() ).contains( "Cannot reassign final key" );
	}

	@DisplayName( "It supports static capture assignments inside a class" )
	@Test
	public void testExpressionSyntaxSupportsStatic() {
		instance.executeSource(
		    "result = new src.test.java.ortus.boxlang.runtime.components.system.SaveContentStatic().run();",
		    this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "static content" );
	}

	@DisplayName( "It rejects savecontent as a direct function argument" )
	@Test
	public void testExpressionSyntaxRejectsDirectArgument() {
		assertThrows( ParseException.class, () -> instance.executeSource(
		    "writeDump( savecontent { writeOutput( 'test' ); } );", this.context, BoxSourceType.CFSCRIPT ) );
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

	@DisplayName( "It applies arithmetic assignment operators to captured content" )
	@ParameterizedTest
	@CsvSource( { "+=,10", "-=,0", "*=,25", "/=,1", "%=,0" } )
	public void testExpressionSyntaxArithmeticAssignment( String operator, int expected ) {
		instance.executeSource( "result = 5; result " + operator + " savecontent { writeOutput( '5' ); }",
		    this.context, BoxSourceType.CFSCRIPT );
		assertThat( ( ( Number ) this.variables.get( result ) ).doubleValue() ).isEqualTo( ( double ) expected );
	}

	@Test
	public void testExpressionSyntaxComputedTargets() {
		instance.executeSource(
		    """
		    data = {};
		    key = "html";
		    data[ "#key#" ] = savecontent { writeOutput( "first" ); }
		    data[ key & "Suffix" ] = savecontent { writeOutput( "second" ); }
		    result = data.html & data.htmlSuffix;
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "firstsecond" );
		assertThat( this.variables.getAsStruct( Key.of( "data" ) ).containsKey( Key.of( "#key#" ) ) ).isFalse();
	}

	@Test
	public void testExpressionSyntaxFunctionTargetAndDeepAssignment() {
		instance.executeSource(
		    """
		    data = {};
		    calls = 0;
		    function getData() { calls++; return data; }
		    getData().name = savecontent { writeOutput( "brad" ); }
		    foo = { baz: () => data };
		    foo.baz().bar.qux = savecontent { writeOutput( "test" ); }
		    result = data.name & data.bar.qux;
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "bradtest" );
		assertThat( this.variables.getAsInteger( Key.of( "calls" ) ) ).isEqualTo( 1 );
	}

	@Test
	public void testExpressionSyntaxPreservesPreviousLocalDuringCapture() {
		instance.executeSource(
		    """
		    function getContent() {
		        var html = "before";
		        var html = savecontent { writeOutput( html & "after" ); }
		        return html;
		    }
		    result = getContent();
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "beforeafter" );
		assertThat( this.variables.containsKey( Key.of( "html" ) ) ).isFalse();
	}

	@Test
	public void testExpressionSyntaxNestedCapture() {
		instance.executeSource(
		    """
		    result = savecontent {
		        inner = savecontent { writeOutput( "inner" ); }
		        writeOutput( inner & "outer" );
		    }
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "innerouter" );
	}

	@Test
	public void testExpressionSyntaxReturnSkipsAssignment() {
		instance.executeSource(
		    """
		    target = "before";
		    function getContent() {
		        target = savecontent {
		            writeOutput( "discard" );
		            return "exit";
		        }
		        return "unreachable";
		    }
		    result = getContent();
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "exit" );
		assertThat( this.variables.getAsString( Key.of( "target" ) ) ).isEqualTo( "before" );
		assertThat( this.context.getBuffer().toString() ).isEmpty();
	}

	@Test
	public void testExpressionSyntaxRestoresBufferAfterException() {
		instance.executeSource(
		    """
		    result = "before";
		    try {
		        result = savecontent { writeOutput( "discard" ); throw "capture failure"; }
		    } catch ( any e ) {}
		    writeOutput( "after" );
		    output = getBoxContext().getBuffer().toString();
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "before" );
		assertThat( this.variables.getAsString( Key.of( "output" ) ) ).isEqualTo( "after" );
	}

	@Test
	public void testExpressionSyntaxBreakAndContinueInBodyLoops() {
		instance.executeSource(
		    """
		    result = savecontent {
		        for ( i = 1; i <= 5; i++ ) {
		            if ( i == 2 ) continue;
		            if ( i == 4 ) break;
		            writeOutput( i );
		        }
		    }
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "13" );
	}

	@Test
	public void testExpressionSyntaxPropagatesComponentLoopExits() {
		instance.executeSource(
		    """
		    target = "before";
		    count = 0;
		    loop times=3 {
		        count++;
		        target = savecontent { writeOutput( "discard" ); continue; }
		        count += 100;
		    }
		    loop times=3 {
		        count++;
		        target = savecontent { writeOutput( "discard" ); break; }
		        count += 100;
		    }
		    result = count;
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsInteger( result ) ).isEqualTo( 4 );
		assertThat( this.variables.getAsString( Key.of( "target" ) ) ).isEqualTo( "before" );
		assertThat( this.context.getBuffer().toString() ).isEmpty();
	}

	@Test
	public void testExpressionSyntaxDoesNotCaptureDestinationOutput() {
		instance.executeSource(
		    """
		    data = {};
		    function getTarget() { writeOutput( "target" ); return data; }
		    getTarget().html = savecontent { writeOutput( "body" ); }
		    result = data.html;
		    output = getBoxContext().getBuffer().toString();
		    """, this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "body" );
		assertThat( this.variables.getAsString( Key.of( "output" ) ) ).isEqualTo( "target" );
	}
}
