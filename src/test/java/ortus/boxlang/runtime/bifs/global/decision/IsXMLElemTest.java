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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

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

public class IsXMLElemTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
		variables.put(
		    Key.of( "xmlString" ),
		    """
		    <rootNode>
		    	<subNode attr="value" />
		    </rootNode>
		    """ );
	}

	@DisplayName( "It works on Document" )
	@Test
	public void testDocument() {
		instance.executeSource(
		    """
		    result = IsXMLElem( XMLParse( xmlString ) )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@DisplayName( "It works on root node" )
	@Test
	public void testRootNode() {
		instance.executeSource(
		    """
		    result = IsXMLElem( XMLParse( xmlString ).rootNode )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It works on Element" )
	@Test
	public void testElement() {
		instance.executeSource(
		    """
		    result = IsXMLElem( XMLParse( xmlString ).rootNode.subNode )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It works Random Node" )
	@Test
	public void testRandomNode() {
		instance.executeSource(
		    """
		    xml = XMLParse( xmlString );
		    search = xmlSearch( xml, '//@attr' )
		      result = IsXMLElem( search[1] )
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@DisplayName( "It works on Non-XML var" )
	@Test
	public void testNonXMLVar() {
		instance.executeSource(
		    """
		    result = IsXMLElem( [] )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

}
