package ortus.boxlang.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyDefinition;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.util.FRTransService;

public abstract class Boxpiler implements IBoxpiler {

	/**
	 * Logger
	 */
	protected static final Logger		logger			= LoggerFactory.getLogger( JavaBoxpiler.class );
	/**
	 * Keeps track of the classes we've compiled
	 */
	protected Map<String, ClassInfo>	classPool		= new HashMap<>();
	/**
	 * The transaction service used to track subtransactions
	 */
	protected FRTransService			frTransService	= FRTransService.getInstance();
	/**
	 * The disk class util
	 */
	protected DiskClassUtil				diskClassUtil;
	/**
	 * The directory where the generated classes are stored
	 */
	protected Path						classGenerationDirectory;

	public Boxpiler() {
		this.classGenerationDirectory	= Paths.get( BoxRuntime.getInstance().getConfiguration().compiler.classGenerationDirectory );
		this.diskClassUtil				= new DiskClassUtil( classGenerationDirectory );
		this.classGenerationDirectory.toFile().mkdirs();

		// If we are in debug mode, let's clean out the class generation directory
		if ( BoxRuntime.getInstance().inDebugMode() && Files.exists( this.classGenerationDirectory ) ) {
			try {
				logger.atDebug().log( "Running in debugmode, first startup cleaning out class generation directory: " + classGenerationDirectory );
				// if ( false )
				FileUtils.cleanDirectory( classGenerationDirectory.toFile() );
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Error cleaning out class generation directory on first run", e );
			}
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	public Map<String, ClassInfo> getClassPool() {
		return classPool;
	}

	/**
	 * Parse source text into BoxLang AST nodes. This method will NOT throw an exception if the parse fails.
	 *
	 * @param source The source to parse.
	 * @param type   The BoxSourceType of the source.
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	@Override
	public ParsingResult parse( String source, BoxSourceType type ) {
		DynamicObject	trans	= frTransService.startTransaction( "BL Source Parse", type.name() );
		Parser			parser	= new Parser();
		try {
			return parser.parse( source, type );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		} finally {
			frTransService.endTransaction( trans );
		}
	}

	/**
	 * Parse a file on disk into BoxLang AST nodes. This method will throw an exception if the parse fails.
	 *
	 * @param file The file to parse
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	@Override
	public ParsingResult parseOrFail( File file ) {
		return validateParse( parse( file ), file.toString() );
	}

	/**
	 * Parse a file on disk into BoxLang AST nodes. This method will NOT throw an exception if the parse fails.
	 *
	 * @param file The file to parse
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	@Override
	public ParsingResult parse( File file ) {
		DynamicObject	trans	= frTransService.startTransaction( "BL File Parse", file.toString() );
		Parser			parser	= new Parser();
		try {
			return parser.parse( file );
		} finally {
			frTransService.endTransaction( trans );
		}
	}

	/**
	 * Parse source text into BoxLang AST nodes. This method will throw an exception if the parse fails.
	 *
	 * @param source The source to parse.
	 * @param type   The BoxSourceType of the source.
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	@Override
	public ParsingResult parseOrFail( String source, BoxSourceType type ) {
		return validateParse( parse( source, type ), "ad-hoc source" );
	}

	/**
	 * Validate a parsing result and throw an exception if the parse failed.
	 *
	 * @param result The parsing result to validate
	 *
	 * @return The parsing result if the parse was successful
	 */
	@Override
	public ParsingResult validateParse( ParsingResult result, String source ) {
		if ( !result.isCorrect() ) {
			throw new ParseException( result.getIssues(), source );
		}
		return result;
	}

	/**
	 * Compile a single BoxLang statement into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 * @param type   The type of BoxLang source code
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IBoxRunnable> compileStatement( String source, BoxSourceType type ) {
		ClassInfo classInfo = ClassInfo.forStatement( source, type, this );
		classPool.putIfAbsent( classInfo.FQN(), classInfo );
		classInfo = classPool.get( classInfo.FQN() );

		return classInfo.getDiskClass();

	}

	/**
	 * Compile a BoxLang script into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 * @param type   The type of BoxLang source code
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IBoxRunnable> compileScript( String source, BoxSourceType type ) {
		ClassInfo classInfo = ClassInfo.forScript( source, type, this );
		classPool.putIfAbsent( classInfo.FQN(), classInfo );
		classInfo = classPool.get( classInfo.FQN() );

		return classInfo.getDiskClass();
	}

	/**
	 * Compile a BoxLang template (file on disk) into a Java class
	 *
	 * @param path        The BoxLang source on disk as a Path
	 * @param packagePath The package path used to resolve this file path
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IBoxRunnable> compileTemplate( Path path, String packagePath ) {
		ClassInfo classInfo = ClassInfo.forTemplate( path, packagePath, Parser.detectFile( path.toFile() ), this );
		classPool.putIfAbsent( classInfo.FQN(), classInfo );
		// If the new class is newer than the one on disk, recompile it
		if ( classPool.get( classInfo.FQN() ).lastModified() < classInfo.lastModified() ) {
			try {
				// Don't know if this does anything, but calling it for good measure
				classPool.get( classInfo.FQN() ).getClassLoader().close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			classPool.put( classInfo.FQN(), classInfo );
			compileClassInfo( classInfo.FQN() );
		} else {
			classInfo = classPool.get( classInfo.FQN() );
		}
		return classInfo.getDiskClass();
	}

	/**
	 * Compile a BoxLang Class from source into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IClassRunnable> compileClass( String source, BoxSourceType type ) {
		ClassInfo classInfo = ClassInfo.forClass( source, type, this );
		classPool.putIfAbsent( classInfo.FQN(), classInfo );
		classInfo = classPool.get( classInfo.FQN() );

		return classInfo.getDiskClassClass();
	}

	/**
	 * Compile a BoxLang Class from a file into a Java class
	 *
	 * @param path        The BoxLang source code as a Path on disk
	 * @param packagePath The package path representing the mapping used to resolve this class
	 *
	 * @return The loaded class
	 */
	public Class<IClassRunnable> compileClass( Path path, String packagePath ) {
		ClassInfo classInfo = ClassInfo.forClass( path, packagePath.replace( "-", "_" ), Parser.detectFile( path.toFile() ), this );
		classPool.putIfAbsent( classInfo.FQN(), classInfo );
		// If the new class is newer than the one on disk, recompile it
		if ( classPool.get( classInfo.FQN() ).lastModified() < classInfo.lastModified() ) {
			try {
				// Don't know if this does anything, but calling it for good measure
				classPool.get( classInfo.FQN() ).getClassLoader().close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			classPool.put( classInfo.FQN(), classInfo );
			compileClassInfo( classInfo.FQN() );
		} else {
			classInfo = classPool.get( classInfo.FQN() );
		}
		return classInfo.getDiskClassClass();
	}

	@Override
	public Class<IProxyRunnable> compileInterfaceProxy( IBoxContext context, InterfaceProxyDefinition definition ) {
		ClassInfo classInfo = ClassInfo.forInterfaceProxy( definition.name(), definition, this );
		classPool.putIfAbsent( classInfo.FQN(), classInfo );
		classInfo = classPool.get( classInfo.FQN() );

		return classInfo.getDiskClassProxy();

	}

	@Override
	public SourceMap getSourceMapFromFQN( String FQN ) {
		return diskClassUtil.readLineNumbers( IBoxpiler.getBaseFQN( FQN ) );
	}
}
