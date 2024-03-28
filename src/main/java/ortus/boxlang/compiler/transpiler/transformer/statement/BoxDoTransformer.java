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
package ortus.boxlang.compiler.transpiler.transformer.statement;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.transpiler.JavaTranspiler;
import ortus.boxlang.compiler.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.transpiler.transformer.TransformerContext;

public class BoxDoTransformer extends AbstractTransformer {

	public BoxDoTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxDo		boxDo		= ( BoxDo ) node;
		Expression	condition	= ( Expression ) transpiler.transform( boxDo.getCondition(), TransformerContext.RIGHT );

		String		template	= "do  {} while(  ${condition}  );";
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
		BlockStmt			body	= new BlockStmt();
		for ( BoxStatement statement : boxDo.getBody() ) {
			body.getStatements().add( ( Statement ) transpiler.transform( statement ) );
		}
		javaDo.setBody( body );
		return javaDo;
	}
}
