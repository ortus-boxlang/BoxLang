
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.XML;

public class XMLElemNewTest {

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

	@DisplayName( "It tests the BIF XMLElemNew" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		          myXML=xmlNew();
		          result = XMLElemNew( myXML, "root" );
		       myXML.xmlRoot = result;
		    myXML.xmlRoot.xmlChildren.append( xmlElemNew( myXML, "myChild" ) ).append( xmlElemNew( myXML, "myChild" ) ).prepend( xmlElemNew( myXML, "mySecondChild" ) );
		          """,
		    context );
		assertTrue( variables.get( result ) instanceof XML );
		XML doc = variables.getAsXML( Key.of( "myXML" ) );
		assertEquals( "root", doc.getNode().getFirstChild().getNodeName() );
		assertEquals( 3, doc.getNode().getFirstChild().getChildNodes().getLength() );

	}

	@DisplayName( "It tests the BIF XMLElemNew with a namespace" )
	@Test
	public void testBifNamespace() {
		instance.executeSource(
		    """
		       myXML=xmlNew();
		       result = XMLElemNew( myXML, "Envelope", "http://schemas.xmlsoap.org/soap/envelope/" );
		    myXML.xmlRoot = result;
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof XML );
		XML doc = variables.getAsXML( Key.of( "myXML" ) );
		assertEquals( "Envelope", doc.getNode().getFirstChild().getNodeName() );
		NamedNodeMap attributes = doc.getNode().getFirstChild().getAttributes();
		assertEquals( "http://schemas.xmlsoap.org/soap/envelope/", attributes.getNamedItem( "xmlns" ).getNodeValue() );

	}

	@DisplayName( "It tests the ability to append the result of XMLElemNew to the XML children" )
	@Test
	public void testNodeAppend() {
		instance.executeSource(
		    """
		    xmlObj = xmlParse( '<Ortus></Ortus>' );
		    newNode = xmlElemNew( xmlObj.xmlRoot, "BoxLang" );
		    xmlObj.xmlRoot.xmlChildren.append( newNode );
		    result = xmlObj.xmlRoot.BoxLang.xmlName;
		    childCount = xmlObj.xmlRoot.xmlChildren.len();
		         """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( "BoxLang", variables.getAsString( result ) );
		assertEquals( 1, variables.getAsInteger( Key.of( "childCount" ) ) );

	}

	@DisplayName( "It tests the ability to use the return XMLElemNew as a node" )
	@Test
	public void testNodeReturn() {
		instance.executeSource(
		    """
		    xmlObj = xmlParse( '<Ortus></Ortus>' );
		    newNode = xmlElemNew( xmlObj.xmlRoot, "BoxLang" );
		    result = newNode.xmlName;
		         """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( "BoxLang", variables.getAsString( result ) );

	}

	@DisplayName( "It can accept namespace arg as second argument" )
	@Test
	public void testNameSpaceTwo() {
		instance.executeSource(
		    """
		    envelope = xmlParse( "<soapenv:Envelope xmlns:soapenv=""http://schemas.xmlsoap.org/soap/envelope/""><soapenv:Header/><soapenv:Body/></soapenv:Envelope>" );
		    soapBody    = envelope.xmlRoot[ "soapenv:Body" ];
		    result = xmlElemNew(
		       soapBody,
		       "http://services.rccl.com/Interfaces/GroupList",
		       "getGroupList"
		    );
		           """,
		    context );
		assertTrue( variables.get( result ) instanceof XML );
		// assertEquals( "BoxLang", variables.getAsString( result ) );

	}

	@DisplayName( "Tests some typing and abstraction of XML objects" )
	@Test
	@Disabled
	void testTyping() {
		//@formatter:off
		instance.executeSource(
		    """
			xml function createXMLNode(
				required xml parent,
				required string name,
				any value,
				struct attributes = {},
				struct children   = {}
			){
				var newNode = xmlElemNew( arguments.parent, arguments.name );
				if ( !isNull( arguments.value ) && isSimpleValue( arguments.value ) ) {
					newNode.xmlText = arguments.value;
				} else if ( !isNull( arguments.value ) && isStruct( arguments.value ) ) {
					arguments.attributes = arguments.value;
				}
				arguments.attributes
					.keyArray()
					.each( function( key ){
						if ( !isNull( attributes[ key ] ) ) {
							newNode.xmlAttributes[ key ] = attributes[ key ];
						}
					} );
				if ( !arguments.children.isEmpty() ) {
					appendChildNodes( parent = newNode, children = arguments.children );
				}
				arrayAppend( parent.xmlChildren, newNode );
				return parent.xmlChildren[ parent.xmlChildren.len() ];
			}

			void function appendChildNodes( required xml parent, required struct children ){
				arguments.children
					.keyArray()
					.filter( ( childName ) => !isNull( children[ childName ] ) )
					.each( function( childName ){
						if ( !isStruct( children[ childName ] ) ) {
							createXMLNode(
								parent = parent,
								name   = childName,
								value  = children[ childName ]
							);
						} else {
							var nodeArgs         = children[ childName ];
							nodeArgs[ "parent" ] = parent;
							nodeArgs[ "name" ]   = childName;
							createXMLNode( argumentCollection = nodeArgs );
						}
					} );
			}

			xmlObj = xmlParse( "<Ortus></Ortus>" );
			createXMLNode( xmlObj.xmlRoot, "BoxLang", "Rocks!" );
			// createXMLNode( 
			// 	xmlObj.xmlRoot.BoxLang,
			// 	{
			// 		"Developers" : {
			// 			"Majano" : {
			// 				attributes : {
			// 					"firstName" : "Luis",
			// 					"lastName" : "Majano"
			// 				}
			// 			}
			// 		}

			// 	},
			// 	{
			// 		"tagline" : "Boxlang is good, better, best"
			// 	}
			// );
				printLine( toString( xmlObj ) );
		         """,
		    context );
		// @formatter:on
		// assertTrue( variables.get( result ) instanceof String );
		// assertTrue( variables.get( result ) instanceof String );
		// assertEquals( "BoxLang", variables.getAsString( result ) );
		// assertEquals( "Ortus", variables.getAsString( result ) );
	}

}
