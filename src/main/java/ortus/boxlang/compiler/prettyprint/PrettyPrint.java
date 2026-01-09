package ortus.boxlang.compiler.prettyprint;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public final class PrettyPrint {

	private PrettyPrint() {
		// Prevent instantiation
	}

	public static String prettyPrint( BoxNode node ) {
		return prettyPrint( node, new Config() );
	}

	public static String prettyPrint( BoxNode node, Config config ) {
		var doc = generateDoc( node, config );
		return printDoc( doc, config );
	}

	public static Doc generateDoc( BoxNode node, Config config ) {
		BoxSourceType sourceType;
		if ( node instanceof BoxScript boxScriptNode ) {
			sourceType = boxScriptNode.getBoxSourceType();
		} else if ( node instanceof BoxTemplate boxTemplateNode ) {
			sourceType = boxTemplateNode.getBoxSourceType();
		} else if ( node instanceof BoxClass boxClassNode ) {
			sourceType = boxClassNode.getBoxSourceType();
		} else if ( node instanceof BoxInterface boxInterfaceNode ) {
			sourceType = boxInterfaceNode.getBoxSourceType();
		} else {
			throw new BoxRuntimeException( "Unexpected BoxNode type: " + node.getClass().getName() );
		}
		Visitor visitor = new Visitor( sourceType, config );
		node.accept( visitor );
		var doc = visitor.getRoot();
		doc.condense();
		doc.propagateWillBreak();
		return doc;
	}

	public static String printDoc( Doc doc, Config config ) {
		var printer = new Printer( config );
		return printer.print( doc );
	}

}
