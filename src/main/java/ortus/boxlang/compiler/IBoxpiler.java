package ortus.boxlang.compiler;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyDefinition;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public interface IBoxpiler {

	/**
	 * This is the bytecode version used when compiling classes. We will increment this value any time we make breaking changes to the bytecode format
	 * or to the portions of the runtime that the bytecode directly interacts with. It will be baked into every class file we generate and allow us to determine
	 * if a given class file is compatible with the current runtime when loading pre-compiled classes.
	 */
	public static final int		BYTECODE_VERSION	= 1;

	/**
	 * A set of reserved words in BoxLang that cannot be used as identifiers.
	 */
	static final Set<String>	RESERVED_WORDS		= new HashSet<>(
	    Arrays.asList( "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
	        "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements",
	        "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
	        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while" ) );

	/**
	 * Generate an MD5 hash.
	 * TODO: Move to util class
	 *
	 * @param md5 String to hash
	 *
	 * @return MD5 hash
	 */
	static String MD5( String md5 ) {
		try {
			java.security.MessageDigest	md		= java.security.MessageDigest.getInstance( "MD5" );
			byte[]						array	= md.digest( md5.getBytes() );
			StringBuilder				sb		= new StringBuilder();
			for ( byte b : array ) {
				sb.append( Integer.toHexString( ( b & 0xFF ) | 0x100 ).substring( 1, 3 ) );
			}
			return sb.toString();
		} catch ( java.security.NoSuchAlgorithmException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		}
	}

	Map<String, ClassInfo> getClassPool( String classPoolName );

	Class<IBoxRunnable> compileStatement( String source, BoxSourceType type );

	Class<IBoxRunnable> compileScript( String source, BoxSourceType type );

	Class<IBoxRunnable> compileTemplate( ResolvedFilePath resolvedFilePath );

	List<byte[]> compileTemplateBytes( ResolvedFilePath resolvedFilePath );

	Class<IBoxRunnable> compileClass( String source, BoxSourceType type );

	Class<IBoxRunnable> compileClass( ResolvedFilePath resolvedFilePath );

	Class<IProxyRunnable> compileInterfaceProxy( IBoxContext context, InterfaceProxyDefinition definition );

	ParsingResult parse( File file );

	ParsingResult parse( String source, BoxSourceType type, Boolean classOrInterface );

	ParsingResult parseOrFail( File file );

	ParsingResult parseOrFail( String source, BoxSourceType type, Boolean classOrInterface );

	ParsingResult validateParse( ParsingResult result, String source );

	void printTranspiledCode( ParsingResult result, ClassInfo classInfo, PrintStream target );

	SourceMap getSourceMapFromFQN( String FQN );

	/**
	 * Get the base FQN for an inner class
	 * 
	 * @param FQN The full FQN
	 * 
	 * @return The base FQN
	 */
	static String getBaseFQN( String FQN ) {
		// If fqn ends with $Func_xxx, $Closure_xxx, or $Lambda_xxx, then we need to strip that off to get the original FQN
		// Check $Func_ first as it's most common
		int funcIndex = FQN.indexOf( "$Func_" );
		if ( funcIndex != -1 ) {
			return FQN.substring( 0, funcIndex );
		}

		int closureIndex = FQN.indexOf( "$Closure_" );
		if ( closureIndex != -1 ) {
			return FQN.substring( 0, closureIndex );
		}

		int lambdaIndex = FQN.indexOf( "$Lambda_" );
		if ( lambdaIndex != -1 ) {
			return FQN.substring( 0, lambdaIndex );
		}

		// $ComponentBodyLambda_
		int componentBodyLambdaIndex = FQN.indexOf( "$ComponentBodyLambda_" );
		if ( componentBodyLambdaIndex != -1 ) {
			return FQN.substring( 0, componentBodyLambdaIndex );
		}

		// Check for anonymous inner classes (e.g., ClassName$1, ClassName$2, etc.)
		// Look for dollar sign followed by only digits at the end of the string
		int lastDollarIndex = FQN.lastIndexOf( '$' );
		if ( lastDollarIndex != -1 && lastDollarIndex < FQN.length() - 1 ) {
			// Check if everything after the last $ is digits
			boolean allDigits = true;
			for ( int i = lastDollarIndex + 1; i < FQN.length(); i++ ) {
				if ( !Character.isDigit( FQN.charAt( i ) ) ) {
					allDigits = false;
					break;
				}
			}
			if ( allDigits ) {
				return FQN.substring( 0, lastDollarIndex );
			}
		}

		return FQN;
	}

	List<byte[]> compileClassInfo( String classPoolName, String FQN );

	void clearPagePool();

	Key getName();
}
