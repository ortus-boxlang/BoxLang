
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

import org.apache.commons.text.StringEscapeUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Format special characters for XML output" )
@BoxMember( type = BoxLangType.STRING_STRICT )

public class XMLFormat extends BIF {

	/**
	 * Constructor
	 */
	public XMLFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( false, "boolean", Key.escapeChars, false )
		};
	}

	/**
	 * Escapes a string so that it can be used as text in XML, replacing the characters &lt; &gt; &amp; " and ' with entities. Also available as
	 * the string member function {@code xmlFormat()}.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to format
	 *
	 * @argument.escapeChars Defaults to false, which applies XML 1.1 escaping where control characters are encoded as numeric entities.
	 *                       When true, XML 1.0 escaping is applied instead, which removes characters that are not allowed in XML 1.0 documents.
	 *                       For details, see http://www.w3.org/TR/2006/REC-xml11-20060816/#NT-RestrictedChar.
	 *
	 * @return The escaped string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return arguments.getAsBoolean( Key.escapeChars )
		    ? StringEscapeUtils.escapeXml10( arguments.getAsString( Key.string ) )
		    : StringEscapeUtils.escapeXml11( arguments.getAsString( Key.string ) );
	}

}
