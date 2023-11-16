package ortus.boxlang.transpiler;

import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

/**
 * Results of the Java transformation
 * Contains a Java AST for the entry point and a collection of
 * AST for each class such as UDF other callables
 */
public class TranspiledCode {

	private final CompilationUnit		entryPoint;
	private final List<CompilationUnit>	callables;

	/**
	 *
	 * @param script    the compilation unit containing the entry point
	 * @param callables list of
	 */
	public TranspiledCode( CompilationUnit script, List<CompilationUnit> callables ) {
		this.entryPoint	= script;
		this.callables	= callables;
	}

	public CompilationUnit getEntryPoint() {
		return entryPoint;
	}

	public List<CompilationUnit> getCallables() {
		return callables;
	}
}
