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

	static final Set<String> RESERVED_WORDS = new HashSet<>(
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

		return FQN;
	}

	void compileClassInfo( String classPoolName, String FQN );

	void clearPagePool();

	Key getName();
}
