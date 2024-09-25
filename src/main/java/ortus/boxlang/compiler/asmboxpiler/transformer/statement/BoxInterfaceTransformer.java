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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxInterfaceTransformer {

	public static final Type	CLASS_TYPE					= Type.getType( Class.class );
	public static final Type	CLASS_ARRAY_TYPE			= Type.getType( Class[].class );

	private static final String	EXTENDS_ANNOTATION_MARKER	= "overrideJava";

	@SuppressWarnings( "unchecked" )
	public static ClassNode transpile( Transpiler transpiler, BoxInterface boxClass ) throws BoxRuntimeException {
		Source	source			= boxClass.getPosition().getSource();
		String	sourceType		= transpiler.getProperty( "sourceType" );

		String	filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath()
		    : "unknown";
		String	fileName		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String	boxPackageName	= transpiler.getProperty( "boxPackageName" );
		String	rawBoxClassName	= boxPackageName + "." + fileName.replace( ".bx", "" ).replace( ".cfc", "" ), boxClassName;
		// trim leading . if exists
		if ( rawBoxClassName.startsWith( "." ) ) {
			boxClassName = rawBoxClassName.substring( 1 );
		} else {
			boxClassName = rawBoxClassName;
		}
		transpiler.setProperty( "boxClassName", boxClassName );
		String	mappingName		= transpiler.getProperty( "mappingName" );
		String	mappingPath		= transpiler.getProperty( "mappingPath" );
		String	relativePath	= transpiler.getProperty( "relativePath" );

		Type	type			= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" ) + ";" );
		transpiler.setProperty( "classType", type.getDescriptor() );
		transpiler.setProperty( "classTypeInternal", type.getInternalName() );

		List<Type>	interfaces	= new ArrayList<>();

		ClassNode	classNode	= new ClassNode();

		AsmHelper.init( classNode, false, type, Type.getType( ortus.boxlang.runtime.runnables.BoxInterface.class ), methodVisitor -> {

		}, interfaces.toArray( Type[]::new ) );

		addGetInstance( classNode, type );

		return classNode;
	}

	private static void addGetInstance( ClassVisitor classVisitor, Type type ) {
		FieldVisitor fieldVisitor = classVisitor.visitField(
		    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
		    "instance",
		    type.getDescriptor(),
		    null,
		    null );
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod(
		    Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_STATIC,
		    "getInstance",
		    Type.getMethodDescriptor( type, Type.getType( IBoxContext.class ) ),
		    null,
		    null );
		methodVisitor.visitCode();
		Label after = new Label();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitJumpInsn( Opcodes.IFNONNULL, after );
		methodVisitor.visitTypeInsn( Opcodes.NEW, type.getInternalName() );
		methodVisitor.visitInsn( Opcodes.DUP );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    type.getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false );
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitLabel( after );
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}
}
