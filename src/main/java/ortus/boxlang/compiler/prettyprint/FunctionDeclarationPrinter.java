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

import java.util.List;

import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.parser.BoxSourceType;

public class FunctionDeclarationPrinter {

	private Visitor visitor;

	public FunctionDeclarationPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void print( BoxFunctionDeclaration node, BoxSourceType sourceType ) {
		switch ( sourceType ) {
			case BOXSCRIPT -> printScriptFunctionDeclaration( node );
			case BOXTEMPLATE -> printTemplateFunctionDeclaration( node );
			case CFSCRIPT -> printCFScriptFunctionDeclaration( node );
			case CFTEMPLATE -> printCFTemplateFunctionDeclaration( node );
			default -> {
			}
		}
	}

	public void printScriptFunctionDeclaration( BoxFunctionDeclaration node ) {
		var	currentDoc				= visitor.getCurrentDoc();

		var	defaultInterfaceMethod	= node.getFirstNodeOfType( BoxInterface.class ) != null;

		printBoxAnnotations( node.getAnnotations() );

		if ( defaultInterfaceMethod ) {
			currentDoc.append( "default " );
		}

		if ( node.getAccessModifier() != null ) {
			currentDoc
			    .append( node.getAccessModifier().toString().toLowerCase() )
			    .append( " " );
		}

		if ( node.getType() != null ) {
			node.getType().accept( visitor );
			currentDoc.append( " " );
		}

		currentDoc
		    .append( "function " )
		    .append( node.getName() );

		visitor.parametersPrinter.print( node.getArgs() );

		if ( node.getBody() != null ) {
			currentDoc.append( " " );
			visitor.helperPrinter.printBlock( node, node.getBody() );
		} else {
			currentDoc.append( ";" );
		}

	}

	public void printTemplateFunctionDeclaration( BoxFunctionDeclaration node ) {
	}

	public void printCFScriptFunctionDeclaration( BoxFunctionDeclaration node ) {
		var	currentDoc				= visitor.getCurrentDoc();

		var	defaultInterfaceMethod	= node.getFirstNodeOfType( BoxInterface.class ) != null;

		if ( defaultInterfaceMethod ) {
			currentDoc.append( "default " );
		}

		if ( node.getAccessModifier() != null ) {
			currentDoc
			    .append( node.getAccessModifier().toString().toLowerCase() )
			    .append( " " );
		}

		if ( node.getType() != null ) {
			node.getType().accept( visitor );
			currentDoc.append( " " );
		}

		currentDoc
		    .append( "function " )
		    .append( node.getName() );

		visitor.parametersPrinter.print( node.getArgs() );

		if ( node.getBody() != null ) {
			currentDoc.append( " " );
			visitor.helperPrinter.printBlock( node, node.getBody() );
		} else {
			currentDoc.append( ";" );
		}
	}

	public void printCFTemplateFunctionDeclaration( BoxFunctionDeclaration node ) {
		var currentDoc = visitor.getCurrentDoc();
		currentDoc.append( "<cffunction" );
		visitor.helperPrinter.printKeyValueAnnotations( node.getAnnotations(), false );

		if ( node.getBody() != null ) {
			if ( node.getBody().isEmpty() ) {
				currentDoc.append( "/>" );
			} else {
				currentDoc.append( ">" );
				var bodyDoc = visitor.pushDoc( DocType.INDENT );
				bodyDoc.append( Line.HARD );
				// visitor.helperPrinter.printStatements( node.getBody() );
				for ( var statement : node.getBody() ) {
					statement.accept( visitor );
				}
				currentDoc.append( visitor.popDoc() );
				currentDoc.append( "</cffunction>" );
			}
		} else {
			currentDoc.append( ">" );
		}

		visitor.printPostComments( node );
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
}
