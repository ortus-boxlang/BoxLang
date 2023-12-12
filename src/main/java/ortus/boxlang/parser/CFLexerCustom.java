package ortus.boxlang.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;

import ortus.boxlang.parser.antlr.CFLexer;

public class CFLexerCustom extends CFLexer {

    public CFLexerCustom( CharStream input ) {
        super( input );
    }

    public boolean hasUnpoppedModes() {
        return !_modeStack.isEmpty();
    }

    // get mode stack
    public List<String> getUnpoppedModes() {
        // loop over _modeStack.toArray() and map each mode to its name in a new List
        List<String> results = new ArrayList<String>();
        for ( int mode : _modeStack.toArray() ) {
            results.add( modeNames[ mode ] );
        }
        return results;
    }
}