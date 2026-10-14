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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import com.github.javaparser.ast.Node;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxFQNTransformer extends AbstractTransformer {

	public BoxFQNTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a fully qualified name into its string value.
	 * <p>
	 * Every OTHER consumer of a {@link BoxFQN} node (BoxNewTransformer, BoxStaticMethodInvocationTransformer,
	 * BoxStaticAccessTransformer, BoxTryTransformer's catch type, etc.) special-cases {@code instanceof BoxFQN}
	 * directly and reads {@link BoxFQN#getValue()} as a plain string - this generic transform() is only ever
	 * reached when a BoxFQN appears as an ordinary sub-expression via generic dispatch, with no such special
	 * case above it (e.g. the right-hand side of "instanceof"/"as" in BoxBinaryOperationTransformer, which
	 * always calls {@code InstanceOf.invoke}/{@code CastAs.invoke} - both of which StringCaster.cast() their
	 * "type name" argument, so they need a String value here, not a bare Java identifier). Previously emitted
	 * a raw {@code NameExpr(boxFQN.getValue())}, generating invalid Java source for any unqualified name (e.g.
	 * "String" alone is not a legal value expression - it resolves as an undeclared variable, "cannot find
	 * symbol") - a latent, general (not Groovy-specific) java-backend bug, first exercised by this Groovy
	 * parser's own instanceof-shaped smart-switch desugaring. Now mirrors the ASM backend's own BoxFQNTransformer,
	 * which has always pushed a plain string constant here.
	 *
	 * @param node    a BoxFQN instance
	 * @param context transformation context
	 *
	 * @return A Java string literal expression for the FQN's text
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxFQN boxFQN = ( BoxFQN ) node;
		return BoxStringLiteralTransformer.transform( boxFQN.getValue() );
	}
}