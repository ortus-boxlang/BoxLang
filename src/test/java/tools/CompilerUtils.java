package tools;

import ortus.boxlang.runtime.runnables.RunnableLoader;

public class CompilerUtils {

	/**
	 * Boolean test for the presence of the ASMBoxpiler.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean isASMBoxpiler() {
		try {
			var boxpiler = RunnableLoader.getInstance().getBoxpiler();
			return boxpiler instanceof ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler;
		} catch ( Exception e ) {
			return false;
		}
	}
}
