package ortus.boxlang.compiler.asmboxpiler;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.DiskClassUtil;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.visitor.QueryEscapeSingleQuoteVisitor;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService.ExecutorType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.Timer;

public class ASMBoxpiler extends Boxpiler {

	public static final boolean DEBUG = Boolean.getBoolean( "asmboxpiler.debug" );

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	public ASMBoxpiler() {
		super();
	}

	@Override
	public Key getName() {
		return Key.asm;
	}

	@Override
	public void printTranspiledCode( ParsingResult result, ClassInfo classInfo, PrintStream target ) {
		try ( PrintWriter writer = new PrintWriter( target ) ) {
			doCompileClassInfo( transpiler( classInfo ), classInfo, parseClassInfo( classInfo ).getRoot(),
			    ( fqn, node ) -> node.accept( new TraceClassVisitor( null, writer ) ) );
		}
	}

	@Override
	public List<byte[]> compileClassInfo( String classPoolName, String FQN ) {
		Timer timer = null;
		if ( runtime.inDebugMode() ) {
			timer = new Timer();
			timer.start( FQN );
		}
		// logger.debug( "ASM BoxPiler Compiling " + FQN );
		ClassInfo classInfo = getClassPool( classPoolName ).get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}

		List<byte[]> classes;
		if ( classInfo.resolvedFilePath() != null ) {
			File sourceFile = classInfo.resolvedFilePath().absolutePath().toFile();
			// Check if the source file contains Java bytecode by reading the first few bytes
			if ( DiskClassUtil.isJavaByteCode( sourceFile ) ) {
				return classInfo.getClassLoader().defineClasses( FQN, sourceFile, classInfo );
			}
			ParsingResult result = parseOrFail( sourceFile );
			classes = doWriteClassInfo( result.getRoot(), classInfo );
		} else if ( classInfo.source() != null ) {
			ParsingResult result = parseOrFail( classInfo.source(), classInfo.sourceType(), classInfo.isClass() );
			classes = doWriteClassInfo( result.getRoot(), classInfo );
		} else if ( classInfo.interfaceProxyDefinition() != null ) {
			if ( timer != null )
				timer.stop( FQN );
			throw new UnsupportedOperationException();
		} else {
			if ( timer != null )
				timer.stop( FQN );
			throw new BoxRuntimeException( "Unknown class info type: " + classInfo.toString() );
		}

		if ( timer != null ) {
			logger.trace( "ASM BoxPiler Compiled " + FQN + " in " + timer.stop( FQN ) );
		}
		return classes;
	}

	private List<byte[]> doWriteClassInfo( BoxNode node, ClassInfo classInfo ) {
		node.accept( new QueryEscapeSingleQuoteVisitor() );
		final List<byte[]> classes = new ArrayList<>();
		doCompileClassInfo( transpiler( classInfo ), classInfo, node, ( fqn, classNode ) -> {
			ClassWriter classWriter = new ClassWriter( ClassWriter.COMPUTE_FRAMES );
			try {
				if ( DEBUG ) {
					classNode.accept( new CheckClassAdapter( new TraceClassVisitor( classWriter, new PrintWriter( System.out ) ) ) );
				} else {
					classNode.accept( classWriter );
				}
				byte[] bytes = classWriter.toByteArray();
				classes.addFirst( bytes );
				classInfo.getClassLoader().defineClass( fqn, bytes );

				// Are we storing class files on disk?
				if ( runtime.getConfiguration().storeClassFilesOnDisk ) {
					// Run this async
					runtime.getAsyncService().newExecutor( "ASM-disk-class-writer", ExecutorType.VIRTUAL, 0 ).submit( () -> {
						diskClassUtil.writeBytes( classInfo.classPoolName(), fqn, "class", bytes, classInfo.lastModified() );
					} );
				}
			} catch ( Exception e ) {
				StringWriter out = new StringWriter();
				classNode.accept( new CheckClassAdapter( new TraceClassVisitor( classWriter, new PrintWriter( out ) ) ) );

				try {
					e.printStackTrace( new PrintWriter( out ) );
					logger.error( out.toString() );
				} catch ( Exception ex ) {
					logger.error( "Unabel to output ASM error info: " + ex.getMessage() );
				}
				throw e;
			}
		} );
		classes.addFirst( classInfo.fqn().toString().getBytes() );
		return classes;
	}

	private static Transpiler transpiler( ClassInfo classInfo ) {
		Transpiler transpiler = Transpiler.getTranspiler();
		transpiler.setProperty( "classname", classInfo.className() );
		transpiler.setProperty( "packageName", classInfo.packageName().toString() );
		transpiler.setProperty( "boxFQN", classInfo.boxFqn().toString() );
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
		} else if ( node instanceof BoxInterface boxInterface ) {
			classNode = transpiler.transpile( boxInterface );
		} else {
			throw new IllegalStateException( "Unexpected root type: " + node.getClass() + ": " + node );
		}
		transpiler.getAuxiliary().forEach( consumer );
		consumer.accept( classInfo.fqn().toString(), classNode );
	}

	private ParsingResult parseClassInfo( ClassInfo info ) {
		if ( info.resolvedFilePath() != null ) {
			return parseOrFail( info.resolvedFilePath().absolutePath().toFile() );
		} else if ( info.source() != null ) {
			return parseOrFail( info.source(), info.sourceType(), info.isClass() );
		}
		return null;
	}

}
