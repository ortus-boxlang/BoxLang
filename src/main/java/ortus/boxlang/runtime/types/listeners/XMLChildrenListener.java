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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.meta.IIndexedChangeListener;

/**
 * Listener class to synchronize changes between XMLChildren arrays and the native nodes
 */
public class XMLChildrenListener implements IIndexedChangeListener<Array> {

	private final XML parent;

	public XMLChildrenListener( XML parent ) {
		this.parent = parent;
	}

	/**
	 * Call when a value is being changed in an IListenable, with insert context
	 *
	 * @param key      The key of the value being changed. For arrays, the key will be 1-based
	 * @param newValue The new value (null if being removed)
	 * @param oldValue The old value (null if being added)
	 * @param object   The array being listened to
	 * @param isInsert Whether this is an insert (true) or a set/replace (false)
	 *
	 * @return The new value to be set (you can override)
	 */
	@Override
	public Object notify( Key key, Object newValue, Object oldValue, Array object, boolean isInsert ) {
		int		elementIndex	= IntegerCaster.cast( key.getName() ) - 1;
		Node	parentNode		= this.parent.getNode();
		// Resolve the actual DOM node at this element-only index
		Node	domNode			= getNthElementNode( parentNode, elementIndex );

		if ( oldValue == null && newValue == null ) {
			// Remove by index
			if ( domNode != null ) {
				parentNode.removeChild( domNode );
			}
		} else if ( newValue != null && oldValue == null ) {
			// Insert: no existing element at this index
			Node importNode = resolveImportNode( parentNode, newValue );
			if ( domNode == null ) {
				parentNode.appendChild( importNode );
			} else {
				parentNode.insertBefore( importNode, domNode );
			}
		} else if ( newValue == null && oldValue != null ) {
			// Remove by old value reference
			Node oldNode = oldValue instanceof Node ? ( Node ) oldValue : ( ( XML ) oldValue ).getNode();
			parentNode.removeChild( oldNode );
		} else if ( isInsert ) {
			// Insert before existing element at this index
			Node importNode = resolveImportNode( parentNode, newValue );
			if ( domNode == null ) {
				parentNode.appendChild( importNode );
			} else {
				parentNode.insertBefore( importNode, domNode );
			}
		} else {
			// Replace: both non-null, not an insert — replace the existing DOM node
			Node importNode = resolveImportNode( parentNode, newValue );
			if ( domNode != null ) {
				parentNode.replaceChild( importNode, domNode );
			} else {
				parentNode.appendChild( importNode );
			}
		}
		return null;
	}

	/**
	 * Get the nth ELEMENT_NODE child of a parent node (0-based).
	 * Returns null if the index is past the number of element children.
	 *
	 * @param parentNode   The parent DOM node
	 * @param elementIndex The 0-based index counting only ELEMENT_NODE children
	 *
	 * @return The DOM node at that element index, or null
	 */
	private Node getNthElementNode( Node parentNode, int elementIndex ) {
		NodeList	childNodes		= parentNode.getChildNodes();
		int			elementCount	= 0;
		for ( int i = 0; i < childNodes.getLength(); i++ ) {
			Node child = childNodes.item( i );
			if ( child.getNodeType() == Node.ELEMENT_NODE ) {
				if ( elementCount == elementIndex ) {
					return child;
				}
				elementCount++;
			}
		}
		return null;
	}

	/**
	 * Resolve a new value to a DOM Node, importing across documents if needed.
	 *
	 * @param parentNode The parent DOM node that will receive the import
	 * @param newValue   The new value (Node or XML)
	 *
	 * @return The resolved DOM node ready for insertion
	 */
	private Node resolveImportNode( Node parentNode, Object newValue ) {
		Node importNode = newValue instanceof Node ? ( Node ) newValue : ( ( XML ) newValue ).getNode();
		if ( parentNode instanceof Document docParent && importNode instanceof Document docImport ) {
			importNode = docParent.importNode( docImport.getDocumentElement(), true );
		}
		return importNode;
	}

}
