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
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;

/**
 * BL-2736: tag-form {@code <cffunction>} in a .cfm template was silently dropped by {@code cftranspile}.
 */
@DisplayName( "CFTranspiler tag function in template (BL-2736)" )
public class CFTranspilerTemplateFunctionTest {

	static BoxRuntime	instance;

	@TempDir
	Path				tempDir;

	/**
	 * Starts the BoxLang runtime once for all tests in this class.
	 */
	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	/**
	 * Transpile the given CFML template source to a .bxm and return the transpiled file path.
	 */
	private Path transpile( String name, String cfmlSource ) throws IOException {
		Path	source	= this.tempDir.resolve( name + ".cfm" );
		Path	target	= this.tempDir.resolve( name + ".bxm" );
		Files.writeString( source, cfmlSource, StandardCharsets.UTF_8 );
		int exitCode = CFTranspiler.run( new String[] { "--source", source.toString(), "--target", target.toString() } );
		assertThat( exitCode ).isEqualTo( 0 );
		return target;
	}

	/**
	 * Execute the transpiled template and return its (whitespace-collapsed) output.
	 */
	private String execute( Path bxm ) {
		IBoxContext		context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		StringBuffer	buffer	= new StringBuffer();
		context.pushBuffer( buffer );
		try {
			instance.executeTemplate( bxm.toAbsolutePath().toString(), context );
		} finally {
			context.popBuffer();
		}
		return buffer.toString().replaceAll( "\\s+", " " ).trim();
	}

