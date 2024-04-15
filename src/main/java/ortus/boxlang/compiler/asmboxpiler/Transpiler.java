package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.util.HashMap;

public abstract class Transpiler implements ITranspiler {

	private final HashMap<String, String> properties = new HashMap<String, String>();

	/**
	 * Set a property
	 *
	 * @param key   key of the Property
	 * @param value value of the Property
	 */
	public void setProperty( String key, String value ) {
		properties.put( key, value );
	}

	/**
	 * Get a Propoerty
	 *
	 * @param key key of the Property
	 *
	 * @return the value of the property or null if not defined
	 */
	public String getProperty( String key ) {
		return ( String ) properties.get( key );
	}

	public static Transpiler getTranspiler() {
		return new AsmTranspiler();
	}

	@Override
	public abstract void transpile( BoxScript script, ClassVisitor classVisitor ) throws BoxRuntimeException;

	public abstract AbstractInsnNode transform(BoxNode node );
}
