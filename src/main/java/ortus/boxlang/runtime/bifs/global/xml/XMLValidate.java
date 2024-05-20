
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.XMLValidationHandler;

@BoxBIF

public class XMLValidate extends BIF {

	/**
	 * Constructor
	 */
	public XMLValidate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.XML ),
		    new Argument( false, "string", Key.validator )
		};
	}

	/**
	 * Uses a Document Type Definition (DTD) or XML Schema to validate an XML text document or an XML document object.
	 * Returns keys status (boolean), errors (array), fatalerrors (array) and warnings (array)
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.xml The XML text document or XML document object to validate.
	 *
	 * @argument.validator The DTD or XML Schema to use for validation. If not provided, the DTD declaration within the XML document is used.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object					xmlObject		= arguments.get( Key.XML );
		String					validator		= arguments.getAsString( Key.validator );
		String					xmlString		= null;
		Struct					response		= new Struct( new HashMap<Key, Object>() {

													{
														put( Key.errors, new Array() );
														put( Key.fatalErrors, new Array() );
														put( Key.status, true );
														put( Key.warning, new Array() );
													}
												} );
		XMLValidationHandler	errorHandler	= new XMLValidationHandler( response );
		if ( xmlObject instanceof XML ) {
			xmlString = xmlObject.toString();
		} else if ( StringUtils.equals( StringCaster.cast( xmlObject ).substring( 0, 3 ), "http" ) ) {
			xmlString = StringCaster.cast( FileSystemUtil.read( StringCaster.cast( xmlObject ) ) );
		} else {
			xmlString = StringCaster.cast( xmlObject );
		}

		ByteArrayInputStream	xmlInputStream	= new ByteArrayInputStream( xmlString.getBytes() );
		StreamSource			xmlStreamSource	= new StreamSource( xmlInputStream );
		Schema					schema			= null;

		try {
			if ( validator == null ) {
				SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.XML_DTD_NS_URI );
				schema = factory.newSchema();
			} else {
				SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
				if ( StringUtils.equals( validator.substring( 0, 3 ), "http" ) ) {
					try {
						schema = factory.newSchema( new URI( validator ).toURL() );
					} catch ( URISyntaxException u ) {
						throw new BoxRuntimeException(
						    String.format( "The valdator argument, [%s], is not a valid URI", validator ),
						    u
						);
					}
				} else {
					schema = factory.newSchema( new SAXSource( new InputSource( new ByteArrayInputStream( validator.getBytes() ) ) ) );
				}
			}
			Validator schemaValidator = schema.newValidator();
			schemaValidator.setErrorHandler( errorHandler );
			schemaValidator.validate( xmlStreamSource );

		} catch ( IOException | SAXException e ) {
			response.put( Key.status, false );
			ArrayCaster.cast( response.get( Key.fatalErrors ) ).push( e.getMessage() );
		}

		return response;
	}

}
