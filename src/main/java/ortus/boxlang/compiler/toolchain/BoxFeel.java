package ortus.boxlang.compiler.toolchain;

import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptGrammarBaseListener;

public class BoxFeel extends BoxScriptGrammarBaseListener {

	private final TypeResolver typeResolver = new TypeResolver();

	@Override
	public void exitExprAdd(BoxScriptGrammar.ExprAddContext ctx) {

		// In the case of addition, we ask the type resolver if the types are compatible
		// otherwise we raise an issue that can be used for further reporting etc
		if (!ctx.accept(typeResolver)
				.isNumeric()) {

			// This is a type error - here we print as an example, but later we will
			// raise true issues
			System.out.println("Type error: incompatible types for addition");
		}
	}

}
