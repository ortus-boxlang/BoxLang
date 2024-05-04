
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

import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class XMLFormatTest {

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

	@DisplayName( "It tests the BIF XMLFormat" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    stringXML = '<root foo="bar" />';
		    result = XMLFormat( stringXML );
		    """,
		    context );

		assertEquals( "&lt;root foo=&quot;bar&quot; /&gt;", variables.getAsString( result ) );

		instance.executeSource(
		    """
		    stringXML = '<root foo="bar" illegal="#char(00)##char(12)#" />';
		    result = XMLFormat( stringXML, true );
		    """,
		    context );

		assertEquals( "&lt;root foo=&quot;bar&quot; illegal=&quot;&quot; /&gt;", variables.getAsString( result ) );

	}

	@DisplayName( "It tests the member function for String.XMLFormat" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    stringXML = '<root foo="bar" />';
		    result = stringXML.XMLFormat();
		    """,
		    context );

		assertEquals( "&lt;root foo=&quot;bar&quot; /&gt;", variables.getAsString( result ) );

		instance.executeSource(
		    """
		    stringXML = '<root foo="bar" illegal="#char(00)##char(12)#" />';
		    result = stringXML.XMLFormat( true );
		    """,
		    context );

		assertEquals( "&lt;root foo=&quot;bar&quot; illegal=&quot;&quot; /&gt;", variables.getAsString( result ) );
	}

}
