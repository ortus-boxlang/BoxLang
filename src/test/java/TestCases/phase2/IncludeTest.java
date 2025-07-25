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
package TestCases.phase2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;

public class IncludeTest {

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

	@DisplayName( "can include file" )
	@Test
	public void testCanIncludeFile() {
		instance.executeSource(
		    """
		    myVar = "before"
		       include "src/test/java/TestCases/phase2/myInclude.cfs";
		    result = fromInclude & " " & brad();
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "found the value before wood" );
	}

	@DisplayName( "can include file relative" )
	@Test
	public void testCanIncludeFileRelative() {
		instance.executeTemplate(
		    "src/test/java/TestCases/phase2/IncludeTest.cfs",
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "found the value before wood" );
	}

	@DisplayName( "can include file relative with dot dot slash" )
	@Test
	public void testCanIncludeFileRelativeWithDotDotSlash() {
		instance.executeTemplate(
		    "src/test/java/TestCases/phase2/../../../java/TestCases/phase2/IncludeTest.cfs",
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "found the value before wood" );
	}

	@DisplayName( "can include file relative with dot dot slash at start" )
	@Test
	public void testCanIncludeFileRelativeWithDotDotSlashAtStart() {
		instance.executeTemplate(
		    "src/test/java/TestCases/phase2/RelativeIncludeTest.cfm",
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "found the value before wood" );
	}

	@DisplayName( "can include non-compilable file extensions" )
	@Test
	public void testCanIncludeNonCompilableFileExtensions() {
		instance.executeSource(
		    """
		    bx:savecontent variable="result" {
		    	include "src/test/java/TestCases/phase2/NonCompilableFileExtension.txt";
		    }
		          """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "<cfoutput>This will #not# compile<cfset now()></cfoutput>" );
	}

	@DisplayName( "can include obey external mapping flag for static files" )
	@Test
	public void testCanIncludeObeyExternalMappingFlagForStaticFiles() {
		String mappingPath = Paths.get( "src/test/java/TestCases/phase2/" ).toAbsolutePath().toString();
		instance.getConfiguration().registerMapping( "/externalTest", Struct.of(
		    Key.path, mappingPath,
		    Key.external, true
		) );
		instance.getConfiguration().registerMapping( "/internalTest", Struct.of(
		    Key.path, mappingPath,
		    Key.external, false
		) );

		instance.executeSource(
		    """
		    bx:savecontent variable="result" {
		    	bx:include template="/externalTest/NonCompilableFileExtension.txt";
		    }
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "<cfoutput>This will #not# compile<cfset now()></cfoutput>" );

		instance.executeSource(
		    """
		    bx:savecontent variable="result" {
		    	bx:include template="/internalTest/NonCompilableFileExtension.txt";
		    }
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "<cfoutput>This will #not# compile<cfset now()></cfoutput>" );

		Throwable t = assertThrows( BoxIOException.class, () -> instance.executeSource(
		    """
		    bx:include template="/internalTest/NonCompilableFileExtension.txt" externalOnly="true";
		         """,
		    context ) );
		assertThat( t.getMessage() ).contains( "could not be found" );
	}

	@DisplayName( "can include obey external mapping flag for dynamic files" )
	@Test
	public void testCanIncludeObeyExternalMappingFlagForDynamicFiles() {
		String mappingPath = Paths.get( "src/test/java/TestCases/phase2/" ).toAbsolutePath().toString();
		instance.getConfiguration().registerMapping( "/externalTest", Struct.of(
		    Key.path, mappingPath,
		    Key.external, true
		) );
		instance.getConfiguration().registerMapping( "/internalTest", Struct.of(
		    Key.path, mappingPath,
		    Key.external, false
		) );

		instance.executeSource(
		    """
		    bx:include template="/externalTest/IncludeTest.cfs";
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "found the value before wood" );

		instance.executeSource(
		    """
		    bx:include template="/internalTest/IncludeTest.cfs";
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "found the value before wood" );

		Throwable t = assertThrows( MissingIncludeException.class, () -> instance.executeSource(
		    """
		    bx:include template="/internalTest/IncludeTest.cfs" externalOnly="true";
		         """,
		    context ) );
		assertThat( t.getMessage() ).contains( "could not be found" );
	}

	@DisplayName( "can include file again" )
	@Test
	@Disabled
	public void testCanIncludeFileAgain() {
		instance.executeSource(
		    """
		    include "generatePrimes3.cfs";
		    include "generatePrimes3.cfs";
		    include "generatePrimes3.cfs";
		    """,
		    context );
	}

}
