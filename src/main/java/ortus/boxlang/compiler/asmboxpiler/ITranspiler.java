package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.ClassVisitor;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public interface ITranspiler {

	void transpile( BoxScript script, ClassVisitor classVisitor ) throws BoxRuntimeException;
}
