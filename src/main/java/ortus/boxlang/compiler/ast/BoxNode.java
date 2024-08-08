/**
 * [BoxLang]
 * <p>
 * Copyright [2023] [Ortus Solutions, Corp]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.ast;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import ortus.boxlang.compiler.ast.comment.BoxComment;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.visitor.BoxVisitable;
import ortus.boxlang.compiler.ast.visitor.PrettyPrintBoxVisitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Base class for the BoxLang AST Nodes
 */
public abstract class BoxNode implements BoxVisitable {

	protected Position			position;
	private String				sourceText;
	protected BoxNode			parent	= null;
	private List<BoxNode>		children;
	private List<BoxComment>	comments;

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
		this.comments	= new ArrayList<>();
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
				parent.getChildren()
				    .add( this );
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
	 * Returns the list of comments of the current node
	 *
	 * @return a list of comments Node
	 */
	public List<BoxComment> getComments() {
		return comments;
	}

	/**
	 * Get the last documentation comment
	 *
	 * @return the last documentation comment
	 */
	public BoxDocComment getDocComment() {
		for ( int i = comments.size() - 1; i >= 0; i-- ) {
			BoxComment comment = comments.get( i );
			if ( comment instanceof BoxDocComment bc ) {
				return bc;
			}
		}
		return null;
	}

	/**
	 * Provided a list of comments, sorted in the order the appeared in the source code,
	 * associate all comments with their respective node. Comments will be assocaited with the
	 * node they appear before, unless the comment appears at the end of the same line the
	 * node appears on, in which case the commend will associate with that node on the same line.
	 * Any remaining comments left will be associated with the outer-most node.
	 *
	 * @param incomingComments the list of comments to associate
	 *
	 * @return this node with the comments associated
	 */
	public BoxNode associateComments( List<BoxComment> incomingComments ) {
		if ( incomingComments.isEmpty() ) {
			return this;
		}
		_associateComments( incomingComments );
		// Any comments left in the list, assocate with me
		for ( BoxComment doc : incomingComments ) {
			this.addComment( doc );
		}
		return this;
	}

	/**
	 * Provided a list of comments, sorted in the order the appeared in the source code.
	 *
	 * @param incomingComments the list of comments to associate
	 */
	private void _associateComments( List<BoxComment> incomingComments ) {
		_associateComments( incomingComments, false );
	}

