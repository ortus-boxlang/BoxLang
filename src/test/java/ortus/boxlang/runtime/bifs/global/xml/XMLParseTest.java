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

package ortus.boxlang.runtime.bifs.global.xml;

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
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class XMLParseTest {

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

	@DisplayName( "It can parse" )
	@Test
	public void testCanParse() {
		instance.executeSource(
		    """
		    result = XMLParse( '<root><brad name="wood" /></root>' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It can parse from file" )
	@Test
	public void testCanParseFromFile() {
		instance.executeSource(
		    """
		    import java.io.File;
		      xml = new File( "src/test/java/ortus/boxlang/runtime/bifs/global/xml/document.xml" ).getAbsolutePath();

		         result = XMLParse( xml );
		         """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It can parse XML with an inaccessible DTD URL" )
	@Test
	public void testCanParseWithInaccessibleDTD() {
		instance.executeSource(
		    """
		       result = XMLParse(
		    	xml='<!DOCTYPE root SYSTEM "http://www.mach-ii.com/dtds/mach-ii_1_9_0.dtd"><root><brad name="wood" /></root>',
		    	caseSensitive=false,
		    	validator={ "disallowDoctypeDecl": false },
		    	lenient=true
		    );
		       """,
		    context
		);
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	// -------------------------------------------------------------------
	// Default security settings tests (no bx:application overrides)
	// -------------------------------------------------------------------

	@DisplayName( "It fails to parse XML with DOCTYPE when disallowDoctypeDeclaration is true (default)" )
	@Test
	public void testFailsToParseWithDoctypeByDefault() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://example.invalid/dtd\"><root><item/></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = XMLParse( xmlWithDoctype );
		        """,
		        context )
		);
	}

	@DisplayName( "It parses well-formed XML successfully with default settings" )
	@Test
	public void testParsesWellFormedXmlWithDefaults() {
		instance.executeSource(
		    """
		    result = XMLParse( '<root><item>hello</item></root>' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	// -------------------------------------------------------------------
	// bx:application xmlSettings override tests
	// -------------------------------------------------------------------

	@DisplayName( "It parses XML with DOCTYPE when disallowDoctypeDeclaration is false via bx:application" )
	@Test
	public void testParsesWithDoctypeWhenDisallowDoctypeDeclarationFalse() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://example.invalid/dtd\"><root><item>test</item></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		instance.executeSource(
		    """
		    bx:application name="xmlTestApp1" xmlSettings={ disallowDoctypeDeclaration: false, lenientProcessing: true };

		    result = XMLParse( xmlWithDoctype );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It fails to parse XML with DOCTYPE when disallowDoctypeDeclaration is true via bx:application" )
	@Test
	public void testFailsWithDoctypeWhenDisallowDoctypeDeclarationTrue() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://example.invalid/dtd\"><root><item/></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        bx:application name="xmlTestApp2" xmlSettings={ disallowDoctypeDeclaration: true };
		        result = XMLParse( xmlWithDoctype );
		        """,
		        context )
		);
	}

	@DisplayName( "It parses with lenientProcessing true in bx:application settings" )
	@Test
	public void testParsesWithLenientProcessingTrueInAppSettings() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://www.mach-ii.com/dtds/mach-ii_1_9_0.dtd\"><root><item>test</item></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		instance.executeSource(
		    """
		    bx:application name="xmlTestApp3" xmlSettings={ disallowDoctypeDeclaration: false, lenientProcessing: true };

		    result = XMLParse( xmlWithDoctype );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It rejects well-formed DOCTYPE when disallowDoctypeDeclaration is true even with lenientProcessing" )
	@Test
	public void testRejectsDoctypeWithDisallowDoctypeDeclarationTrueAndLenientProcessing() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://example.invalid/dtd\"><root><item/></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        bx:application name="xmlTestApp4" xmlSettings={ disallowDoctypeDeclaration: true, lenientProcessing: true };
		        result = XMLParse( xmlWithDoctype );
		        """,
		        context )
		);
	}

	// -------------------------------------------------------------------
	// Legacy key backward compatibility tests (non-canonical key names)
	// -------------------------------------------------------------------

	@DisplayName( "It supports legacy key disallowDoctypeDecl in bx:application xmlSettings" )
	@Test
	public void testSupportsLegacyDisallowDoctypeDeclInAppSettings() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://example.invalid/dtd\"><root><item>test</item></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		instance.executeSource(
		    """
		    bx:application name="xmlTestApp5" xmlSettings={ disallowDoctypeDecl: false, lenient: true };

		    result = XMLParse( xmlWithDoctype );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It supports legacy key secure in bx:application xmlSettings" )
	@Test
	public void testSupportsLegacySecureInAppSettings() {
		instance.executeSource(
		    """
		    bx:application name="xmlTestApp6" xmlSettings={ secure: true, lenient: false };

		    result = XMLParse( '<root><item>secure</item></root>' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It supports legacy key externalGeneralEntities in bx:application xmlSettings" )
	@Test
	public void testSupportsLegacyExternalGeneralEntitiesInAppSettings() {
		instance.executeSource(
		    """
		    bx:application name="xmlTestApp7" xmlSettings={ externalGeneralEntities: false, lenient: false };

		    result = XMLParse( '<root><item>entities</item></root>' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It supports all legacy keys together in bx:application xmlSettings" )
	@Test
	public void testSupportsAllLegacyKeysInAppSettings() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://example.invalid/dtd\"><root><item>test</item></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		instance.executeSource(
		    """
		    bx:application name="xmlTestAppAllLegacy" xmlSettings={ secure: false, disallowDoctypeDecl: false, externalGeneralEntities: false, lenient: true };

		    result = XMLParse( xmlWithDoctype );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	// -------------------------------------------------------------------
	// BIF argument override tests
	// -------------------------------------------------------------------

	@DisplayName( "Explicit lenient=true argument overrides config lenientProcessing=false" )
	@Test
	public void testLenientArgumentOverridesConfig() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://www.mach-ii.com/dtds/mach-ii_1_9_0.dtd\"><root><item>test</item></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		instance.executeSource(
		    """
		    bx:application name="xmlTestApp8" xmlSettings={ disallowDoctypeDeclaration: false, lenientProcessing: false };

		    // lenient=true in the BIF call should override the config's lenientProcessing=false
		    result = XMLParse( xmlWithDoctype, false, { "disallowDoctypeDecl": false }, true );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "Explicit lenient=false argument overrides config lenientProcessing=true" )
	@Test
	public void testLenientFalseArgumentOverridesConfig() {
		String xmlWithDoctype = "<!DOCTYPE root SYSTEM \"http://www.mach-ii.com/dtds/mach-ii_1_9_0.dtd\"><root><item/></root>";
		variables.put( Key.of( "xmlWithDoctype" ), xmlWithDoctype );

		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        bx:application name="xmlTestApp9" xmlSettings={ disallowDoctypeDeclaration: false, lenientProcessing: true };

		        // lenient=false in the BIF call should override the config's lenientProcessing=true
		        result = XMLParse( xmlWithDoctype, false, { "disallowDoctypeDeclaration": false }, false );
		        """,
		        context )
		);
	}

	@DisplayName( "It parses with secureProcessing false via bx:application" )
	@Test
	public void testParsesWithSecureProcessingFalseInAppSettings() {
		instance.executeSource(
		    """
		    bx:application name="xmlTestApp10" xmlSettings={ secureProcessing: false };

		    result = XMLParse( '<root><item>hello</item></root>' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
	}

}
