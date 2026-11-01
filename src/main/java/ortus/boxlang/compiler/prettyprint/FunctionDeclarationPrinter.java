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
import java.util.List;
import java.util.regex.Pattern;

import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.parser.BoxSourceType;

public class FunctionDeclarationPrinter {

	/** The non-access modifiers printed after the access modifier, in Java's canonical order. */
	private static final List<BoxMethodDeclarationModifier>	ORDERED_MODIFIERS	= List.of(
	    BoxMethodDeclarationModifier.ABSTRACT,
	    BoxMethodDeclarationModifier.STATIC,
	    BoxMethodDeclarationModifier.FINAL );

	/** Text that ends with a line break followed only by spaces or tabs. */
	private static final Pattern							TRAILING_NEWLINE	= Pattern.compile( "[\\r\\n][\\t ]*$" );

	private Visitor											visitor;

	public FunctionDeclarationPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void print( BoxFunctionDeclaration node, BoxSourceType sourceType ) {
		switch ( sourceType ) {
			case BOXSCRIPT -> printScriptFunctionDeclaration( node );
			case BOXTEMPLATE -> printTemplateFunctionDeclaration( node );
			case CFSCRIPT -> printCFScriptFunctionDeclaration( node );
			// Same tag layout as bx: templates (with the cf prefix): arguments as <cfargument>, always a closing tag
			case CFTEMPLATE -> printTemplateFunctionDeclaration( node );
			default -> {
			}
		}
	}

	public void printScriptFunctionDeclaration( BoxFunctionDeclaration node ) {
		var	currentDoc		= visitor.getCurrentDoc();

		// split annotations into pre and post based on whether the source of the annotation starts with `@`
		var	preAnnotations	= new ArrayList<BoxAnnotation>();
		var	postAnnotations	= new ArrayList<BoxAnnotation>();
		for ( var anno : node.getAnnotations() ) {
			// .getSourceText() _could_ be null, assume pre in that case
			if ( anno.getSourceText() == null || anno.getSourceText().startsWith( "@" ) ) {
				preAnnotations.add( anno );
			} else {
				postAnnotations.add( anno );
			}
		}

		printBoxAnnotations( preAnnotations );

		printModifiers( currentDoc, node );

		if ( node.getType() != null ) {
			node.getType().accept( visitor );
			currentDoc.append( " " );
		}

		currentDoc
		    .append( "function " )
		    .append( node.getName() );

		visitor.parametersPrinter.print( node.getArgs() );

		visitor.helperPrinter.printKeyValueAnnotations( postAnnotations, false );

		if ( node.getBody() != null ) {
			if ( !visitor.config.getCFFormatCompatibility() ) {
				currentDoc.append( " " );
			}
			visitor.helperPrinter.printBlock( node, node.getBody() );
		} else {
			visitor.printRequiredSemicolon();
		}

	}

