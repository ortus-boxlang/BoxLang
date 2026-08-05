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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.compiler.ast.comment.BoxComment;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.visitor.BoxVisitable;
import ortus.boxlang.compiler.prettyprint.PrettyPrint;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.util.RegexBuilder;

/**
 * Base class for the BoxLang AST Nodes
 */
public abstract class BoxNode implements BoxVisitable {

	private static final String				POSITION_SOURCE_TEXT	= new String();
	private static final List<BoxNode>		EMPTY_CHILDREN			= List.of();
	private static final List<BoxComment>	EMPTY_COMMENTS			= List.of();
	private static final Object				NO_POSITION_SOURCE		= new Object();
	private static final int				COMPACT_COORDINATE_MAX	= 0xffff;

	private long							positionCoordinates;
	private long							positionIndexes;
	private Object							positionData;
	protected Position						position;
	private String							sourceText;
	protected BoxNode						parent					= null;
	private List<BoxNode>					children;
	private List<BoxComment>				comments;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement or expression in the source code
	 * @param sourceText source code of the statement/expression
	 */
	protected BoxNode( Position position, String sourceText ) {
		this.sourceText = position != null && position.sourceTextEquals( sourceText ) ? POSITION_SOURCE_TEXT : sourceText;
		storePosition( position );
		this.children	= EMPTY_CHILDREN;
		this.comments	= EMPTY_COMMENTS;
	}

	/**
	 * Returns the position in code that the node represents
	 *
	 * @return a Position instance
	 *
	 * @see Position
	 */
	public Position getPosition() {
		if ( this.position != null ) {
			return this.position;
		}
		if ( this.positionData == null ) {
			return null;
		}
		return new NodePosition( this );
	}

	/**
	 * Set the position of the node
	 *
	 * @param position the position within the source code that originated the node
	 */
	public void setPosition( Position position ) {
		if ( this.sourceText == POSITION_SOURCE_TEXT ) {
			this.sourceText = getSourceText();
		}
		storePosition( position );
	}

