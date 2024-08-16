package ortus.boxlang.compiler.asmboxpiler.transformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

public abstract class AbstractTransformer implements Transformer {

	protected Transpiler	transpiler;
	protected Logger		logger;

	public enum ExitsAllowed {
		COMPONENT, LOOP, FUNCTION, DEFAULT
	}

	public AbstractTransformer( Transpiler transpiler ) {
		this.transpiler	= transpiler;
		this.logger		= LoggerFactory.getLogger( this.getClass() );
	}

	@SuppressWarnings( "unchecked" )
	public ExitsAllowed getExitsAllowed( BoxNode node ) {
		BoxNode ancestor = node.getFirstNodeOfTypes( BoxFunctionDeclaration.class, BoxClosure.class, BoxLambda.class, BoxComponent.class, BoxDo.class,
		    BoxForIndex.class, BoxForIn.class,
		    BoxSwitch.class, BoxWhile.class );
		if ( ancestor instanceof BoxFunctionDeclaration || ancestor instanceof BoxClosure || ancestor instanceof BoxLambda ) {
			return ExitsAllowed.FUNCTION;
		} else if ( ancestor instanceof BoxComponent ) {
			return ExitsAllowed.COMPONENT;
		} else if ( ancestor != null ) {
			return ExitsAllowed.LOOP;
		} else {
			return ExitsAllowed.DEFAULT;
		}
	}
}
