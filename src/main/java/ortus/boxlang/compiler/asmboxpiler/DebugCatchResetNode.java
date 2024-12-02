package ortus.boxlang.compiler.asmboxpiler;

import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

public class DebugCatchResetNode extends AbstractInsnNode {

	public DebugCatchResetNode() {
		super( -1 );
	}

	@Override
	public int getType() {
		return -1;
	}

	@Override
	public void accept( MethodVisitor methodVisitor ) {
		return;
	}

	@Override
	public AbstractInsnNode clone( Map<LabelNode, LabelNode> clonedLabels ) {
		return new DebugCatchResetNode();
	}

}
