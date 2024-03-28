package ortus.boxlang.runtime.runnables.compiler;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyDefinition;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public interface IBoxpiler {

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

	/**
	 * Transforms the path into the package name
	 *
	 * @param file File object to grab the package name for.
	 *
	 * @return returns the class name according the name conventions Test.ext -
	 *         Test$ext
	 */
	static String getPackageName( File file ) {
		String packg = file.toString().replace( File.separatorChar + file.getName(), "" );
		if ( packg.startsWith( "/" ) ) {
			packg = packg.substring( 1 );
		}
		// trim trailing period
		if ( packg.endsWith( "." ) ) {
			packg = packg.substring( 0, packg.length() - 1 );
		}
		// trim trailing \ or /
		if ( packg.endsWith( "\\" ) || packg.endsWith( "/" ) ) {
			packg = packg.substring( 0, packg.length() - 1 );
		}
		// TODO: This needs a lot more work. There are tons of disallowed edge cases
		// such as a folder that is a number.
		// We probably need to iterate each path segment and clean or remove as
		// neccessary to make it a valid package name.
		// Also, I'd like cfincluded files to use the relative path as the package name,
		// which will require some refactoring.

		// Take out periods in folder names
		packg	= packg.replaceAll( "\\.", "" );
		// Replace / with .
		packg	= packg.replaceAll( "/", "." );
		// Remove any : from Windows drives
		packg	= packg.replaceAll( ":", "" );
		// Replace \ with .
		packg	= packg.replaceAll( "\\\\", "." );
		// Replace .. with .
		packg	= packg.replaceAll( "\\.\\.", "." );
		// Remove any non alpha-numeric chars.
		packg	= packg.replaceAll( "[^a-zA-Z0-9\\.]", "" );
		return packg.toLowerCase();

	}

	/**
	 * Transforms the filename into the class name
	 *
	 * @param file File object to grab the class name for.
	 *
	 * @return returns the class name according the name conventions Test.ext -
	 *         Test$ext
	 */
	static String getClassName( File file ) {
		String name = file.getName().replace( ".", "$" );
		name = name.substring( 0, 1 ).toUpperCase() + name.substring( 1 );
		return name;
	}

	Map<String, ClassInfo> getClassPool();

	Class<IBoxRunnable> compileStatement( String source, BoxSourceType type );

	Class<IBoxRunnable> compileScript( String source, BoxSourceType type );

	Class<IBoxRunnable> compileTemplate( Path path, String packagePath );

	Class<IClassRunnable> compileClass( String source, BoxSourceType type );

	Class<IClassRunnable> compileClass( Path path, String packagePath );

	Class<IProxyRunnable> compileInterfaceProxy( IBoxContext context, InterfaceProxyDefinition definition );

	ParsingResult parse( File file );

	ParsingResult parse( String source, BoxSourceType type );

	ParsingResult parseOrFail( File file );

	ParsingResult parseOrFail( String source, BoxSourceType type );

	ParsingResult validateParse( ParsingResult result, String source );

	@SuppressWarnings( "unused" )
	String generateJavaSource( BoxNode node, ClassInfo classInfo );

	SourceMap getSourceMapFromFQN( String FQN );

	static String getBaseFQN( String FQN ) {
		// If fqn ends with $Cloure_xxx or $Func_xxx, $Lambda_xxx, then we need to strip that off to get the original FQN
		Matcher m = Pattern.compile( "(.*?)(\\$Closure_.*|\\$Func_.*|\\$Lambda_.*)$" ).matcher( FQN );
		if ( m.find() ) {
			FQN = m.group( 1 );
		}
		return FQN;
	}

	void compileClassInfo( String FQN );
}