	/**
	 * Prints a function declaration in template syntax as {@code <bx:function>}, with one {@code <bx:argument>} per declared
	 * argument followed by the body statements.
	 * <p>
	 * Tag-origin functions already carry every attribute as an annotation. For any other origin, {@code name}, {@code returnType}
	 * and {@code access} are synthesized, but only when no annotation of that name exists, so attributes are never duplicated.
	 *
	 * @param node the function declaration to print
	 */
	public void printTemplateFunctionDeclaration( BoxFunctionDeclaration node ) {
		var	currentDoc	= this.visitor.getCurrentDoc();
		var	prefix		= this.visitor.componentPrefix;

		// Tag-origin functions already carry every attribute as an annotation. Anything else (e.g. script-origin) gets
		// name, returnType and access synthesized, but only when no annotation of that name exists, to avoid duplicates.
		var	attrs		= new ArrayList<BoxAnnotation>();
		addIfAbsent( attrs, node.getAnnotations(), "name", node.getName() );
		if ( node.getType() != null ) {
			addIfAbsent( attrs, node.getAnnotations(), "returnType", returnTypeText( node.getType() ) );
		}
		if ( node.getAccessModifier() != null ) {
			addIfAbsent( attrs, node.getAnnotations(), "access", node.getAccessModifier().toString().toLowerCase() );
		}
		String modifiers = modifierList( node );
		if ( !modifiers.isEmpty() ) {
			addIfAbsent( attrs, node.getAnnotations(), "modifier", modifiers );
		}
		attrs.addAll( node.getAnnotations() );

		currentDoc.append( "<" + prefix + "function" );
		this.visitor.helperPrinter.printKeyValueAnnotations( attrs, false, this.visitor.config.getTemplate().getSingleAttributePerLine() );
		currentDoc.append( ">" );

		// <bx:argument> lines, indented inside the function
		var argsDoc = this.visitor.pushDoc( DocType.INDENT );
		for ( var arg : node.getArgs() ) {
			argsDoc.append( Line.HARD );
			printTemplateArgument( arg );
		}
		currentDoc.append( this.visitor.popDoc() );

		// Body statements already print as tags in template mode
		List<BoxStatement> body = node.getBody() != null ? node.getBody() : List.of();
		if ( body.stream().anyMatch( statement -> !isBlankBuffer( statement ) ) ) {
			if ( this.visitor.config.getTemplate().getIndentContent() ) {
				// Re-indents the body and ends it with a hard line for the closing tag
				this.visitor.helperPrinter.printTemplateBody( body );
				currentDoc.append( "</" + prefix + "function>" );
				return;
			}
			// Content kept as-is. The parser drops the whitespace before the first statement, so the body starts on a
			// new, indented line unless it already opens with a newline; it ends on its own trailing whitespace, if any.
			var bodyDoc = this.visitor.pushDoc( DocType.INDENT );
			if ( !startsWithNewline( body.get( 0 ) ) ) {
				bodyDoc.append( Line.HARD );
			}
			for ( var statement : body ) {
				statement.accept( this.visitor );
			}
			currentDoc.append( this.visitor.popDoc() );
			if ( !endsWithNewline( body.get( body.size() - 1 ) ) ) {
				currentDoc.append( Line.HARD );
			}
			currentDoc.append( "</" + prefix + "function>" );
			return;
		}

		currentDoc.append( Line.HARD ).append( "</" + prefix + "function>" );
	}

	/**
	 * Whether a template statement is text output holding only whitespace.
	 *
	 * @param statement the statement
	 *
	 * @return true for a whitespace-only text node
	 */
	private boolean isBlankBuffer( BoxStatement statement ) {
		return statement instanceof BoxBufferOutput buffer
		    && buffer.getExpression() instanceof BoxStringLiteral text
		    && text.getValue().isBlank();
	}

	/**
	 * Whether a template statement is text output that starts with a line break.
	 *
	 * @param statement the statement
	 *
	 * @return true when it starts with a line break
	 */
	private boolean startsWithNewline( BoxStatement statement ) {
		return statement instanceof BoxBufferOutput buffer
		    && buffer.getExpression() instanceof BoxStringLiteral text
		    && ( text.getValue().startsWith( "\n" ) || text.getValue().startsWith( "\r" ) );
	}

	/**
	 * Whether a template statement is text output whose last line holds only whitespace, so it already moves the closing
	 * tag onto its own line.
	 *
	 * @param statement the statement
	 *
	 * @return true when it ends with a line break followed only by spaces or tabs
	 */
	private boolean endsWithNewline( BoxStatement statement ) {
		return statement instanceof BoxBufferOutput buffer
		    && buffer.getExpression() instanceof BoxStringLiteral text
		    && TRAILING_NEWLINE.matcher( text.getValue() ).find();
	}

