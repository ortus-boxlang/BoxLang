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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper.LineNumberIns;
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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CollectionCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;

public class BoxForInTransformer extends AbstractTransformer {

	public BoxForInTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnValueContext ) {
		BoxForIn				forIn	= ( BoxForIn ) node;
		List<AbstractInsnNode>	nodes	= new ArrayList<>();
		AsmHelper.addDebugLabel( nodes, "BoxForIn" );
		Optional<MethodContextTracker> trackerOption = transpiler.getCurrentMethodContextTracker();

		if ( trackerOption.isEmpty() ) {
			throw new IllegalStateException();
		}

		MethodContextTracker	tracker				= trackerOption.get();

		LabelNode				loopStart			= new LabelNode();
		LabelNode				loopEnd				= new LabelNode();
		LabelNode				breakTarget			= new LabelNode();
		LabelNode				continueTarget		= new LabelNode();
		LabelNode				tryStartLabel		= new LabelNode();
		LabelNode				tryEndLabel			= new LabelNode();
		LabelNode				finallyStartLabel	= new LabelNode();
		LabelNode				finallyEndLabel		= new LabelNode();

		tracker.setContinue( forIn, continueTarget );
		tracker.setBreak( forIn, breakTarget );
		if ( forIn.getLabel() != null ) {
			tracker.setStringLabel( forIn.getLabel(), forIn );
		}

		LineNumberIns expressionPos = AsmHelper.translatePosition( forIn.getExpression() );

		nodes.addAll( expressionPos.start() );

		// access the collection
		nodes.addAll( transpiler.transform( forIn.getExpression(), context, ReturnValueContext.VALUE_OR_NULL ) );
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

		nodes.add( new LdcInsnNode( -1 ) );
		VarStore originalQueryIndexVar = tracker.storeNewVariable( Opcodes.ISTORE );
		nodes.addAll( originalQueryIndexVar.nodes() );

		// determine if it is a struct
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		nodes.add( new TypeInsnNode( Opcodes.INSTANCEOF, Type.getType( Struct.class ).getInternalName() ) );

		VarStore isStructVar = tracker.storeNewVariable( Opcodes.ISTORE );
		nodes.addAll( isStructVar.nodes() );

		nodes.add( tryStartLabel );

		// need to register query loop
		// ${contextName}.registerQueryLoop( (Query) ${collectionName}, 0 );
		nodes.add( new VarInsnNode( Opcodes.ILOAD, isQueryVar.index() ) );
		LabelNode endQueryLabel = new LabelNode();
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, endQueryLabel ) );

		// track if query was registered in context
		nodes.addAll( tracker.loadCurrentContext() );
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( Query.class ) ) );
		nodes.add( new LdcInsnNode( -1 ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "getQueryRow",
		    Type.getMethodDescriptor(
		        Type.INT_TYPE,
		        Type.getType( Query.class ),
		        Type.INT_TYPE
		    ),
		    true
		) );
		nodes.addAll( originalQueryIndexVar.nodes() );

		// push context
		nodes.addAll( tracker.loadCurrentContext() );
		// push collection
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( Query.class ) ) );
		// push constant 0
		nodes.add( new LdcInsnNode( 0 ) );
		// invoke regiserQueryLoop
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "registerQueryLoop",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( Query.class ),
		        Type.INT_TYPE
		    ),
		    true
		) );
		nodes.add( endQueryLabel );

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

		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );

		nodes.add( loopStart );

		var varStore = tracker.storeNewVariable( Opcodes.ASTORE );
		// every iteration we will swap the values and pop in order to remove the older value
		nodes.addAll( varStore.nodes() );

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

		nodes.addAll( expressionPos.end() );

		nodes.addAll( transpiler.transform( forIn.getBody(), context, ReturnValueContext.VALUE_OR_NULL ) );

		nodes.add( continueTarget );

		// increment query loop
		nodes.add( new VarInsnNode( Opcodes.ILOAD, isQueryVar.index() ) );
		LabelNode endQueryIncrementLabel = new LabelNode();
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, endQueryIncrementLabel ) );
		// push context
		nodes.addAll( tracker.loadCurrentContext() );
		// push collection
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( Query.class ) ) );
		// invoke regiserQueryLoop
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "incrementQueryLoop",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( Query.class )
		    ),
		    true
		) );
		nodes.add( endQueryIncrementLabel );

		nodes.add( new JumpInsnNode( Opcodes.GOTO, loopStart ) );

		nodes.add( breakTarget );

		nodes.addAll( varStore.nodes() );

		nodes.add( loopEnd );

		// unregister query loop
		nodes.addAll( getUnregisterQueryNodes( collectionVar, isQueryVar, originalQueryIndexVar ) );

		nodes.add( new VarInsnNode( Opcodes.ALOAD, varStore.index() ) );

		if ( returnValueContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

		nodes.add( tryEndLabel );
		nodes.add( finallyStartLabel );

		AsmHelper.addDebugLabel( nodes, "BoxForIn - finally start" );

		nodes.addAll( getUnregisterQueryNodes( collectionVar, isQueryVar, originalQueryIndexVar ) );

		nodes.add( new InsnNode( Opcodes.ATHROW ) );

		nodes.add( finallyEndLabel );

		AsmHelper.addDebugLabel( nodes, "BoxForIn - end" );

		tracker.addTryCatchBlock( new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel, null ) );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}

	private List<AbstractInsnNode> getUnregisterQueryNodes( VarStore collectionVar, VarStore isQueryVar, VarStore originalQueryIndexVar ) {
		List<AbstractInsnNode>	nodes		= new ArrayList<>();
		MethodContextTracker	tracker		= transpiler.getCurrentMethodContextTracker()
		    .orElseThrow( () -> new IllegalStateException( "No current method context tracker found." ) );
		LabelNode				ifLabel		= new LabelNode();
		LabelNode				elseLabel	= new LabelNode();

		// if (isQuery) {
		nodes.add( new VarInsnNode( Opcodes.ILOAD, isQueryVar.index() ) );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, ifLabel ) );

		// if( originalQueryIndexVar > -1 ) {
		nodes.add( new VarInsnNode( Opcodes.ILOAD, originalQueryIndexVar.index() ) );
		nodes.add( new JumpInsnNode( Opcodes.IFLT, elseLabel ) );

		// ${contextName}.registerQueryLoop( (Query) ${collectionName}, ${originalQueryIndexName} );
		nodes.addAll( tracker.loadCurrentContext() );
		// push collection
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( Query.class ) ) );

		nodes.add( new VarInsnNode( Opcodes.ILOAD, originalQueryIndexVar.index() ) );

		// invoke regiserQueryLoop
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "registerQueryLoop",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( Query.class ),
		        Type.INT_TYPE
		    ),
		    true
		) );

		nodes.add( new JumpInsnNode( Opcodes.GOTO, ifLabel ) );

		nodes.add( elseLabel );
		// push context
		nodes.addAll( tracker.loadCurrentContext() );
		// push collection
		nodes.add( new VarInsnNode( Opcodes.ALOAD, collectionVar.index() ) );
		nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( Query.class ) ) );
		// invoke regiserQueryLoop
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "unregisterQueryLoop",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( Query.class )
		    ),
		    true
		) );

		nodes.add( ifLabel );

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