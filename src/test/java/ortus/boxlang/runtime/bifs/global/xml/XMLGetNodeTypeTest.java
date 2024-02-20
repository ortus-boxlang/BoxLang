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

public class XMLGetNodeTypeTest {

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

	@DisplayName( "It can get node type" )
	@Test
	public void testCanGetNodeType() {
		instance.executeSource(
		    """
		          xml = XMLParse( '<?xml version="1.0"?>
		    <breakfast_menu>
		     <food>
		      <name>Eggs Benedict</name>
		      <price>$8.95</price>
		     </food>
		     <food>
		      <name>Pancakes</name>
		      <price>$7.95</price>
		     </food>
		     <food>
		      <name>French Toast</name>
		      <price>$6.95</price>
		     </food>
		    </breakfast_menu>' );


		       result = XMLGetNodeType(xml);
		    result2 = XMLGetNodeType(xml.XMLRoot);
		    result3 = XMLGetNodeType(xml.breakfast_menu.food);
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "DOCUMENT_NODE" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "ELEMENT_NODE" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "ELEMENT_NODE" );

	}

	@DisplayName( "It can get node type member" )
	@Test
	public void testCanGetNodeTypeMember() {
		instance.executeSource(
		    """
		          xml = XMLParse( '<?xml version="1.0"?>
		    <breakfast_menu>
		     <food>
		      <name>Eggs Benedict</name>
		      <price>$8.95</price>
		     </food>
		     <food>
		      <name>Pancakes</name>
		      <price>$7.95</price>
		     </food>
		     <food>
		      <name>French Toast</name>
		      <price>$6.95</price>
		     </food>
		    </breakfast_menu>' );


		       result = xml.getNodeType();
		    result2 = xml.XMLRoot.getNodeType();
		    result3 = xml.breakfast_menu.food.getNodeType();
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "DOCUMENT_NODE" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "ELEMENT_NODE" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "ELEMENT_NODE" );

	}

}