	/**
	 * A function defined before the output is kept and callable.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It keeps a tag function defined before the output" )
	@Test
	public void testFunctionBeforeOutput() throws IOException {
		Path	bxm		= transpile( "before", """
		                                       <cffunction name="greet" output="false">
		                                       	<cfargument name="n">
		                                       	<cfreturn "hi " & arguments.n>
		                                       </cffunction>
		                                       <cfoutput>Hello #greet( "x" )#</cfoutput>
		                                       """ );
		String	text	= Files.readString( bxm );
		assertThat( text ).contains( "<bx:function" );
		assertThat( text ).contains( "<bx:argument" );
		assertThat( text ).contains( "<bx:return" );
		assertThat( execute( bxm ) ).isEqualTo( "Hello hi x" );
	}

	/**
	 * A function defined after the output is kept and callable.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It keeps a tag function defined after the output" )
	@Test
	public void testFunctionAfterOutput() throws IOException {
		Path bxm = transpile( "after", """
		                               <cfoutput>Hello #greet( "x" )#</cfoutput>
		                               <cffunction name="greet" output="false">
		                               	<cfargument name="n">
		                               	<cfreturn "hi " & arguments.n>
		                               </cffunction>
		                               """ );
		assertThat( Files.readString( bxm ) ).contains( "<bx:function" );
		assertThat( execute( bxm ) ).isEqualTo( "Hello hi x" );
	}

	/**
	 * A function defined inside cfoutput is kept and callable.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It keeps a tag function defined inside cfoutput" )
	@Test
	public void testFunctionInsideOutput() throws IOException {
		Path bxm = transpile( "inside", """
		                                <cfoutput>
		                                <cffunction name="greet" output="false">
		                                	<cfargument name="n">
		                                	<cfreturn "hi " & arguments.n>
		                                </cffunction>
		                                Hello #greet( "x" )#
		                                </cfoutput>
		                                """ );
		assertThat( Files.readString( bxm ) ).contains( "<bx:function" );
		assertThat( execute( bxm ) ).isEqualTo( "Hello hi x" );
	}

	/**
	 * Argument type, required, default and function attributes are kept exactly once and honored at runtime.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It keeps argument type, required, default and function attributes" )
	@Test
	public void testArgumentsAndDefaults() throws IOException {
		Path	bxm		= transpile( "args", """
		                                     <cffunction name="describe" returntype="string" access="public" output="false">
		                                     	<cfargument name="name" type="string" required="true">
		                                     	<cfargument name="greeting" type="string" required="false" default="Hello">
		                                     	<cfargument name="count" type="numeric" default="2">
		                                     	<cfreturn greeting & " " & name & " " & count>
		                                     </cffunction>
		                                     <cfoutput>#describe( "Bob" )#|#describe( "Ann", "Yo", 5 )#</cfoutput>
		                                     """ );
		String	text	= Files.readString( bxm );
		assertThat( text ).contains( "<bx:function" );
		assertThat( text ).contains( "name=\"describe\"" );
		assertThat( text ).contains( "output=\"false\"" );
		assertThat( text ).contains( "required=\"true\"" );
		assertThat( text ).contains( "default=\"Hello\"" );
		assertThat( text ).contains( "type=\"numeric\"" );
		// Each attribute must only be printed once
		assertThat( text.split( "returntype=|returnType=", -1 ).length - 1 ).isAtMost( 1 );
		assertThat( text.split( "access=", -1 ).length - 1 ).isAtMost( 1 );
		assertThat( execute( bxm ) ).isEqualTo( "Hello Bob 2|Yo Ann 5" );
	}

	/**
	 * A body with cfsavecontent, cfloop, cfif and cfset var is kept and executes correctly.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It keeps a body with cfsavecontent, cfloop, cfif and cfset var" )
	@Test
	public void testComplexBody() throws IOException {
		Path	bxm		= transpile( "body", """
		                                     <cfoutput>#report( [ 1, 2, 3 ], "T" )#</cfoutput>
		                                     <cffunction name="report" output="false" returntype="string">
		                                     	<cfargument name="items" type="array" required="true">
		                                     	<cfargument name="title" type="string" default="Untitled">
		                                     	<cfset var out = "">
		                                     	<cfset var total = 0>
		                                     	<cfsavecontent variable="out">
		                                     		<cfoutput>#title#:</cfoutput>
		                                     		<cfloop array="#items#" item="i">
		                                     			<cfif i GT 1>
		                                     				<cfoutput>[#i#]</cfoutput>
		                                     			<cfelse>
		                                     				<cfoutput>(#i#)</cfoutput>
		                                     			</cfif>
		                                     			<cfset total += i>
		                                     		</cfloop>
		                                     	</cfsavecontent>
		                                     	<cfreturn out & " total=" & total>
		                                     </cffunction>
		                                     """ );
		String	text	= Files.readString( bxm );
		assertThat( text ).contains( "<bx:savecontent" );
		assertThat( text ).contains( "<bx:loop" );
		assertThat( execute( bxm ) ).isEqualTo( "T: (1) [2] [3] total=6" );
	}

	/**
	 * Regression control: a function inside cfscript still transpiles and runs.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It still transpiles a function inside cfscript" )
	@Test
	public void testScriptFunctionStillWorks() throws IOException {
		Path bxm = transpile( "script", """
		                                <cfscript>
		                                	function greet( n ) { return "hi " & n; }
		                                </cfscript>
		                                <cfoutput>Hello #greet( "x" )#</cfoutput>
		                                """ );
		assertThat( execute( bxm ) ).isEqualTo( "Hello hi x" );
	}

	/**
	 * Greater-than comparisons in a tag condition must not print a bare {@code >}, which would end the tag early.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It keeps GT and GTE comparisons inside a tag condition" )
	@Test
	public void testGreaterThanInTagCondition() throws IOException {
		Path bxm = transpile( "gt", """
		                            <cfset i = 2>
		                            <cfif i GT 1><cfoutput>gt</cfoutput><cfelse><cfoutput>not-gt</cfoutput></cfif>
		                            <cfif i GTE 2><cfoutput>gte</cfoutput><cfelse><cfoutput>not-gte</cfoutput></cfif>
		                            <cfif i GT 1 AND i LT 3><cfoutput>both</cfoutput><cfelse><cfoutput>not-both</cfoutput></cfif>
		                            <cfif i GT 5><cfoutput>big</cfoutput><cfelseif i GTE 2><cfoutput>elseif-gte</cfoutput></cfif>
		                            <cfset big = i GT 1>
		                            <cfoutput>#big#</cfoutput>
		                            """ );
		assertThat( Files.readString( bxm ) ).doesNotContain( "<bx:if i >" );
		assertThat( execute( bxm ) ).isEqualTo( "gt gte both elseif-gte true" );
	}

	/**
	 * Greater-than comparisons inside a tag-form function body must survive and execute.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It keeps GT inside a function body" )
	@Test
	public void testGreaterThanInFunction() throws IOException {
		Path bxm = transpile( "gtfn", """
		                              <cfoutput>#size( 5 )#|#size( 1 )#</cfoutput>
		                              <cffunction name="size" output="false">
		                              	<cfargument name="n">
		                              	<cfif n GTE 3><cfreturn "big"><cfelse><cfreturn "small"></cfif>
		                              </cffunction>
		                              """ );
		assertThat( execute( bxm ) ).isEqualTo( "big|small" );
	}

	/**
	 * {@code DOES NOT CONTAIN} must transpile to a form the BoxLang parser accepts, in tags, in script and inside a function.
	 *
	 * @throws IOException if the temp files cannot be written or read
	 */
	@DisplayName( "It keeps DOES NOT CONTAIN in tags, script and function bodies" )
	@Test
	public void testDoesNotContain() throws IOException {
		Path bxm = transpile( "dnc", """
		                             <cfset s = "hello">
		                             <cfif s DOES NOT CONTAIN "zz"><cfoutput>a-yes</cfoutput><cfelse><cfoutput>a-no</cfoutput></cfif>
		                             <cfif s DOES NOT CONTAIN "ell"><cfoutput>b-yes</cfoutput><cfelse><cfoutput>b-no</cfoutput></cfif>
		                             <cfset r = s DOES NOT CONTAIN "zz">
		                             <cfoutput>#r#</cfoutput>
		                             <cfscript>z = s DOES NOT CONTAIN "ell";</cfscript>
		                             <cfoutput>#z# #check( "abc" )#</cfoutput>
		                             <cffunction name="check" output="false">
		                             	<cfargument name="v">
		                             	<cfif v DOES NOT CONTAIN "z"><cfreturn "no-z"><cfelse><cfreturn "has-z"></cfif>
		                             </cffunction>
		                             """ );
		assertThat( Files.readString( bxm ) ).doesNotContain( "not contains" );
		assertThat( execute( bxm ) ).isEqualTo( "a-yes b-no true false no-z" );
	}
}
