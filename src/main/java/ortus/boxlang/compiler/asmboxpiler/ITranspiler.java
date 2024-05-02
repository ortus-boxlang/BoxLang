package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.tree.ClassNode;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public interface ITranspiler {

	ClassNode transpile( BoxScript script ) throws BoxRuntimeException;

	ClassNode transpile( BoxClass clazz ) throws BoxRuntimeException;
}
