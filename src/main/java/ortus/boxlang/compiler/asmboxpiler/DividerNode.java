package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;

public class DividerNode extends InsnNode {

	public DividerNode() {
		super( Opcodes.NOP );
	}

}
