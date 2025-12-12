package ortus.boxlang.compiler.asmboxpiler;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
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
			if ( diskClassUtil.isJavaBytecode( sourceFile ) ) {
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
		final List<byte[]>					classes		= new ArrayList<>();
		final Map<String, ClassNode>		allClasses	= new LinkedHashMap<>();
		final BiConsumer<String, ClassNode>	collector	= ( fqn, classNode ) -> allClasses.put( fqn, classNode );

		// First, collect all ClassNodes (main + auxiliaries) from the transpiler
		doCompileClassInfo( transpiler( classInfo ), classInfo, node, collector );

		// Now process them in two phases to ensure auxiliary classes are defined before the main class

		// Phase 1: Process and define all auxiliary classes (closures, lambdas, etc.)
		// This must happen first so they're available when ASM computes frames for the main class
		String mainClassFqn = classInfo.fqn().toString();
		for ( Map.Entry<String, ClassNode> entry : allClasses.entrySet() ) {
			String		fqn			= entry.getKey();
			ClassNode	classNode	= entry.getValue();

			// Skip the main class in this phase
			if ( fqn.equals( mainClassFqn ) ) {
				continue;
			}

			// Process auxiliary class
			byte[] bytes = convertClassNodeToBytes( fqn, classNode, classInfo );
			classes.addFirst( bytes );
			classInfo.getClassLoader().defineClass( fqn, bytes );

			// Store on disk if configured
			if ( runtime.getConfiguration().storeClassFilesOnDisk ) {
				runtime.getAsyncService().newExecutor( "ASM-disk-class-writer", ExecutorType.VIRTUAL, 0 ).submit( () -> {
					diskClassUtil.writeBytes( classInfo.classPoolName(), fqn, "class", bytes, classInfo.lastModified() );
				} );
			}
		}

		// Phase 2: Process the main class (now that all auxiliary classes are defined)
		ClassNode mainClassNode = allClasses.get( mainClassFqn );
		if ( mainClassNode != null ) {
			byte[] bytes = convertClassNodeToBytes( mainClassFqn, mainClassNode, classInfo );
			classes.addFirst( bytes );
			classInfo.getClassLoader().defineClass( mainClassFqn, bytes );

			// Store on disk if configured
			if ( runtime.getConfiguration().storeClassFilesOnDisk ) {
				runtime.getAsyncService().newExecutor( "ASM-disk-class-writer", ExecutorType.VIRTUAL, 0 ).submit( () -> {
					diskClassUtil.writeBytes( classInfo.classPoolName(), mainClassFqn, "class", bytes, classInfo.lastModified() );
				} );
			}
		}

		// Add the FQN as the first element
		classes.addFirst( classInfo.fqn().toString().getBytes() );
		return classes;
	}

	/**
	 * Convert a ClassNode to bytecode using ASM ClassWriter
	 *
	 * @param fqn       Fully qualified name of the class
	 * @param classNode ASM ClassNode to convert
	 * @param classInfo The ClassInfo containing the ClassLoader to use
	 *
	 * @return Bytecode as byte array
	 */
	private byte[] convertClassNodeToBytes( String fqn, ClassNode classNode, ClassInfo classInfo ) {
		// Capture classInfo's ClassLoader in a local variable so it can be used in the anonymous inner class
		final ClassLoader	boxLangClassLoader	= classInfo.getClassLoader();

		// Use a custom ClassWriter that uses our ClassLoader instead of the system ClassLoader
		// This is critical for finding auxiliary classes (closures) that have been defined in our ClassLoader
		ClassWriter			classWriter			= new ClassWriter( ClassWriter.COMPUTE_FRAMES ) {

													@Override
													protected String getCommonSuperClass( String type1, String type2 ) {
														// Only use custom ClassLoader for classes in the boxgenerated package (closures, etc)
														// For all other classes, use the default implementation to avoid class loading issues
														String	dotType1	= type1.replace( '/', '.' );
														String	dotType2	= type2.replace( '/', '.' );

														if ( dotType1.startsWith( "boxgenerated." ) || dotType2.startsWith( "boxgenerated." ) ) {
															try {
																Class<?>	class1	= Class.forName( dotType1, false, boxLangClassLoader );
																Class<?>	class2	= Class.forName( dotType2, false, boxLangClassLoader );

																if ( class1.isAssignableFrom( class2 ) ) {
																	return type1;
																}
																if ( class2.isAssignableFrom( class1 ) ) {
																	return type2;
																}
																if ( class1.isInterface() || class2.isInterface() ) {
																	return "java/lang/Object";
																}

																do {
																	class1 = class1.getSuperclass();
																} while ( !class1.isAssignableFrom( class2 ) );

																return class1.getName().replace( '.', '/' );
															} catch ( ClassNotFoundException e ) {
																// Fall back to the default implementation if classes can't be loaded
																logger.warn( "ClassNotFoundException in getCommonSuperClass for " + type1 + " and " + type2 + ": "
																    + e.getMessage() );
																return "java/lang/Object";
															}
														} else {
															// Use the default implementation for non-boxgenerated classes
															return super.getCommonSuperClass( type1, type2 );
														}
													}
												};

		try {
			if ( DEBUG ) {
				classNode.accept( new CheckClassAdapter( new TraceClassVisitor( classWriter, new PrintWriter( System.out ) ) ) );
			} else {
				classNode.accept( classWriter );
			}
			return classWriter.toByteArray();
		} catch ( Exception e ) {
			StringWriter out = new StringWriter();
			classNode.accept( new CheckClassAdapter( new TraceClassVisitor( classWriter, new PrintWriter( out ) ) ) );

			try {
				e.printStackTrace( new PrintWriter( out ) );
				logger.error( out.toString() );
			} catch ( Exception ex ) {
				logger.error( "Unable to output ASM error info: " + ex.getMessage() );
			}
			throw e;
		}
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
