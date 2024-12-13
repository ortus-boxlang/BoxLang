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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.listeners.XMLChildrenListener;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;
import ortus.boxlang.runtime.types.util.ListUtil;

/**
 * This type represents an XML Object in BoxLang
 */
public class XML implements Serializable, IStruct {

	/**
	 * XML data as List of arrays
	 */
	private Node					node;

	/**
	 * Metadata object
	 */
	public BoxMeta					$bx;

	/**
	 * The type of struct ( private so that the interface method `getType` will be used )
	 */
	private final TYPES				type;

	/**
	 * Function service
	 */
	private static FunctionService	functionService		= BoxRuntime.getInstance().getFunctionService();

	/**
	 * Keys that are only valid for document nodes
	 */
	private static final Set<Key>	documentOnlyKeys	= Set.of( Key.XMLRoot, Key.XMLDocType );

	/**
	 * Keys that are only valid for element nodes
	 */
	private static final Set<Key>	elementOnlyKeys		= Set.of( Key.XMLText, Key.XMLCdata, Key.XMLAttributes, Key.XMLChildren, Key.XMLParent,
	    Key.XMLNodes, Key.XMLNsPrefix, Key.XMLNsURI );

	/**
	 * Serial version UID
	 */
	private static final long		serialVersionUID	= 1L;

	/**
	 * Create a new XML Document from the given string
	 */
	public XML( String xmlData ) {

		this.type = TYPES.DEFAULT;

		DocumentBuilderFactory	factory	= DocumentBuilderFactory.newInstance();
		DocumentBuilder			builder;
		try {
			builder = factory.newDocumentBuilder();
		} catch ( ParserConfigurationException e ) {
			throw new BoxRuntimeException( "Error creating XML document builder", e );
		}
		InputSource inputSource = new InputSource( new StringReader( xmlData ) );
		try {
			node = builder.parse( inputSource );
		} catch ( SAXException e ) {
			throw new BoxRuntimeException( "Error parsing XML document", e );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error parsing XML document", e );
		}
	}

	/**
	 * Create a new XML Document from the given string
	 */
	public XML( Node node ) {
		this.type	= TYPES.DEFAULT;
		this.node	= node;
	}

	/**
	 * Create a new XML Document from the given string
	 */
	public XML( Boolean caseSenstive ) {
		this.type = caseSenstive ? TYPES.CASE_SENSITIVE : TYPES.DEFAULT;
	}