	/**
	 * The same as _associateComments(), but will LEAVE any comments left in the list after this
	 * node for the next node to claim.
	 *
	 * @param incomingComments   the list of comments to associate
	 * @param lastNodeOnThisLine true if this node is the last node on the line
	 */
	private void _associateComments( List<BoxComment> incomingComments, boolean lastNodeOnThisLine ) {
		if ( incomingComments.isEmpty() ) {
			return;
		}
		try {

			// If this is a class or interface, stop and let imports grab comments first
			if ( this instanceof BoxClass bc ) {
				for ( int i = 0; i < bc.getImports().size(); i++ ) {
					BoxNode child = bc.getImports().get( i );
					// If we are the last child, or the next child starts on a different line, then we are the last node on this line
					lastNodeOnThisLine = i == bc.getImports().size() - 1 || !bc.getImports().get( i + 1 ).startsOnEndLineOf( child );
					child._associateComments( incomingComments, lastNodeOnThisLine );
				}
			}
			if ( this instanceof BoxInterface bi ) {
				for ( int i = 0; i < bi.getImports().size(); i++ ) {
					BoxNode child = bi.getImports().get( i );
					// If we are the last child, or the next child starts on a different line, then we are the last node on this line
					lastNodeOnThisLine = i == bi.getImports().size() - 1 || !bi.getImports().get( i + 1 ).startsOnEndLineOf( child );
					child._associateComments( incomingComments, lastNodeOnThisLine );
				}
			}

			// Grab any comments starting before me
			while ( !incomingComments.isEmpty() ) {
				BoxComment doc = incomingComments.get( 0 );
				if ( doc.isBefore( this ) ) {
					this.addComment( doc );
					incomingComments.remove( doc );
				} else {
					break;
				}
			}

			if ( incomingComments.isEmpty() ) {
				return;
			}

			// sort by position start line number followed by column start char
			children.sort( ( a, b ) -> {
				if ( a.getPosition() == null ) {
					return 0;
					// throw new BoxRuntimeException( a.getClass().getName() + " position is null " + a.getSourceText() );
				}
				if ( b.getPosition() == null ) {
					return 0;
					// throw new BoxRuntimeException( a.getClass().getName() + " position is null " + a.getSourceText() );
				}
				int lineDiff = a.getPosition()
				    .getStart()
				    .getLine()
				    - b.getPosition()
				        .getStart()
				        .getLine();
				if ( lineDiff == 0 ) {
					return a.getPosition()
					    .getStart()
					    .getColumn()
					    - b.getPosition()
					        .getStart()
					        .getColumn();
				}
				return lineDiff;
			} );

			// let my children whittle away at what's left.
			for ( int i = 0; i < children.size(); i++ ) {
				BoxNode child = children.get( i );
				// Don't let annotations grab commennts (Need to differentiate between pre and post annotations)
				// Also, imports are processed separately
				if ( child instanceof BoxAnnotation || child instanceof BoxImport ) {
					continue;
				}
				// If we are the last child, or the next child starts on a different line, then we are the last node on this line
				lastNodeOnThisLine = i == children.size() - 1 || !children.get( i + 1 )
				    .startsOnEndLineOf( child );
				child._associateComments( incomingComments, lastNodeOnThisLine );
			}

			if ( incomingComments.isEmpty() ) {
				return;
			}

			// Any remaining comments that are inside of me, get associated with me
			while ( !incomingComments.isEmpty() ) {
				BoxComment doc = incomingComments.get( 0 );
				if ( doc.isInside( this ) ) {
					this.addComment( doc );
					incomingComments.remove( doc );
				} else {
					break;
				}
			}

			if ( incomingComments.isEmpty() ) {
				return;
			}

			// if I am the last node on this line, get any additional comments on my ending line
			if ( lastNodeOnThisLine && ( getParent() == null || !this.endsOnSameLineAs( getParent() ) ) ) {
				while ( !incomingComments.isEmpty() ) {
					BoxComment doc = incomingComments.get( 0 );
					if ( doc.startsOnEndLineOf( this ) ) {
						this.addComment( doc );
						incomingComments.remove( doc );
					} else {
						break;
					}
				}
			}
		} finally {
			// Now that we've associated them comments, if this node is documentable then ask it to process any doc comment
			if ( this instanceof IBoxDocumentableNode bdn ) {
				bdn.finalizeDocumentation();
			}
		}
	}

	/**
	 * Check if this node is before another node
	 *
	 * @param node the node to compare to
	 *
	 * @return true if this node is before the other node
	 */
	public boolean isBefore( BoxNode node ) {
		if ( this.getPosition() == null || node.getPosition() == null ) {
			return false;
		}
		int	thisEndLine		= this.getPosition()
		    .getEnd()
		    .getLine();
		int	thisEndCol		= this.getPosition()
		    .getEnd()
		    .getColumn();
		int	nodeStartLine	= node.getPosition()
		    .getStart()
		    .getLine();
		int	nodeStartCol	= node.getPosition()
		    .getStart()
		    .getColumn();

		return thisEndLine < nodeStartLine || ( thisEndLine == nodeStartLine && thisEndCol <= nodeStartCol );
	}

	/**
	 * Check if this node is after another node
	 *
	 * @param node the node to compare to
	 *
	 * @return true if this node is after the other node
	 */
	public boolean isAfter( BoxNode node ) {
		if ( this.getPosition() == null || node.getPosition() == null ) {
			return false;
		}
		int	thisStartLine	= this.getPosition()
		    .getStart()
		    .getLine();
		int	thisStartCol	= this.getPosition()
		    .getStart()
		    .getColumn();
		int	nodeEndLine		= node.getPosition()
		    .getEnd()
		    .getLine();
		int	nodeEndCol		= node.getPosition()
		    .getEnd()
		    .getColumn();

		return thisStartLine > nodeEndLine || ( thisStartLine == nodeEndLine && thisStartCol >= nodeEndCol );
	}

