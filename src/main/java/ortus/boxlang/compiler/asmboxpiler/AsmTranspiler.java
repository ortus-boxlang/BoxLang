package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxWhileTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.util.HashMap;

public class AsmTranspiler extends Transpiler {

	static Logger logger			= LoggerFactory.getLogger( AsmTranspiler.class );

	private static HashMap<Class<?>, Transformer> registry		= new HashMap<>();

	public AsmTranspiler() {
		// TODO: instance write to static field. Seems like an oversight in Java version (retained until clarified).
		registry.put( BoxWhileTransformer.class, new BoxWhileTransformer( this ) );
		registry.put( BoxStringLiteralTransformer.class, new BoxStringLiteralTransformer( this ) );
	}

	@Override
	public void transpile( BoxNode node, MethodVisitor visitor ) throws BoxRuntimeException {
		// TODO: Maybe handle ClassVisitor method/field creation here?
		transform( node, visitor );
	}

	@Override
	public void transform( BoxNode node, MethodVisitor visitor ) {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			transformer.transform( node, visitor );
			logger.atTrace().log( "Transforming {} node with source {} - transformer is {}", transformer.getClass().getSimpleName(), node.getSourceText(), transformer );
			return;
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}
}
