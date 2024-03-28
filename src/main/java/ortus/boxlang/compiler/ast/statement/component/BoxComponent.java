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
package ortus.boxlang.compiler.ast.statement.component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;

/**
 * Represents a tag or script component
 */
public class BoxComponent extends BoxStatement {

	// If null, there was no body
	private String				name;
	private List<BoxStatement>	body;
	private List<BoxAnnotation>	attributes;
	private int					sourceStartIndex;
	public Boolean				requiresBody	= false;

	/**
	 * Create an AST for a component
	 *
	 * @param name       name of the component
	 * @param attributes list of the annotations
	 * @param position   position within the source code
	 * @param sourceText source code
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxComponent( String name, List<BoxAnnotation> attributes, Position position, String sourceText ) {
		this( name, attributes, null, 0, position, sourceText );
	}

	/**
	 * Create an AST for a component
	 *
	 * @param name        name of the component
	 * @param annotations list of the annotations
	 * @param body        list of the statements nodes
	 * @param position    position within the source code
	 * @param sourceText  source code
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxComponent( String name, List<BoxAnnotation> attributes, List<BoxStatement> body, Position position, String sourceText ) {
		this( name, attributes, body, 0, position, sourceText );
	}

	/**
	 * Create an AST for a component
	 *
	 * @param name        name of the component
	 * @param annotations list of the annotations
	 * @param body        list of the statements nodes
	 * @param sourceText  source code
	 * @param position    position within the source code
	 * @param sourceText  source code
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxComponent( String name, List<BoxAnnotation> attributes, List<BoxStatement> body, int sourceStartIndex, Position position, String sourceText ) {
		super( position, sourceText );
		setName( name );
		setAttributes( attributes );
		setBody( body );
		setSourceStartIndex( sourceStartIndex );
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public List<BoxAnnotation> getAttributes() {
		return attributes;
	}

	public String getName() {
		return name;
	}

	public int getSourceStartIndex() {
		return sourceStartIndex;
	}

	void setSourceStartIndex( int sourceStartIndex ) {
		this.sourceStartIndex = sourceStartIndex;
	}

	public void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		if ( this.body != null ) {
			this.body.forEach( statement -> statement.setParent( this ) );
		}
	}

	public void setRequiresBody( Boolean requiresBody ) {
		this.requiresBody = requiresBody;
	}

	public Boolean getRequiresBody() {
		return requiresBody;
	}

	void setAttributes( List<BoxAnnotation> attributes ) {
		replaceChildren( this.attributes, attributes );
		this.attributes = attributes;
		if ( this.attributes != null ) {
			this.attributes.forEach( arg -> arg.setParent( this ) );
		}
	}

	void setName( String name ) {
		this.name = name;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "name", name );
		if ( attributes != null ) {
			map.put( "attributes", attributes.stream().map( BoxNode::toMap ).collect( Collectors.toList() ) );
		} else {
			map.put( "attributes", null );
		}
		if ( body != null ) {
			map.put( "body", body.stream().map( BoxNode::toMap ).collect( Collectors.toList() ) );
		} else {
			map.put( "body", null );
		}

		return map;
	}

}
