package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;

import java.util.*;

public abstract class Transpiler implements ITranspiler {

	private final HashMap<String, String>	properties	= new HashMap<String, String>();
	private Map<String, BoxExpression>		keys		= new LinkedHashMap<String, BoxExpression>();
	private Map<String, ClassNode>			auxiliaries	= new LinkedHashMap<String, ClassNode>();
	private int lambdaCounter = 0;

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

	public abstract List<AbstractInsnNode> transform( BoxNode node );

	public int registerKey( BoxExpression key ) {
		String name;
		if ( key instanceof BoxStringLiteral str ) {
			name = str.getValue();
		} else if ( key instanceof BoxIntegerLiteral intr ) {
			name = intr.getValue();
		} else {
			throw new IllegalStateException( "Key must be a string or integer literal" );
		}
		// check if exists
		if ( keys.containsKey( name ) ) {
			return new ArrayList<>( keys.keySet() ).indexOf( name );
		}
		keys.put( name, key );
		return keys.size() - 1;
	}

	public Map<String, BoxExpression> getKeys() {
		return keys;
	}

	public Map<String, ClassNode> getAuxiliary() {
		return auxiliaries;
	}

	public void setAuxiliary( String name, ClassNode classNode ) {
		if ( auxiliaries.putIfAbsent( name, classNode ) != null ) {
			throw new IllegalArgumentException( "Auxiliary already registered: " + name );
		}
	}
	public int incrementAndGetLambdaCounter() {
		return ++lambdaCounter;
	}
}
