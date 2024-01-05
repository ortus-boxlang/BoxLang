package ortus.boxlang.parser;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
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
}
