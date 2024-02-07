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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.XML;

@BoxBIF
@BoxMember( type = BoxLangType.XML )
public class XMLGetNodeType extends BIF {

	/**
	 * Constructor
	 */
	public XMLGetNodeType() {
		super();
		this.declaredArguments = new Argument[] {
		    new Argument( true, "XML", Key.XMLNode )
		};
	}

	/**
	 * Get XML values according to given xPath query
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument XMLNode An XML DOM object node.
	 * 
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		XML xml = arguments.getAsXML( Key.XMLNode );
		return xml.getXMLType().replace( " ", "_" ) + "_NODE";
	}

}
