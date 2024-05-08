package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.BiConsumer;

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
		doCompileClassInfo( transpiler( classInfo ), classInfo, parseClassInfo( classInfo ).getRoot(),
		    ( fqn, node ) -> node.accept( new TraceClassVisitor( null, new PrintWriter( target ) ) ) );
	}

	@Override
	public void compileClassInfo( String FQN ) {
		ClassInfo classInfo = classPool.get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}

		if ( classInfo.resolvedFilePath() != null ) {
			File sourceFile = classInfo.resolvedFilePath().absolutePath().toFile();
			// Check if the source file contains Java bytecode by reading the first few bytes
			if ( diskClassUtil.isJavaBytecode( sourceFile ) ) {
				System.out.println( "Loading bytecode direct from pre-compiled source file for " + FQN );
				classInfo.getClassLoader().defineClasses( FQN, sourceFile );
				return;
			}
			ParsingResult result = parseOrFail( sourceFile );
			doWriteClassInfo( result.getRoot(), classInfo );
		} else if ( classInfo.source() != null ) {
			ParsingResult result = parseOrFail( classInfo.source(), classInfo.sourceType(), classInfo.isClass() );
			doWriteClassInfo( result.getRoot(), classInfo );
		} else if ( classInfo.interfaceProxyDefinition() != null ) {
			throw new UnsupportedOperationException();
		} else {
			throw new BoxRuntimeException( "Unknown class info type: " + classInfo.toString() );
		}
	}

	private void doWriteClassInfo( BoxNode node, ClassInfo classInfo ) {
		doCompileClassInfo( transpiler( classInfo ), classInfo, node, ( fqn, classNode ) -> {
			ClassWriter classWriter = new ClassWriter( ClassWriter.COMPUTE_FRAMES );
			classNode.accept( new CheckClassAdapter( new TraceClassVisitor( classWriter, new PrintWriter( System.out ) ) ) );
			// classNode.accept(classWriter);
			byte[] bytes = classWriter.toByteArray();
			diskClassUtil.writeBytes( fqn, "class", bytes );
		} );
	}

	private static Transpiler transpiler( ClassInfo classInfo ) {
		Transpiler transpiler = Transpiler.getTranspiler();
		transpiler.setProperty( "classname", classInfo.className() );
		transpiler.setProperty( "packageName", classInfo.packageName().toString() );
		transpiler.setProperty( "boxPackageName", classInfo.boxPackageName().toString() );
		transpiler.setProperty( "baseclass", classInfo.baseclass() );
		transpiler.setProperty( "returnType", classInfo.returnType() );
		transpiler.setProperty( "sourceType", classInfo.sourceType().name() );
		transpiler.setProperty( "mappingName", classInfo.resolvedFilePath() == null ? null : classInfo.resolvedFilePath().mappingName() );
		transpiler.setProperty( "mappingPath", classInfo.resolvedFilePath() == null ? null : classInfo.resolvedFilePath().mappingPath() );
		transpiler.setProperty( "relativePath", classInfo.resolvedFilePath() == null ? null : classInfo.resolvedFilePath().relativePath() );
		return transpiler;
	}

	private void doCompileClassInfo( Transpiler transpiler, ClassInfo classInfo, BoxNode node, BiConsumer<String, ClassNode> consumer ) {
		ClassNode classNode;
		if ( node instanceof BoxScript boxScript ) {
			classNode = transpiler.transpile( boxScript );
		} else if ( node instanceof BoxClass boxClass ) {
			classNode = transpiler.transpile( boxClass );
		} else {
			throw new IllegalStateException( "Unexpected root type: " + node );
		}
		transpiler.getAuxiliary().forEach( consumer );
		consumer.accept( classInfo.FQN(), classNode );
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
		throw new UnsupportedOperationException( "Unimplemented method 'compileTemplateBytes'" );
	}
}
