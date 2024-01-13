package ortus.boxlang.runtime.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.IntStream;

import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public final class EncodingUtil {

	public static String checksum( Path filePath ) {
		return checksum( filePath, "MD5" );
	}

	public static String checksum( Path filePath, String algorithm ) {
		StringBuilder result = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance( algorithm.toUpperCase() );
			md.update( Files.readAllBytes( filePath ) );
			byte[] digest = md.digest();
			IntStream
			    .range( 0, digest.length )
			    .forEach( idx -> result.append( String.format( "%02x", digest[ idx ] ) ) );
			return result.toString();
		} catch ( NoSuchAlgorithmException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The algorithm [%s] provided is not a valid digest algorithm.",
			        algorithm.toUpperCase()
			    )
			);
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

}
