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
package ortus.boxlang.compiler.prettyprint;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.config.Config;

/**
 * Formatting must not change what code means, and formatting formatted code must change nothing. Each case formats the
 * source, parses the result and compares the two ASTs (ignoring positions, source text, comments and structural
 * template whitespace), then formats the result again and expects the same text.
 */
@DisplayName( "Formatter round trip tests" )
public class RoundTripTest extends PrettyPrintTest {

	@Test
	@DisplayName( "static, final and abstract function modifiers are kept (BL-2780)" )
	public void testFunctionModifiers() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                                       abstract class {
		                                                       	static function count(){ return 1; }
		                                                       	public static final function make(){ return 2; }
		                                                       	abstract function todo();
		                                                       }
		                                                       """ );
		assertThat( out ).contains( "static function count()" );
		assertThat( out ).contains( "public static final function make()" );
		assertThat( out ).contains( "abstract function todo()" );

		out = roundTrip( BoxSourceType.CFSCRIPT, true, """
		                                               component {
		                                               	static function count(){ return 1; }
		                                               }
		                                               """ );
		assertThat( out ).contains( "static function count()" );
	}

	@Test
	@DisplayName( "final and abstract class keywords print before class and component" )
	public void testClassModifiers() throws IOException {
		assertThat( roundTrip( BoxSourceType.BOXSCRIPT, true, "final class { }" ) ).startsWith( "final class" );
		assertThat( roundTrip( BoxSourceType.CFSCRIPT, true, "abstract component { }" ) ).startsWith( "abstract component" );
	}

	@Test
	@DisplayName( "a single-expression lambda body has no semicolon" )
	public void testLambdaExpressionBody() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, "names = list.map( item -> item.getName() );" );
		assertThat( out ).contains( "-> item.getName() );" );
		assertThat( out ).doesNotContain( ";;" );
	}

	@Test
	@DisplayName( "script attribute values keep their type" )
	public void testScriptAttributeValues() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                                       class serializable=false {
		                                                       	function f(){
		                                                       		lock name="x" timeout=10 throwOnTimeout=true { a = 1; }
		                                                       	}
		                                                       }
		                                                       """ );
		assertThat( out ).contains( "serializable=false" );
		assertThat( out ).contains( "lock name=\"x\" timeout=10 throwOnTimeout=true {" );
	}

	@Test
	@DisplayName( "empty component bodies and body-less tags are kept" )
	public void testComponentBodies() throws IOException {
		assertThat( roundTrip( BoxSourceType.BOXSCRIPT, false, "bx:thread name=\"t\" {}" ) ).contains( "{" );
		String out = roundTrip( BoxSourceType.CFTEMPLATE, false, "<cf_foo>\n<cf_bar />\n<cfabort>\n" );
		assertThat( out ).contains( "<cf_foo>" );
		assertThat( out ).contains( "<cf_bar />" );
	}

	@Test
	@DisplayName( "CF script components print without a cf prefix" )
	public void testCFScriptComponentPrefix() throws IOException {
		String out = roundTrip( BoxSourceType.CFSCRIPT, true, """
		                                                      component {
		                                                      	function f(){
		                                                      		savecontent variable="local.out" { writeOutput( "x" ); }
		                                                      		return local.out;
		                                                      	}
		                                                      }
		                                                      """ );
		assertThat( out ).contains( "savecontent variable=\"local.out\" {" );
		assertThat( out ).doesNotContain( "cfsavecontent" );
	}

	@Test
	@DisplayName( "tag components: imports, arguments, empty functions and interfaces" )
	public void testTagComponents() throws IOException {
		String out = roundTrip( BoxSourceType.CFTEMPLATE, true, """
		                                                        <cfimport prefix="java" name="java.lang.String">
		                                                        <cfcomponent output="false">
		                                                        	<cffunction name="init" access="public" returntype="any">
		                                                        	</cffunction>
		                                                        	<cffunction name="foo">
		                                                        		<cfargument name="a" type="string" required="false">
		                                                        		<cfreturn arguments.a>
		                                                        	</cffunction>
		                                                        </cfcomponent>
		                                                        """ );
		assertThat( out ).contains( "<cfimport prefix=\"java\" name=\"java.lang.String\">" );
		assertThat( out ).contains( "<cfargument name=\"a\"" );
		assertThat( out ).doesNotContain( "/>\n\t<cffunction" );

		roundTrip( BoxSourceType.CFTEMPLATE, true, """
		                                           <cfinterface>
		                                           	<cffunction name="init">
		                                           	</cffunction>
		                                           	<cffunction name="foo">
		                                           	</cffunction>
		                                           </cfinterface>
		                                           """ );
	}

	@Test
	@DisplayName( "a plain block comment never becomes a doc comment" )
	public void testPlainCommentStaysPlain() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                                       class {
		                                                       	/* ****************
		                                                       	 ** DI
		                                                       	 ***************** */
		                                                       	property name="log";
		                                                       }
		                                                       """ );
		assertThat( out ).contains( "/* " );
		assertThat( out ).doesNotContain( "/**" );
	}

	@Test
	@DisplayName( "doc comment banners keep the hint" )
	public void testDocCommentBanners() throws IOException {
		roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                          /**
		                                          ********************************************************
		                                          Copyright Ortus Solutions
		                                          ********************************************************
		                                          * @author Luis
		                                          */
		                                          class {
		                                          	/**
		                                          	 ** PROPERTIES
		                                          	 */
		                                          	property name="a";
		                                          	/**
		                                          	 *	* bullet item
		                                          	 */
		                                          	property name="b";
		                                          }
		                                          """ );
	}

	@Test
	@DisplayName( "a comment before else or catch keeps the branch" )
	public void testCommentBeforeElseAndCatch() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                        if ( a ) {
		                                                        	x = 1;
		                                                        }
		                                                        // the string case
		                                                        else if ( b ) {
		                                                        	x = 2;
		                                                        } // end of b
		                                                        else {
		                                                        	x = 3;
		                                                        }
		                                                        try {
		                                                        	y = 1;
		                                                        }
		                                                        // assertion failures
		                                                        catch ( "TestBox.AssertionFailed" e ) {
		                                                        	y = 2;
		                                                        }
		                                                        """ );
		assertThat( out ).contains( "// the string case\nelse if ( b ) {" );
		assertThat( out ).contains( "// assertion failures\ncatch (" );
	}

	@Test
	@DisplayName( "destructuring patterns and the second for-in variable" )
	public void testDestructuringAndForIn() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                        var [ a, b = 2, [ c, d ], ...rest ] = arr;
		                                                        var { name, address: { city }, zip: z, n = 5, ...others } = user;
		                                                        static [ only, ] = data;
		                                                        for ( var item, i in items ) { println( i ); }
		                                                        """ );
		assertThat( out ).contains( "var [ a, b = 2, [ c, d ], ...rest ] = arr;" );
		assertThat( out ).contains( "var { name, address: { city }, zip: z, n = 5, ...others } = user;" );
		assertThat( out ).contains( "static [ only, ] = data;" );
		assertThat( out ).contains( "for ( var item, i in items )" );
	}

	@Test
	@DisplayName( "castAs types, open ranges, member references and quoted argument names" )
	public void testExpressions() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                        a = x & ( Math.pow( 2, s ) castAs int - 1 );
		                                                        b = ..10;
		                                                        c = 5..;
		                                                        d = tools.map( .getSchema );
		                                                        command( "docbox" ).params( "strategy-outputDir" = dir, source = "models" );
		                                                        """ );
		assertThat( out ).contains( "castAs int - 1" );
		assertThat( out ).contains( "b = ..10;" );
		assertThat( out ).contains( "c = 5..;" );
		assertThat( out ).contains( ".map( .getSchema )" );
		assertThat( out ).contains( "\"strategy-outputDir\" = dir" );
	}

	@Test
	@DisplayName( "template output, comments and conditions" )
	public void testTemplates() throws IOException {
		String out = roundTrip( BoxSourceType.BOXTEMPLATE, false, """
		                                                          <!--- header ---><bx:script>x = 1;</bx:script>
		                                                          <bx:set i = 0>
		                                                          <bx:while condition="i < 3">
		                                                          	<bx:set i++>
		                                                          </bx:while>
		                                                          <bx:if a>
		                                                          	<!--- one --->
		                                                          	one
		                                                          <bx:elseif b>
		                                                          	two
		                                                          <bx:else>
		                                                          	<!--- three --->
		                                                          	three
		                                                          </bx:if>
		                                                          <bx:try>
		                                                          	<bx:set a = 1>
		                                                          	<bx:catch>
		                                                          		<bx:set c = 3>
		                                                          	</bx:catch>
		                                                          	<bx:finally>
		                                                          		<bx:set d = 4>
		                                                          	</bx:finally>
		                                                          </bx:try>
		                                                          """ );
		assertThat( out ).contains( "---><bx:script>" );
		assertThat( out ).contains( "condition=\"i < 3\"" );
		assertThat( out ).contains( "<!--- three --->" );
		assertThat( out ).contains( "<bx:finally>" );
	}

	@Test
	@DisplayName( "comments between chained calls stay with their call" )
	public void testChainComments() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                        var r = qb
		                                                        	.from( "x" )
		                                                        	// Filter by a
		                                                        	.when( a, function( q ){ q.where( 1 ); } )
		                                                        	// Filter by b
		                                                        	.when( b, function( q ){ q.where( 2 ); } )
		                                                        	.first()[ "total" ];
		                                                        """ );
		assertThat( out.indexOf( "// Filter by a" ) ).isLessThan( out.indexOf( ".when(" ) );
		assertThat( out.indexOf( "// Filter by b" ) ).isGreaterThan( out.indexOf( ".when(" ) );
	}

	@Test
	@DisplayName( "a comment after a multi-line call is kept" )
	public void testCommentAfterMultiLineCall() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                        logLines
		                                                        	.append(
		                                                        		jsonSerialize( data = arguments.dataPacket, pretty = true )
		                                                        	); // Pretty print JSON
		                                                        """ );
		assertThat( out ).contains( "); // Pretty print JSON" );
	}

	@Test
	@DisplayName( "struct key alignment and line breaks are stable" )
	public void testStableLayout() throws IOException {
		roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                          class {
		                                          	this.settings = {
		                                          		apiUrl   : "x",
		                                          		features : { beta : false, level : 5 },
		                                          		limits   : [ 1, 2, 3 ]
		                                          	};
		                                          	function onError( task, exception ){
		                                          		println( "Any task [#task.getName()#]  blew up " & exception.getMessage() );
		                                          	}
		                                          	function empty( data ){
		                                          		// nothing yet
		                                          	}
		                                          }
		                                          """ );
	}

	@Test
	@DisplayName( "continue followed by a comment stays a keyword" )
	public void testContinueBeforeComment() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                        for ( item in items ) {
		                                                        	if ( item.skip ) {
		                                                        		continue // not this one
		                                                        	}
		                                                        	total++;
		                                                        }
		                                                        """ );
		assertThat( out ).contains( "continue" );
		assertThat( out ).contains( "// not this one" );
	}

	@Test
	@DisplayName( "a comment after the closing component tag stays after it" )
	public void testCommentAfterClosingComponentTag() throws IOException {
		String out = roundTrip( BoxSourceType.CFTEMPLATE, true, """
		                                                        <cfcomponent>
		                                                        	<cffunction name="foo">
		                                                        	</cffunction>
		                                                        </cfcomponent>
		                                                        <!--- end of file --->
		                                                        """ );
		assertThat( out.indexOf( "<!--- end of file --->" ) ).isGreaterThan( out.indexOf( "</cfcomponent>" ) );
	}

	@Test
	@DisplayName( "a nested struct that prints on several lines ends its alignment run" )
	public void testNestedStructAlignment() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                        x = {
		                                                        	"type": "object",
		                                                        	"properties": {
		                                                        		"items": {
		                                                        			"type": "array",
		                                                        			"items": itemSchemaStruct
		                                                        		}
		                                                        	},
		                                                        	"required": [ "items" ],
		                                                        	"additionalProperties": false
		                                                        };
		                                                        """ );
		assertThat( out ).contains( "\"type\"       : \"object\"" );
		assertThat( out ).contains( "\"required\"             : [ \"items\" ]" );
	}

	@Test
	@DisplayName( "numbers print as written" )
	public void testNumbersAsWritten() throws IOException {
		String out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                        function crop( required img, numeric threshold = 0.95 ){
		                                                        	big = 1e5;
		                                                        	padded = 007;
		                                                        	first = args.1;
		                                                        	return threshold <= 0.0;
		                                                        }
		                                                        """ );
		assertThat( out ).contains( "threshold = 0.95" );
		assertThat( out ).contains( "0.0" );
		assertThat( out ).contains( "1e5" );
		assertThat( out ).contains( "007" );
		assertThat( out ).contains( "first = args.1;" );
	}

	@Test
	@DisplayName( "a comment before a chained expression stays before it" )
	public void testCommentBeforeChain() throws IOException {
		String out = roundTrip( BoxSourceType.CFSCRIPT, false, """
		                                                       if (
		                                                       	isReady
		                                                       	&&
		                                                       	// Does the setting have value?
		                                                       	variables.properties.securityModules[ currentModule ].firewall[ "validator" ].len()
		                                                       ) {
		                                                       	x = 1;
		                                                       }
		                                                       """ );
		assertThat( out.indexOf( "// Does the setting have value?" ) ).isLessThan( out.indexOf( "variables.properties" ) );
	}

	@Test
	@DisplayName( "cfformat settings: layout decisions do not change on the next pass" )
	public void testCFFormatStableLayout() throws IOException {
		Config config = new Config().setCFFormatCompatibility( true );
		config.getStruct().getMultiline().setElementCount( 2 );
		config.getStruct().getMultiline().setMinLength( 60 );
		config.getChain().setBreakCount( 3 );

		// a struct with one key prints on one line, so it does not end the alignment run
		String out = roundTrip( BoxSourceType.CFSCRIPT, false, """
		                                                       x = {
		                                                       	appenders : {
		                                                       		console : { class : "ConsoleAppender" }
		                                                       	},
		                                                       	root : { levelmax : "WARN", appenders : "*" }
		                                                       };
		                                                       """, config );
		assertThat( out ).contains( "root      : {" );

		// property and parameter lengths are measured from how they print, not from their source
		roundTrip( BoxSourceType.CFSCRIPT, true, """
		                                         component {
		                                         	property
		                                                 name="name"
		                                                 type="string";
		                                         	private any function crop( required Img, numeric threshold = 0.95 ){
		                                         		return 1;
		                                         	}
		                                         }
		                                         """, config );

		// a comment between two chained calls breaks the chain, even under the break count
		out = roundTrip( BoxSourceType.CFSCRIPT, false, """
		                                                var targetAppenders = target
		                                                	.getAppenders()
		                                                	// Only go through appenders that can log
		                                                	.filter( function( key, thisAppender ){
		                                                		return thisAppender.canLog( severity );
		                                                	} );
		                                                """, config );
		assertThat( out ).doesNotContain( ")// Only" );
		assertThat( out.indexOf( "// Only go through" ) ).isLessThan( out.indexOf( ".filter(" ) );
	}

	@Test
	@DisplayName( "non-default options keep the code and format the same on the next pass" )
	public void testNonDefaultOptions() throws IOException {
		// operators without padding: a word operator keeps its spaces, a sign before a negative operand too
		Config	config	= new Config().setBinaryOperatorsPadding( false );
		String	out		= roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                             isStruct = x instanceOf "Struct";
		                                                             y = a - -b;
		                                                             z = a + b;
		                                                             """, config );
		assertThat( out ).contains( "x instanceOf \"Struct\"" );
		assertThat( out ).contains( "a - -b" );
		assertThat( out ).contains( "a+b" );
		// the parser does not read these operators without spaces
		out = roundTrip( BoxSourceType.BOXSCRIPT, false, "x = ( b && c ) || ( d & e );", config );
		assertThat( out ).contains( "b && c" );
		assertThat( out ).contains( "d & e" );

		// semicolons off: the ones the next line would otherwise join are kept
		config	= new Config().setSemicolons( false );
		out		= roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                                    import java.lang.String;

		                                                    @BoxBIF
		                                                    class {
		                                                    	function f( items ){
		                                                    		param name = "x";
		                                                    		for ( item in items ) {
		                                                    			if ( item ) {
		                                                    				continue;
		                                                    			}
		                                                    			break;
		                                                    		}
		                                                    		return;
		                                                    	}
		                                                    }
		                                                    """, config );
		assertThat( out ).contains( "import java.lang.String;" );
		assertThat( out ).contains( "continue;" );
		assertThat( out ).contains( "return;" );
		roundTrip( BoxSourceType.CFSCRIPT, true, """
		                                         interface {
		                                         	any function configure( required properties );
		                                         	any function set( required key );
		                                         }
		                                         """, config );
		// a statement the next line would join: before "(" or "[", and after an open-ended range
		out = roundTrip( BoxSourceType.BOXSCRIPT, false, """
		                                                 a = function( x ){ return x; };
		                                                 ( y ) => !y;
		                                                 r = 1..;
		                                                 variables.next = 2;
		                                                 """, config );
		assertThat( out ).contains( "r = 1..;" );

		// comment wrapping keeps the closing line and star banners
		config = new Config().setMaxLineLength( 60 );
		config.getComments().setWrap( true );
		out = roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                                /**
		                                                 * BL-2759: records which context a context-less announcement runs on, which is long
		                                                 */
		                                                class {
		                                                	/********************* Methods *********************/
		                                                	function f(){
		                                                		/* This is a very long comment that should be wrapped because it exceeds the line length */
		                                                		return 1;
		                                                	}
		                                                }
		                                                """, config );
		assertThat( out ).doesNotContain( "* */" );

		// a doc comment line with an "@" is not wrapped: the doc parser would read the "@" as a new tag
		out = roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                                /**
		                                                 * This service manages all the disks in your application.
		                                                 *
		                                                 * @author Luis Majano <lmajano@ortussolutions.com>, Grant Copley <gcopley@ortussolutions.com>
		                                                 */
		                                                class {
		                                                }
		                                                """, config );
		assertThat( out ).contains( "@author Luis Majano <lmajano@ortussolutions.com>, Grant Copley <gcopley@ortussolutions.com>" );

		// blank lines not preserved, and wider member spacing
		config = new Config();
		config.getComments().setPreserveBlankLines( false );
		config.getClassConfig().setMemberSpacing( 2 );
		roundTrip( BoxSourceType.BOXSCRIPT, true, """
		                                          class {
		                                          	function init(){
		                                          		return this;
		                                          	}

		                                          	// these events fire on the global handler
		                                          	function onEvict(){
		                                          	}
		                                          }
		                                          """, config );

		// dangling commas in arguments with a closure argument, and braces on their own line with a commented else
		config = new Config();
		config.getArguments().setCommaDangle( true );
		config.getBraces().setStyle( "new-line" );
		roundTrip( BoxSourceType.CFSCRIPT, false, """
		                                          expect( routes.filter( function( item ){ return item.name == "home"; } ) ).notToBeEmpty();
		                                          if ( x ) {
		                                          	y = 1;
		                                          }
		                                          // otherwise block
		                                          else {
		                                          	y = 2;
		                                          }
		                                          """, config );
	}

	/**
	 * Formats the source, checks that the result means the same and formats to itself, and returns it.
	 *
	 * @param type             the source type
	 * @param classOrInterface whether the source is a class or interface
	 * @param source           the source
	 *
	 * @return the formatted source
	 */
	private String roundTrip( BoxSourceType type, boolean classOrInterface, String source ) throws IOException {
		return roundTrip( type, classOrInterface, source, new Config() );
	}

	/**
	 * Formats the source with a config, checks that the result means the same and formats to itself, and returns it.
	 *
	 * @param type             the source type
	 * @param classOrInterface whether the source is a class or interface
	 * @param source           the source
	 * @param config           the formatter config
	 *
	 * @return the formatted source
	 */
	private String roundTrip( BoxSourceType type, boolean classOrInterface, String source, Config config ) throws IOException {
		ParsingResult original = parser.parse( source, type, classOrInterface, false );
		assertWithMessage( "input parses: " + original.getIssues() ).that( original.isCorrect() ).isTrue();
		Object			before		= normalize( original.getRoot().toMap() );

		String			once		= PrettyPrint.prettyPrint( original.getRoot(), config );
		ParsingResult	reparsed	= parser.parse( once, type, classOrInterface, false );
		assertWithMessage( "formatted output parses: " + reparsed.getIssues() + "\n" + once ).that( reparsed.isCorrect() ).isTrue();
		assertWithMessage( "formatting changed the code:\n" + once ).that( normalize( reparsed.getRoot().toMap() ) ).isEqualTo( before );

		String twice = PrettyPrint.prettyPrint( reparsed.getRoot(), config );
		assertWithMessage( "formatting is not idempotent" ).that( normalizeLineEndings( twice ) ).isEqualTo( normalizeLineEndings( once ) );
		return normalizeLineEndings( once );
	}

	/**
	 * An AST map without what formatting is allowed to change: positions, source text, comments, structural template
	 * whitespace, a self-closed body-less component's empty body, and blocks around a single statement.
	 */
	@SuppressWarnings( "unchecked" )
	private static Object normalize( Object value ) {
		if ( value instanceof Map<?, ?> map ) {
			Object type = map.get( "ASTType" );
			if ( "BoxStatementBlock".equals( type ) && map.get( "body" ) instanceof List<?> body && body.size() == 1 ) {
				return normalize( body.get( 0 ) );
			}
			Map<String, Object> copy = new LinkedHashMap<>( ( Map<String, Object> ) map );
			if ( "BoxComponent".equals( type ) && copy.get( "body" ) instanceof List<?> body && body.isEmpty() ) {
				copy.put( "body", null );
			}
			// a catch type is a name either way: <bx:catch> (any) prints as type="any"
			if ( "BoxTryCatch".equals( type ) && copy.get( "catchTypes" ) instanceof List<?> types ) {
				copy.put( "catchTypes", types.stream().map( t -> t instanceof Map<?, ?> m ? String.valueOf( m.get( "value" ) ).toLowerCase() : t ).toList() );
			}
			boolean				node	= copy.containsKey( "position" ) || copy.containsKey( "comments" );
			Map<String, Object>	result	= new TreeMap<>();
			for ( var entry : copy.entrySet() ) {
				String key = entry.getKey();
				if ( key.equals( "position" ) || key.equals( "comments" ) || ( node && key.equals( "sourceText" ) ) ) {
					continue;
				}
				if ( key.equals( "documentation" ) || ( "BoxBufferOutput".equals( type ) && key.equals( "expression" ) ) ) {
					result.put( key, normalizeText( entry.getValue() ) );
					continue;
				}
				result.put( key, normalize( entry.getValue() ) );
			}
			return result;
		}
		if ( value instanceof List<?> list ) {
			List<Object> result = new ArrayList<>();
			for ( Object item : list ) {
				if ( !isStructuralWhitespace( item ) ) {
					result.add( normalize( item ) );
				}
			}
			return result;
		}
		return value;
	}

	/** Collapses whitespace in the string values of a subtree (template text and doc text are re-flowed by design). */
	@SuppressWarnings( "unchecked" )
	private static Object normalizeText( Object value ) {
		Object normalized = normalize( value );
		if ( normalized instanceof Map<?, ?> map ) {
			Map<String, Object> copy = new TreeMap<>( ( Map<String, Object> ) map );
			copy.replaceAll( ( k, v ) -> v instanceof String s && k.equals( "value" ) ? s.replaceAll( "\\s+", " " ).strip() : normalizeText( v ) );
			return copy;
		}
		if ( normalized instanceof List<?> list ) {
			return list.stream().map( RoundTripTest::normalizeText ).toList();
		}
		return normalized;
	}

	private static boolean isStructuralWhitespace( Object item ) {
		return item instanceof Map<?, ?> map
		    && "BoxBufferOutput".equals( map.get( "ASTType" ) )
		    && map.get( "expression" ) instanceof Map<?, ?> expression
		    && "BoxStringLiteral".equals( expression.get( "ASTType" ) )
		    && String.valueOf( expression.get( "value" ) ).isBlank()
		    && ( String.valueOf( expression.get( "value" ) ).isEmpty() || String.valueOf( expression.get( "value" ) ).contains( "\n" ) );
	}
}
