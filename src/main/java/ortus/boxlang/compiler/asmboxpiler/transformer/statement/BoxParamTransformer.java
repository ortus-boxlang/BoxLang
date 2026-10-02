/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxParam;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

public class BoxParamTransformer extends AbstractTransformer {

	public BoxParamTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) {
		if ( node instanceof BoxComponent component ) {
			return transformComponent( component, context, returnContext );
		}
		BoxParam			boxParam	= ( BoxParam ) node;
		List<BoxAnnotation>	attrs		= new ArrayList<BoxAnnotation>();
		attrs.add(
		    new BoxAnnotation(
		        new BoxFQN( "name",
		            boxParam.getVariable().getPosition(),
		            boxParam.getVariable().getSourceText() ),
		        boxParam.getVariable(),
		        boxParam.getVariable().getPosition(),
		        boxParam.getVariable().getSourceText()
		    )
		);
		if ( boxParam.getType() != null ) {
			attrs.add(
			    new BoxAnnotation(
			        new BoxFQN( "type",
			            boxParam.getType().getPosition(),
			            boxParam.getType().getSourceText() ),
			        boxParam.getType(),
			        boxParam.getType().getPosition(),
			        boxParam.getType().getSourceText()
			    )
			);
		}
		if ( boxParam.getDefaultValue() != null ) {
			attrs.add(
			    new BoxAnnotation(
			        new BoxFQN( "default",
			            boxParam.getDefaultValue().getPosition(),
			            boxParam.getDefaultValue().getSourceText() ),
			        boxParam.getDefaultValue(),
			        boxParam.getDefaultValue().getPosition(),
			        boxParam.getDefaultValue().getSourceText()
			    )
			);
		}
		// Delegate to the component transformer
		return transformComponent( new BoxComponent( "param", attrs, node.getPosition(), node.getSourceText() ), context, returnContext );
	}

	private List<AbstractInsnNode> transformComponent( BoxComponent component, TransformerContext context, ReturnValueContext returnContext ) {
		List<BoxAnnotation> attributes = new ArrayList<>();
		for ( BoxAnnotation attribute : component.getAttributes() ) {
			if ( !attribute.getKey().getValue().equalsIgnoreCase( "default" ) ) {
				attributes.add( attribute );
				continue;
			}
			BoxExpression value = attribute.getValue();
			if ( value instanceof BoxStringInterpolation interpolation && interpolation.getValues().size() == 1 ) {
				value = interpolation.getValues().get( 0 );
			} else if ( value == null ) {
				value = new BoxBooleanLiteral( true, attribute.getPosition(), attribute.getSourceText() );
			}
			BoxClosure deferred = new BoxClosure( List.of(), List.of(),
			    new BoxReturn( value, value.getPosition(), value.getSourceText() ), value.getPosition(), value.getSourceText() );
			attributes.add( new BoxAnnotation( attribute.getKey(), deferred, attribute.getPosition(), attribute.getSourceText() ) );
		}
		BoxComponent wrapped = new BoxComponent( component.getName(), attributes, component.getBody(), component.getSourceStartIndex(),
		    component.getPosition(), component.getSourceText() );
		// Delegate directly so the wrapped AST is not routed through this transformer again.
		return new BoxComponentTransformer( this.transpiler ).transform( wrapped, context, returnContext );
	}
}
