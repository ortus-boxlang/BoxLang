package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.io.PrintStream;
import java.io.PrintWriter;
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
		doCompileClassInfo( classInfo, (fqn, node) -> node.accept(new TraceClassVisitor( null, new PrintWriter( target ) )));
	}

	@Override
	public void compileClassInfo( String FQN ) {
		ClassInfo classInfo = classPool.get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}

		doCompileClassInfo( classInfo, (fqn, node) ->  {
			ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			node.accept(new TraceClassVisitor( new CheckClassAdapter(classWriter), new PrintWriter( System.out) )); // TODO: remove tracer
//			node.accept(new TraceClassVisitor( classWriter, new PrintWriter( System.out) )); // TODO: remove tracer
			byte[] bytes = classWriter.toByteArray();
			diskClassUtil.writeBytes( fqn, "class", bytes );
		});
	}

	private void doCompileClassInfo(ClassInfo classInfo, BiConsumer<String, ClassNode> consumer ) {
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

		consumer.accept( classInfo.FQN(), transpiler.transpile((BoxScript) result.getRoot()) );
		transpiler.getAuxiliary().forEach(consumer);
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
