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

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;

/**
 * Represent a Node in the AST
 */
public class Node {

	protected Position			position;
	private String				sourceText;
	protected Node				parent	= null;
	private final List<Node>	children;
	private Node				originator;

	/**
	 * AST node constructor
	 *
	 * @param position   the position within the source code that originated the node
	 * @param sourceText the original source code that represented by the node
	 */
	public Node( Position position, String sourceText ) {
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
	public void setParent( Node parent ) {
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
	public Node getParent() {
		return parent;
	}

	/**
	 * Returns the list ov children of the current node
	 *
	 * @return a list of children Node
	 */
	public List<Node> getChildren() {
		return children;
	}

	public Node getOriginator() {
		return originator;
	}

	/**
	 * Walk the tree
	 *
	 * @return a list of nodes traversed
	 */
	public List<Node> walk() {
		List<Node> result = new ArrayList<>();
		result.add( this );
		for ( Node node : this.children ) {
			result.addAll( node.walk() );
		}
		return result;
	}

	/**
	 * Walk the ancestors of a node
	 *
	 * @return a list of ancestor nodes
	 */
	public List<Node> walkAncestors() {
		List<Node>	result	= new ArrayList<>();
		Node		node	= this.parent;
		while ( node != null ) {
			result.add( node );
			node = node.parent;
		}
		return result;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();

		map.put( "ASTType", getClass().getSimpleName() );
		map.put( "ASTPackage", getClass().getPackageName() );
		map.put( "sourceText", sourceText );
		if ( position != null )
			map.put( "position", position.toMap() );

		// I'm not sure if children is used at all right now
		// map.put( "children", children.stream().map( Node::toMap ).toList() );

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
}
