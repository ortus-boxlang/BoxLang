package ortus.boxlang.runtime.types.exceptions;

import java.util.List;
import java.util.stream.Collectors;

import ortus.boxlang.ast.Issue;

/**
 * Thrown when a scope is not found
 */
public class ParseException extends BoxRuntimeException {

    List<Issue> issues;

    /**
     * Constructor
     *
     * @param message The message to display
     */
    public ParseException( List<Issue> issues ) {
        super( "Error compiling source. " + issuesAsString( issues ) );

        this.issues       = issues;
        this.extendedInfo = issuesAsString( issues );
    }

    /**
     * Constructor
     *
     * @param message The message to display
     * @param cause   The cause
     */
    public ParseException( String message, Throwable cause ) {
        super( message, cause );
    }

    public static String issuesAsString( List<Issue> issues ) {
        return issues.stream()
            .map( Issue::toString )
            .collect( Collectors.joining( "\n" ) );
    }

}
