package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.MethodVisitor;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public interface ITranspiler {

	void transpile(BoxNode node, MethodVisitor visitor ) throws BoxRuntimeException;
}
