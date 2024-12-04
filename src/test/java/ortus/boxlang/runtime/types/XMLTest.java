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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

class XMLTest {

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

	@DisplayName( "create simple document" )
	@Test
	void testEqualsAndHashCode() {
		XML xml = new XML( """
		                                 <root>
		                     pre
		                                 	<child1>value1</child1>
		                   middle
		                                 	<child2>value2</child2>
		                   post
		                                 </root>
		                                 """ );
	}

	@DisplayName( "access document fields" )
	@Test
	void testAccessDocumentFields() {
		instance.executeSource(
		    """
		             result = XMLParse( '<!-- my doc comment --><root attr="my value">
		    	  pre
		    <![CDATA[cdata]]>
		    					  <child>value1</child>
		    	   middle
		     <!-- my inline comment -->
		    					  <child>value2</child>
		    	   post

		    				  </root>' );
		       rootName = result.XMLName;
		       rootType = result.XMLType;
		       rootValue = result.XMLValue;
		       rootRoot = result.root;
		       rootXMLRoot = result.XMLroot;
		       rootXMLComment = result.XMLComment;
		       rootXMLDocType = result.XMLDocType;

		             """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
		assertThat( variables.get( Key.of( "rootName" ) ) ).isEqualTo( "#document" );
		assertThat( variables.get( Key.of( "rootType" ) ) ).isEqualTo( "DOCUMENT" );
		assertThat( variables.get( Key.of( "rootValue" ) ) ).isEqualTo( "" );
		assertThat( variables.get( Key.of( "rootRoot" ) ) ).isInstanceOf( XML.class );
		assertThat( variables.get( Key.of( "rootXMLRoot" ) ) ).isInstanceOf( XML.class );
		assertThat( variables.getAsString( Key.of( "rootXMLComment" ) ).replaceAll( "\\s", "" ) ).isEqualTo( "mydoccomment" );
		assertThat( variables.get( Key.of( "rootXMLDocType" ) ) ).isInstanceOf( XML.class );

	}

	@DisplayName( "access element fields" )
	@Test
	void testAccessElementFields() {
		instance.executeSource(
		    """
		                          result = XMLParse( '<!-- my doc comment --><root attr="my value">
		                       pre
		              <![CDATA[cdata]]>
		                       				<child>value1</child>
		                        middle
		            <!-- my inline comment -->
		                       				<child>value2</child>
		                        post

		                       			</root>' );
		       root = result.XMLroot;
		       rootName = root.XMLName;
		       rootType = root.XMLType;
		       rootValue = root.XMLValue;

		    // Gets first matching child
		       rootChild = root.child;

		    // Gets first matching child
		       rootChild1 = root.child[1];

		    // Gets second matching child
		       rootChild2 = root.child[2];

		       rootXMLComment = root.XMLComment;
		       rootXMLParent = root.XMLParent;
		       rootXMLChildren = root.XMLChildren;
		       rootXMLNodes = root.XMLNodes;
		       rootXMLText = root.XMLText;
		       rootXMLCdata = root.XMLCdata;
		       rootXMLAttributes = root.XMLAttributes;
		       rootXMLNsPrefix = root.XMLNsPrefix;
		       rootXMLNsURI = root.XMLNsURI;
		                          """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( XML.class );
		assertThat( variables.get( Key.of( "rootName" ) ) ).isEqualTo( "root" );
		assertThat( variables.get( Key.of( "rootType" ) ) ).isEqualTo( "ELEMENT" );
		assertThat( variables.get( Key.of( "rootValue" ) ) ).isEqualTo( "" );

		assertThat( variables.get( Key.of( "rootChild" ) ) ).isInstanceOf( XML.class );
		assertThat( variables.getAsXML( Key.of( "rootChild" ) ).getXMLText() ).isEqualTo( "value1" );

		assertThat( variables.get( Key.of( "rootChild1" ) ) ).isInstanceOf( XML.class );
		assertThat( variables.getAsXML( Key.of( "rootChild1" ) ).getXMLText() ).isEqualTo( "value1" );

		assertThat( variables.get( Key.of( "rootChild2" ) ) ).isInstanceOf( XML.class );
		assertThat( variables.getAsXML( Key.of( "rootChild2" ) ).getXMLText() ).isEqualTo( "value2" );

		assertThat( variables.getAsString( Key.of( "rootXMLComment" ) ).replaceAll( "\\s", "" ) ).isEqualTo( "myinlinecomment" );
		assertThat( variables.get( Key.of( "rootXMLParent" ) ) ).isInstanceOf( XML.class );

		assertThat( variables.get( Key.of( "rootXMLChildren" ) ) ).isInstanceOf( Array.class );
		Array children = variables.getAsArray( Key.of( "rootXMLChildren" ) );
		assertThat( children.size() ).isEqualTo( 2 );
		assertThat( children.get( 0 ) ).isInstanceOf( XML.class );
		assertThat( ( ( XML ) children.get( 0 ) ).dereference( context, Key.XMLName, false ) ).isEqualTo( "child" );
		assertThat( ( ( XML ) children.get( 0 ) ).dereference( context, Key.XMLText, false ) ).isEqualTo( "value1" );
		assertThat( children.get( 1 ) ).isInstanceOf( XML.class );
		assertThat( ( ( XML ) children.get( 1 ) ).dereference( context, Key.XMLName, false ) ).isEqualTo( "child" );
		assertThat( ( ( XML ) children.get( 1 ) ).dereference( context, Key.XMLText, false ) ).isEqualTo( "value2" );

		assertThat( variables.get( Key.of( "rootXMLNodes" ) ) ).isInstanceOf( Array.class );
		Array nodes = variables.getAsArray( Key.of( "rootXMLNodes" ) );
		assertThat( nodes.size() ).isEqualTo( 9 );

		assertThat( variables.getAsString( Key.of( "rootXMLText" ) ).replaceAll( "\\s", "" ) ).isEqualTo( "precdatamiddlepost" );
		assertThat( variables.getAsString( Key.of( "rootXMLCdata" ) ).replaceAll( "\\s", "" ) ).isEqualTo( "precdatamiddlepost" );

		assertThat( variables.get( Key.of( "rootXMLAttributes" ) ) ).isInstanceOf( Struct.class );
		IStruct attributes = variables.getAsStruct( Key.of( "rootXMLAttributes" ) );
		assertThat( attributes.size() ).isEqualTo( 1 );
		assertThat( attributes.get( "attr" ) ).isEqualTo( "my value" );

		assertThat( variables.get( Key.of( "rootXMLNsPrefix" ) ) ).isEqualTo( "" );
		assertThat( variables.get( Key.of( "rootXMLNsURI" ) ) ).isEqualTo( "" );

		assertThat( variables.getAsXML( result ).asString() ).contains( "<!-- my inline comment -->" );

	}

	@DisplayName( "XML Node Is Struct" )
	@Test
	void testXMLNodeIsStruct() {
		instance.executeSource(
		    """
		          result = XMLParse( '<company name="Ortus Solutions">
		       	<employee fname="Luis" lname="Majano"  />
		       	<employee fname="Brad" lname="Wood" />
		       </company>' );
		       isStructDoc = isStruct( result );
		       isStructRoot = isStruct( result.company );
		       isStructChild = isStruct( result.company.employee[1] );
		    structGetEmp = structGet( "result.company.employee" );
		    structExists = structKeyExists( result.company, "employee" );
		    structExists2 = structKeyExists( result.company, "employee2" );
		                            """,
		    context );
		assertThat( variables.get( Key.of( "isStructDoc" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "isStructRoot" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "isStructChild" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "structExists" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "structExists2" ) ) ).isEqualTo( false );
		assertThat( variables.getAsXML( Key.of( "structGetEmp" ) ).getXMLName() ).isEqualTo( "employee" );
		assertThat( variables.getAsXML( Key.of( "structGetEmp" ) ).getXMLAttributes().get( "fname" ) ).isEqualTo( "Luis" );
	}

	@Test
	void testSafeDereference() {
		instance.executeSource(
		    """
		       		       		          result = XMLParse( '<?xml version="1.0" encoding="UTF-8"?>

		       		       <Config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		       		       	xsi:noNamespaceSchemaLocation="http://www.coldbox.org/schema/config_3.0.0.xsd">
		       		       		<IOC>
		       		       			<Framework type="coldspring or lightwire" reload="true or false" objectCaching="true or false">definition file</Framework>
		       		       		</IOC>
		       		       </Config>' );

		       		       			iocNodes = XMLSearch(result,"//IOC");

		     result = isNull( iocNodes[1]["ParentFactory"] );
		    result2 = isNull( iocNodes[1].ParentFactory )
		    result3 = structKeyExists( iocNodes[1], "ParentFactory" )
		       		       		                            """,
		    context );
		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( false );
	}

	/**
	 * @TODO: Brad will need to look at how we can handle the expected behavior where using a named key returns an array
	 *
	 */
	@DisplayName( "XML Nodes used with struct annotation in a loop return an array" )
	@Disabled
	void testXMLNodeNamedArray() {
		instance.executeSource(
		    """
		          myXML = XMLParse( '<company name="Ortus Solutions">
		         	<employee fname="Luis" lname="Majano"  />
		         	<employee fname="Brad" lname="Wood" />
		         </company>' );
		         result = [];
		    for( employee in myXML.xmlRoot.employee ){
		    	result.append( employee.fname + " " + employee.lname );
		    }
		      	""",
		    context );
		assertEquals( 2, variables.getAsArray( result ).size() );

	}

	@DisplayName( "Can check for XMLChildren with StructKeyExists" )
	@Test
	void textStructKeyExistsXmlChildren() {
		instance.executeSource(
		    """
		       myXML = XMLParse(
		    	'<company name="Ortus Solutions">
		            	<employee fname="Luis" lname="Majano"  />
		            	<employee fname="Brad" lname="Wood" />
		            </company>'
		    );
		       result = structKeyExists( myXML.xmlRoot, "XmlChildren" );
		       """,
		    context );
		Object r = variables.get( result );
		assertTrue( variables.getAsBoolean( result ) );
	}

}
