package ortus.boxlang.transpiler;

import com.github.javaparser.ast.CompilationUnit;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

import java.util.List;

/**
 * BoxLang AST transpiler interface
 */
public interface ITranspiler {

	TranspiledCode transpile( BoxNode node ) throws ApplicationException;

	String compileJava( CompilationUnit cu, String outputPath, List<String> classPath ) throws ApplicationException;

	void run( String fqn, List<String> classPath ) throws Throwable;

}
