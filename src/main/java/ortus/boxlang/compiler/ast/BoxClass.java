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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

/**
 * Root node for a Class
 */
public class BoxClass extends BoxNode implements IBoxDocumentableNode {

	private List<BoxStatement>					body;
	private List<BoxImport>						imports;
	private List<BoxAnnotation>					annotations;
	private List<BoxDocumentationAnnotation>	documentation;
	private List<BoxProperty>					properties;

	/**
	 * Creates an AST for a Class
	 *
	 * @param imports       list of imports
	 * @param body          list of statements
	 * @param annotations   list of annotations
	 * @param documentation list of documentation annotations
	 * @param properties    list of properties
	 * @param position      position in the source file
	 * @param sourceText    the source text
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxClass( List<BoxImport> imports, List<BoxStatement> body, List<BoxAnnotation> annotations,
	    List<BoxDocumentationAnnotation> documentation, List<BoxProperty> properties, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setImports( imports );
		setBody( body );
		setAnnotations( annotations );
		setDocumentation( documentation );
		setProperties( properties );
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public List<BoxAnnotation> getAnnotations() {
		return annotations;
	}

	public List<BoxDocumentationAnnotation> getDocumentation() {
		return documentation;
	}

	public List<BoxImport> getImports() {
		return imports;
	}

	public List<BoxProperty> getProperties() {
		return properties;
	}

	public void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		this.body.forEach( arg -> arg.setParent( this ) );
	}

	public void setAnnotations( List<BoxAnnotation> annotations ) {
		replaceChildren( this.annotations, annotations );
		this.annotations = annotations;
		this.annotations.forEach( arg -> arg.setParent( this ) );
	}

	public void setDocumentation( List<BoxDocumentationAnnotation> documentation ) {
		replaceChildren( this.documentation, documentation );
		this.documentation = documentation;
		this.documentation.forEach( arg -> arg.setParent( this ) );
	}

	public void setImports( List<BoxImport> imports ) {
		replaceChildren( this.imports, imports );
		this.imports = imports;
		this.imports.forEach( arg -> arg.setParent( this ) );
	}

	public void setProperties( List<BoxProperty> properties ) {
		replaceChildren( this.properties, properties );
		this.properties = properties;
		this.properties.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "imports", imports.stream().map( BoxImport::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "annotations", annotations.stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "documentation", documentation.stream().map( BoxDocumentationAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "properties", properties.stream().map( BoxProperty::toMap ).collect( java.util.stream.Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

	public static List<BoxAnnotation> normlizePropertyAnnotations( BoxProperty prop ) {

		/**
		 * normalize annotations to allow for
		 * property String userName;
		 * This means all inline and pre annotations are treated as post annotations
		 */
		List<BoxAnnotation>	finalAnnotations	= new ArrayList<>();
		// Start wiith all inline annotatinos
		List<BoxAnnotation>	annotations			= prop.getPostAnnotations();
		// Add in any pre annotations that have a value, which allows type, name, or default to be set before
		annotations.addAll( prop.getAnnotations().stream().filter( it -> it.getValue() != null ).toList() );

		// Find the position of the name, type, and default annotations
		int					namePosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "name" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );
		int					typePosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "type" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );
		int					defaultPosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "default" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );

		// Count the number of non-valued keys to determine how to handle them by position later
		int					numberOfNonValuedKeys	= ( int ) annotations.stream()
		    .map( BoxAnnotation::getValue )
		    .filter( Objects::isNull )
		    .count();
		List<BoxAnnotation>	nonValuedKeys			= annotations.stream()
		    .filter( it -> it.getValue() == null )
		    .collect( java.util.stream.Collectors.toList() );

		// Find the name, type, and default annotations
		BoxAnnotation		nameAnnotation			= null;
		BoxAnnotation		typeAnnotation			= null;
		BoxAnnotation		defaultAnnotation		= null;
		if ( namePosition > -1 )
			nameAnnotation = annotations.get( namePosition );
		if ( typePosition > -1 )
			typeAnnotation = annotations.get( typePosition );
		if ( defaultPosition > -1 )
			defaultAnnotation = annotations.get( defaultPosition );

		/**
		 * If there is no name, if there is more than one nonvalued keys and no type, use the first nonvalued key
		 * as the type and second nonvalued key as the name. Otherwise, if there are more than one non-valued key, use the first as the name.
		 */
		if ( namePosition == -1 ) {
			if ( numberOfNonValuedKeys > 1 && typePosition == -1 ) {
				typeAnnotation	= new BoxAnnotation( new BoxFQN( "type", null, null ),
				    new BoxStringLiteral( nonValuedKeys.get( 0 ).getKey().getValue(), null, null ), null,
				    null );
				nameAnnotation	= new BoxAnnotation( new BoxFQN( "name", null, null ),
				    new BoxStringLiteral( nonValuedKeys.get( 1 ).getKey().getValue(), null, null ), null,
				    null );
				finalAnnotations.add( nameAnnotation );
				finalAnnotations.add( typeAnnotation );
				annotations.remove( nonValuedKeys.get( 0 ) );
				annotations.remove( nonValuedKeys.get( 1 ) );
			} else if ( numberOfNonValuedKeys > 0 ) {
				nameAnnotation = new BoxAnnotation( new BoxFQN( "name", null, null ),
				    new BoxStringLiteral( nonValuedKeys.get( 0 ).getKey().getValue(), null, null ), null,
				    null );
				finalAnnotations.add( nameAnnotation );
				annotations.remove( nonValuedKeys.get( 0 ) );
			} else {
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] has no name", prop );
			}
		}

		// add type with value of any if not present
		if ( typeAnnotation == null ) {
			typeAnnotation = new BoxAnnotation( new BoxFQN( "type", null, null ), new BoxStringLiteral( "any", null, null ), null,
			    null );
			finalAnnotations.add( typeAnnotation );
		}

		// add default with value of null if not present
		if ( defaultPosition == -1 ) {
			defaultAnnotation = new BoxAnnotation( new BoxFQN( "default", null, null ), new BoxNull( null, null ), null,
			    null );
			finalAnnotations.add( defaultAnnotation );
		}

		// add remaining annotations
		finalAnnotations.addAll( annotations );
		// Now that name, type, and default are finalized, add in any remaining non-valued keys
		finalAnnotations.addAll( prop.getAnnotations().stream().filter( it -> it.getValue() == null ).toList() );

		return finalAnnotations;
	}

}
