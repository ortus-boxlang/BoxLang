
package ortus.boxlang.transpiler.transformer.expression;

import ortus.boxlang.transpiler.Transpiler;

/**
 * Transform a String Interpolatiion the equivalent Java Parser AST nodes
 */
public class BoxStringInterpolationTransformer extends BoxStringConcatTransformer {

	public BoxStringInterpolationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

}