	/**
	 * Check if this node is inside another node
	 *
	 * @param node the node to compare to
	 *
	 * @return true if this node is inside the other node
	 */
	public boolean isInside( BoxNode node ) {
		if ( this.getPosition() == null || node.getPosition() == null ) {
			return false;
		}
		return !this.isAfter( node ) && !this.isBefore( node );
	}

	/**
	 * Check if this node starts on the end line of another node
	 *
	 * @param node the node to compare to
	 *
	 * @return true if this node starts on the end line of the other node
	 */
	public boolean startsOnEndLineOf( BoxNode node ) {
		if ( this.getPosition() == null || node.getPosition() == null ) {
			return false;
		}
		int	thisStartLine	= this.getPosition()
		    .getStart()
		    .getLine();
		int	nodeEndLine		= node.getPosition()
		    .getEnd()
		    .getLine();
		return thisStartLine == nodeEndLine;
	}

	/**
	 * Check if this node starts on the end line of another node
	 *
	 * @param node the node to compare to
	 *
	 * @return true if this node starts on the end line of the other node
	 */
	public boolean endsOnSameLineAs( BoxNode node ) {
		if ( this.getPosition() == null || node.getPosition() == null ) {
			return false;
		}
		int	thisEndLine	= this.getPosition()
		    .getEnd()
		    .getLine();
		int	nodeEndLine	= node.getPosition()
		    .getEnd()
		    .getLine();
		return thisEndLine == nodeEndLine;
	}

	/**
	 * Set the comments of the node
	 *
	 * @param comments the list of children
	 *
	 * @return the node with the children set
	 */
	public BoxNode setComments( List<BoxComment> comments ) {
		this.comments = comments;
		comments.forEach( comment -> comment.setParent( this ) );
		return this;
	}

	/**
	 * Add a single comment
	 *
	 * @param comment the comment to add
	 *
	 * @return the node with the comment added
	 */
	public BoxNode addComment( BoxComment comment ) {
		this.comments.add( comment );
		comment.setParent( this );
		return this;
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
	 * Find all decedant nodes of a given type that match the supplied predicate
	 *
	 * @param type      The class of node to look for
	 * @param predicate A predicate to test the node
	 *
	 * @return a list of nodes traversed
	 */
	@SuppressWarnings( "unchecked" )
	public <T> List<T> getDescendantsOfType( Class<T> type, Predicate<T> predicate ) {
		List<T> result = new ArrayList<>();
		if ( type.isAssignableFrom( this.getClass() ) && predicate.test( ( T ) this ) ) {
			result.add( ( T ) this );
		}
		for ( BoxNode node : this.children ) {
			result.addAll( node.getDescendantsOfType( type, predicate ) );
		}
		return result;
	}

	/**
	 * Find all decedant nodes of a given type
	 *
	 * @param type The class of node to look for
	 *
	 * @return a list of nodes traversed
	 */
	public <T> List<T> getDescendantsOfType( Class<T> type ) {
		return getDescendantsOfType( type, ( T ) -> true );
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
		if ( position != null ) {
			map.put( "position", position.toMap() );
		}
		map.put( "comments", comments.stream()
		    .map( BoxNode::toMap )
		    .toList() );

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
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( toMap() );
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

	/**
	 * Returns a human-readable description of the node, which it manufactures from the class name.
	 * <p>
	 * While that is quite often good enough, override this method in subclasses to provide a better description
	 * when this default does not work quite right.
	 * </p>
	 *
	 * @return human readable description of the expression, for use in error messages etc
	 */
	public String getDescription() {
		String className = getClass().getSimpleName();
		if ( className.startsWith( "Box" ) ) {
			className = className.substring( 3 );
		}
		var name = className.replaceAll( "([A-Z])", " $1" ).toLowerCase().trim();

		if ( name.matches( "^[aeiou].*" ) ) {
			return "an " + name;
		} else {
			return "a " + name;
		}
	}
}
