package ortus.boxlang.transpiler;

import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

/**
 * Results of the Java conversion
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
