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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
@BoxMember( type = BoxLangType.XML )
public class XMLTransform extends BIF {

	/**
	 * Constructor
	 */
	public XMLTransform() {
		super();
		this.declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.XML ),
		    new Argument( true, "String", Key.XSL ),
		    new Argument( false, "Struct", Key.parameters, Struct.EMPTY )
		};
	}

	/**
	 * Get XML values according to given xPath query
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument XML The XML to transform
	 * @argument XSL The XSL to use for the transformation
	 * @argument parameters The parameters to pass to the xsl transformation
	 *
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	xmlAny	= arguments.get( Key.XML );
		XML		xml;
		if ( xmlAny instanceof XML xmlCast ) {
			xml = xmlCast;
		} else {
			xml = new XML( StringCaster.cast( xmlAny ) );
		}
		String xsl = arguments.getAsString( Key.XSL );
		// Is not XML. Must be file or URL
		if ( !xsl.trim().startsWith( "<" ) ) {
			xsl = StringCaster.cast( FileSystemUtil.read( xsl ) );
		}
		IStruct parameters = arguments.getAsStruct( Key.parameters );
		try {
			Document			document			= ( Document ) xml.getNode();

			// Parse the XSLT stylesheet
			Source				xslt				= new StreamSource( new StringReader( xsl ) );

			TransformerFactory	transformerFactory	= TransformerFactory.newInstance();

			// loop over parameters and set into transformer
			Transformer			transformer			= transformerFactory.newTransformer( xslt );
			for ( Key key : parameters.keySet() ) {
				transformer.setParameter( key.getName(), parameters.get( key ) );
			}

			// Create a new DOMSource for the input document
			DOMSource		source	= new DOMSource( document );

			// Create a new StreamResult for the output
			StringWriter	writer	= new StringWriter();
			StreamResult	result	= new StreamResult( writer );

			// Perform the transformation
			transformer.transform( source, result );

			// Get the transformed XML as a string
			return writer.toString();
		} catch ( TransformerException e ) {
			throw new BoxRuntimeException( "Error transforming XML", e );
		}
	}

}
