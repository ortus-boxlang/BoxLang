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
package ortus.boxlang.compiler.javaboxpiler.transformer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;

public class BoxStaticInitializerTransformer extends AbstractTransformer {

	/**
	 * Constructor
	 *
	 * @param transpiler parent transpiler
	 */
	public BoxStaticInitializerTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStaticInitializer	staticInitializer	= ( BoxStaticInitializer ) node;
		BlockStmt				block				= new BlockStmt();
		staticInitializer.getBody().forEach( stmt -> block.addStatement( ( Statement ) transpiler.transform( stmt ) ) );
		transpiler.addStaticInitializer( block );

		addIndex( block, node );
		return new EmptyStmt();
	}

}
