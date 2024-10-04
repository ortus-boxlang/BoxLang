package ortus.boxlang.compiler;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyDefinition;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public interface IBoxpiler {

	static final Set<String> RESERVED_WORDS = new HashSet<>( Arrays.asList( "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
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

	static String getBaseFQN( String FQN ) {
		// If fqn ends with $Cloure_xxx or $Func_xxx, $Lambda_xxx, then we need to strip that off to get the original FQN
		Matcher m = Pattern.compile( "(.*?)(\\$Closure_.*|\\$Func_.*|\\$Lambda_.*)$" ).matcher( FQN );
		if ( m.find() ) {
			FQN = m.group( 1 );
		}
		return FQN;
	}

	void compileClassInfo( String classPoolName, String FQN );

	void clearPagePool();
}
