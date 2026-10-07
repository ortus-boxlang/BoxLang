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
		                                     			<cfif i EQ 1>
		                                     				<cfoutput>(#i#)</cfoutput>
		                                     			<cfelse>
		                                     				<cfoutput>[#i#]</cfoutput>
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
}
