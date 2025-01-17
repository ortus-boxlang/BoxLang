
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

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class XMLElemNew extends BIF {

	/**
	 * Constructor
	 */
	public XMLElemNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "xml", Key.XML ),
		    new Argument( true, "string", Key.childname ),
		    new Argument( false, "string", Key.namespace )
		};
	}

	/**
	 * Creates a new XML Element which can be appended to an XML document
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.xml The parent XML object to associate the new node to
	 * 
	 * @argument.childName The XML name of the new child node
	 * 
	 * @argument.namespace The XML namespace to attach to the new child node
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		XML		xmlObject		= arguments.getAsXML( Key.XML );
		String	childName		= arguments.getAsString( Key.childname );
		String	namespace		= arguments.getAsString( Key.namespace );

		Node	documentNode	= xmlObject.getNode();

		if ( documentNode == null ) {
			String xmlString = null;
			try {
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

				if ( namespace != null ) {
					xmlString = "<" + childName + " xmlns=\"" + namespace + "\"/>";
				} else {
					xmlString = "<" + childName + "/>";
				}

				return new XML( builder.parse( new InputSource( new StringReader( xmlString ) ) ) );

			} catch ( ParserConfigurationException e ) {
				throw new BoxRuntimeException( "Error creating XML parser", e );
			} catch ( SAXException e ) {
				throw new BoxRuntimeException( "Error parsing XML elemement" + xmlString, e );
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Error parsing XML element" + xmlString, e );
			}
		} else if ( namespace != null ) {
			Document ownerDocument = documentNode.getOwnerDocument() == null ? ( Document ) documentNode : documentNode.getOwnerDocument();
			return new XML( ownerDocument.createElementNS( namespace, childName ) );
		} else {
			Document ownerDocument = documentNode.getOwnerDocument() == null ? ( Document ) documentNode : documentNode.getOwnerDocument();
			return new XML( ownerDocument.createElement( childName ) );
		}

	}

}