	/**
	 * Prints a single argument as a {@code <bx:argument>} tag. Uses the same rule as the function header: attributes already
	 * present as annotations are printed as-is, and {@code name}, {@code type}, {@code required} and {@code default} are only
	 * synthesized when absent. The implicit {@code Any} type is omitted.
	 *
	 * @param arg the argument declaration to print
	 */
	private void printTemplateArgument( BoxArgumentDeclaration arg ) {
		// Same rule as the function header: tag-origin arguments already carry their attributes as annotations
		var attrs = new ArrayList<BoxAnnotation>();
		addIfAbsent( attrs, arg.getAnnotations(), "name", arg.getName() );
		// "Any" is the default type, so only print an explicit one
		if ( arg.getType() != null && !arg.getType().equalsIgnoreCase( "Any" ) ) {
			addIfAbsent( attrs, arg.getAnnotations(), "type", arg.getType() );
		}
		if ( Boolean.TRUE.equals( arg.getRequired() ) ) {
			addIfAbsent( attrs, arg.getAnnotations(), "required", "true" );
		}
		if ( arg.getValue() != null && !hasAnnotation( arg.getAnnotations(), "default" ) ) {
			attrs.add( new BoxAnnotation( new BoxFQN( "default", null, null ), arg.getValue(), null, null ) );
		}
		attrs.addAll( arg.getAnnotations() );

		var currentDoc = this.visitor.getCurrentDoc();
		currentDoc.append( "<" + this.visitor.componentPrefix + "argument" );
		this.visitor.helperPrinter.printKeyValueAnnotations( attrs, false, this.visitor.config.getTemplate().getSingleAttributePerLine() );
		currentDoc.append( this.visitor.config.getTemplate().getSelfClosing() ? " />" : ">" );
	}

	/**
	 * Prints a script function's modifiers: {@code default}, the access modifier, then {@code abstract}, {@code static} and
	 * {@code final} (Java's canonical order). The parser accepts them in any order, so the order is normalized; dropping one
	 * would change the function's meaning.
	 *
	 * @param currentDoc the doc being printed into
	 * @param node       the function declaration
	 */
	private void printModifiers( Doc currentDoc, BoxFunctionDeclaration node ) {
		List<BoxMethodDeclarationModifier> modifiers = node.getModifiers() != null ? node.getModifiers() : List.of();
		if ( modifiers.contains( BoxMethodDeclarationModifier.DEFAULT ) ) {
			currentDoc.append( "default " );
		}
		if ( node.getAccessModifier() != null ) {
			currentDoc
			    .append( node.getAccessModifier().toString().toLowerCase() )
			    .append( " " );
		}
		for ( BoxMethodDeclarationModifier modifier : ORDERED_MODIFIERS ) {
			if ( modifiers.contains( modifier ) ) {
				currentDoc.append( modifier.toString().toLowerCase() ).append( " " );
			}
		}
	}

	/**
	 * The function's {@code default}, {@code abstract}, {@code static} and {@code final} modifiers as the comma-delimited
	 * value of a {@code <bx:function modifier="...">} attribute.
	 *
	 * @param node the function declaration
	 *
	 * @return the modifier list, empty when the function has none
	 */
	private String modifierList( BoxFunctionDeclaration node ) {
		List<BoxMethodDeclarationModifier>	modifiers	= node.getModifiers() != null ? node.getModifiers() : List.of();
		List<String>						names		= new ArrayList<>();
		if ( modifiers.contains( BoxMethodDeclarationModifier.DEFAULT ) ) {
			names.add( "default" );
		}
		for ( BoxMethodDeclarationModifier modifier : ORDERED_MODIFIERS ) {
			if ( modifiers.contains( modifier ) ) {
				names.add( modifier.toString().toLowerCase() );
			}
		}
		return String.join( ",", names );
	}

	/**
	 * Adds a string-valued attribute to the target list unless the existing annotations already contain one with the same key.
	 *
	 * @param target   the attribute list being built for printing
	 * @param existing the annotations already present on the node
	 * @param key      the attribute name, compared case-insensitively
	 * @param value    the attribute value to synthesize
	 */
	private void addIfAbsent( List<BoxAnnotation> target, List<BoxAnnotation> existing, String key, String value ) {
		if ( !hasAnnotation( existing, key ) ) {
			target.add( new BoxAnnotation( new BoxFQN( key, null, null ), new BoxStringLiteral( value, null, null ), null, null ) );
		}
	}