	/**
	 * Returns the source code that originated the Node
	 *
	 * @return the snipped of the source code
	 */
	public String getSourceText() {
		return this.sourceText == POSITION_SOURCE_TEXT ? getPosition().getSourceText() : this.sourceText;
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
		if ( parent != null && !parent.children.contains( this ) ) {
			if ( parent.children == EMPTY_CHILDREN ) {
				parent.children = new ArrayList<>( 2 );
			}
			parent.children.add( this );
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
	 * node appears on, in which case the comment will associate with that node on the same line.
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
			if ( children.size() > 1 ) {
				children.sort( ( a, b ) -> {
					if ( !a.hasPosition() ) {
						return 0;
						// throw new BoxRuntimeException( a.getClass().getName() + " position is null " + a.getSourceText() );
					}
					if ( !b.hasPosition() ) {
						return 0;
						// throw new BoxRuntimeException( a.getClass().getName() + " position is null " + a.getSourceText() );
					}
					int lineDiff = a.getPositionStartLine() - b.getPositionStartLine();
					if ( lineDiff == 0 ) {
						return a.getPositionStartColumn() - b.getPositionStartColumn();
					}
					return lineDiff;
				} );
			}

			// let my children whittle away at what's left.
			for ( int i = 0; i < children.size(); i++ ) {
				BoxNode child = children.get( i );
				// Don't let annotations grab commennts (Need to differentiate between pre and post annotations)
				// Also, imports are processed separately
				// comments cannot have other comments associated with them
				if ( child instanceof BoxAnnotation || child instanceof BoxImport || child instanceof BoxComment ) {
					continue;
				}
				// If we are the last child, or the next child starts on a different line, then we are the last node on this line
				boolean childLastNodeOnThisLine = i == children.size() - 1 || !children.get( i + 1 )
				    .startsOnEndLineOf( child );
				child._associateComments( incomingComments, childLastNodeOnThisLine );
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
		if ( !this.hasPosition() || !node.hasPosition() ) {
			return false;
		}
		int	thisEndLine		= this.getPositionEndLine();
		int	thisEndCol		= this.getPositionEndColumn();
		int	nodeStartLine	= node.getPositionStartLine();
		int	nodeStartCol	= node.getPositionStartColumn();

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
		if ( !this.hasPosition() || !node.hasPosition() ) {
			return false;
		}
		int	thisStartLine	= this.getPositionStartLine();
		int	thisStartCol	= this.getPositionStartColumn();
		int	nodeEndLine		= node.getPositionEndLine();
		int	nodeEndCol		= node.getPositionEndColumn();

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
		if ( !this.hasPosition() || !node.hasPosition() ) {
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
		if ( !this.hasPosition() || !node.hasPosition() ) {
			return false;
		}
		return this.getPositionStartLine() == node.getPositionEndLine();
	}

	/**
	 * Check if this node starts on the end line of another node
	 *
	 * @param node the node to compare to
	 *
	 * @return true if this node starts on the end line of the other node
	 */
	public boolean endsOnSameLineAs( BoxNode node ) {
		if ( !this.hasPosition() || !node.hasPosition() ) {
			return false;
		}
		return this.getPositionEndLine() == node.getPositionEndLine();
	}

	/**
	 * Check if this node has lines between it and another node
	 *
	 * @param node the node to compare to
	 *
	 * @return true if this node has lines between it and the other node
	 */
	public boolean hasLinesBetween( BoxNode node ) {
		if ( !this.hasPosition() || !node.hasPosition() ) {
			return false;
		}
		int	thisStartLine	= this.getPositionStartLine();
		int	nodeEndLine		= node.getPositionEndLine();

		if ( thisStartLine > nodeEndLine ) {
			return thisStartLine - nodeEndLine > 1;
		}

		int	thisEndLine		= this.getPositionEndLine();
		int	nodeStartLine	= node.getPositionStartLine();

		return nodeStartLine - thisEndLine > 1;
	}

	/**
	 * Check if there are empty lines between this node and another node, considering their associated comments.
	 *
	 * @param node the node to compare to
	 *
	 * @return true if there are empty lines between this node and the other node, accounting for their closest comments
	 */
	public boolean hasLinesBetweenWithComments( BoxNode node ) {
		if ( !this.hasPosition() || !node.hasPosition() ) {
			return false;
		}
		// Determine node order once
		boolean				isThisBefore	= this.isBefore( node );
		BoxNode				firstNode		= isThisBefore ? this : node;
		BoxNode				secondNode		= isThisBefore ? node : this;

		// Get the end line of the first node, considering its last comment
		int					firstEndLine	= firstNode.getPositionEndLine();
		List<BoxComment>	firstComments	= firstNode.getComments();
		if ( !firstComments.isEmpty() ) {
			// Last comment in order of appearance has the latest end line in the document
			int lastCommentEndLine = ( ( BoxNode ) firstComments.get( firstComments.size() - 1 ) ).getPositionEndLine();
			// Use the latest end line (node or last comment)
			firstEndLine = Math.max( firstEndLine, lastCommentEndLine );
		}

		// Get the start line of the second node, considering its first comment
		int					secondStartLine	= secondNode.getPositionStartLine();
		List<BoxComment>	secondComments	= secondNode.getComments();
		if ( !secondComments.isEmpty() ) {
			// First comment in order of appearance has the earliest start line in the document
			int firstCommentStartLine = ( ( BoxNode ) secondComments.get( 0 ) ).getPositionStartLine();
			// Use the earliest start line (node or first comment)
			secondStartLine = Math.min( secondStartLine, firstCommentStartLine );
		}

		// Check if there is at least one empty line between the first node's end (or its last comment)
		// and the second node's start (or its first comment)
		return secondStartLine - firstEndLine > 1;
	}

	/**
	 * Set the comments of the node
	 *
	 * @param comments the list of children
	 *
	 * @return the node with the children set
	 */
	public BoxNode setComments( List<BoxComment> comments ) {
		this.comments = comments.isEmpty() ? EMPTY_COMMENTS : comments;
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
		if ( this.comments == EMPTY_COMMENTS ) {
			this.comments = new ArrayList<>( 1 );
		}
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
		if ( oldChild != null && !this.children.isEmpty() ) {
			children.remove( oldChild );
		}
		if ( newChild != null ) {
			if ( this.children == EMPTY_CHILDREN ) {
				this.children = new ArrayList<>( 2 );
			}
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
		if ( oldChildren != null && !this.children.isEmpty() ) {
			children.removeAll( oldChildren );
		}
		if ( newChildren != null && !newChildren.isEmpty() ) {
			if ( this.children == EMPTY_CHILDREN ) {
				this.children = new ArrayList<>( newChildren.size() );
			}
			children.addAll( newChildren );
		}
	}

	/**
	 * Trim mutable child-list capacity throughout this node's graph.
	 */
	public void trimChildLists() {
		for ( BoxNode child : this.children ) {
			child.trimChildLists();
		}
		if ( this.children instanceof ArrayList<?> childList ) {
			childList.trimToSize();
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
		return getDescendantsOfType( type, predicate, ( BoxNode ) -> false );
	}

	/**
	 * Find all decedant nodes of a given type that match the supplied predicate
	 *
	 * @param type                   The class of node to look for
	 * @param predicate              A predicate to test the node
	 * @param stopTraversalPredicate A predicate to test whether to stop traversing down a branch of the tree
	 *
	 * @return a list of nodes traversed
	 */
	@SuppressWarnings( "unchecked" )
	public <T> List<T> getDescendantsOfType( Class<T> type, Predicate<T> predicate, Predicate<BoxNode> stopTraversalPredicate ) {
		List<T> result = new ArrayList<>();
		if ( type.isAssignableFrom( this.getClass() ) && predicate.test( ( T ) this ) ) {
			result.add( ( T ) this );
		}
		for ( BoxNode node : this.children ) {
			if ( stopTraversalPredicate.test( node ) ) {
				continue;
			}
			result.addAll( node.getDescendantsOfType( type, predicate, stopTraversalPredicate ) );
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
		map.put( "sourceText", getSourceText() );
		Position position = getPosition();
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
		Config config = new Config();
		config.getTemplate().setEnabled( true );
		return PrettyPrint.prettyPrint( this, config );
	}

	public String toString( Config config ) {
		return PrettyPrint.prettyPrint( this, config );
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
		var name = RegexBuilder.of( className, RegexBuilder.UPPERCASE_GROUP )
		    .replaceAllAndGet( " $1" )
		    .toLowerCase()
		    .trim();

		if ( RegexBuilder.of( name, RegexBuilder.VOWELS ).matches() ) {
			return "an " + name;
		} else {
			return "a " + name;
		}
	}

	private void storePosition( Position position ) {
		if ( position == null ) {
			this.position		= null;
			this.positionData	= null;
			return;
		}
		if ( !position.isCompactable() ) {
			this.position		= position;
			this.positionData	= null;
			return;
		}

		this.position = null;
		long	start	= position.getPackedStart();
		long	end		= position.getPackedEnd();
		if ( canCompact( start ) && canCompact( end ) ) {
			this.positionCoordinates	= packCoordinates( start, end );
			this.positionIndexes		= packIndexes( position.getStartIndex(), position.getEndIndex() );
			Source source = position.getPositionSource();
			this.positionData = source == null ? NO_POSITION_SOURCE : source;
		} else {
			this.position		= position.snapshot();
			this.positionData	= null;
		}
	}

	private boolean hasPosition() {
		return this.position != null || this.positionData != null;
	}

	private int getPositionStartLine() {
		return ( int ) ( getPackedStart() >> 32 );
	}

	private int getPositionStartColumn() {
		return ( int ) getPackedStart();
	}

	private int getPositionEndLine() {
		return ( int ) ( getPackedEnd() >> 32 );
	}

	private int getPositionEndColumn() {
		return ( int ) getPackedEnd();
	}

	private static boolean canCompact( long point ) {
		return Integer.compareUnsigned( ( int ) ( point >> 32 ), COMPACT_COORDINATE_MAX ) <= 0
		    && Integer.compareUnsigned( ( int ) point, COMPACT_COORDINATE_MAX ) <= 0;
	}

	private static long packCoordinates( long start, long end ) {
		return ( ( start >> 32 ) & 0xffffL ) << 48
		    | ( start & 0xffffL ) << 32
		    | ( ( end >> 32 ) & 0xffffL ) << 16
		    | ( end & 0xffffL );
	}

	private static long packPoint( int line, int column ) {
		return ( ( long ) line << 32 ) | ( column & 0xffffffffL );
	}

	private static long packIndexes( int startIndex, int endIndex ) {
		return ( ( long ) startIndex << 32 ) | ( endIndex & 0xffffffffL );
	}

	private long getPackedStart() {
		if ( this.position != null ) {
			return this.position.getPackedStart();
		}
		return packPoint( ( int ) ( this.positionCoordinates >>> 48 ), ( int ) ( this.positionCoordinates >>> 32 ) & 0xffff );
	}

	private void setPackedStart( long start ) {
		if ( this.position != null ) {
			this.position.setPackedStart( start );
			return;
		}
		long end = getPackedEnd();
		if ( canCompact( start ) ) {
			this.positionCoordinates = packCoordinates( start, end );
		} else {
			promotePosition().setPackedStart( start );
		}
	}

	private long getPackedEnd() {
		if ( this.position != null ) {
			return this.position.getPackedEnd();
		}
		return packPoint( ( int ) ( this.positionCoordinates >>> 16 ) & 0xffff, ( int ) this.positionCoordinates & 0xffff );
	}

	private void setPackedEnd( long end ) {
		if ( this.position != null ) {
			this.position.setPackedEnd( end );
			return;
		}
		long start = getPackedStart();
		if ( canCompact( end ) ) {
			this.positionCoordinates = packCoordinates( start, end );
		} else {
			promotePosition().setPackedEnd( end );
		}
	}

	private Source getPositionSource() {
		if ( this.position != null ) {
			return this.position.getPositionSource();
		}
		return this.positionData == NO_POSITION_SOURCE ? null : ( Source ) this.positionData;
	}

	private void setPositionSource( Source source ) {
		if ( this.position != null ) {
			this.position.setPositionSource( source );
		} else {
			this.positionData = source == null ? NO_POSITION_SOURCE : source;
		}
	}

	private int getPositionStartIndex() {
		if ( this.position != null ) {
			return this.position.getStartIndex();
		}
		return ( int ) ( this.positionIndexes >> 32 );
	}

	private void setPositionStartIndex( int startIndex ) {
		if ( this.position != null ) {
			this.position.setStartIndex( startIndex );
		} else {
			this.positionIndexes = packIndexes( startIndex, getPositionEndIndex() );
		}
	}

	private int getPositionEndIndex() {
		if ( this.position != null ) {
			return this.position.getEndIndex();
		}
		return ( int ) this.positionIndexes;
	}

	private void setPositionEndIndex( int endIndex ) {
		if ( this.position != null ) {
			this.position.setEndIndex( endIndex );
		} else {
			this.positionIndexes = packIndexes( getPositionStartIndex(), endIndex );
		}
	}

	private Position promotePosition() {
		Position position = new Position( ( int ) ( getPackedStart() >> 32 ), ( int ) getPackedStart(), ( int ) ( getPackedEnd() >> 32 ),
		    ( int ) getPackedEnd(), getPositionSource(), getPositionStartIndex(), getPositionEndIndex() );
		this.position		= position;
		this.positionData	= null;
		return position;
	}

	private static class NodePosition extends Position implements Serializable {

		private static final long				serialVersionUID	= 1L;

		private final WeakReference<BoxNode>	node;

		private NodePosition( BoxNode node ) {
			this( node, node.getPackedStart(), node.getPackedEnd(), node.getPositionSource(), node.getPositionStartIndex(), node.getPositionEndIndex() );
		}

		private NodePosition( BoxNode node, long start, long end, Source source, int startIndex, int endIndex ) {
			super( ( int ) ( start >> 32 ), ( int ) start, ( int ) ( end >> 32 ), ( int ) end, source, startIndex, endIndex );
			this.node = new WeakReference<>( node );
		}

		@Override
		protected long getPackedStart() {
			BoxNode node = this.node.get();
			if ( node != null ) {
				setSnapshotStart( node.getPackedStart() );
			}
			return super.getPackedStart();
		}

		@Override
		protected void setPackedStart( long start ) {
			setSnapshotStart( start );
			BoxNode node = this.node.get();
			if ( node != null ) {
				node.setPackedStart( start );
			}
		}

		@Override
		protected long getPackedEnd() {
			BoxNode node = this.node.get();
			if ( node != null ) {
				setSnapshotEnd( node.getPackedEnd() );
			}
			return super.getPackedEnd();
		}

		@Override
		protected void setPackedEnd( long end ) {
			setSnapshotEnd( end );
			BoxNode node = this.node.get();
			if ( node != null ) {
				node.setPackedEnd( end );
			}
		}

		@Override
		protected Source getPositionSource() {
			BoxNode node = this.node.get();
			if ( node != null ) {
				setSnapshotSource( node.getPositionSource() );
			}
			return super.getPositionSource();
		}

		@Override
		protected void setPositionSource( Source source ) {
			setSnapshotSource( source );
			BoxNode node = this.node.get();
			if ( node != null ) {
				node.setPositionSource( source );
			}
		}

		@Override
		protected int getStartIndex() {
			BoxNode node = this.node.get();
			if ( node != null ) {
				setSnapshotStartIndex( node.getPositionStartIndex() );
			}
			return super.getStartIndex();
		}

		@Override
		protected void setStartIndex( int startIndex ) {
			setSnapshotStartIndex( startIndex );
			BoxNode node = this.node.get();
			if ( node != null ) {
				node.setPositionStartIndex( startIndex );
			}
		}

		@Override
		protected int getEndIndex() {
			BoxNode node = this.node.get();
			if ( node != null ) {
				setSnapshotEndIndex( node.getPositionEndIndex() );
			}
			return super.getEndIndex();
		}

		@Override
		protected void setEndIndex( int endIndex ) {
			setSnapshotEndIndex( endIndex );
			BoxNode node = this.node.get();
			if ( node != null ) {
				node.setPositionEndIndex( endIndex );
			}
		}

		@Override
		protected boolean isCompactable() {
			return true;
		}

		@Override
		public Position snapshot() {
			return new Position( ( int ) ( getPackedStart() >> 32 ), ( int ) getPackedStart(), ( int ) ( getPackedEnd() >> 32 ), ( int ) getPackedEnd(),
			    getPositionSource(), getStartIndex(), getEndIndex() );
		}

		private Object writeReplace() throws ObjectStreamException {
			return snapshot();
		}

		private void setSnapshotStart( long start ) {
			super.setPackedStart( start );
		}

		private void setSnapshotEnd( long end ) {
			super.setPackedEnd( end );
		}

		private void setSnapshotSource( Source source ) {
			super.setPositionSource( source );
		}

		private void setSnapshotStartIndex( int startIndex ) {
			super.setStartIndex( startIndex );
		}

		private void setSnapshotEndIndex( int endIndex ) {
			super.setEndIndex( endIndex );
		}
	}
}
