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

import com.github.javaparser.resolution.Context;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.UDF;

import java.util.List;

public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	// @formatter:off
	private String registrationTemplate = "${contextName}.registerUDF( ${enclosingClassName}.${className}.getInstance() );";
	private String classTemplate = """
		package ${packageName};

		public class ${classname} extends UDF {
			private static ${classname}				instance;
			private final static Key				name		= ${functionName};
			private final static Argument[]			arguments	= new Argument[] {};
			private final static String				returnType	= "${returnType}";
			private              Access		   		access		= Access.${access};

			private final static IStruct	annotations;
			private final static IStruct	documentation;

			private static final long					compileVersion	= ${compileVersion};
			private static final LocalDateTime			compiledOn		= ${compiledOnTimestamp};
			private static final Object					ast				= null;

			public Key getName() {
				return name;
			}
			public Argument[] getArguments() {
				return arguments;
			}
			public String getReturnType() {
				return returnType;
			}

			public Access getAccess() {
   				return access;
   			}

			public  long getRunnableCompileVersion() {
				return ${className}.compileVersion;
			}

			public LocalDateTime getRunnableCompiledOn() {
				return ${className}.compiledOn;
			}

			public Object getRunnableAST() {
				return ${className}.ast;
			}

			private ${classname}() {
				super();
			}

			public static synchronized ${classname} getInstance() {
				if ( instance == null ) {
					instance = new ${classname}();
				}
				return instance;
			}

			@Override
			public IStruct getAnnotations() {
				return annotations;
			}

			@Override
			public IStruct getDocumentation() {
				return documentation;
			}

			public List<ImportDefinition> getImports() {
				return imports;
			}

			public Path getRunnablePath() {
				return ${enclosingClassName}.path;
			}

			/**
			 * The original source type
			 */
			public BoxSourceType getSourceType() {
				return ${enclosingClassName}.sourceType;
			}

			@Override
			public Object _invoke( FunctionBoxContext context ) {
				ClassLocator classLocator = ClassLocator.getInstance();

			}
		}
 	""";
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

		// TODO: define function similar to other things

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
