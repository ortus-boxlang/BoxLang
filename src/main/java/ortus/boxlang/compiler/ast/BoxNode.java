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
package ortus.boxlang.compiler.ast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.compiler.ast.visitor.BoxVisitable;
import ortus.boxlang.compiler.ast.visitor.PrettyPrintBoxVisitor;

/**
 * Base class for the BoxLang AST Nodes
 */
public abstract class BoxNode implements BoxVisitable {

	protected Position		position;
	private String			sourceText;
	protected BoxNode		parent	= null;
	private List<BoxNode>	children;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement or expression in the source code
	 * @param sourceText source code of the statement/expression
	 */
	protected BoxNode( Position position, String sourceText ) {
		this.position	= position;
		this.sourceText	= sourceText;
		this.children	= new ArrayList<>();
	}

	/**
	 * Returns the position in code that the node represents
	 *
	 * @return a Position instance
	 *
	 * @see Position
	 */
	public Position getPosition() {
		return position;
	}

	/**
	 * Set the position of the node
	 *
	 * @param position the position within the source code that originated the node
	 */
	public void setPosition( Position position ) {
		this.position = position;
	}

	/**
	 * Returns the source code that originated the Node
	 *
	 * @return the snipped of the source code
	 */
	public String getSourceText() {
		return sourceText;
	}

	public void setSourceText( String sourceText ) {
		this.sourceText = sourceText;
	}

	/**
	 * Set the parent and the children of the Node
	 *
	 * @param parent an instance of the parent code
	 */
	public void setParent( BoxNode parent ) {
		this.parent = parent;
		if ( parent != null ) {
			if ( !parent.children.contains( this ) )
				parent.getChildren().add( this );
		}
	}

	/**
	 * Returns the parent Node of node or null if has no parent
	 *
	 * @return the parent Node of the current Node
	 */
	public BoxNode getParent() {
		return parent;
	}

	/**
	 * Returns the list of children of the current node
	 *
	 * @return a list of children Node
	 */
	public List<BoxNode> getChildren() {
		return children;
	}

	/**
	 * Swap a single child. oldChild can be null.
	 *
	 * @param oldChild The child to remove, if not null
	 * @param newChild The child to add
	 */
	public void replaceChildren( BoxNode oldChild, BoxNode newChild ) {
		if ( oldChild != null ) {
			children.remove( oldChild );
		}
		if ( newChild != null ) {
			children.add( newChild );
		}
	}

	/**
	 * Swap a list of children. oldChildren can be null.
	 *
	 * @param oldChildren The children to remove, if not null
	 * @param newChildren The children to add
	 */
	public void replaceChildren( List<? extends BoxNode> oldChildren, List<? extends BoxNode> newChildren ) {
		if ( oldChildren != null ) {
			children.removeAll( oldChildren );
		}
		if ( newChildren != null ) {
			children.addAll( newChildren );
		}
	}

	/**
	 * Walk the tree
	 *
	 * @return a list of nodes traversed
	 */
	public List<BoxNode> getDescendants() {
		List<BoxNode> result = new ArrayList<>();
		result.add( this );
		for ( BoxNode node : this.children ) {
			result.addAll( node.getDescendants() );
		}
		return result;
	}

	/**
	 * Walk the tree
	 *
	 * @return a list of nodes traversed
	 */
	public <T> List<T> getDescendantsOfType( Class<T> type ) {
		List<T> result = new ArrayList<>();
		if ( type.isAssignableFrom( this.getClass() ) ) {
			result.add( ( T ) this );
		}
		for ( BoxNode node : this.children ) {
			result.addAll( node.getDescendantsOfType( type ) );
		}
		return result;
	}

	/**
	 * Walk the ancestors of a node
	 *
	 * @return a list of ancestor nodes
	 */
	public List<BoxNode> getAncestors() {
		List<BoxNode>	result	= new ArrayList<>();
		BoxNode			node	= this.parent;
		while ( node != null ) {
			result.add( node );
			node = node.parent;
		}
		return result;
	}

	/**
	 * Walk the ancestors of a node to look for one of a specific type
	 * 
	 * @param type The class of ancestor to look for
	 *
	 * @return The requested ancestor node, null if none found
	 */
	public <T> T getFirstAncestorOfType( Class<T> type ) {
		return getFirstAncestorOfType( type, ( T ) -> true );
	}

	/**
	 * Walk the ancestors of a node to look for one of a specific type.
	 * This can return the current node, as opposed to getFirstAncestorOfType
	 * which starts with the parent
	 * 
	 * @param type The class of ancestor to look for
	 *
	 * @return The requested ancestor node, null if none found
	 */
	public <T> T getFirstNodeOfType( Class<T> type ) {
		return getFirstNodeOfType( type, ( T ) -> true );
	}

	/**
	 * Walk the ancestors of a node to look for one of a specific type
	 * 
	 * @param type      The class of ancestor to look for
	 * @param predicate A predicate to test the ancestor
	 *
	 * @return The requested ancestor node, null if none found
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T getFirstNodeOfType( Class<T> type, Predicate<T> predicate ) {
		if ( type.isAssignableFrom( this.getClass() ) && predicate.test( ( T ) this ) ) {
			return ( T ) this;
		}
		if ( this.parent != null ) {
			return this.parent.getFirstNodeOfType( type, predicate );
		}
		return null;
	}

	/**
	 * Walk the ancestors of a node to look for one of a specific type
	 * 
	 * @param type The classes of ancestors to look for
	 *
	 * @return The requested ancestor node, null if none found
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T getFirstNodeOfTypes( Class<? extends BoxNode>... type ) {
		for ( Class<? extends BoxNode> t : type ) {
			if ( t.isAssignableFrom( this.getClass() ) ) {
				return ( T ) this;
			}
		}
		if ( this.parent != null ) {
			return this.parent.getFirstNodeOfTypes( type );
		}
		return null;
	}

	/**
	 * Walk the ancestors of a node to look for one of a specific type
	 * 
	 * @param type      The class of ancestor to look for
	 * @param predicate A predicate to test the ancestor
	 *
	 * @return The requested ancestor node, null if none found
	 */
	public <T> T getFirstAncestorOfType( Class<T> type, Predicate<T> predicate ) {
		if ( this.parent != null ) {
			return this.parent.getFirstNodeOfType( type, predicate );
		}
		return null;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put( "ASTType", getClass().getSimpleName() );
		map.put( "ASTPackage", getClass().getPackageName() );
		map.put( "sourceText", sourceText );
		if ( position != null )
			map.put( "position", position.toMap() );

		return map;
	}

	public Map<String, Object> enumToMap( Enum<?> e ) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put( "ASTType", getClass().getSimpleName() );
		map.put( "ASTPackage", getClass().getPackageName() );
		map.put( "sourceText", e.name() );

		return map;
	}

	public String toJSON() {
		try {
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES ).asString( toMap() );
		} catch ( JSONObjectException e ) {
			e.printStackTrace();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		throw new RuntimeException( "Failed to convert to JSON" );
	}

	public String toString() {
		PrettyPrintBoxVisitor visitor = new PrettyPrintBoxVisitor();
		accept( visitor );
		return visitor.getOutput();
	}
}
