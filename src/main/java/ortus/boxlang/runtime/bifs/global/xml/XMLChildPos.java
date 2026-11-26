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

import java.util.List;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.XML;

@BoxBIF( description = "Get the position of a child element in XML" )
@BoxMember( type = BoxLangType.XML )
public class XMLChildPos extends BIF {

	/**
	 * Constructor
	 */
	public XMLChildPos() {
		super();
		this.declaredArguments = new Argument[] {
		    new Argument( true, "XML", Key.elem ),
		    new Argument( true, "string", Key.childname ),
		    new Argument( true, "integer", Key.n )
		};
	}

	/**
	 * Gets the position of a child element within an XML element. The position is the index, in the {@code xmlChildren} array of the element,
	 * of the Nth child that has the specified name (compared case-insensitively). Also available as the member function {@code childPos()}
	 * on XML objects.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.elem The XML element whose children are searched.
	 *
	 * @argument.childname The name of the child elements to look for.
	 *
	 * @argument.n Which of the matching children to find, 1-based. For example 2 finds the second child with that name.
	 *
	 * @return The 1-based position of the matching child among all children, or -1 if there is no such child.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		XML			xml			= arguments.getAsXML( Key.elem );
		List<XML>	children	= xml.getXMLChildrenAsList();
		String		childname	= arguments.getAsString( Key.childname );
		int			n			= arguments.getAsInteger( Key.n );

		// loop over list and find the nth child with the specified name
		int			count		= 0;
		for ( int i = 0; i < children.size(); i++ ) {
			if ( children.get( i ).getXMLName().equalsIgnoreCase( childname ) ) {
				count++;
				if ( count == n ) {
					return i + 1;
				}
			}
		}
		return -1;
	}

}
