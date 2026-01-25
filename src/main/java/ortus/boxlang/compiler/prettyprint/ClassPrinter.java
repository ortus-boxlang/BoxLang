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
package ortus.boxlang.compiler.prettyprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.parser.BoxSourceType;

public class ClassPrinter {

	private Visitor visitor;

	public ClassPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void print( BoxClass node, BoxSourceType sourceType ) {
		switch ( sourceType ) {
			case BOXSCRIPT -> printBoxClass( node );
			case CFSCRIPT -> printCFScriptComponent( node );
			case CFTEMPLATE -> printCFTemplateComponent( node );
			default -> {
			}
		}
	}

	public void print( BoxInterface node, BoxSourceType sourceType ) {
		switch ( sourceType ) {
			case BOXSCRIPT -> printBoxInterface( node );
			case CFSCRIPT -> printCFScriptInterface( node );
			case CFTEMPLATE -> printCFTemplateInterface( node );
			default -> {
			}
		}
	}

	private void printBoxClass( BoxClass classNode ) {
		var	currentDoc		= visitor.getCurrentDoc();
		var	methodOrder		= visitor.config.getClassConfig().getMethodOrder();

		printImports( classNode.getImports() );
		visitor.printPreComments( classNode );

		// split annotations into pre and post based on whether the source of the annotation starts with `@`
		var	preAnnotations	= new ArrayList<BoxAnnotation>();
		var	postAnnotations	= new ArrayList<BoxAnnotation>();
		for ( var anno : classNode.getAnnotations() ) {
			// .getSourceText() _could_ be null, assume pre in that case
			if ( anno.getSourceText() == null || anno.getSourceText().startsWith( "@" ) ) {
				preAnnotations.add( anno );
			} else {
				postAnnotations.add( anno );
			}
		}

		printBoxAnnotations( preAnnotations );

		currentDoc.append( "class" );
		visitor.helperPrinter.printKeyValueAnnotations( postAnnotations, false );
		currentDoc.append( " {" );

		visitor.pushDoc( DocType.INDENT ).append( Line.HARD );
		printProperties( classNode.getProperties() );
		visitor.helperPrinter.printStatements( sortClassBody( classNode.getBody(), methodOrder ) );
		visitor.printInsideComments( classNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( Line.HARD )
		    .append( "}" );

		visitor.printPostComments( classNode );
	}

	private void printBoxInterface( BoxInterface interfaceNode ) {
		var	currentDoc	= visitor.getCurrentDoc();
		var	methodOrder	= visitor.config.getClassConfig().getMethodOrder();

		printImports( interfaceNode.getImports() );
		visitor.printPreComments( interfaceNode );

		printBoxAnnotations( interfaceNode.getAnnotations() );

		currentDoc.append( "interface" );
		visitor.helperPrinter.printKeyValueAnnotations( interfaceNode.getPostAnnotations(), true );
		currentDoc.append( "{" );

		visitor.pushDoc( DocType.INDENT ).append( Line.HARD );
		visitor.helperPrinter.printStatements( sortClassBody( interfaceNode.getBody(), methodOrder ) );
		visitor.printInsideComments( interfaceNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( Line.HARD )
		    .append( "}" );

		visitor.printPostComments( interfaceNode );
	}

	public void printCFScriptComponent( BoxClass classNode ) {
		var	currentDoc	= visitor.getCurrentDoc();
		var	methodOrder	= visitor.config.getClassConfig().getMethodOrder();

		printImports( classNode.getImports() );
		visitor.printPreComments( classNode );
		currentDoc.append( "component" );
		visitor.helperPrinter.printKeyValueAnnotations( classNode.getAnnotations(), true );
		currentDoc.append( "{" );

		visitor.pushDoc( DocType.INDENT ).append( Line.HARD );
		printProperties( classNode.getProperties() );
		visitor.helperPrinter.printStatements( sortClassBody( classNode.getBody(), methodOrder ) );
		visitor.printInsideComments( classNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( Line.HARD )
		    .append( "}" );

		visitor.printPostComments( classNode );
	}

	public void printCFScriptInterface( BoxInterface interfaceNode ) {
		var	currentDoc	= visitor.getCurrentDoc();
		var	methodOrder	= visitor.config.getClassConfig().getMethodOrder();

		printImports( interfaceNode.getImports() );
		visitor.printPreComments( interfaceNode );
		currentDoc.append( "interface" );
		visitor.helperPrinter.printKeyValueAnnotations( interfaceNode.getAllAnnotations(), true );
		currentDoc.append( "{" );

		visitor.pushDoc( DocType.INDENT ).append( Line.HARD );
		visitor.helperPrinter.printStatements( sortClassBody( interfaceNode.getBody(), methodOrder ) );
		visitor.printInsideComments( interfaceNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( Line.HARD )
		    .append( "}" );

		visitor.printPostComments( interfaceNode );
	}

	public void printCFTemplateComponent( BoxClass classNode ) {
		var	currentDoc		= visitor.getCurrentDoc();
		var	propertyOrder	= visitor.config.getClassConfig().getPropertyOrder();
		var	methodOrder		= visitor.config.getClassConfig().getMethodOrder();

		currentDoc.append( "<cfcomponent" );
		visitor.helperPrinter.printKeyValueAnnotations( classNode.getAnnotations(), false );
		currentDoc.append( ">" );

		var					bodyDoc				= visitor.pushDoc( DocType.INDENT );
		List<BoxProperty>	sortedProperties	= sortProperties( classNode.getProperties(), propertyOrder );
		for ( var property : sortedProperties ) {
			bodyDoc.append( Line.HARD );
			property.accept( visitor );
		}
		List<BoxStatement>	sortedBody			= sortClassBody( classNode.getBody(), methodOrder );
		for ( var statement : sortedBody ) {
			statement.accept( visitor );
		}
		visitor.printInsideComments( classNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( "</cfcomponent>" );

		visitor.printPostComments( classNode );
	}

	public void printCFTemplateInterface( BoxInterface interfaceNode ) {
		var	currentDoc	= visitor.getCurrentDoc();
		var	methodOrder	= visitor.config.getClassConfig().getMethodOrder();

		currentDoc.append( "<cfinterface" );
		visitor.helperPrinter.printKeyValueAnnotations( interfaceNode.getAnnotations(), false );
		currentDoc.append( ">" );

		visitor.pushDoc( DocType.INDENT );
		List<BoxStatement> sortedBody = sortClassBody( interfaceNode.getBody(), methodOrder );
		for ( var statement : sortedBody ) {
			statement.accept( visitor );
		}

		visitor.printInsideComments( interfaceNode, false );
		currentDoc
		    .append( visitor.popDoc() )
		    .append( "</cfinterface>" );

		visitor.printPostComments( interfaceNode );
	}

	private void printImports( List<BoxImport> imports ) {
		for ( var importNode : imports ) {
			importNode.accept( visitor );
			visitor.newLine();
		}
		if ( imports.size() > 0 ) {
			visitor.newLine();
		}
	}

	private void printBoxAnnotations( List<BoxAnnotation> annotations ) {
		var currentDoc = visitor.getCurrentDoc();
		for ( var anno : annotations ) {
			visitor.printPreComments( anno );
			currentDoc.append( "@" );
			anno.getKey().accept( visitor );
			if ( anno.getValue() != null ) {
				currentDoc.append( "( " );
				anno.getValue().accept( visitor );
				currentDoc.append( " )" );
			}
			currentDoc.append( Line.HARD );
			visitor.printPostComments( anno );
		}
	}

	private void printProperties( List<BoxProperty> properties ) {
		var	currentDoc		= visitor.getCurrentDoc();
		var	propertyOrder	= visitor.config.getClassConfig().getPropertyOrder();

		// Sort properties based on configuration
		List<BoxProperty> sortedProperties = sortProperties( properties, propertyOrder );

		for ( int i = 0; i < sortedProperties.size(); i++ ) {
			sortedProperties.get( i ).accept( visitor );
			currentDoc.append( Line.HARD );
		}
		// Note: member_spacing between properties and methods is handled by printStatements
	}

	/**
	 * Sort properties based on the configured order.
	 *
	 * @param properties    the list of properties to sort
	 * @param propertyOrder the ordering strategy: "alphabetical", "length", "type", or "preserve"
	 *
	 * @return sorted list of properties (original list if "preserve")
	 */
	private List<BoxProperty> sortProperties( List<BoxProperty> properties, String propertyOrder ) {
		if ( properties.isEmpty() || "preserve".equalsIgnoreCase( propertyOrder ) ) {
			return properties;
		}

		List<BoxProperty> sorted = new ArrayList<>( properties );

		switch ( propertyOrder.toLowerCase() ) {
			case "alphabetical" -> sorted.sort( Comparator.comparing( this::getPropertyName, String.CASE_INSENSITIVE_ORDER ) );
			case "length" -> sorted.sort( Comparator.comparingInt( p -> {
				String sourceText = p.getSourceText();
				return sourceText != null ? sourceText.length() : 0;
			} ) );
			case "type" -> sorted.sort( Comparator.comparing( this::getPropertyType, String.CASE_INSENSITIVE_ORDER ) );
			default -> {
				// preserve order - return as-is
			}
		}

		return sorted;
	}

	/**
	 * Extract the property name from a BoxProperty node.
	 *
	 * @param property the property to get the name from
	 *
	 * @return the property name, or empty string if not found
	 */
	private String getPropertyName( BoxProperty property ) {
		// Get all annotations (post annotations are the inline ones like name="foo")
		List<BoxAnnotation> allAnnotations = property.getAllAnnotations();

		// First check for an explicit name annotation with a value
		for ( BoxAnnotation annotation : allAnnotations ) {
			if ( annotation.getKey().getValue().equalsIgnoreCase( "name" ) && annotation.getValue() != null ) {
				return extractStringValue( annotation.getValue() );
			}
		}

		// If no explicit name, look for non-valued annotations in post annotations
		// In shorthand syntax like "property String userName;", the name is the last non-valued annotation
		List<BoxAnnotation>	postAnnotations	= property.getPostAnnotations();
		List<BoxAnnotation>	nonValuedKeys	= postAnnotations.stream()
		    .filter( a -> a.getValue() == null )
		    .toList();

		if ( !nonValuedKeys.isEmpty() ) {
			// If there are multiple non-valued keys and no explicit type, first is type, second is name
			// If only one non-valued key, it's the name
			boolean hasExplicitType = allAnnotations.stream()
			    .anyMatch( a -> a.getKey().getValue().equalsIgnoreCase( "type" ) && a.getValue() != null );

			if ( nonValuedKeys.size() >= 2 && !hasExplicitType ) {
				return nonValuedKeys.get( 1 ).getKey().getValue();
			} else if ( nonValuedKeys.size() >= 1 ) {
				return nonValuedKeys.get( nonValuedKeys.size() - 1 ).getKey().getValue();
			}
		}

		return "";
	}

	/**
	 * Extract the property type from a BoxProperty node.
	 *
	 * @param property the property to get the type from
	 *
	 * @return the property type, or "any" if not found
	 */
	private String getPropertyType( BoxProperty property ) {
		// Get all annotations
		List<BoxAnnotation> allAnnotations = property.getAllAnnotations();

		// First check for an explicit type annotation with a value
		for ( BoxAnnotation annotation : allAnnotations ) {
			if ( annotation.getKey().getValue().equalsIgnoreCase( "type" ) && annotation.getValue() != null ) {
				return extractStringValue( annotation.getValue() );
			}
		}

		// If no explicit type, look for non-valued annotations in post annotations
		// In shorthand syntax like "property String userName;", type is the first non-valued annotation
		// when there are 2+ non-valued annotations
		List<BoxAnnotation>	postAnnotations	= property.getPostAnnotations();
		List<BoxAnnotation>	nonValuedKeys	= postAnnotations.stream()
		    .filter( a -> a.getValue() == null )
		    .toList();

		boolean				hasExplicitName	= allAnnotations.stream()
		    .anyMatch( a -> a.getKey().getValue().equalsIgnoreCase( "name" ) && a.getValue() != null );

		if ( nonValuedKeys.size() >= 2 && !hasExplicitName ) {
			// First non-valued key is the type
			return nonValuedKeys.get( 0 ).getKey().getValue();
		}

		return "any";
	}

	/**
	 * Extract a string value from a BoxExpression.
	 *
	 * @param expr the expression to extract the string from
	 *
	 * @return the string value, or the source text if not a string literal
	 */
	private String extractStringValue( BoxExpression expr ) {
		if ( expr instanceof BoxStringLiteral stringLiteral ) {
			return stringLiteral.getValue();
		}
		// Fall back to source text for other expression types
		return expr.getSourceText() != null ? expr.getSourceText() : "";
	}

	/**
	 * Sort class body statements based on method ordering configuration.
	 * Only BoxFunctionDeclaration nodes are sorted; other statements maintain their relative order.
	 *
	 * @param statements  the list of statements in the class body
	 * @param methodOrder the ordering strategy: "alphabetical" or "preserve"
	 *
	 * @return sorted list of statements (original list if "preserve")
	 */
	private List<BoxStatement> sortClassBody( List<BoxStatement> statements, String methodOrder ) {
		if ( statements.isEmpty() || "preserve".equalsIgnoreCase( methodOrder ) ) {
			return statements;
		}

		// Extract methods and non-methods
		List<BoxFunctionDeclaration>	methods		= new ArrayList<>();
		List<Integer>					methodIndices	= new ArrayList<>();

		for ( int i = 0; i < statements.size(); i++ ) {
			if ( statements.get( i ) instanceof BoxFunctionDeclaration func ) {
				methods.add( func );
				methodIndices.add( i );
			}
		}

		// If no methods or only one method, no sorting needed
		if ( methods.size() <= 1 ) {
			return statements;
		}

		// Sort methods based on configuration
		switch ( methodOrder.toLowerCase() ) {
			case "alphabetical" -> methods.sort( Comparator.comparing( this::getMethodName, String.CASE_INSENSITIVE_ORDER ) );
			default -> {
				// preserve order - return as-is
				return statements;
			}
		}

		// Rebuild the list with sorted methods in their original positions
		List<BoxStatement>	result		= new ArrayList<>( statements );
		int					methodIdx	= 0;
		for ( int originalIndex : methodIndices ) {
			result.set( originalIndex, methods.get( methodIdx++ ) );
		}

		return result;
	}

	/**
	 * Extract the method name from a BoxFunctionDeclaration node.
	 *
	 * @param method the method to get the name from
	 *
	 * @return the method name
	 */
	private String getMethodName( BoxFunctionDeclaration method ) {
		return method.getName() != null ? method.getName() : "";
	}
}
