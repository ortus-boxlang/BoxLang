package ortus.boxlang.compiler.asmboxpiler;

import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.io.PrintStream;

public class ASMBoxpiler extends Boxpiler {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static ASMBoxpiler instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private ASMBoxpiler() {
		super();
	}

	/**
	 * Get the singleton instance
	 *
	 * @return TemplateLoader
	 */
	public static synchronized ASMBoxpiler getInstance() {
		if ( instance == null ) {
			instance = new ASMBoxpiler();
		}
		return instance;
	}

	@Override
	public Class<IBoxRunnable> compileStatement( String source, BoxSourceType type ) {
		ClassInfo classInfo = ClassInfo.forStatement( source, type, this );
		classPool.putIfAbsent( classInfo.FQN(), classInfo );
		classInfo = classPool.get( classInfo.FQN() );

		return classInfo.getDiskClass();

	}

	@Override
	public void printTranspiledCode( ParsingResult result, ClassInfo classInfo, PrintStream target ) {
		// TODO: Use ASM to create javap-like output.
		target.println( "Placeholder for " + classInfo.toString() );
	}

	@Override
	public void compileClassInfo( String FQN ) {
		ClassInfo classInfo = classPool.get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}

		// This is the entry point for generating bytecode before this function returns it should generate a class file

		// This result holds the AST we want to convert to bytecode
		ParsingResult	result	= parseClassInfo( classInfo );

		// Generate the bytes...
		byte[]			bytes	= new byte[ 256 ];

		// use diskClassUtil to write your class files to the appropriate location
		diskClassUtil.writeBytes( classInfo.FQN(), ".class", bytes );

		throw new UnsupportedOperationException( "Unimplemented method 'generateJavaSource'" );
	}

	private ParsingResult parseClassInfo( ClassInfo info ) {
		if ( info.path() != null ) {
			return parseOrFail( info.path().toFile() );
		} else if ( info.source() != null ) {
			return parseOrFail( info.source(), info.sourceType() );
		}

		return null;
	}

}
