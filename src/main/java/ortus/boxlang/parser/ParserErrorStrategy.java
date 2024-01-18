package ortus.boxlang.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;
import ortus.boxlang.parser.antlr.CFLexer;

/**
 * Recover form errors having Javadoc in body
 */
public class ParserErrorStrategy extends DefaultErrorStrategy {

	@Override
	protected void reportUnwantedToken( Parser recognizer ) {
		if ( inErrorRecoveryMode( recognizer ) ) {
			return;
		}
		Token t = recognizer.getCurrentToken();
		if ( t.getType() != CFLexer.JAVADOC_COMMENT ) {
			super.reportUnwantedToken( recognizer );
		}
	}

	@Override
	protected Token singleTokenDeletion( Parser recognizer ) {
		return super.singleTokenDeletion( recognizer );
	}

	@Override
	protected void reportNoViableAlternative( Parser recognizer, NoViableAltException e ) {
		if ( e.getOffendingToken().getType() != CFLexer.JAVADOC_COMMENT ) {
			super.reportNoViableAlternative( recognizer, e );
		}
	}

	@Override
	protected void reportInputMismatch( Parser recognizer, InputMismatchException e ) {
		super.reportInputMismatch( recognizer, e );
	}
}
