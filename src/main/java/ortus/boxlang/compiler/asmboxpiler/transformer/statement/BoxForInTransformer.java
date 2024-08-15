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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker.VarStore;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxAssignmentTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.runtime.dynamic.casters.CollectionCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;

public class BoxForInTransformer extends AbstractTransformer {

	public BoxForInTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnValueContext ) {
		BoxForIn						forIn			= ( BoxForIn ) node;
		List<AbstractInsnNode>			nodes			= new ArrayList<>();
		Optional<MethodContextTracker>	trackerOption	= transpiler.getCurrentMethodContextTracker();

		if ( trackerOption.isEmpty() ) {
			throw new IllegalStateException();
		}

		MethodContextTracker	tracker		= trackerOption.get();

		LabelNode				loopStart	= new LabelNode();
		LabelNode				loopEnd		= new LabelNode();

		// access the collection
		nodes.addAll( transpiler.transform( forIn.getExpression(), context ) );
		// unwrap it
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( DynamicObject.class ),
		    "unWrap",
		    Type.getMethodDescriptor(
		        Type.getType( Object.class ),
		        Type.getType( Object.class )
		    ),
		    false
		) );

		// store it
		VarStore collectionVar = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( collectionVar.nodes() );

		// track if it is a query
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		nodes.add( new TypeInsnNode( Opcodes.INSTANCEOF, Type.getType( Query.class ).getInternalName() ) );

		VarStore isQueryVar = tracker.storeNewVariable( Opcodes.ISTORE );
		nodes.addAll( isQueryVar.nodes() );

		// determine if it is a struct
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		nodes.add( new TypeInsnNode( Opcodes.INSTANCEOF, Type.getType( Struct.class ).getInternalName() ) );

		VarStore isStructVar = tracker.storeNewVariable( Opcodes.ISTORE );
		nodes.addAll( isStructVar.nodes() );

		// need to register query loop

		// create iterator
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		// CollectionCaster.cast( ${collectionName} ).iterator();
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( CollectionCaster.class ),
		    "cast",
		    Type.getMethodDescriptor(
		        Type.getType( Collection.class ),
		        Type.getType( Object.class )
		    ),
		    false
		) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( Iterable.class ),
		    "iterator",
		    Type.getMethodDescriptor(
		        Type.getType( Iterator.class )
		    ),
		    true
		) );
		VarStore iteratorVar = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( iteratorVar.nodes() );

		// push two nulls onto the stack in order to initialize our strategy for keeping the stack height consistent
		// this is to allow the statement to return an expression in the case of a BoxScript execution
		if ( returnValueContext == ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.add( loopStart );

		// every iteration we will swap the values and pop in order to remove the older value
		if ( returnValueContext == ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.SWAP ) );
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		nodes.add( new VarInsnNode( Opcodes.ALOAD, iteratorVar.index() ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( Iterator.class ),
		    "hasNext",
		    Type.getMethodDescriptor(
		        Type.BOOLEAN_TYPE
		    ),
		    true
		) );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, loopEnd ) );

		// assign the variable
		nodes.addAll( assignVar( forIn, iteratorVar.index(), context ) );
		nodes.add( new InsnNode( Opcodes.POP ) );

		nodes.addAll( transpiler.transform( forIn.getBody(), context, returnValueContext ) );

		nodes.add( new JumpInsnNode( Opcodes.GOTO, loopStart ) );
		// increment query loop
		nodes.add( loopEnd );

		return nodes;
	}

	private List<AbstractInsnNode> assignVar( BoxForIn forIn, int iteratorIndex, TransformerContext context ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();

		nodes.add( new VarInsnNode( Opcodes.ALOAD, iteratorIndex ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( Iterator.class ),
		    "next",
		    Type.getMethodDescriptor(
		        Type.getType( Object.class )
		    ),
		    true
		) );

		List<BoxAssignmentModifier> modifiers = new ArrayList<BoxAssignmentModifier>();
		if ( forIn.getHasVar() ) {
			modifiers.add( BoxAssignmentModifier.VAR );
		}

		return new BoxAssignmentTransformer( ( AsmTranspiler ) transpiler ).transformEquals(
		    forIn.getVariable(),
		    nodes,
		    BoxAssignmentOperator.Equal,
		    modifiers );
	}

}