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
package ortus.boxlang.runtime.types.listeners;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.meta.IChangeListener;

/**
 * Listener class to synchronize changes between XMLChildren arrays and the native nodes
 */
public class XMLChildrenListener implements IChangeListener {

	private final XML parent;

	public XMLChildrenListener( XML parent ) {
		this.parent = parent;
	}

	/**
	 * Call when a value is being changed in an IListenable
	 *
	 * @param key      The key of the value being changed. For arrays, the key will be 1-based
	 * @param newValue The new value (null if being removed)
	 * @param oldValue The old value (null if being added)
	 *
	 * @return The new value to be set (you can override)
	 */
	public Object notify( Key key, Object newValue, Object oldValue ) {
		Integer		index			= IntegerCaster.cast( key.getName() ) - 1;
		Node		parentNode		= parent.getNode();
		NodeList	childNodeList	= parent.getNode().getChildNodes();
		if ( oldValue == null && newValue == null ) {
			parentNode.removeChild( childNodeList.item( index ) );
		} else if ( newValue != null && oldValue == null ) {
			if ( childNodeList.item( index ) == null ) {
				parentNode.appendChild( newValue instanceof Node ? ( Node ) newValue : ( ( XML ) newValue ).getNode() );
			} else {
				parentNode.insertBefore( newValue instanceof Node ? ( Node ) newValue : ( ( XML ) newValue ).getNode(), childNodeList.item( index ) );
			}
		} else if ( newValue == null && oldValue != null ) {
			parentNode.removeChild( oldValue instanceof Node ? ( Node ) oldValue : ( ( XML ) oldValue ).getNode() );
		}
		return null;
	}

}
