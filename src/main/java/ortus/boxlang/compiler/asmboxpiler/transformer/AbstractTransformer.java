package ortus.boxlang.compiler.asmboxpiler.transformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;

public abstract class AbstractTransformer implements Transformer {

	protected Transpiler transpiler;
	protected Logger logger;

	public AbstractTransformer(Transpiler transpiler) {
		this.transpiler = transpiler;
		this.logger		= LoggerFactory.getLogger( this.getClass() );
	}
}
