package ortus.boxlang.compiler.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;

import java.util.Comparator;

public abstract class ParserErrorStrategy extends DefaultErrorStrategy {

	@Override
	protected void reportNoViableAlternative( org.antlr.v4.runtime.Parser recognizer, NoViableAltException e ) {
		TokenStream	tokens	= recognizer.getInputStream();
		String		input;
		if ( tokens != null ) {
			if ( e.getStartToken().getType() == Token.EOF ) {
				input = "<EOF>";
			} else {
				input = tokens.getText( e.getStartToken(), e.getOffendingToken() );
			}
		} else {
			input = "<unknown input>";
		}
		String msg = escapeWSAndQuote( input ) + " is nonsensical here";
		recognizer.notifyErrorListeners( e.getOffendingToken(), msg, e );
	}

	@Override
	protected void reportInputMismatch( org.antlr.v4.runtime.Parser recognizer, InputMismatchException e ) {
		String msg = getTokenErrorDisplay( e.getOffendingToken() ) +
		    " was unexpected " +
		    generateMessage( recognizer, e ) +
		    "\nexpecting one of: " +
		    buildExpectedMessage( recognizer, e.getExpectedTokens() );
		recognizer.notifyErrorListeners( e.getOffendingToken(), msg, e );
	}

	@Override
	protected void reportUnwantedToken( org.antlr.v4.runtime.Parser recognizer ) {
		if ( inErrorRecoveryMode( recognizer ) )
			return;
		beginErrorCondition( recognizer );
		Token		t			= recognizer.getCurrentToken();
		String		tokenName	= getTokenErrorDisplay( t );
		IntervalSet	expecting	= getExpectedTokens( recognizer );
		String		msg			= "unexpected extra input " +
		    tokenName +
		    ' ' +
		    generateMessage( recognizer, new InputMismatchException( recognizer ) ) +
		    "\nexpecting one of: " +
		    buildExpectedMessage( recognizer, expecting );
		recognizer.notifyErrorListeners( t, msg, null );
	}

	@Override
	protected void reportMissingToken( org.antlr.v4.runtime.Parser recognizer ) {
		if ( inErrorRecoveryMode( recognizer ) )
			return;
		beginErrorCondition( recognizer );
		Token		t			= recognizer.getCurrentToken();
		IntervalSet	expecting	= getExpectedTokens( recognizer );
		String		msg			= "missing " +
		    buildExpectedMessage( recognizer, expecting ) +
		    " at " +
		    getTokenErrorDisplay( t ) +
		    '\n' +
		    generateMessage( recognizer, new InputMismatchException( recognizer ) );
		recognizer.notifyErrorListeners( t, msg, null );
	}

	public static final Comparator<String> capitalizedSort = ( a, b ) -> {
		boolean	aHasLower	= a.chars().anyMatch( Character::isLowerCase );
		boolean	bHasLower	= b.chars().anyMatch( Character::isLowerCase );
		if ( aHasLower && !bHasLower )
			return -1;
		if ( !aHasLower && bHasLower )
			return 1;
		return a.compareTo( b );
	};

	protected abstract String generateMessage( org.antlr.v4.runtime.Parser recognizer, RecognitionException e );

	protected abstract String buildExpectedMessage( org.antlr.v4.runtime.Parser recognizer, IntervalSet expected );
}