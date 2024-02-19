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
package ortus.boxlang.transpiler.transformer.statement;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxFQN;
import ortus.boxlang.ast.statement.BoxAnnotation;
import ortus.boxlang.ast.statement.BoxParam;
import ortus.boxlang.ast.statement.component.BoxComponent;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxParamTransformer extends AbstractTransformer {

	public BoxParamTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
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
		return transpiler.transform( new BoxComponent( "param", attrs, node.getPosition(), node.getSourceText() ), context );
	}
}
