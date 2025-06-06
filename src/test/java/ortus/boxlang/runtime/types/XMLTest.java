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

import ortus.boxlang.compiler.parser.BoxSourceType;
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

	@DisplayName( "It can assign XMLText" )
	@Test
	void testAssignXMLText() {
		instance.executeSource(
		    """
		    xmlObj = xmlParse( '<Ortus></Ortus>' );
		    xmlObj.xmlRoot.xmlText = "BoxLang";
		    result = xmlObj.xmlRoot.xmlText;
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "BoxLang" );
	}

	@DisplayName( "It can assign XMLCDATA" )
	@Test
	void testAssignXMLCDATA() {
		instance.executeSource(
		    """
		    xmlObj = xmlParse( '<Ortus></Ortus>' );
		    xmlObj.xmlRoot.xmlCDATA = "<BoxLang/>";
		    result = xmlObj.xmlRoot.xmlCDATA;
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "<![CDATA[<BoxLang/>]]>" );
	}

	@DisplayName( "It can assign an XML Atrribute" )
	@Test
	void testAssignXMLAttributes() {
		instance.executeSource(
		    """
		    xmlObj = xmlParse( '<Ortus></Ortus>' );
		    xmlObj.xmlRoot.xmlAttributes.Product = "BoxLang";
		    result = xmlObj.xmlRoot.xmlAttributes;
		         """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		assertThat( variables.getAsStruct( result ).get( "Product" ) ).isEqualTo( "BoxLang" );
	}

	@DisplayName( "It can assign an XML Atrribute" )
	@Test
	void testChangeXMLAttributes() {
		instance.executeSource(
		    """
		       xmlObj = xmlParse( '<Ortus Product="BoxLang"></Ortus>' );
		       initial = xmlObj.xmlRoot.xmlAttributes.Product;
		    xmlObj.xmlRoot.xmlAttributes.Product = "BoxLang Rocks!";
		    result = xmlObj.xmlRoot.xmlAttributes.Product;
		            """,
		    context );
		assertThat( variables.getAsString( Key.of( "initial" ) ) ).isEqualTo( "BoxLang" );
		assertThat( variables.getAsString( result ) ).isEqualTo( "BoxLang Rocks!" );
	}

	@DisplayName( "It can use the member function keyExists on a node" )
	@Test
	void testKeyExistsMember() {
		// @formatter:off
		instance.executeSource(
			"""
			xmlText = "<Ortus><Products><Product>BoxLang</Product><Product>CommandBox</Product><Product>Coldbox</Product></Products></Ortus>";
			parsed = xmlParse( xmlText );
			result = parsed.xmlRoot.xmlChildren.filter( (child ) => child.keyExists( "Product" ) );
			""",
			context
		);
		// @formatter:on
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 1 );
	}

	@DisplayName( "It can remove an XML Atrribute" )
	@Test
	void testRemoveXMLAttributes() {
		instance.executeSource(
		    """
		       xmlObj = xmlParse( '<Ortus Product="BoxLang"></Ortus>' );
		    structDelete( xmlObj.xmlRoot.xmlAttributes, "Product" );
		       result = xmlObj.xmlRoot.xmlAttributes;
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		assertThat( variables.getAsStruct( result ).get( "Product" ) ).isEqualTo( null );
	}

	@DisplayName( "It has the correct values when namespaces are present" )
	@Test
	void testNameSpaceValues() {
		//@formatter:off
		instance.executeSource(
		    """
		    xmlObj = xmlParse( '
			   <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
			   xmlns:xsd="http://www.w3.org/2001/XMLSchema"
			   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			   <SOAP-ENV:Header>
				   <ns1:MessageHeader xmlns:ns1="http://www.ebxml.org/namespaces/messageHeader">
					   <ns1:From>
						   <ns1:PartyId ns1:type="urn:x12.org:IO5:01">Ortus Solutions</ns1:PartyId>
					   </ns1:From>
					   <ns1:Action>Testing</ns1:Action>
				   </ns1:MessageHeader>
			   </SOAP-ENV:Header>
		   </SOAP-ENV:Envelope>
			' );
			envelope = xmlObj.xmlRoot;
			envelopePrefix = envelope.xmlNsPrefix;
			envelopeName = envelope.xmlName;
			envelopeAttributes = envelope.xmlAttributes;
			header = xmlObj.xmlRoot.Header;
			headerPrefix = header.xmlNsPrefix;
		            """,
		    context );
			//@formatter:on

		assertThat( variables.getAsString( Key.of( "envelopePrefix" ) ) ).isEqualTo( "SOAP-ENV" );
		assertThat( variables.getAsString( Key.of( "envelopeName" ) ) ).isEqualTo( "SOAP-ENV:Envelope" );
		assertThat( variables.get( Key.of( "envelopeAttributes" ) ) ).isInstanceOf( Struct.class );
		assertThat( variables.getAsStruct( Key.of( "envelopeAttributes" ) ).get( Key.of( "xmlns:SOAP-ENV" ) ) )
		    .isEqualTo( "http://schemas.xmlsoap.org/soap/envelope/" );
		assertThat( variables.getAsString( Key.of( "headerPrefix" ) ) ).isEqualTo( "SOAP-ENV" );
	}

	@DisplayName( "It can correctly stringify XML objects" )
	@Test
	void testStringification() {

		instance.executeSource(
		    """
		       xmlObj = xmlParse( '<Ortus></Ortus>' );
		       result = toString( xmlObj.xmlRoot );
		    result2 = xmlObj.xmlRoot.toString();
		    xmlObj = xmlParse( '<Ortus><Product>BoxLang</Product></Ortus>' );
		    result3 = toString( xmlObj.xmlRoot.Product );
		            """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Ortus/>" );
		assertThat( variables.get( result ) ).isEqualTo( variables.get( Key.of( "result2" ) ) );
		assertThat( variables.getAsString( Key.of( "result3" ) ) ).isEqualTo( "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Product>BoxLang</Product>" );

	}

	@DisplayName( "It can clone an node and append it to another xmlObject" )
	@Test
	void testNodeCloningAndAppend() {

		//@formatter:off
		instance.executeSource(
		    """
			xmlObj = xmlParse( '<Ortus></Ortus>' );
			xmlContent = xmlParse( '<Products><Product>BoxLang</Product><Product>CommandBox</Product><Product>Coldbox</Product></Products>' );
			target = xmlElemNew( xmlObj, "Products" );
			xmlContent.xmlRoot.xmlChildren.each( ( node ) => {
				target.appendChild(
					target
						.getOwnerDocument()
						.importNode( node.cloneNode( javacast( "boolean", true ) ), javacast( "boolean", true ) )
				)
			} );
			xmlObj.xmlRoot.xmlChildren.append( target );
			result = xmlObj.xmlRoot.Products.xmlChildren.len();
			""",
		context );
		//@formatter:off
		assertThat( variables.getAsInteger( result ) ).isEqualTo( 3 );

	}

	@DisplayName( "It can append XML children using array index notation" )
	@Test
	public void testAppendChildrenArrayNotation() {
		instance.executeSource(
		    """
			xmlObj = xmlParse( '<Ortus><Products></Products></Ortus>' );
			products = [ "BoxLang", "CommandBox" ];
			for( i = 1; i <= products.len(); i++ ){
				product = xmlElemNew( xmlObj, "Product" );
				product.xmlText = products[ i ];
				xmlObj.xmlRoot.Products.xmlChildren[ i ] = product;
			}
			result = xmlObj.xmlRoot.Products.xmlChildren.len();
			xmlString = toString( xmlObj );
		    """,
		    context, BoxSourceType.CFSCRIPT );

		System.out.println( variables.get( Key.of( "xmlString" ) ) );

		assertThat( variables.getAsInteger( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "Can do keyExists on XML nodes" )
	@Test
	public void testKeyExists() {
		instance.executeSource(
		    """
			xmlObj = xmlParse( '<Ortus><Products></Products></Ortus>' );
			println( xmlObj.getReferencableKeys() )
			result = xmlObj.keyExists( "Ortus" );
		    """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.getAsBoolean( result ) ).isEqualTo( true );
	}

}
