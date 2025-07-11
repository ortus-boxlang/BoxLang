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

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
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
		var currentDoc = visitor.getCurrentDoc();

		printImports( classNode.getImports() );
		visitor.printPreComments( classNode );

		// TODO: need to separate pre and inline annotations in AST
		printBoxAnnotations( classNode.getAnnotations() );

		currentDoc.append( "class {" );

		visitor.pushDoc( DocType.INDENT ).append( Line.HARD );
		printProperties( classNode.getProperties() );
		visitor.helperPrinter.printStatements( classNode.getBody() );
		visitor.printInsideComments( classNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( Line.HARD )
		    .append( "}" );

		visitor.printPostComments( classNode );
	}

	private void printBoxInterface( BoxInterface interfaceNode ) {
		var currentDoc = visitor.getCurrentDoc();

		printImports( interfaceNode.getImports() );
		visitor.printPreComments( interfaceNode );

		printBoxAnnotations( interfaceNode.getAnnotations() );

		currentDoc.append( "interface" );
		visitor.helperPrinter.printKeyValueAnnotations( interfaceNode.getPostAnnotations(), true );
		currentDoc.append( "{" );

		visitor.pushDoc( DocType.INDENT ).append( Line.HARD );
		visitor.helperPrinter.printStatements( interfaceNode.getBody() );
		visitor.printInsideComments( interfaceNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( Line.HARD )
		    .append( "}" );

		visitor.printPostComments( interfaceNode );
	}

	public void printCFScriptComponent( BoxClass classNode ) {
		var currentDoc = visitor.getCurrentDoc();

		printImports( classNode.getImports() );
		visitor.printPreComments( classNode );
		currentDoc.append( "component" );
		visitor.helperPrinter.printKeyValueAnnotations( classNode.getAnnotations(), true );
		currentDoc.append( "{" );

		visitor.pushDoc( DocType.INDENT ).append( Line.HARD );
		printProperties( classNode.getProperties() );
		visitor.helperPrinter.printStatements( classNode.getBody() );
		visitor.printInsideComments( classNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( Line.HARD )
		    .append( "}" );

		visitor.printPostComments( classNode );
	}

	public void printCFScriptInterface( BoxInterface interfaceNode ) {
		var currentDoc = visitor.getCurrentDoc();

		printImports( interfaceNode.getImports() );
		visitor.printPreComments( interfaceNode );
		currentDoc.append( "interface" );
		visitor.helperPrinter.printKeyValueAnnotations( interfaceNode.getAllAnnotations(), true );
		currentDoc.append( "{" );

		visitor.pushDoc( DocType.INDENT ).append( Line.HARD );
		visitor.helperPrinter.printStatements( interfaceNode.getBody() );
		visitor.printInsideComments( interfaceNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( Line.HARD )
		    .append( "}" );

		visitor.printPostComments( interfaceNode );
	}

	public void printCFTemplateComponent( BoxClass classNode ) {
		var currentDoc = visitor.getCurrentDoc();

		currentDoc.append( "<cfcomponent" );
		visitor.helperPrinter.printKeyValueAnnotations( classNode.getAnnotations(), false );
		currentDoc.append( ">" );

		var bodyDoc = visitor.pushDoc( DocType.INDENT );
		for ( var property : classNode.getProperties() ) {
			bodyDoc.append( Line.HARD );
			property.accept( visitor );
		}
		for ( var statement : classNode.getBody() ) {
			statement.accept( visitor );
		}
		visitor.printInsideComments( classNode, false );

		currentDoc
		    .append( visitor.popDoc() )
		    .append( "</cfcomponent>" );

		visitor.printPostComments( classNode );
	}

	public void printCFTemplateInterface( BoxInterface interfaceNode ) {
		var currentDoc = visitor.getCurrentDoc();

		currentDoc.append( "<cfinterface" );
		visitor.helperPrinter.printKeyValueAnnotations( interfaceNode.getAnnotations(), false );
		currentDoc.append( ">" );

		visitor.pushDoc( DocType.INDENT );
		for ( var statement : interfaceNode.getBody() ) {
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
		var currentDoc = visitor.getCurrentDoc();
		for ( var property : properties ) {
			property.accept( visitor );
			currentDoc.append( Line.HARD );
		}
		if ( properties.size() > 0 ) {
			currentDoc.append( Line.HARD );
		}
	}
}
