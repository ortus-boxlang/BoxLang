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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.UDF;

import java.util.List;

public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	public BoxFunctionDeclarationTransformer(AsmTranspiler transpiler) {
    	super(transpiler);
    }
	// @formatter:on
	@Override
	public List<AbstractInsnNode> transform(BoxNode node ) throws IllegalStateException {
		BoxFunctionDeclaration function			= ( BoxFunctionDeclaration ) node;

		Type type = Type.getType("L" + transpiler.getProperty("packageName").replace('.', '/')
			+ "/" + transpiler.getProperty("classname")
			+ "$Func_" + function.getName() + ";");

		ClassNode classNode = new ClassNode();
		AsmHelper.init( classNode, type, UDF.class );
		transpiler.setAuxiliary( type.getClassName(), classNode );


		AsmHelper.invokeWithContextAndClassLocator(classNode, methodVisitor -> {
			for ( BoxStatement statement : function.getBody() ) {
				transpiler.transform( statement ).forEach(methodInsNode -> methodInsNode.accept(methodVisitor));
			};
		});

		AsmHelper.complete( classNode, type, visitor -> {} );

		// TODO: function specific attributes.

		return List.of(
			new VarInsnNode(Opcodes.ALOAD, 1),
			new MethodInsnNode(Opcodes.INVOKESTATIC,
				type.getInternalName(),
				"getInstance",
				Type.getMethodDescriptor(type),
				false),
			new MethodInsnNode(Opcodes.INVOKEINTERFACE,
				Type.getInternalName(IBoxContext.class),
				"registerUDF",
				Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(UDF.class)),
				true)
		);
	}
}