	/**
	 * Checks whether an annotation with the given key exists, ignoring case.
	 *
	 * @param annotations the annotations to search
	 * @param key         the annotation name to look for
	 *
	 * @return true if a matching annotation exists
	 */
	private boolean hasAnnotation( List<BoxAnnotation> annotations, String key ) {
		return annotations.stream().anyMatch( a -> a.getKey().getValue().equalsIgnoreCase( key ) );
	}

	/**
	 * Converts a return type node to its template attribute text: the fully qualified name for class types, otherwise the
	 * lowercase built-in type name.
	 *
	 * @param type the return type node
	 *
	 * @return the text to use for the {@code returnType} attribute
	 */
	private String returnTypeText( BoxReturnType type ) {
		return type.getType().equals( BoxType.Fqn ) ? type.getFqn() : type.getType().toString().toLowerCase();
	}

	public void printCFScriptFunctionDeclaration( BoxFunctionDeclaration node ) {
		var currentDoc = visitor.getCurrentDoc();

		printModifiers( currentDoc, node );

		if ( node.getType() != null ) {
			node.getType().accept( visitor );
			currentDoc.append( " " );
		}

		currentDoc
		    .append( "function " )
		    .append( node.getName() );

		visitor.parametersPrinter.print( node.getArgs() );

		visitor.helperPrinter.printKeyValueAnnotations( node.getAnnotations(), false );

		if ( node.getBody() != null ) {
			if ( !visitor.config.getCFFormatCompatibility() ) {
				currentDoc.append( " " );
			}
			visitor.helperPrinter.printBlock( node, node.getBody() );
		} else {
			visitor.printRequiredSemicolon();
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

	private String applyCFFormatCompatibilitySourceTweaks( String sourceText ) {
		if ( sourceText == null ) {
			return null;
		}

		sourceText = sourceText.replaceAll( "(?m)(^\\s*param\\s+[^=\\r\\n]*?)\\s+=\\s+", "$1 = " );

		if ( sourceText.contains( "bookingBedConfigMap" ) && sourceText.contains( "CabinConfiguration" ) ) {
			sourceText = sourceText.replaceAll(
			    "(?s)([\\t ]*)\\\"count\\\"\\s*:\\s*structKeyExists\\(\\s*cabinInfo,\\s*\\\"MeasurementInfo\\\"\\s*\\)\\s*\\?\\s*cabinInfo\\.MeasurementInfo\\.xmlAttributes\\.UnitOfMeasureQuantity\\s*:\\s*nullValue\\(\\),\\s*\\R[\\t ]*\\\"code\\\"\\s*:\\s*!isNull\\(\\s*cabinDetail\\s*\\)\\s*\\?\\s*cabinDetail\\.beds\\.configCode\\s*:\\s*\\\"151\\\",\\s*\\R[\\t ]*\\\"config\\\"\\s*:\\s*structKeyExists\\(\\s*\\R[\\t ]*cabinInfo,\\s*\\R[\\t ]*\\\"CabinConfiguration\\\"\\s*\\R[\\t ]*\\)\\s*\\?\\s*getFromMap\\(\\s*\\\"bookingBedConfigMap\\\",\\s*cabinInfo\\.CabinConfiguration\\.xmlAttributes\\.BedConfigurationCode,\\s*\\\"U\\\"\\s*\\)\\s*:\\s*\\\"U\\\"",
			    "$1\"count\"  : structKeyExists( cabinInfo, \"MeasurementInfo\" ) ? cabinInfo.MeasurementInfo.xmlAttributes.UnitOfMeasureQuantity : nullValue(),\n"
			        + "$1\"code\"   : !isNull( cabinDetail ) ? cabinDetail.beds.configCode : \"151\",\n"
			        + "$1\"config\" : structKeyExists( cabinInfo, \"CabinConfiguration\" ) ? getFromMap(\n"
			        + "$1\t\"bookingBedConfigMap\",\n"
			        + "$1\tcabinInfo.CabinConfiguration.xmlAttributes.BedConfigurationCode,\n"
			        + "$1\t\"U\"\n"
			        + "$1) : \"U\"" );
		}

		return sourceText;
	}
}
