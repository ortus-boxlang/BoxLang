
package ortus.boxlang.runtime.bifs.global.encryption;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "Hmac" )

public class Hmac extends BIF {

	private static final String		DEFAULT_ALGORITHM	= "HmacMD5";
	private static final String		DEFAULT_ENCODING	= "utf-8";
	private static final Integer	DEFAULT_ITERATIONS	= 1;

	// The hash item object - non-local so we can reassign it in streams
	private static Object			hashItem			= null;

	/**
	 * Constructor
	 */
	public Hmac() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.input ),
		    new Argument( true, "string", Key.key ),
		    new Argument( false, "string", Key.algorithm, DEFAULT_ALGORITHM ),
		    new Argument( false, "string", Key.encoding, DEFAULT_ENCODING ),
		    new Argument( false, "integer", Key.numIterations, DEFAULT_ITERATIONS )
		};
	}

	/**
	 * Creates an algorithmic hash of an object
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.input The item to be hashed
	 *
	 * @argument.algorithm The supported {@link java.security.MessageDigest } algorithm (case-insensitive)
	 *
	 * @argument.encoding Applicable to strings ( default "utf-8" )
	 *
	 * @argument.iterations The number of iterations to re-digest the object ( default 1 );
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		hashItem = arguments.get( Key.input );
		String	key			= arguments.getAsString( Key.key );
		String	algorithm	= arguments.getAsString( Key.algorithm );
		String	encoding	= arguments.getAsString( Key.encoding );

		return EncryptionUtil.hmac( hashItem, key, algorithm, encoding );
	}
}