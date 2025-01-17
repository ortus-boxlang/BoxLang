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
package ortus.boxlang.runtime.types.util;

import javax.xml.namespace.NamespaceContext;
import javax.xml.XMLConstants;
import org.w3c.dom.Document;
import java.util.Iterator;
import java.util.ArrayList;

public class XMLNamespaceResolver implements NamespaceContext {

	private Document searchDocument;

	/**
	 * Constructor
	 * 
	 * @param document source document
	 */
	public XMLNamespaceResolver( Document document ) {
		searchDocument = document;
	}

	/**
	 * Retrieves the namespace uri for a given prefix in the document
	 * 
	 * @param prefix to search for
	 * 
	 * @return uri
	 */
	@Override
	public String getNamespaceURI( String prefix ) {
		return searchDocument.lookupNamespaceURI( prefix.equals( XMLConstants.DEFAULT_NS_PREFIX ) ? null : prefix );
	}

	/**
	 * Retrieves the namespace prefix from the URL
	 * 
	 * @param uri the URI to search for
	 */
	@Override
	public String getPrefix( String uri ) {
		return searchDocument.lookupPrefix( uri );
	}

	/**
	 * Retrieves all prefixes for a given namespace URI
	 * 
	 * @param uri the URI to search for
	 */
	@Override
	public Iterator<String> getPrefixes( String uri ) {
		ArrayList<String> prefixes = new ArrayList<String>();
		prefixes.add( searchDocument.lookupPrefix( uri ) );
		return prefixes.iterator();
	}

}