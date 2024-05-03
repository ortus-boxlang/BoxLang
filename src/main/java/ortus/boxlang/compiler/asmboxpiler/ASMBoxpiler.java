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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
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
		doCompileClassInfo( classInfo, parseClassInfo( classInfo ).getRoot(), ( fqn, node ) -> node.accept( new TraceClassVisitor( null, new PrintWriter( target ) ) ) );
	}

	@Override
	public void compileClassInfo( String FQN ) {
		ClassInfo classInfo = classPool.get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}

		if ( classInfo.path() != null ) {
			ParsingResult result = parseOrFail( classInfo.path().toFile() );
			doWriteClassInfo( result.getRoot(), classInfo );
		} else if ( classInfo.source() != null ) {
			ParsingResult result = parseOrFail( classInfo.source(), classInfo.sourceType() );
			doWriteClassInfo( result.getRoot(), classInfo );
		} else if ( classInfo.interfaceProxyDefinition() != null ) {
			throw new UnsupportedOperationException();
		} else {
			throw new BoxRuntimeException( "Unknown class info type: " + classInfo.toString() );
		}
	}

	private void doWriteClassInfo( BoxNode node, ClassInfo classInfo ) {
		doCompileClassInfo( classInfo, node, ( fqn, classNode ) -> {
			ClassWriter classWriter = new ClassWriter( ClassWriter.COMPUTE_FRAMES );
			classNode.accept(classWriter);
			byte[] bytes = classWriter.toByteArray();
			diskClassUtil.writeBytes( fqn, "class", bytes );
		} );
	}

	private void doCompileClassInfo(ClassInfo classInfo, BoxNode node, BiConsumer<String, ClassNode> consumer ) {
		Transpiler transpiler = Transpiler.getTranspiler();
		transpiler.setProperty( "classname", classInfo.className() );
		transpiler.setProperty( "packageName", classInfo.packageName() );
		transpiler.setProperty( "boxPackageName", classInfo.boxPackageName() );
		transpiler.setProperty( "baseclass", classInfo.baseclass() );
		transpiler.setProperty( "returnType", classInfo.returnType() );
		transpiler.setProperty( "sourceType", classInfo.sourceType().name() );

		ClassNode		classNode;
		if ( node instanceof BoxScript boxScript ) {
			classNode = transpiler.transpile( boxScript );
		} else if ( node instanceof BoxClass boxClass ) {
			classNode = transpiler.transpile( boxClass );
		} else {
			throw new IllegalStateException( "Unexpected root type: " + node );
		}
		consumer.accept( classInfo.FQN(), classNode );
		transpiler.getAuxiliary().forEach( consumer );
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
