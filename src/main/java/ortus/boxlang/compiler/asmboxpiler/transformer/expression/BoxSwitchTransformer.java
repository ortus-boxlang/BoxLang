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
package ortus.boxlang.compiler.asmboxpiler.transformer.expression;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker.VarStore;
import ortus.boxlang.compiler.asmboxpiler.MethodSplitter;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.types.FlowControlResult;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.util.ListUtil;

public class BoxSwitchTransformer extends AbstractTransformer {

	private static final int	SWITCH_CHUNK_MAX_CASES		= 100;
	private static final int	SWITCH_CHUNK_TARGET_WEIGHT	= 12000;
	private static final Type	CONTEXT_TYPE				= Type.getType( IBoxContext.class );
	private static final Type	OBJECT_TYPE					= Type.getType( Object.class );
	private static final Type	FLOW_CONTROL_RESULT_TYPE	= Type.getType( FlowControlResult.class );

	public BoxSwitchTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxSwitch					boxSwitch		= ( BoxSwitch ) node;
		List<AbstractInsnNode>		condition		= transpiler.transform( boxSwitch.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE );
		MethodContextTracker		tracker			= transpiler.getCurrentMethodContextTracker().get();

		List<BoxSwitchCase>			regularCases	= boxSwitch.getCases().stream().filter( c -> c.getCondition() != null ).toList();
		List<List<BoxSwitchCase>>	caseChunks		= createCaseChunks( regularCases );
		if ( caseChunks.size() > 1 ) {
			return transformChunkedSwitch( boxSwitch, condition, tracker, returnContext, caseChunks );
		}

		List<AbstractInsnNode> inlineNodes = transformInlineSwitch( boxSwitch, condition, tracker, returnContext );
		if ( MethodSplitter.estimateBytecodeSize( inlineNodes ) >= MethodSplitter.BYTECODE_SIZE_LIMIT ) {
			List<List<BoxSwitchCase>> fallbackChunks = createFallbackCaseChunks( regularCases );
			if ( fallbackChunks.size() > 1 ) {
				return transformChunkedSwitch( boxSwitch, condition, tracker, returnContext, fallbackChunks );
			}
		}

