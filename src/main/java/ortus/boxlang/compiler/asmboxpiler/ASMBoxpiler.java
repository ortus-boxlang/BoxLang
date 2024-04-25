package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.io.PrintStream;
import java.io.PrintWriter;

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
	public void printTranspiledCode( ParsingResult result, ClassInfo classInfo, PrintStream target ) {
		doCompileClassInfo( classInfo, new TraceClassVisitor( null, new PrintWriter( target ) ) );
	}

	@Override
	public void compileClassInfo( String FQN ) {
		ClassInfo classInfo = classPool.get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}

		ClassWriter classWriter = new ClassWriter( ClassWriter.COMPUTE_FRAMES );

		doCompileClassInfo( classInfo, classWriter );

		byte[] bytes = classWriter.toByteArray();

		diskClassUtil.writeBytes( classInfo.FQN(), "class", bytes );
	}

	private void doCompileClassInfo( ClassInfo classInfo, ClassVisitor classVisitor ) {
		Transpiler transpiler = Transpiler.getTranspiler();
		transpiler.setProperty( "classname", classInfo.className() );
		transpiler.setProperty( "packageName", classInfo.packageName() );
		transpiler.setProperty( "boxPackageName", classInfo.boxPackageName() );
		transpiler.setProperty( "baseclass", classInfo.baseclass() );
		transpiler.setProperty( "returnType", classInfo.returnType() );
		transpiler.setProperty( "sourceType", classInfo.sourceType().name() );

		ParsingResult result = parseClassInfo( classInfo );

		if ( ! ( result.getRoot() instanceof BoxScript ) ) {
			throw new IllegalStateException( "Expected root node to be of type BoxScript" );
		}

		classVisitor = new TraceClassVisitor( new CheckClassAdapter( classVisitor, true ), new PrintWriter( System.out ) ); // TODO: remove

		transpiler.transpile( ( BoxScript ) result.getRoot(), classVisitor );
		classVisitor.visitEnd();
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
