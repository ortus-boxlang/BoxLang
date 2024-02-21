/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.xml;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;

import org.w3c.dom.NodeList;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.XML )
public class XMLSearch extends BIF {

	/**
	 * Constructor
	 */
	public XMLSearch() {
		super();
		this.declaredArguments = new Argument[] {
		    new Argument( true, "XML", Key.XMLNode ),
		    new Argument( true, "String", Key.xpath ),
		    new Argument( false, "Struct", Key.params, Struct.EMPTY )
		};
	}

	/**
	 * Get XML values according to given xPath query
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument XMLNode The XML node to search
	 * @argument xpath The xpath query to search for
	 * @argument params The parameters to pass to the xpath query
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		XML				xml			= arguments.getAsXML( Key.XMLNode );
		String			xpathString	= arguments.getAsString( Key.xpath );
		final IStruct	params		= arguments.getAsStruct( Key.params );
		try {

			// Create an XPathFactory
			XPathFactory	xPathFactory	= XPathFactory.newInstance();

			// Create an XPath object
			XPath			xpath			= xPathFactory.newXPath();

			xpath.setXPathVariableResolver( new XPathVariableResolver() {

				public Object resolveVariable( QName variableName ) {
					return params.get( Key.of( variableName.getLocalPart() ) );
				}
			} );

			// TODO: cache compiled expressions
			XPathExpression	expression	= xpath.compile( xpathString );

			// Evaluate the XPath expression on the Document
			Object			result		= expression.evaluate( xml.getNode(), XPathConstants.NODESET );
			Array			results		= new Array();
			// Process the result
			if ( result instanceof NodeList nodeList ) {
				for ( int i = 0; i < nodeList.getLength(); i++ ) {
					results.append( new XML( nodeList.item( i ) ) );
				}
			}
			return results;

		} catch (

		XPathExpressionException e ) {
			throw new BoxRuntimeException( "Invalid XPath: " + xpathString, e );
		}
	}

}