		return inlineNodes;
	}

	private List<AbstractInsnNode> transformInlineSwitch(
	    BoxSwitch boxSwitch,
	    List<AbstractInsnNode> condition,
	    MethodContextTracker tracker,
	    ReturnValueContext returnContext ) {

		List<AbstractInsnNode> nodes = new ArrayList<>();
		AsmHelper.addDebugLabel( nodes, "BoxSwitch" );
		nodes.addAll( condition );
		var switchConditionVarStore = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( switchConditionVarStore.nodes() );
		nodes.add( new LdcInsnNode( 0 ) );
		var switchVarStore = tracker.storeNewVariable( Opcodes.ISTORE );
		nodes.addAll( switchVarStore.nodes() );

		LabelNode	endLabel	= new LabelNode();
		LabelNode	breakTarget	= new LabelNode();

		tracker.setBreak( boxSwitch, breakTarget );
		tracker.setContinue( boxSwitch, breakTarget );

		boxSwitch.getCases().forEach( c -> {
			if ( c.getCondition() == null ) {
				return;
			}

			AsmHelper.addDebugLabel( nodes, "BoxSwitch - case start" );
			LabelNode startOfCase = new LabelNode(), endOfCase = new LabelNode(), endOfAll = new LabelNode();
			nodes.add( new VarInsnNode( Opcodes.ILOAD, switchVarStore.index() ) );
			nodes.add( new JumpInsnNode( Opcodes.IFNE, startOfCase ) );
			// this dupes the condition
			// nodes.add( new InsnNode( Opcodes.DUP ) );
			nodes.add( new VarInsnNode( Opcodes.ALOAD, switchConditionVarStore.index() ) );

			if ( c.getDelimiter() == null ) {
				nodes.addAll( transpiler.transform( c.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( EqualsEquals.class ),
				    "invoke",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ), Type.getType( Object.class ) ),
				    false ) );
			} else {
				// If the switch value is null, skip the containsNoCase check and push false
				LabelNode	nullSkipLabel	= new LabelNode();
				LabelNode	afterNullCheck	= new LabelNode();
				nodes.add( new InsnNode( Opcodes.DUP ) );
				nodes.add( new JumpInsnNode( Opcodes.IFNULL, nullSkipLabel ) );

				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( StringCaster.class ),
				    "cast",
				    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
				    false ) );

				nodes.addAll( transpiler.transform( c.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE ) );

				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( StringCaster.class ),
				    "cast",
				    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
				    false ) );

				nodes.add( new InsnNode( Opcodes.SWAP ) );
				nodes.addAll( transpiler.transform( c.getDelimiter(), TransformerContext.NONE, ReturnValueContext.VALUE ) );

				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( ListUtil.class ),
				    "containsNoCase",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( String.class ), Type.getType( String.class ),
				        Type.getType( String.class ) ),
				    false ) );
				nodes.add( new JumpInsnNode( Opcodes.GOTO, afterNullCheck ) );

				// Null path: pop the null value and push Boolean.FALSE
				nodes.add( nullSkipLabel );
				nodes.add( new InsnNode( Opcodes.POP ) );
				nodes.add( new InsnNode( Opcodes.ICONST_0 ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( Boolean.class ),
				    "valueOf",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.BOOLEAN_TYPE ),
				    false ) );
				nodes.add( afterNullCheck );
			}
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( Boolean.class ),
			    "booleanValue",
			    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			    false ) );
			nodes.add( new JumpInsnNode( Opcodes.IFEQ, endOfCase ) );
			nodes.add( startOfCase );

			c.getBody().forEach( stmt -> nodes.addAll( transpiler.transform( stmt, TransformerContext.NONE, ReturnValueContext.EMPTY_UNLESS_JUMPING ) ) );

			nodes.add( new LdcInsnNode( 1 ) );
			nodes.addAll( switchVarStore.nodes() );
			AsmHelper.addDebugLabel( nodes, "BoxSwitch - goto endOfAll" );
			nodes.add( new JumpInsnNode( Opcodes.GOTO, endOfAll ) );
			AsmHelper.addDebugLabel( nodes, "BoxSwitch - endOfCase" );
			nodes.add( endOfCase );
			nodes.add( new LdcInsnNode( 0 ) );
			nodes.addAll( switchVarStore.nodes() );

			AsmHelper.addDebugLabel( nodes, "BoxSwitch - endOfAll" );
			nodes.add( endOfAll );
			AsmHelper.addDebugLabel( nodes, "BoxSwitch - case end" );
		} );

		// TODO: Can there be more than one default case?
		boolean hasDefault = false;
		for ( var c : boxSwitch.getCases() ) {
			if ( c.getCondition() == null ) {
				if ( hasDefault ) {
					throw new ExpressionException( "Multiple default cases not supported", c.getPosition(), c.getSourceText() );
				}
				AsmHelper.addDebugLabel( nodes, "BoxSwitch - default case" );
				hasDefault = true;

				// pop the initial 0 constant in case we didn't match any cases
				// nodes.add( new InsnNode( Opcodes.POP ) );
				// pop the condition off the stack
				// nodes.add( new InsnNode( Opcodes.POP ) );
				if ( c.getBody() != null ) {
					c.getBody()
					    .forEach( stmt -> nodes.addAll( transpiler.transform( stmt, TransformerContext.NONE, ReturnValueContext.EMPTY_UNLESS_JUMPING ) ) );
				}

				AsmHelper.addDebugLabel( nodes, "BoxSwitch - goto Endlabel" );
				nodes.add( new JumpInsnNode( Opcodes.GOTO, endLabel ) );
			}
		}
		nodes.add( new JumpInsnNode( Opcodes.GOTO, endLabel ) );
		// pop the initial 0 constant in case we didn't match any cases
		AsmHelper.addDebugLabel( nodes, "BoxSwitch - breakTarget" );
		nodes.add( breakTarget );
		nodes.add( new InsnNode( Opcodes.POP ) );

		AsmHelper.addDebugLabel( nodes, "BoxSwitch - endLabel" );
		nodes.add( endLabel );

		// pop the condition off the stack
		// nodes.add( new InsnNode( Opcodes.POP ) );

		if ( !returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		AsmHelper.addDebugLabel( nodes, "BoxSwitch - done" );

		return AsmHelper.addLineNumberLabels( nodes, boxSwitch );
	}

	private List<AbstractInsnNode> transformChunkedSwitch(
	    BoxSwitch boxSwitch,
	    List<AbstractInsnNode> condition,
	    MethodContextTracker tracker,
	    ReturnValueContext returnContext,
	    List<List<BoxSwitchCase>> caseChunks ) {

		List<AbstractInsnNode>	nodes		= new ArrayList<>();
		boolean					isStatic	= isStaticMethod( tracker );

		AsmHelper.addDebugLabel( nodes, "BoxSwitch" );
		nodes.addAll( condition );
		VarStore switchConditionVarStore = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( switchConditionVarStore.nodes() );
		nodes.add( new LdcInsnNode( 0 ) );
		VarStore switchVarStore = tracker.storeNewVariable( Opcodes.ISTORE );
		nodes.addAll( switchVarStore.nodes() );
		VarStore	chunkResultStore	= tracker.storeNewVariable( Opcodes.ASTORE );

		LabelNode	endLabel			= new LabelNode();
		LabelNode	breakTarget			= new LabelNode();

		tracker.setBreak( boxSwitch, breakTarget );
		tracker.setContinue( boxSwitch, breakTarget );

		for ( int chunkIndex = 0; chunkIndex < caseChunks.size(); chunkIndex++ ) {
			List<BoxSwitchCase>	chunkCases			= caseChunks.get( chunkIndex );
			String				helperMethodName	= generateChunkMethodName( boxSwitch, chunkIndex );

			createSwitchChunkMethod( helperMethodName, boxSwitch, chunkCases, isStatic );

			if ( !isStatic ) {
				nodes.add( new VarInsnNode( Opcodes.ALOAD, 0 ) );
			}
			nodes.addAll( tracker.loadCurrentContext() );
			nodes.add( new VarInsnNode( Opcodes.ALOAD, switchConditionVarStore.index() ) );
			nodes.add( new VarInsnNode( Opcodes.ILOAD, switchVarStore.index() ) );
			nodes.add( new MethodInsnNode(
			    isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
			    Type.getObjectType( transpiler.getOwningClass().name ).getInternalName(),
			    helperMethodName,
			    Type.getMethodDescriptor( FLOW_CONTROL_RESULT_TYPE, CONTEXT_TYPE, OBJECT_TYPE, Type.BOOLEAN_TYPE ),
			    false
			) );
			nodes.addAll( chunkResultStore.nodes() );

			LabelNode	normalResult	= new LabelNode();
			LabelNode	returnResult	= new LabelNode();

			nodes.add( new VarInsnNode( Opcodes.ALOAD, chunkResultStore.index() ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( FlowControlResult.class ),
			    "isNormal",
			    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			    false
			) );
			nodes.add( new JumpInsnNode( Opcodes.IFNE, normalResult ) );

			nodes.add( new VarInsnNode( Opcodes.ALOAD, chunkResultStore.index() ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( FlowControlResult.class ),
			    "isReturn",
			    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			    false
			) );
			nodes.add( new JumpInsnNode( Opcodes.IFNE, returnResult ) );
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			nodes.add( new JumpInsnNode( Opcodes.GOTO, breakTarget ) );

			nodes.add( returnResult );
			nodes.add( new VarInsnNode( Opcodes.ALOAD, chunkResultStore.index() ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( FlowControlResult.class ),
			    "getValue",
			    Type.getMethodDescriptor( OBJECT_TYPE ),
			    false
			) );
			nodes.add( new InsnNode( Opcodes.ARETURN ) );

			nodes.add( normalResult );
			nodes.add( new VarInsnNode( Opcodes.ALOAD, chunkResultStore.index() ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( FlowControlResult.class ),
			    "getValue",
			    Type.getMethodDescriptor( OBJECT_TYPE ),
			    false
			) );
			nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( Boolean.class ) ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( Boolean.class ),
			    "booleanValue",
			    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			    false
			) );
			nodes.add( new VarInsnNode( Opcodes.ISTORE, switchVarStore.index() ) );
		}

		for ( BoxSwitchCase c : boxSwitch.getCases() ) {
			if ( c.getCondition() != null ) {
				continue;
			}

			LabelNode skipDefault = new LabelNode();
			nodes.add( new VarInsnNode( Opcodes.ILOAD, switchVarStore.index() ) );
			nodes.add( new JumpInsnNode( Opcodes.IFNE, skipDefault ) );
			if ( c.getBody() != null ) {
				c.getBody().forEach( stmt -> nodes.addAll( transpiler.transform( stmt, TransformerContext.NONE, ReturnValueContext.EMPTY_UNLESS_JUMPING ) ) );
			}
			nodes.add( skipDefault );
			break;
		}

		nodes.add( new JumpInsnNode( Opcodes.GOTO, endLabel ) );
		nodes.add( breakTarget );
		nodes.add( new InsnNode( Opcodes.POP ) );
		nodes.add( endLabel );

		if ( !returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		AsmHelper.addDebugLabel( nodes, "BoxSwitch - done" );

		return AsmHelper.addLineNumberLabels( nodes, boxSwitch );
	}

	private List<List<BoxSwitchCase>> createCaseChunks( List<BoxSwitchCase> regularCases ) {
		return createCaseChunks( regularCases, SWITCH_CHUNK_TARGET_WEIGHT, SWITCH_CHUNK_MAX_CASES );
	}

	private List<List<BoxSwitchCase>> createFallbackCaseChunks( List<BoxSwitchCase> regularCases ) {
		int	fallbackTargetWeight	= Math.max( 1, SWITCH_CHUNK_TARGET_WEIGHT / 2 );
		int	fallbackMaxCases		= Math.max( 1, SWITCH_CHUNK_MAX_CASES / 2 );

		return createCaseChunks( regularCases, fallbackTargetWeight, fallbackMaxCases );
	}

	private List<List<BoxSwitchCase>> createCaseChunks( List<BoxSwitchCase> regularCases, int targetWeight, int maxCasesPerChunk ) {
		List<List<BoxSwitchCase>>	chunks			= new ArrayList<>();
		List<BoxSwitchCase>			currentChunk	= new ArrayList<>();
		int							currentWeight	= 0;

		for ( BoxSwitchCase switchCase : regularCases ) {
			int		caseWeight		= estimateCaseWeight( switchCase );
			boolean	exceedsTarget	= !currentChunk.isEmpty() && currentWeight + caseWeight > targetWeight;
			boolean	exceedsMaxCases	= currentChunk.size() >= maxCasesPerChunk;

			if ( exceedsTarget || exceedsMaxCases ) {
				chunks.add( currentChunk );
				currentChunk	= new ArrayList<>();
				currentWeight	= 0;
			}

			currentChunk.add( switchCase );
			currentWeight += caseWeight;
		}

		if ( !currentChunk.isEmpty() ) {
			chunks.add( currentChunk );
		}

		return chunks;
	}

	private int estimateCaseWeight( BoxSwitchCase switchCase ) {
		int weight = 200;

		if ( switchCase.getSourceText() != null ) {
			weight += switchCase.getSourceText().length();
		}

		if ( switchCase.getCondition() != null && switchCase.getCondition().getSourceText() != null ) {
			weight += switchCase.getCondition().getSourceText().length();
		}

		if ( switchCase.getDelimiter() != null && switchCase.getDelimiter().getSourceText() != null ) {
			weight += switchCase.getDelimiter().getSourceText().length();
		}

		if ( switchCase.getBody() != null ) {
			for ( var statement : switchCase.getBody() ) {
				weight += 100;
				if ( statement.getSourceText() != null ) {
					weight += statement.getSourceText().length();
				}
			}
		}

		return weight;
	}

	private void createSwitchChunkMethod( String methodName, BoxSwitch boxSwitch, List<BoxSwitchCase> chunkCases, boolean isStatic ) {
		ClassNode				classNode			= transpiler.getOwningClass();
		MethodContextTracker	helperTracker		= new MethodContextTracker( isStatic );
		int						contextParamSlot	= isStatic ? 0 : 1;
		int						switchValueSlot		= isStatic ? 1 : 2;
		int						matchedParamSlot	= isStatic ? 2 : 3;

		reserveHelperParameterSlots( helperTracker );
		transpiler.addMethodContextTracker( helperTracker );

		MethodVisitor methodVisitor = classNode.visitMethod(
		    Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | ( isStatic ? Opcodes.ACC_STATIC : 0 ),
		    methodName,
		    Type.getMethodDescriptor( FLOW_CONTROL_RESULT_TYPE, CONTEXT_TYPE, OBJECT_TYPE, Type.BOOLEAN_TYPE ),
		    null,
		    null
		);
		methodVisitor.visitCode();

		methodVisitor.visitVarInsn( Opcodes.ALOAD, contextParamSlot );
		helperTracker.trackNewContext().forEach( node -> node.accept( methodVisitor ) );
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ClassLocator.class ),
		    "getInstance",
		    Type.getMethodDescriptor( Type.getType( ClassLocator.class ) ),
		    false
		);
		VarStore classLocatorStore = helperTracker.storeNewVariable( Opcodes.ASTORE );
		helperTracker.setClassLocatorSlot( classLocatorStore.index() );
		classLocatorStore.nodes().forEach( node -> node.accept( methodVisitor ) );

		List<AbstractInsnNode> helperNodes = buildChunkMethodNodes( boxSwitch, chunkCases, helperTracker, switchValueSlot, matchedParamSlot );
		transformReturnInstructions( helperNodes );
		helperNodes.forEach( node -> node.accept( methodVisitor ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();

		transpiler.popMethodContextTracker();
	}

	private List<AbstractInsnNode> buildChunkMethodNodes(
	    BoxSwitch boxSwitch,
	    List<BoxSwitchCase> chunkCases,
	    MethodContextTracker tracker,
	    int switchValueSlot,
	    int matchedParamSlot ) {

		List<AbstractInsnNode>	nodes			= new ArrayList<>();
		VarStore				matchedStore	= tracker.storeNewVariable( Opcodes.ISTORE );
		LabelNode				breakLabel		= new LabelNode();
		LabelNode				continueLabel	= new LabelNode();

		nodes.add( new VarInsnNode( Opcodes.ILOAD, matchedParamSlot ) );
		nodes.addAll( matchedStore.nodes() );

		tracker.setBreak( boxSwitch, breakLabel );
		tracker.setContinue( boxSwitch, continueLabel );

		for ( BoxSwitchCase c : chunkCases ) {
			appendCaseNodes( nodes, c, switchValueSlot, matchedStore.index() );
		}

		nodes.add( new VarInsnNode( Opcodes.ILOAD, matchedStore.index() ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( Boolean.class ),
		    "valueOf",
		    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.BOOLEAN_TYPE ),
		    false
		) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( FlowControlResult.class ),
		    "ofNormal",
		    Type.getMethodDescriptor( FLOW_CONTROL_RESULT_TYPE, OBJECT_TYPE ),
		    false
		) );
		nodes.add( new InsnNode( Opcodes.ARETURN ) );

		nodes.add( continueLabel );
		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( FlowControlResult.class ),
		    "ofContinue",
		    Type.getMethodDescriptor( FLOW_CONTROL_RESULT_TYPE, Type.getType( String.class ) ),
		    false
		) );
		nodes.add( new InsnNode( Opcodes.ARETURN ) );

		nodes.add( breakLabel );
		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( FlowControlResult.class ),
		    "ofBreak",
		    Type.getMethodDescriptor( FLOW_CONTROL_RESULT_TYPE, Type.getType( String.class ) ),
		    false
		) );
		nodes.add( new InsnNode( Opcodes.ARETURN ) );

		return nodes;
	}

	private void appendCaseNodes( List<AbstractInsnNode> nodes, BoxSwitchCase c, int switchValueSlot, int matchedSlot ) {
		AsmHelper.addDebugLabel( nodes, "BoxSwitch - case start" );
		LabelNode	startOfCase	= new LabelNode();
		LabelNode	endOfCase	= new LabelNode();
		LabelNode	endOfAll	= new LabelNode();

		nodes.add( new VarInsnNode( Opcodes.ILOAD, matchedSlot ) );
		nodes.add( new JumpInsnNode( Opcodes.IFNE, startOfCase ) );
		nodes.add( new VarInsnNode( Opcodes.ALOAD, switchValueSlot ) );

		if ( c.getDelimiter() == null ) {
			nodes.addAll( transpiler.transform( c.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( EqualsEquals.class ),
			    "invoke",
			    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ), Type.getType( Object.class ) ),
			    false ) );
		} else {
			LabelNode	nullSkipLabel	= new LabelNode();
			LabelNode	afterNullCheck	= new LabelNode();
			nodes.add( new InsnNode( Opcodes.DUP ) );
			nodes.add( new JumpInsnNode( Opcodes.IFNULL, nullSkipLabel ) );

			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( StringCaster.class ),
			    "cast",
			    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
			    false ) );

			nodes.addAll( transpiler.transform( c.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE ) );

			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( StringCaster.class ),
			    "cast",
			    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
			    false ) );

			nodes.add( new InsnNode( Opcodes.SWAP ) );
			nodes.addAll( transpiler.transform( c.getDelimiter(), TransformerContext.NONE, ReturnValueContext.VALUE ) );

			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( ListUtil.class ),
			    "containsNoCase",
			    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( String.class ), Type.getType( String.class ),
			        Type.getType( String.class ) ),
			    false ) );
			nodes.add( new JumpInsnNode( Opcodes.GOTO, afterNullCheck ) );

			nodes.add( nullSkipLabel );
			nodes.add( new InsnNode( Opcodes.POP ) );
			nodes.add( new InsnNode( Opcodes.ICONST_0 ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Boolean.class ),
			    "valueOf",
			    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.BOOLEAN_TYPE ),
			    false ) );
			nodes.add( afterNullCheck );
		}

		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( Boolean.class ),
		    "booleanValue",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false ) );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, endOfCase ) );
		nodes.add( startOfCase );

		c.getBody().forEach( stmt -> nodes.addAll( transpiler.transform( stmt, TransformerContext.NONE, ReturnValueContext.EMPTY_UNLESS_JUMPING ) ) );

		nodes.add( new LdcInsnNode( 1 ) );
		nodes.add( new VarInsnNode( Opcodes.ISTORE, matchedSlot ) );
		AsmHelper.addDebugLabel( nodes, "BoxSwitch - goto endOfAll" );
		nodes.add( new JumpInsnNode( Opcodes.GOTO, endOfAll ) );
		AsmHelper.addDebugLabel( nodes, "BoxSwitch - endOfCase" );
		nodes.add( endOfCase );
		nodes.add( new LdcInsnNode( 0 ) );
		nodes.add( new VarInsnNode( Opcodes.ISTORE, matchedSlot ) );
		AsmHelper.addDebugLabel( nodes, "BoxSwitch - endOfAll" );
		nodes.add( endOfAll );
		AsmHelper.addDebugLabel( nodes, "BoxSwitch - case end" );
	}

	private boolean isStaticMethod( MethodContextTracker tracker ) {
		return ( ( VarInsnNode ) tracker.loadCurrentContext().getFirst() ).var == 0;
	}

	private void reserveHelperParameterSlots( MethodContextTracker tracker ) {
		tracker.storeNewVariable( Opcodes.ASTORE );
		tracker.storeNewVariable( Opcodes.ASTORE );
		tracker.storeNewVariable( Opcodes.ASTORE );
	}

	private String generateChunkMethodName( BoxSwitch boxSwitch, int chunkIndex ) {
		return "_switchChunk_" + Integer.toUnsignedString( System.identityHashCode( boxSwitch ) ) + "_" + chunkIndex;
	}

	private void transformReturnInstructions( List<AbstractInsnNode> nodes ) {
		for ( int i = nodes.size() - 1; i >= 0; i-- ) {
			AbstractInsnNode node = nodes.get( i );
			if ( ! ( node instanceof InsnNode ) ) {
				continue;
			}

			int opcode = node.getOpcode();
			if ( opcode < Opcodes.IRETURN || opcode > Opcodes.RETURN ) {
				continue;
			}

			nodes.remove( i );
			nodes.addAll( i, createReturnWrapInstructions( opcode ) );
		}
	}

	private List<AbstractInsnNode> createReturnWrapInstructions( int originalOpcode ) {
		List<AbstractInsnNode> instructions = new ArrayList<>();

		switch ( originalOpcode ) {
			case Opcodes.IRETURN :
				instructions.add( new MethodInsnNode( Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf",
				    Type.getMethodDescriptor( Type.getType( Integer.class ), Type.INT_TYPE ), false ) );
				break;
			case Opcodes.LRETURN :
				instructions.add( new MethodInsnNode( Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf",
				    Type.getMethodDescriptor( Type.getType( Long.class ), Type.LONG_TYPE ), false ) );
				break;
			case Opcodes.FRETURN :
				instructions.add( new MethodInsnNode( Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf",
				    Type.getMethodDescriptor( Type.getType( Float.class ), Type.FLOAT_TYPE ), false ) );
				break;
			case Opcodes.DRETURN :
				instructions.add( new MethodInsnNode( Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf",
				    Type.getMethodDescriptor( Type.getType( Double.class ), Type.DOUBLE_TYPE ), false ) );
				break;
			case Opcodes.RETURN :
				instructions.add( new InsnNode( Opcodes.ACONST_NULL ) );
				break;
			case Opcodes.ARETURN :
			default :
				break;
		}

		instructions.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( FlowControlResult.class ),
		    "ofReturn",
		    Type.getMethodDescriptor( FLOW_CONTROL_RESULT_TYPE, OBJECT_TYPE ),
		    false
		) );
		instructions.add( new InsnNode( Opcodes.ARETURN ) );

		return instructions;
	}
}
