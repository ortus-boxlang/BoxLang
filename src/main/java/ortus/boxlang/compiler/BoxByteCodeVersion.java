package ortus.boxlang.compiler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention( RetentionPolicy.CLASS )
public @interface BoxByteCodeVersion {

	/**
	 * More informational than anything. Stores the version of BoxLang that produced the bytecode.
	 */
	String boxlangVersion();

	/**
	 * The bytecode version used when compiling this class. We will increment this value any time we make breaking changes to the bytecode format
	 * or to the portions of the runtime that the bytecode directly interacts with.
	 */
	int bytecodeVersion();
}