package ortus.boxlang.compiler.asmboxpiler.transformer;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.runtime.scopes.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractTransformer implements Transformer {

	protected Transpiler	transpiler;
	protected Logger		logger;

	public AbstractTransformer( Transpiler transpiler ) {
		this.transpiler	= transpiler;
		this.logger		= LoggerFactory.getLogger( this.getClass() );
	}

	protected List<AbstractInsnNode> createKey( BoxExpression expr ) {
		// If this key is a literal, we can optimize it
		if ( expr instanceof BoxStringLiteral || expr instanceof BoxIntegerLiteral ) {
			int pos = transpiler.registerKey( expr );
			// Instead of Key.of(), we'll reference a static array of pre-created keys on the class
			return List.of( new FieldInsnNode(
			    Opcodes.GETSTATIC,
			    transpiler.getProperty( "packageName" ).replace( '.', '/' )
			        + "/"
			        + transpiler.getProperty( "classname" ),
			    "keys",
			    Type.getDescriptor( Key[].class ) ), new LdcInsnNode( pos ), new InsnNode( Opcodes.AALOAD ) );
		} else {
			// TODO: likely needs to retain return type info on transformed expression or extract from "expr"
			// Dynamic values will be created at runtime
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( transpiler.transform( expr ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Key.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( String.class ) ),
			    false ) );
			return nodes;
		}
	}

	protected List<AbstractInsnNode> createKey( String expr ) {
		return createKey( new BoxStringLiteral( expr, null, expr ) );
	}
}
