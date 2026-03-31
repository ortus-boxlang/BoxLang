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
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DumpTest {

	static BoxRuntime			instance;
	ScriptingRequestBoxContext	context;
	IScope						variables;
	ByteArrayOutputStream		baos;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.setOut( new PrintStream( ( baos = new ByteArrayOutputStream() ), true ) );
		variables = context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can dump tag" )
	@Test
	public void testCanDumpTag() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="My Value" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
		// If we change our cfdump template, this may break
		assertThat( baos.toString() ).contains( "String" );
	}

	@DisplayName( "It can dump tag" )
	@Test
	public void testCanDumpXMLName() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="#XMLParse( '<root><item attr="value" /></root>' ).root#" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "root" );
		assertThat( baos.toString() ).contains( "item" );
	}

	@DisplayName( "It can dump tag struct" )
	@Test
	public void testCanDumpTagStruct() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="#{ foo : 'bar' }#" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "bar" );
	}

	@DisplayName( "It can dump byte array" )
	@Test
	public void testCanDumpByteArray() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="#'brad'.getBytes()#" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "Raw" );
		assertThat( baos.toString() ).contains( "98,114,97,100" );
	}

	@DisplayName( "It can dump empty byte array" )
	@Test
	public void testCanDumpEmptyByteArray() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="#''.getBytes()#" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "Raw" );
		assertThat( baos.toString() ).contains( "[]" );
	}

	@DisplayName( "It can dump truncated byte array over 1000 chars" )
	@Test
	public void testCanDumpTruncatedByteArray() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="#repeatstring("*", 1001).getBytes()#" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "Raw" );
		assertThat( baos.toString() ).contains( "... truncated" );
	}

	@DisplayName( "It can dump tag struct with sorted keys" )
	@Test
	public void testCanDumpTagStructSorted() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="#{ 'z_key' : 'z', 'p_key' : 'p', 'a_key' : 'a', 'b_key' : 'b' }#" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).matches( "(?s).*a_key.*b_key.*p_key.*z_key.*" );
	}

	@DisplayName( "It can dump tag sorted struct with sorted keys" )
	@Test
	public void testCanDumpTagSortedStructSorted() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="#[ 'z_key' : 'z', 'p_key' : 'p', 'a_key' : 'a', 'b_key' : 'b' ]#" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).matches( "(?s).*z_key.*p_key.*a_key.*b_key.*" );
	}

	@DisplayName( "It can dump BL tag" )
	@Test
	public void testCanDumpBLTag() {
		// @formatter:off
		instance.executeSource(
		    """
		    	<bx:dump var='My Value'>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
	}

	@DisplayName( "It can dump BL tag 2" )
	@Test
	public void testCanDumpBLTag2() {
		// @formatter:off
		instance.executeSource(
		    """
		    	<bx:try>
					<bx:throw message="inner" />
					<bx:catch type="any">
						<bx:dump var="#bxCatch#" />
					</bx:catch>
				</bx:try>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "inner" );
	}

	@DisplayName( "It can dump script" )
	@Test
	public void testCanDumpScript() {
		// @formatter:off
		instance.executeSource(
		    """
		       	bx:dump var="My Value" format="html";
		    """,
		    context );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
		// If we change our cfdump template, this may break
		assertThat( baos.toString() ).contains( "String" );
	}

	@DisplayName( "It can dump ACF script" )
	@Test
	public void testCanDumpACFScript() {
		// @formatter:off
		instance.executeSource(
		    """
		      	cfdump( var="My Value");
		    """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
	}

	@DisplayName( "It can dump using the BIF" )
	@Test
	public void testCanDumpUsingBIF() {
		// @formatter:off
		instance.executeSource(
		    """
		      	dump( "My Value");
		    """,
		    context );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
	}

	@DisplayName( "It can dump an XML object" )
	@Test
	public void testCanDumpEmptyXML() {
		// @formatter:off
		instance.executeSource(
		    """
				<cfset myXML = xmlNew()/>
				<cfdump var="#myXML#" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:onp
		assertThat( baos.toString() ).contains( "xml" );
	}

	@DisplayName( "It can dump an isEmpty method " )
	@Test
	public void testCanDumpisEmptyMethod() {
		// @formatter:off
		instance.executeSource(
		    """
				cfc = new src.test.java.ortus.boxlang.runtime.components.system.ClassWithIsEmpty();
				dump( var = cfc, format = "html" );
		    """,
		    context );
 		assertThat( baos.toString() ).contains( "<strong>isEmpty</strong>" );
		// @formatter:on
	}

	@DisplayName( "It can dump a Duration" )
	@Test
	public void testCanDumpDuration() {
		// @formatter:off
		instance.executeSource(
		    """
				timespan = createTimeSpan( 0, 1, 0, 0 );
				dump( var = timespan, format = "html" );
		    """,
		    context );
 		assertThat( baos.toString() ).contains( "Timespan:" );
 		assertThat( baos.toString() ).contains( "0, 1, 0, 0" );
		// @formatter:on
	}

	@DisplayName( "It can dump an array list " )
	@Test
	public void testCanDumpAnArrayInHTML() {
		// @formatter:off
		instance.executeSource(
		    """
				val = [1,2,3,null,5];
				dump( var = val, format = "html" );
		    """,
		    context );
			assertThat( baos.toString().replaceAll( "[ \\t\\r\\n]", "" ) ).contains( "Array:5" );
			// @formatter:on
	}

	@DisplayName( "It can dump an struct in html" )
	@Test
	public void testCanDumpAnStructHTML() {
		// @formatter:off
		instance.executeSource(
		    """
				val = {"a":1, "b":2, "c":3, "d":4, "brad":"wood"};
				dump( var = val, format = "html" );
		    """,
		    context );
			System.out.println( baos.toString() );
			String results = baos.toString().replaceAll( "[ \\t\\r\\n]", "" );
 		assertThat( results ).contains( "brad" );
 		assertThat( results ).contains( "wood" );
		// @formatter:on
	}

	@DisplayName( "It can dump an Array data type" )
	@Test
	public void testCanDumpArray() {
		// @formatter:off
			instance.executeSource(
				"""
					val = [ "apple", "banana", "cherry" ];
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "Array" );
	}

	@DisplayName( "It can dump a Boolean data type" )
	@Test
	public void testCanDumpBoolean() {
		// @formatter:off
			instance.executeSource(
				"""
					val = true;
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "true" );
	}

	@DisplayName( "It can dump a BoxClass data type" )
	@Test
	public void testCanDumpBoxClass() {
		// @formatter:off
				instance.executeSource(
					"""
						cfc = new src.test.java.ortus.boxlang.runtime.components.system.TestDumpClass();
						dump( var = cfc, format = "html" );
					""",
					context );
				// @formatter:on
		assertThat( baos.toString() ).contains( "components.system.TestDumpClass" );
		assertThat( baos.toString() ).contains( "IDumpClass" );
	}

	@DisplayName( "It can dump a Class data type" )
	@Test
	public void testCanDumpClass() {
		// @formatter:off
			instance.executeSource(
				"""
					val = "Hello".getClass();
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "Class" );
	}

	@DisplayName( "It can dump a DateTime data type" )
	@Test
	public void testCanDumpDateTime() {
		// @formatter:off
			instance.executeSource(
				"""
					// Assuming now() returns the current DateTime
					val = now();
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		// Expect a date format marker (e.g. dashes)
		assertThat( baos.toString() ).contains( "-" );
	}

	@DisplayName( "It can dump the Dump component" )
	@Test
	public void testCanDumpDump() {
		// @formatter:off
			instance.executeSource(
				"""
					dump( var = dump('testing innerdump'), format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "innerdump" );
	}

	@DisplayName( "It can dump a Function data type" )
	@Test
	public void testCanDumpFunction() {
		// @formatter:off
			instance.executeSource(
				"""
					function add(a, b) { 
						return a + b; 
					}
					dump( var = add(2+4), format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "function" );
	}

	@DisplayName( "It can dump an Instant data type" )
	@Test
	public void testCanDumpInstant() {
		// @formatter:off
			instance.executeSource(
				"""
					import java.time.Instant;
					val = Instant.now();
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "Instant" );
	}

	@DisplayName( "It can dump a Key data type" )
	@Test
	public void testCanDumpKey() {
		// @formatter:off
			instance.executeSource(
				"""
					val = new ortus.boxlang.runtime.scopes.Key("testKey");
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "testKey" );
	}

	@DisplayName( "It can dump a List data type" )
	@Test
	public void testCanDumpList() {
		// @formatter:off
			instance.executeSource(
				"""
					val = "apple, banana, cherry";
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "apple" );
	}

	@DisplayName( "It can dump a Map data type" )
	@Test
	public void testCanDumpMap() {
		// @formatter:off
			instance.executeSource(
				"""
					import java.util.HashMap;
					val = new HashMap();
					val.put("key1", "value1");
					val.put("key2", "value2");
					val.put("key3", "value3");
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "value1" );
	}

	@DisplayName( "It can dump a Null value" )
	@Test
	public void testCanDumpNull() {
		// @formatter:off
			instance.executeSource(
				"""
					val = null;
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "null" );
	}

	@DisplayName( "It can dump a Number data type" )
	@Test
	public void testCanDumpNumber() {
		// @formatter:off
			instance.executeSource(
				"""
					val = 12345;
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "12345" );
	}

	@DisplayName( "It can dump a Query data type" )
	@Test
	public void testCanDumpQuery() {
		// @formatter:off
			instance.executeSource(
				"""
					val = queryExecute("SELECT 1",{},{ "dbtype": "query" });
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString().toLowerCase() ).contains( "1" );
	}

	@DisplayName( "It can dump a String data type" )
	@Test
	public void testCanDumpString() {
		// @formatter:off
			instance.executeSource(
				"""
					val = "Hello, BoxLang";
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "Hello, BoxLang" );
	}

	@DisplayName( "It can dump a StringBuffer data type" )
	@Test
	public void testCanDumpStringBuffer() {
		// @formatter:off
			instance.executeSource(
				"""
					val = new java.lang.StringBuffer("Buffer Content");
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "Buffer Content" );
	}

	@DisplayName( "It can dump a Struct data type" )
	@Test
	public void testCanDumpStruct() {
		// @formatter:off
			instance.executeSource(
				"""
					val = { "first": "John", "last": "Doe" };
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "John" );
	}

	@DisplayName( "It can dump a Throwable" )
	@Test
	public void testCanDumpThrowable() {
		// @formatter:off
			instance.executeSource(
				"""
					e =  new java.lang.Exception("Test Exception");
					dump( var = e, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "Test Exception" );
	}

	@DisplayName( "It can dump a ToString result" )
	@Test
	public void testCanDumpToString() {
		// @formatter:off
			instance.executeSource(
				"""
					val = ("Numeric to string").toString();
					dump( var = val, format = "html" );
				""",
				context );
			// @formatter:on
		assertThat( baos.toString() ).contains( "Numeric to string" );
	}

	@DisplayName( "It can dump text to a file" )
	@Test
	public void testCanDumpTextToFile() {
		// @formatter:off
			instance.executeSource(
				"""
					val = "Hello, BoxLang";
					filePath = expandPath( "/src/test/resources/tmp/dump_test.txt" );
					if( fileExists( filePath ) ) fileDelete( filePath );
					dump( var = val, format = "text", output = filePath );
				""",
				context );
			// @formatter:on

		Path filePath = Paths.get( variables.getAsString( Key.of( "filePath" ) ) );
		assertWithMessage( "File [" + filePath + "] should exist" ).that( filePath.toFile().exists() ).isTrue();
		String fileContents = ( String ) FileSystemUtil.read( filePath.toString(), FileSystemUtil.DEFAULT_CHARSET.name(), null, true );
		assertThat( fileContents ).contains( "Hello, BoxLang" );
		// Cleanup
		filePath.toFile().delete();
	}

	@DisplayName( "It can dump HTML to a file" )
	@Test
	public void testCanDumpHTMLToFile() {
		// @formatter:off
			instance.executeSource(
				"""
					val = "Hello, BoxLang";
					filePath = expandPath( "/src/test/resources/tmp/dump_test.html" );
					if( fileExists( filePath ) ) fileDelete( filePath );
					dump( var = val, format = "html", output = filePath );
				""",
				context );
			// @formatter:on

		Path filePath = Paths.get( variables.getAsString( Key.of( "filePath" ) ) );
		assertWithMessage( "File [" + filePath + "] should exist" ).that( filePath.toFile().exists() ).isTrue();
		String fileContents = ( String ) FileSystemUtil.read( filePath.toString(), FileSystemUtil.DEFAULT_CHARSET.name(), null, true );
		assertThat( fileContents ).contains( "Hello, BoxLang" );
		assertThat( fileContents ).contains( "<style>" );
		// Cleanup
		filePath.toFile().delete();
	}

	@DisplayName( "It can dump to a file in temp dir" )
	@Test
	public void testCanDumpToFileInTempDir() {
		// @formatter:off
			instance.executeSource(
				"""
					val = "Hello, BoxLang";
					fileName = "dump_test.txt";
					filePath = getTempDirectory() & "/" & fileName;
					if( fileExists( filePath ) ) fileDelete( filePath );
					dump( var = val, format = "text", output = fileName );
				""",
				context );
			// @formatter:on

		Path filePath = Paths.get( variables.getAsString( Key.of( "filePath" ) ) );
		assertWithMessage( "File [" + filePath + "] should exist" ).that( filePath.toFile().exists() ).isTrue();
		String fileContents = ( String ) FileSystemUtil.read( filePath.toString(), FileSystemUtil.DEFAULT_CHARSET.name(), null, true );
		assertThat( fileContents ).contains( "Hello, BoxLang" );
		// Cleanup
		filePath.toFile().delete();
	}

	@DisplayName( "It can append text to a file" )
	@Test
	public void testCanAppendTextToFile() {
		// @formatter:off
			instance.executeSource(
				"""
					filePath = expandPath( "/src/test/resources/tmp/extra/deep/dump_test.txt" );
					parent = expandPath( "/src/test/resources/tmp/extra" );
					if( directoryExists( parent ) ) directoryDelete( parent, true );
					dump( var = "dump one", format = "text", output = filePath );
					dump( var = "dump two", format = "text", output = filePath );
					dump( var = "dump three", format = "text", output = filePath );
				""",
				context );
			// @formatter:on

		Path filePath = Paths.get( variables.getAsString( Key.of( "filePath" ) ) );
		assertWithMessage( "File [" + filePath + "] should exist" ).that( filePath.toFile().exists() ).isTrue();
		String fileContents = ( String ) FileSystemUtil.read( filePath.toString(), FileSystemUtil.DEFAULT_CHARSET.name(), null, true );
		assertThat( fileContents ).contains( "dump one" );
		assertThat( fileContents ).contains( "dump two" );
		assertThat( fileContents ).contains( "dump three" );
		// Cleanup
		filePath.toFile().delete();
		filePath.getParent().toFile().delete();
		filePath.getParent().getParent().toFile().delete();
	}

	@DisplayName( "It shows properties from super class" )
	@Test
	public void testShowsPropertiesFromSuperClass() {
		// @formatter:off
			instance.executeSource(
				"""
					dump( var = new src.test.java.ortus.boxlang.runtime.components.system.DumpChild(), format = "html" );
				""",
				context );
			// @formatter:on
		String output = baos.toString();
		assertThat( output ).contains( "childProperty" );
		assertThat( output ).contains( "I am a child property" );

		assertThat( output ).contains( "parentProperty" );
		assertThat( output ).contains( "I am a parent property" );

	}

	@DisplayName( "It can dump SQL date and time objects which don't correctly implement the java.util.Date interface" )
	@Test
	public void testCanDumpSqlDateAndTimeObjects() {
		variables.put( "sqlDate", java.sql.Date.valueOf( "2023-01-01" ) );
		// @formatter:off
			instance.executeSource(
				"""
					dump( var = sqlDate, format = "html" );
				""",
				context );
			// @formatter:on
		String output = baos.toString();
		assertThat( output ).contains( "2023-01-01" );

		variables.put( "sqlTime", java.sql.Time.valueOf( "12:34:56" ) );
		// @formatter:off
		// We need to use text format here as otherwise the colons are HTML encoded
			instance.executeSource(
				"""
					dump( var = sqlTime, format = "text" );
				""",
				context );
			// @formatter:on
		output = baos.toString();
		assertThat( output ).contains( "12:34:56" );

	}

}
