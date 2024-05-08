/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxDoTransformer extends AbstractTransformer {

	public BoxDoTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxDo		boxDo			= ( BoxDo ) node;
		Expression	condition		= ( Expression ) transpiler.transform( boxDo.getCondition(), TransformerContext.RIGHT );
		String		doWhileLabel	= "";
		if ( boxDo.getLabel() != null ) {
			doWhileLabel = boxDo.getLabel().toLowerCase();
		}

		String template = "do  {} while(  ${condition}  );";
		if ( requiresBooleanCaster( boxDo.getCondition() ) ) {
			template = "do {} while( BooleanCaster.cast( ${condition} ) );";
		}
		Map<String, String>	values	= new HashMap<>() {

										{
											put( "condition", condition.toString() );
											put( "contextName", transpiler.peekContextName() );
										}
									};
		DoStmt				javaDo	= ( DoStmt ) parseStatement( template, values );

		// May be a single statement or a block statement, which is still a single statement :)
		javaDo.setBody( ( Statement ) transpiler.transform( boxDo.getBody() ) );

		if ( !doWhileLabel.isEmpty() ) {
			LabeledStmt labeledWhile = new LabeledStmt( doWhileLabel, javaDo );
			addIndex( labeledWhile, node );
			return labeledWhile;
		}
		addIndex( javaDo, node );
		return javaDo;
	}
}
