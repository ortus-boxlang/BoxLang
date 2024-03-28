package ortus.boxlang.compiler.parser;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

import ortus.boxlang.parser.antlr.CFScriptLexer;

/**
 * Recover form errors having Javadoc in body
 * NOTE THIS CLASS IS NOT CURRENLTY IN USE. IT'S COMMENTED OUT IN BOXABSTRACTPARSER
 */
public class ParserErrorStrategy extends DefaultErrorStrategy {

	@Override
	protected void reportUnwantedToken( Parser recognizer ) {
		if ( inErrorRecoveryMode( recognizer ) ) {
			return;
		}
		Token t = recognizer.getCurrentToken();
		if ( t.getType() != CFScriptLexer.JAVADOC_COMMENT ) {
			super.reportUnwantedToken( recognizer );
		}
	}

	@Override
	protected Token singleTokenDeletion( Parser recognizer ) {
		return super.singleTokenDeletion( recognizer );
	}

	@Override
	protected void reportNoViableAlternative( Parser recognizer, NoViableAltException e ) {
		if ( e.getOffendingToken().getType() != CFScriptLexer.JAVADOC_COMMENT ) {
			super.reportNoViableAlternative( recognizer, e );
		}
	}

	@Override
	protected void reportInputMismatch( Parser recognizer, InputMismatchException e ) {
		super.reportInputMismatch( recognizer, e );
	}
}
