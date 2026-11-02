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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
	void testStructKeyExistsXmlChildren() {
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

	@DisplayName( "Can set XMLChildren to empty array" )
	@Test
	void testSetXMLChildrenToEmptyArray() {
		instance.executeSource(
		    """
		          myXML = XMLParse(
		       	'<company name="Ortus Solutions">
		               	<employee fname="Luis" lname="Majano"  />
		               	<employee fname="Brad" lname="Wood" />
		               </company>'
		       );
		    myXML.xmlRoot.XmlChildren = [];
		    result = myXML.xmlRoot.XmlChildren;
		          """,
		    context );
		Object r = variables.get( result );
		assertThat( r ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "Assigning XMLChildren replaces the element children, in order" )
	@Test
	void testSetXMLChildrenToArray() {
		instance.executeSource(
		    """
		          myXML = XMLParse( '<company><old1 /><old2 /></company>' );
		    myXML.xmlRoot.XmlChildren = [];
		    myXML.xmlRoot.XmlChildren = [
		        xmlElemNew( myXML, "first" ),
		        xmlElemNew( myXML, "second" ),
		        xmlElemNew( myXML, "third" )
		    ];
		    result  = myXML.xmlRoot.XmlChildren;
		    names   = myXML.xmlRoot.XmlChildren.map( ( child ) => child.xmlName );
		    xml     = toString( myXML );
		          """,
		    context );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 3 );
		assertThat( variables.getAsArray( Key.of( "names" ) ) ).containsExactly( "first", "second", "third" ).inOrder();
		// The previous children are gone (overwrite, not append)
		assertThat( variables.getAsString( Key.of( "xml" ) ) ).doesNotContain( "old1" );
		assertThat( variables.getAsString( Key.of( "xml" ) ) ).doesNotContain( "old2" );
	}

	@DisplayName( "Assigning an empty XMLChildren array keeps xmlText and attributes" )
	@Test
	void testSetXMLChildrenToEmptyArrayKeepsText() {
		instance.executeSource(
		    """
		          myXML  = XMLParse( '<company><employee /></company>' );
		    myXML.xmlRoot.xmlText = "keep me";
		    myXML.xmlRoot.XmlChildren = [];
		    result = {
		        childrenCount : myXML.xmlRoot.XmlChildren.len(),
		        text          : myXML.xmlRoot.xmlText,
		        name          : myXML.xmlRoot.xmlName
		    };
		          """,
		    context );
		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsInteger( Key.of( "childrenCount" ) ) ).isEqualTo( 0 );
		assertThat( result.getAsString( Key.of( "text" ) ) ).contains( "keep me" );
		assertThat( result.getAsString( Key.of( "name" ) ) ).isEqualTo( "company" );
	}

	@DisplayName( "Assigning a non-array to XMLChildren throws" )
	@Test
	void testSetXMLChildrenRequiresArray() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		          myXML = XMLParse( '<company><employee /></company>' );
		    myXML.xmlRoot.XmlChildren = "not an array";
		          """,
		    context ) );

		// A non-element child is rejected rather than corrupting the document
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		          myXML = XMLParse( '<company><employee /></company>' );
		    myXML.xmlRoot.XmlChildren = [ "nope" ];
		          """,
		    context ) );

		// A bare XML node is rejected: an XML casts to an Array of its same-name siblings,
		// which would silently expand into more children than the caller asked for
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		          myXML = XMLParse( '<company><employee /><employee /></company>' );
		    myXML.xmlRoot.XmlChildren = myXML.xmlRoot.employee;
		          """,
		    context ) );
	}

	@DisplayName( "Can assign a Java List to XMLChildren" )
	@Test
	void testSetXMLChildrenFromJavaList() {
		instance.executeSource(
		    """
		          myXML = XMLParse( '<company><old /></company>' );
		          jl    = new java.util.ArrayList();
		          jl.add( xmlElemNew( myXML, "fromList" ) );
		    myXML.xmlRoot.XmlChildren = jl;
		    result = toString( myXML );
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "<fromList/>" );
		assertThat( variables.getAsString( result ) ).doesNotContain( "old" );
	}

	@DisplayName( "Can assign a Java array to XMLChildren" )
	@Test
	void testSetXMLChildrenFromJavaArray() {
		instance.executeSource(
		    """
		          myXML = XMLParse( '<company><old /></company>' );
		          ja    = [ xmlElemNew( myXML, "fromArray" ) ].toArray();
		    myXML.xmlRoot.XmlChildren = ja;
		    result = toString( myXML );
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "<fromArray/>" );
		assertThat( variables.getAsString( result ) ).doesNotContain( "old" );
	}

	@DisplayName( "Assigning cross-document XMLChildren works and the source document is untouched" )
	@Test
	void testSetXMLChildrenCrossDocument() {
		instance.executeSource(
		    """
		          target = XMLParse( '<company><old /></company>' );
		          other  = XMLParse( '<employee fname="Dave" />' );
		    target.xmlRoot.XmlChildren = [ other.xmlRoot ];
		    result        = toString( target );
		    sourceStillOk = toString( other );
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "<employee fname=\"Dave\"/>" );
		assertThat( variables.getAsString( result ) ).doesNotContain( "old" );
		// The source document is not stolen from
		assertThat( variables.getAsString( Key.of( "sourceStillOk" ) ) ).contains( "employee" );
	}

	@DisplayName( "It can assign a cross-document node to a property" )
	@Test
	void testAssignCrossDocumentProperty() {
		instance.executeSource(
		    """
		          target = XMLParse( '<company><old /></company>' );
		          other  = XMLParse( '<employee fname="Dave" />' );
		    target.xmlRoot.employee = other.xmlRoot;
		    result = toString( target );
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "<employee fname=\"Dave\"/>" );
	}

	@DisplayName( "It can append a cross-document node to the XMLChildren array" )
	@Test
	void testAppendCrossDocumentChild() {
		instance.executeSource(
		    """
		          target = XMLParse( '<company><old /></company>' );
		          other  = XMLParse( '<employee fname="Dave" />' );
		    target.xmlRoot.xmlChildren.append( other.xmlRoot );
		    result     = toString( target );
		    childCount = target.xmlRoot.xmlChildren.len();
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "<employee fname=\"Dave\"/>" );
		assertThat( variables.getAsInteger( Key.of( "childCount" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "Setting an XMLChildren index populates the array slot, not null" )
	@Test
	void testSetXMLChildrenIndexDoesNotNullTheSlot() {
		instance.executeSource(
		    """
		          target   = XMLParse( '<company><old /></company>' );
		          children = target.xmlRoot.xmlChildren;
		    children[ 1 ] = xmlElemNew( target, "newOne" );
		    result = {
		        slotIsNull  : isNull( children[ 1 ] ),
		        arrayLen    : arrayLen( children ),
		        freshCount  : target.xmlRoot.xmlChildren.len(),
		        xml         : toString( target )
		    };
		          """,
		    context );
		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsBoolean( Key.of( "slotIsNull" ) ) ).isFalse();
		assertThat( result.getAsInteger( Key.of( "arrayLen" ) ) ).isEqualTo( 1 );
		assertThat( result.getAsInteger( Key.of( "freshCount" ) ) ).isEqualTo( 1 );
		assertThat( result.getAsString( Key.of( "xml" ) ) ).contains( "<newOne/>" );
		assertThat( result.getAsString( Key.of( "xml" ) ) ).doesNotContain( "old" );
	}

	@DisplayName( "It can use XML.appendChild and XML.replaceChild with cross-document nodes" )
	@Test
	void testAppendAndReplaceChildCrossDocument() {
		XML		target		= new XML( "<company><old /></company>", false, null, null, context );
		XML		other		= new XML( "<employee fname=\"Dave\" />", false, null, null, context );
		XML		targetRoot	= new XML( ( ( Document ) target.getNode() ).getDocumentElement() );
		XML		otherRoot	= new XML( ( ( Document ) other.getNode() ).getDocumentElement() );
		Node	oldNode		= ( ( Document ) target.getNode() ).getElementsByTagName( "old" ).item( 0 );

		targetRoot.appendChild( otherRoot.getNode() );
		assertThat( targetRoot.getNode().getChildNodes().getLength() ).isEqualTo( 2 );
		assertThat( targetRoot.getNode().getLastChild().getNodeName() ).isEqualTo( "employee" );

		targetRoot.replaceChild( otherRoot.getNode(), oldNode );
		assertThat( targetRoot.getNode().getChildNodes().getLength() ).isEqualTo( 2 );
		assertThat( targetRoot.getNode().getFirstChild().getNodeName() ).isEqualTo( "employee" );
		// The source document must not be emptied by the append/replace
		assertThat( ( ( Document ) other.getNode() ).getDocumentElement().getNodeName() ).isEqualTo( "employee" );
	}

	@DisplayName( "It can root a brand new XML document with a node from another document" )
	@Test
	void testRootNewDocumentCrossDocument() {
		instance.executeSource(
		    """
		          target = xmlNew();
		          other  = XMLParse( '<otherRoot><x /></otherRoot>' );
		    target.xmlRoot = other.xmlRoot;
		    result = toString( target );
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "otherRoot" );
	}

	@DisplayName( "Assigning XMLRoot to a node from another document replaces the document" )
	@Test
	void testAssignXMLRootCrossDocument() {
		instance.executeSource(
		    """
		          target = XMLParse( '<company><old /></company>' );
		          other  = XMLParse( '<otherRoot><x /></otherRoot>' );
		    target.xmlRoot = other.xmlRoot;
		    result = toString( target );
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "<otherRoot>" );
		assertThat( variables.getAsString( result ) ).doesNotContain( "company" );
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

		assertThat( variables.getAsInteger( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "It can insert XML children at currently-used index" )
	@Test
	public void testInsertChildrenAtCurrentIndex() {
		instance.executeSource(
		    """
			// It's important this XML has the whitespace so we make sure we ignore text nodes
			xmlObj = xmlParse( '<Ortus>
				<Products>
					<Product>CommandBox</Product>
					<Product>BoxLang</Product>
				</Products>
			</Ortus>' );
			
			product = xmlElemNew( xmlObj, "Product" );
			product.xmlText = "ContentBox";
			arrayInsertAt( xmlObj.xmlRoot.Products.xmlChildren, 2, product );
			result = xmlObj.xmlRoot.Products.xmlChildren.len();
			xmlString = toString( xmlObj );
		    """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.getAsInteger( result ) ).isEqualTo( 3 );
		// Verify the order of the children elements
		XML products = variables.getAsXML( Key.of( "xmlObj" ) ).getAsXML( Key.of("Ortus" ) ).getAsXML( Key.of( "Products" ) );
		assertThat( products.dereference( context, Key.XMLChildren, false ) ).isInstanceOf( Array.class );
		Array children = products.getAsArray( Key.XMLChildren );
		assertThat( children.size() ).isEqualTo( 3 );
		assertThat( ( ( XML ) children.get( 0 ) ).dereference( context, Key.XMLText, false ) ).isEqualTo( "CommandBox" );
		assertThat( ( ( XML ) children.get( 1 ) ).dereference( context, Key.XMLText, false ) ).isEqualTo( "ContentBox" );
		assertThat( ( ( XML ) children.get( 2 ) ).dereference( context, Key.XMLText, false ) ).isEqualTo( "BoxLang" );
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
