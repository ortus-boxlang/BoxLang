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
package ortus.boxlang.transpiler.transformer.statement;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NullLiteralExpr;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxThrow;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxThrowTransformer extends AbstractTransformer {

	public BoxThrowTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a throw statement
	 *
	 * @param node    a BoxThrow instance
	 * @param context transformation context
	 *
	 * @return Generates a throw
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxThrow	boxThrow		= ( BoxThrow ) node;
		BoxExpr		object			= boxThrow.getExpression();
		BoxExpr		type			= boxThrow.getType();
		BoxExpr		message			= boxThrow.getMessage();
		BoxExpr		detail			= boxThrow.getDetail();
		BoxExpr		errorcode		= boxThrow.getErrorCode();
		BoxExpr		extendedinfo	= boxThrow.getExtendedInfo();

		Expression	expr			= null;

		if ( object != null ) {
			expr = ( Expression ) transpiler.transform( boxThrow.getExpression(), TransformerContext.RIGHT );
		} else {
			expr = new NullLiteralExpr();
		}

		if ( message != null ) {
			Expression	jType			= new NullLiteralExpr();
			Expression	jMessage		= new NullLiteralExpr();
			Expression	jDetail			= new NullLiteralExpr();
			Expression	jErrorcode		= new NullLiteralExpr();
			Expression	jExtendedinfo	= new NullLiteralExpr();
			if ( type != null ) {
				jType = ( Expression ) transpiler.transform( type );
			}
			if ( message != null ) {
				jMessage = ( Expression ) transpiler.transform( message );
			}
			if ( detail != null ) {
				jDetail = ( Expression ) transpiler.transform( detail );
			}
			if ( errorcode != null ) {
				jErrorcode = ( Expression ) transpiler.transform( errorcode );
			}
			if ( extendedinfo != null ) {
				jExtendedinfo = ( Expression ) transpiler.transform( extendedinfo );
			}

			Map<String, String> values = new HashMap<>();
			values.put( "expr", expr.toString() );
			values.put( "contextName", transpiler.peekContextName() );
			values.put( "type", jType.toString() );
			values.put( "message", jMessage.toString() );
			values.put( "detail", jDetail.toString() );
			values.put( "errorcode", jErrorcode.toString() );
			values.put( "extendedinfo", jExtendedinfo.toString() );

			String template = """
			                  new CustomException(
			                  	${message} == null ? null : StringCaster.cast(${message}),
			                  	${detail} == null ? null : StringCaster.cast(${detail}),
			                  	${errorcode} == null ? null : StringCaster.cast(${errorcode}),
			                  	${type} == null ? null : StringCaster.cast(${type}),
			                  	${extendedinfo},
			                  	(Throwable)DynamicObject.unWrap( ${expr} )
			                  )
			                  """;
			expr = ( Expression ) parseExpression( template, values );

		} else {
			// If no message was provided, there had better be an exeption object
			if ( object == null ) {
				String template = "new CustomException(null, null, null, null, null, null)";
				expr = ( Expression ) parseExpression( template, new HashMap<>() );
			}
		}

		Map<String, String> values = new HashMap<>();
		values.put( "expr", expr.toString() );
		values.put( "contextName", transpiler.peekContextName() );

		String	template	= "ExceptionUtil.throwException(${expr});";
		Node	javaStmt	= parseStatement( template, values );
		logger.atTrace().log( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
