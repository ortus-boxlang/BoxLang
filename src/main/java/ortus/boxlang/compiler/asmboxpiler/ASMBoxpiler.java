package ortus.boxlang.compiler.asmboxpiler;

import java.io.PrintStream;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

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
		ClassInfo	classInfo	= ClassInfo.forStatement( source, type, this );
		var			classPool	= getClassPool( classInfo.classPoolName() );
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
	public void compileClassInfo( String classPoolName, String FQN ) {
		var			classPool	= getClassPool( classPoolName );
		ClassInfo	classInfo	= classPool.get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}

		Transpiler transpiler = Transpiler.getTranspiler();
		transpiler.setProperty( "classname", classInfo.className() );
		transpiler.setProperty( "packageName", classInfo.packageName().toString() );
		transpiler.setProperty( "boxPackageName", classInfo.boxPackageName().toString() );
		transpiler.setProperty( "baseclass", classInfo.baseclass() );
		transpiler.setProperty( "returnType", classInfo.returnType() );
		transpiler.setProperty( "sourceType", classInfo.sourceType().name() );

		ParsingResult	result	= parseClassInfo( classInfo );

		ClassWriter		writer	= new ClassWriter( ClassWriter.COMPUTE_FRAMES );
		// TODO: define method.
		MethodVisitor	visitor	= writer.visitMethod( Opcodes.ACC_PUBLIC, "m", "()V", null, null );
		visitor.visitCode();
		transpiler.transpile( result.getRoot(), visitor );
		visitor.visitEnd();

		byte[] bytes = writer.toByteArray();

		// use diskClassUtil to write your class files to the appropriate location
		diskClassUtil.writeBytes( classPoolName, classInfo.FQN(), ".class", bytes );

		throw new UnsupportedOperationException( "Unimplemented method 'generateJavaSource'" );
	}

	private ParsingResult parseClassInfo( ClassInfo info ) {
		if ( info.resolvedFilePath() != null ) {
			return parseOrFail( info.resolvedFilePath().absolutePath().toFile() );
		} else if ( info.source() != null ) {
			return parseOrFail( info.source(), info.sourceType(), info.isClass() );
		}

		return null;
	}

	@Override
	public List<byte[]> compileTemplateBytes( ResolvedFilePath resolvedFilePath ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'compileTemplateBytes'" );
	}

}
