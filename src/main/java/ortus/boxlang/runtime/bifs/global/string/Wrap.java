package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "Wrap" )
public class Wrap extends BIF {

	public Wrap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.limit ),
		    new Argument( false, "boolean", Key.strip, false )
		};
	}

	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input	= arguments.getAsString( Key.string );
		int		limit	= arguments.getAsInteger( Key.limit );
		boolean	strip	= arguments.getAsBoolean( Key.strip );

		if ( strip ) {
			input = input.replaceAll( "\\r?\\n", " " );
		}

		return wrapText( input, limit );
	}

	private String wrapText( String text, int limit ) {
		if ( text == null ) {
			return "";
		}

		StringBuilder	wrapped	= new StringBuilder();
		int				index	= 0;

		while ( index < text.length() ) {
			// If the remaining text is shorter than the limit, add it all and break
			if ( index + limit > text.length() ) {
				wrapped.append( text, index, text.length() );
				break;
			}

			int spaceToWrapAt = text.lastIndexOf( ' ', index + limit );

			// If there is no acceptable space, force wrap at the limit
			if ( spaceToWrapAt <= index ) {
				wrapped.append( text, index, index + limit ).append( System.lineSeparator() );
				index += limit;
			} else {
				// Else, wrap at the last space within limit
				wrapped.append( text, index, spaceToWrapAt ).append( System.lineSeparator() );
				index = spaceToWrapAt + 1; // skip the space
			}
		}

		return wrapped.toString();
	}
}