	/**
	 * Get the text inside this XML node as a string
	 *
	 * @return the text inside this XML node as a string
	 */
	public String getXMLText() {
		StringBuilder	sb			= new StringBuilder();
		NodeList		children	= node.getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item( i );
			if ( child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE ) {
				sb.append( child.getNodeValue() );
			}
		}
		return sb.toString();
	}

	/**
	 * Get the element children of this XML node as an array of XML objects
	 *
	 * @return the element children
	 */
	public List<XML> getXMLChildrenAsList() {
		List<XML> children = new ArrayList<XML>();
		if ( node != null ) {
			NodeList childNodes = node.getChildNodes();
			for ( int i = 0; i < childNodes.getLength(); i++ ) {
				Node child = childNodes.item( i );
				if ( child.getNodeType() == Node.ELEMENT_NODE ) {
					children.add( new XML( child ) );
				}
			}
		}
		return children;
	}

	/**
	 * Get the element children of this XML node as an array of XML objects
	 *
	 * @return the element children
	 */
	public Array getXMLChildren() {
		XMLChildrenListener	childrenListener	= new XMLChildrenListener( this );
		Array				childArray			= new Array( getXMLChildrenAsList() );
		childArray.registerChangeListener( childrenListener );
		return childArray;
	}

	/**
	 * Get all child nodes except attribute nodes
	 *
	 * @return an array of XML objects representing the child nodes
	 */
	public Array getXMLNodes() {
		Array		children	= new Array();
		NodeList	childNodes	= node.getChildNodes();
		for ( int i = 0; i < childNodes.getLength(); i++ ) {
			Node child = childNodes.item( i );
			if ( child.getNodeType() != Node.ATTRIBUTE_NODE ) {
				children.add( new XML( child ) );
			}
		}
		return children;
	}

	/**
	 * Get the comments inside this XML node as a string
	 *
	 * @return the comments inside this XML node as a string
	 */
	public String getNodeComments() {
		StringBuilder	comments	= new StringBuilder();
		NodeList		children	= node.getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item( i );
			if ( child.getNodeType() == Node.COMMENT_NODE ) {
				comments.append( child.getNodeValue() );
			}
		}
		return comments.toString();
	}

	/**
	 * Get the attributes of this XML node as a struct
	 *
	 * @return the attributes of this XML node as a struct
	 */
	public IStruct getXMLAttributes() {
		// TODO: attach change listener to the struct so changes in the struct will be reflected in the XML
		IStruct			attributes	= new Struct( IStruct.TYPES.LINKED );
		NamedNodeMap	attrs		= node.getAttributes();
		for ( int i = 0; i < attrs.getLength(); i++ ) {
			Node attr = attrs.item( i );
			attributes.put( Key.of( attr.getNodeName() ), attr.getNodeValue() );
		}
		return attributes;
	}

	/**
	 * Get the name of this XML node
	 *
	 * @return the name of this XML node
	 */
	public String getXMLName() {
		switch ( node.getNodeType() ) {
			case Node.DOCUMENT_NODE :
				return "#document";
			case Node.DOCUMENT_FRAGMENT_NODE :
				return "#document-fragment";
			case Node.ELEMENT_NODE :
				return node.getNodeName();
			case Node.TEXT_NODE :
				return "#text";
			case Node.CDATA_SECTION_NODE :
				return "#cdata-section";
			case Node.COMMENT_NODE :
				return "#comment";
			case Node.DOCUMENT_TYPE_NODE :
				return ( ( DocumentType ) node ).getName();
			case Node.ENTITY_REFERENCE_NODE :
				return ( ( EntityReference ) node ).getNodeName();
			case Node.PROCESSING_INSTRUCTION_NODE :
				return ( ( ProcessingInstruction ) node ).getData();
			case Node.ENTITY_NODE :
				return ( ( Entity ) node ).getNodeName();
			case Node.NOTATION_NODE :
				return ( ( Notation ) node ).getNodeName();
			default :
				return "";
		}
	}

	/**
	 * Get the value of this XML node
	 *
	 * @return the value of this XML node
	 */
	public String getXMLValue() {
		switch ( node.getNodeType() ) {
			case Node.DOCUMENT_NODE :
			case Node.DOCUMENT_FRAGMENT_NODE :
			case Node.ELEMENT_NODE :
			case Node.DOCUMENT_TYPE_NODE :
			case Node.ENTITY_REFERENCE_NODE :
			case Node.PROCESSING_INSTRUCTION_NODE :
			case Node.ENTITY_NODE :
			case Node.NOTATION_NODE :
				return "";
			case Node.TEXT_NODE :
			case Node.CDATA_SECTION_NODE :
			case Node.COMMENT_NODE :
				return node.getTextContent();
			default :
				return "";
		}
	}

	/**
	 * Get the type of this XML node as text
	 *
	 * @return the type of this XML node as text
	 */
	public String getXMLType() {
		switch ( node.getNodeType() ) {
			case Node.ELEMENT_NODE :
				return "ELEMENT";
			case Node.ATTRIBUTE_NODE :
				return "ATTRIBUTE";
			case Node.TEXT_NODE :
				return "TEXT";
			case Node.CDATA_SECTION_NODE :
				return "CDATA SECTION";
			case Node.ENTITY_REFERENCE_NODE :
				return "ENTITY REFERENCE";
			case Node.ENTITY_NODE :
				return "ENTITY";
			case Node.PROCESSING_INSTRUCTION_NODE :
				return "PROCESSING INSTRUCTION";
			case Node.COMMENT_NODE :
				return "COMMENT";
			case Node.DOCUMENT_NODE :
				return "DOCUMENT";
			case Node.DOCUMENT_TYPE_NODE :
				return "DOCUMENT TYPE";
			case Node.DOCUMENT_FRAGMENT_NODE :
				return "DOCUMENT FRAGMENT";
			case Node.NOTATION_NODE :
				return "NOTATION";
			default :
				return "UNKNOWN";
		}
	}

	public Set<Key> getReferencableKeys() {
		return getReferencableKeys( true );
	}

	public Set<Key> getReferencableKeys( boolean withChildren ) {
		Set<Key> keys = new HashSet<>();
		switch ( node.getNodeType() ) {
			case Node.DOCUMENT_NODE :
				keys.add( Key.XMLRoot );
				keys.add( Key.XMLComment );
				return keys;
			case Node.ELEMENT_NODE :
				keys.add( Key.XMLText );
				keys.add( Key.XMLChildren );
				keys.add( Key.XMLAttributes );
				keys.add( Key.XMLNsPrefix );
				keys.add( Key.XMLNsURI );
				keys.add( Key.XMLComment );
				if ( withChildren ) {
					keys.addAll( getXMLChildrenAsList().stream().map( XML::getXMLName ).map( Key::of ).collect( Collectors.toSet() ) );
				}
				return keys;
			case Node.ATTRIBUTE_NODE :
				return getXMLAttributes().keySet();
			case Node.TEXT_NODE :
				return keys;
			case Node.CDATA_SECTION_NODE :
				return keys;
			case Node.ENTITY_REFERENCE_NODE :
				return keys;
			case Node.ENTITY_NODE :
				return keys;
			case Node.PROCESSING_INSTRUCTION_NODE :
				return keys;
			case Node.COMMENT_NODE :
				return keys;
			case Node.DOCUMENT_TYPE_NODE :
				return keys;
			case Node.DOCUMENT_FRAGMENT_NODE :
				return keys;
			case Node.NOTATION_NODE :
				return keys;
			default :
				return keys;
		}
	}

	public Object getDumpRepresentation() {
		IStruct dump = new Struct();
		getReferencableKeys( false ).stream().forEach( key -> dump.put( key, dereference( null, key, true ) ) );
		return dump;
	}

	/***************************
	 * IReferencable implementation
	 ****************************/

	@Override
	public Object dereference( IBoxContext context, Key name, Boolean safe ) {
		// Special check for $bx
		if ( name.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		// All node types support the following properties
		if ( name.equals( Key.XMLName ) ) {
			return getXMLName();
		} else if ( name.equals( Key.XMLType ) ) {
			return getXMLType();
		} else if ( name.equals( Key.XMLValue ) ) {
			return getXMLValue();
		}

		// Document nodes support the following:
		if ( node.getNodeType() == Node.DOCUMENT_NODE ) {
			Document document = ( Document ) node;
			if ( name.equals( Key.XMLRoot ) ) {
				return new XML( document.getDocumentElement() );
			} else if ( name.equals( Key.XMLComment ) ) {
				return getNodeComments();
			} else if ( name.equals( Key.XMLDocType ) ) {
				return new XML( document.getDoctype() );
			}
		} else if ( documentOnlyKeys.contains( name ) ) {
			throw new KeyNotFoundException(
			    "Key [" + name.getName() + "] can only be use with an Document node, but you have a [" + getXMLType() + "] node here." );
		}

		// Element nodes support the following:
		if ( node.getNodeType() == Node.ELEMENT_NODE ) {
			if ( name.equals( Key.XMLText ) || name.equals( Key.XMLCdata ) ) {
				return getXMLText();
			} else if ( name.equals( Key.XMLChildren ) ) {
				return getXMLChildren();
			} else if ( name.equals( Key.XMLParent ) ) {
				return new XML( node.getParentNode() );
			} else if ( name.equals( Key.XMLNodes ) ) {
				return getXMLNodes();
			} else if ( name.equals( Key.XMLAttributes ) ) {
				return getXMLAttributes();
			} else if ( name.equals( Key.XMLNsPrefix ) ) {
				String result = node.getPrefix();
				return result == null ? "" : result;
			} else if ( name.equals( Key.XMLNsURI ) ) {
				String result = node.getNamespaceURI();
				return result == null ? "" : result;
			} else if ( name.equals( Key.XMLComment ) ) {
				return getNodeComments();
			}
		} else if ( elementOnlyKeys.contains( name ) ) {
			throw new KeyNotFoundException(
			    "Key [" + name.getName() + "] can only be use with an Element node, but you have a [" + getXMLType() + "] node here." );
		}

		if ( node.getNodeType() == Node.ELEMENT_NODE ) {
			// Check if the key is numeric,
			int index = getIntFromKey( name );
			// If dereferncing a node with a number like xml.users[1], then we ALWAYS get the value from that row
			if ( index > 0 ) {
				return getSiblingAtPosition( index - 1 );
			}
		}

		// Fall back is to check for a child element of this name and return the first one.
		XML child = getFirstChildOfName( name.getName() );
		if ( child != null ) {
			return child;
		}
		if ( safe ) {
			return null;
		}
		throw new KeyNotFoundException( "Key [" + name.getName() + "] not found in XML node." );
	}

	/**
	 * Get the first child of this XML node with the given name
	 *
	 * @param childName The name of the child to get. Case insensitive.
	 *
	 * @return The first child of this XML node with the given name, or null if no such child exists.
	 */
	public XML getFirstChildOfName( String childName ) {
		NodeList children = node.getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item( i );
			if ( child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase( childName ) ) {
				return new XML( child );
			}
		}
		return null;
	}

	/**
	 * Get the sibling of this XML node having the same name at the given index
	 *
	 * @param index The index of the sibling to get. 0-based.
	 *
	 * @return The sibling of this XML node having the same name at the given index
	 */
	public XML getSiblingAtPosition( int index ) {
		String		ourName		= node.getNodeName();
		int			currMatch	= -1;
		// Get sibling nodes (including ourself)
		NodeList	children	= node.getParentNode().getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item( i );
			if ( child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase( ourName ) ) {
				currMatch++;
				if ( currMatch == index ) {
					return new XML( child );
				}
			}
		}
		throw new KeyNotFoundException(
		    "Index [" + index + "] out of bounds for child [" + ourName + "] XML nodes.  There were only " + currMatch + " children." );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {

		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.XML );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, positionalArguments );
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, positionalArguments );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.XML );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, namedArguments );
	}

	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		put( name, value );
		return this;
	}

	public String asString( IStruct transformerOptions ) {
		try {
			// Create a Transformer
			TransformerFactory	transformerFactory	= TransformerFactory.newInstance();
			Transformer			transformer			= transformerFactory.newTransformer();

			// Set properties for the transformation
			for ( var entry : transformerOptions.entrySet() ) {
				transformer.setOutputProperty( entry.getKey().getName(), entry.getValue().toString() );
			}

			// Transform the Node into a StringWriter
			StringWriter writer = new StringWriter();
			transformer.transform( new DOMSource( node ), new StreamResult( writer ) );

			// Get the XML string from the StringWriter
			return writer.toString();
		} catch ( TransformerException e ) {
			throw new BoxRuntimeException( "Error converting XML node to string", e );
		}

	}

	/***************************
	 * IType implementation
	 ****************************/

	@Override
	public String asString() {
		return asString( Struct.of(
		    "omit-xml-declaration", "no",
		    "method", "xml",
		    "indent", "yes"
		) );
	}

	@Override
	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			// TODO: Create XML metadata class
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;
	}

	public Node getNode() {
		return node;
	}

	public static int getIntFromKey( Key key ) {
		Integer index;

		// If key is int, use it directly
		if ( key instanceof IntKey intKey ) {
			index = intKey.getIntValue();
		} else {
			// If key is not an int, we must attempt to cast it
			CastAttempt<Number> indexAtt = NumberCaster.attempt( key.getName() );
			if ( !indexAtt.wasSuccessful() ) {
				return -1;
			}
			Number dIndex = indexAtt.get();
			index = dIndex.intValue();
			// Dissallow non-integer indexes foo[1.5]
			if ( index.doubleValue() != dIndex.doubleValue() ) {
				return -1;
			}
		}
		return index;
	}

	// Implement IStruct methods
	@Override
	public Object get( String key ) {
		return getRaw( KeyCaster.cast( key ) );
	}

	@Override
	public Object get( Object key ) {
		return getRaw( KeyCaster.cast( key ) );
	}

	@Override
	public Object getRaw( Key key ) {
		return dereference( null, key, true );
	}

	@Override
	public Object remove( String key ) {
		return remove( KeyCaster.cast( key ) );
	}

	@Override
	public int size() {
		return getXMLChildrenAsList().size();
	}

	@Override
	public boolean isEmpty() {
		return getXMLChildrenAsList().isEmpty();
	}

	@Override
	public boolean containsKey( Object key ) {
		return containsKey( KeyCaster.cast( key ) );
	}

	@Override
	public boolean containsValue( Object value ) {
		return getXMLChildrenAsList().contains( value );
	}

	@Override
	public Object put( Key key, Object value ) {
		if ( ! ( value instanceof Node ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The value passed [%s] is not a Node instance", value.toString() )
			);
		}
		value = ( ( Node ) value ).cloneNode( true );
		if ( documentOnlyKeys.contains( key ) ) {
			if ( key.equals( Key.XMLRoot ) ) {
				this.node = ( Node ) value;
			} else {
				throw new BoxRuntimeException( String.format( "XML documents do not yet implement a setter for the key [%s]", key.getName() ) );
			}
		} else {
			Array keyArray = ListUtil.asList( key.getName(), "." );
			keyArray.remove( keyArray.size() - 1 );
			if ( keyArray.size() == 0 ) {
				this.node.appendChild( ( Node ) value );
			} else {
				String parent = ListUtil.asString( keyArray, "." );
				if ( Key.of( parent ).equals( Key.XMLRoot ) ) {
					this.node.appendChild( ( Node ) value );
				} else {
					( ( Node ) getFirstChildOfName( parent ) ).appendChild( ( Node ) value );
				}
			}
		}
		return this;
	}

	@Override
	public Object remove( Object key ) {
		return remove( KeyCaster.cast( key ) );
	}

	@Override
	public void putAll( Map<? extends Key, ? extends Object> m ) {
		throw new UnsupportedOperationException( "XML modification not implemented yet" );
	}

	@Override
	public void clear() {
		this.node = null;
	}

	@Override
	public Set<Key> keySet() {
		return getReferencableKeys( true );
	}

	@Override
	public Collection<Object> values() {
		return getXMLChildrenAsList().stream().collect( Collectors.toList() );
	}

	@Override
	public Set<Entry<Key, Object>> entrySet() {
		// The (Object) cast is necessary to avoid the compiler error:
		return getXMLChildrenAsList().stream().map( xml -> Map.entry( Key.of( xml.getXMLName() ), ( Object ) xml ) ).collect( Collectors.toSet() );
	}

	@Override
	public Boolean isSoftReferenced() {
		return false;
	}

	@Override
	public List<String> getKeysAsStrings() {
		return getXMLChildrenAsList().stream().map( XML::getXMLName ).collect( Collectors.toList() );
	}

	@Override
	public void addAll( Map<? extends Object, ? extends Object> m ) {
		throw new UnsupportedOperationException( "XML modification not implemented yet" );
	}

	@Override
	public Object getOrDefault( String key, Object defaultValue ) {
		return getOrDefault( KeyCaster.cast( key ), defaultValue );
	}

	@Override
	public Object getOrDefault( Key key, Object defaultValue ) {
		Object res = get( key );
		if ( res == null ) {
			return defaultValue;
		}
		return res;
	}

	@Override
	public boolean containsKey( String key ) {
		return containsKey( KeyCaster.cast( key ) );
	}

	@Override
	public boolean containsKey( Key key ) {
		Set<Key> keys = keySet();
		return keys.contains( key );
	}

	@Override
	public Object put( String key, Object value ) {
		throw new UnsupportedOperationException( "XML modification not implemented yet" );
	}

	@Override
	public String toString() {
		return asString();
	}

	@Override
	public IStruct.TYPES getType() {
		return IStruct.TYPES.DEFAULT;
	}

	@Override
	public Boolean isCaseSensitive() {
		return type.equals( TYPES.CASE_SENSITIVE );
	}

	@Override
	public Map<Key, Object> getWrapped() {
		throw new UnsupportedOperationException(
		    "XML nodes don't support getting the wrapped Map.  Even though they behave like a struct, they really aren't one." );
	}

	@Override
	public Object putIfAbsent( String key, Object value ) {
		throw new UnsupportedOperationException( "XML modification not implemented yet" );
	}

	@Override
	public Object putIfAbsent( Key key, Object value ) {
		throw new UnsupportedOperationException( "XML modification not implemented yet" );
	}

	@Override
	public List<Key> getKeys() {
		return keySet().stream().collect( Collectors.toList() );
	}

	@Override
	public Object remove( Key key ) {
		Node		parentNode	= ( ( Node ) getFirstChildOfName( key.getName() ) ).getParentNode();
		NodeList	childNodes	= parentNode.getChildNodes();
		IntStream.range( 1, parentNode.getChildNodes().getLength() )
		    .mapToObj( idx -> ( Node ) childNodes.item( idx - 1 ) )
		    .filter(
		        node -> isCaseSensitive() ? node.getNodeName() == key.getName() : key.equals( Key.of( key.getName() ) )
		    ).forEach( node -> parentNode.removeChild( node ) );
		return this;
	}

}
