
package ortus.boxlang.runtime.bifs.global.encryption;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.IntStream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF
@BoxBIF( alias = "Hash40" )
@BoxMember( type = BoxLangType.STRING, name = "hash" )
// Note: We need to comment these out for now because declaring a member function on more than one seems to confuse the runtime, at times
@BoxMember( type = BoxLangType.STRUCT, name = "hash" )
@BoxMember( type = BoxLangType.ARRAY, name = "hash" )
@BoxMember( type = BoxLangType.DATETIME, name = "hash" )

public class Hash extends BIF {

	private static final String		DEFAULT_ALGORITHM	= "MD5";
	private static final String		DEFAULT_ENCODING	= "utf-8";
	private static final Integer	DEFAULT_ITERATIONS	= 1;

	// The hash item object - non-local so we can reassign it in streams
	private static Object			hashItem			= null;

	/**
	 * Constructor
	 */
	public Hash() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.input ),
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
		Integer	iterations		= arguments.getAsInteger( Key.numIterations );
		String	algorithm		= arguments.getAsString( Key.algorithm );

		Key		bifMethodKey	= arguments.getAsKey( __functionName );
		if ( bifMethodKey.equals( Key.hash40 ) ) {
			algorithm = "SHA1";
		}

		if ( hashItem instanceof String ) {
			hashItem = arguments
			    .getAsString( Key.input )
			    .getBytes(
			        Charset.forName( arguments.getAsString( Key.encoding ) )
			    );
		}

		/**
		 * @note We perform this here rather than in the util, because the algorithm
		 *       should really be used for cryptographic soundness. This iterative re-digesting is janky
		 *
		 * @TODO Deprecate this functionality in a future release
		 */
		if ( iterations > 1 ) {
			hashItem = hashItem.toString().getBytes();
			try {
				MessageDigest md = MessageDigest.getInstance( algorithm.toUpperCase() );
				IntStream
				    .range( 1, iterations )
				    .forEach( iteration -> hashItem = md.digest( ( byte[] ) hashItem ) );

			} catch ( NoSuchAlgorithmException e ) {

				throw new BoxRuntimeException(
				    String.format(
				        "The algorithm [%s] provided is not a valid digest algorithm.",
				        algorithm.toUpperCase()
				    )
				);

			}
		}

		return EncryptionUtil.hash( hashItem, algorithm );

	}

}
