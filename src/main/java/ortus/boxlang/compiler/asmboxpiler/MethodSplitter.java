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
package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.runtime.types.FlowControlResult;

/**
 * Handles splitting of large methods to avoid JVM's 64KB method size limit.
 * Uses bytecode size estimation to identify when methods need splitting,
 * and extracts code segments into sub-methods with flow control propagation.
 */
public class MethodSplitter {

	/**
	 * JVM method bytecode size limit is 64KB (65535 bytes).
	 * We use a conservative threshold of 55KB to allow for overhead.
	 */
	public static final int		BYTECODE_SIZE_LIMIT	= 55000;

	/**
	 * Counter for generating unique sub-method names
	 */
	private final AtomicInteger	subMethodCounter	= new AtomicInteger( 0 );

	/**
	 * The transpiler instance for creating auxiliary methods
	 */
	private final Transpiler	transpiler;

	/**
	 * The class node where sub-methods will be added
	 */
	private final ClassNode		classNode;

	/**
	 * The main type for method invocation
	 */
	private final Type			mainType;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new MethodSplitter
	 *
	 * @param transpiler The transpiler instance
	 * @param classNode  The class node to add sub-methods to
	 * @param mainType   The type of the main class
	 */
	public MethodSplitter( Transpiler transpiler, ClassNode classNode, Type mainType ) {
		this.transpiler	= transpiler;
		this.classNode	= classNode;
		this.mainType	= mainType;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Bytecode Size Estimation
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Estimate the bytecode size in bytes for a list of instructions.
	 * This is an approximation based on typical instruction sizes.
	 *
	 * @param nodes The instruction list to estimate
	 *
	 * @return Estimated size in bytes
	 */
	public static int estimateBytecodeSize( List<AbstractInsnNode> nodes ) {
		int size = 0;
		for ( AbstractInsnNode node : nodes ) {
			size += estimateInstructionSize( node );
		}
		return size;
	}

	/**
	 * Estimate the bytecode size of a single instruction.
	 *
	 * @param node The instruction node
	 *
	 * @return Estimated size in bytes
	 */
	public static int estimateInstructionSize( AbstractInsnNode node ) {
		return switch ( node.getType() ) {
			// Simple instructions: 1 byte opcode
			case AbstractInsnNode.INSN -> 1;

			// Int instructions (BIPUSH, SIPUSH, NEWARRAY): 2-3 bytes
			case AbstractInsnNode.INT_INSN -> {
				if ( node.getOpcode() == Opcodes.SIPUSH ) {
					yield 3; // opcode + 2-byte short
				}
				yield 2; // opcode + 1-byte operand
			}

			// Variable instructions (ILOAD, ASTORE, etc.): 2-4 bytes
			case AbstractInsnNode.VAR_INSN -> {
				VarInsnNode varNode = ( VarInsnNode ) node;
				if ( varNode.var <= 3 ) {
					// Optimized single-byte opcodes for slots 0-3
					yield 1;
				} else if ( varNode.var <= 255 ) {
					yield 2; // opcode + 1-byte index
				}
				yield 4; // WIDE prefix + opcode + 2-byte index
			}

			// Type instructions (NEW, CHECKCAST, INSTANCEOF): 3 bytes
			case AbstractInsnNode.TYPE_INSN -> 3;

			// Field instructions: 3 bytes (opcode + 2-byte cp index)
			case AbstractInsnNode.FIELD_INSN -> 3;

			// Method instructions: 3-5 bytes
			case AbstractInsnNode.METHOD_INSN -> {
				MethodInsnNode methodNode = ( MethodInsnNode ) node;
				if ( methodNode.getOpcode() == Opcodes.INVOKEINTERFACE ) {
					yield 5; // opcode + 2-byte cp + count + 0
				}
				yield 3; // opcode + 2-byte cp index
			}

			// InvokeDynamic: 5 bytes
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN -> 5;

			// Jump instructions: 3 bytes (or 5 for GOTO_W, JSR_W)
			case AbstractInsnNode.JUMP_INSN -> {
				int opcode = node.getOpcode();
				if ( opcode == 200 || opcode == 201 ) { // GOTO_W, JSR_W
					yield 5;
				}
				yield 3;
			}

			// LDC instructions: 2-3 bytes
			case AbstractInsnNode.LDC_INSN -> {
				LdcInsnNode	ldcNode	= ( LdcInsnNode ) node;
				Object		cst		= ldcNode.cst;
				// LDC_W and LDC2_W for long/double or high cp index
				if ( cst instanceof Long || cst instanceof Double ) {
					yield 3;
				}
				yield 2; // Assume LDC for simplicity
			}

			// IINC: 3 bytes (or 6 with WIDE)
			case AbstractInsnNode.IINC_INSN -> {
				IincInsnNode iincNode = ( IincInsnNode ) node;
				if ( iincNode.var > 255 || iincNode.incr > 127 || iincNode.incr < -128 ) {
					yield 6; // WIDE version
				}
				yield 3;
			}

			// TABLESWITCH: variable, estimate conservatively
			case AbstractInsnNode.TABLESWITCH_INSN -> {
				TableSwitchInsnNode	switchNode	= ( TableSwitchInsnNode ) node;
				// 1-3 padding + 4 (default) + 4 (low) + 4 (high) + 4 * (high - low + 1)
				int					cases		= switchNode.max - switchNode.min + 1;
				yield 16 + ( cases * 4 );
			}

			// LOOKUPSWITCH: variable, estimate conservatively
			case AbstractInsnNode.LOOKUPSWITCH_INSN -> {
				LookupSwitchInsnNode switchNode = ( LookupSwitchInsnNode ) node;
				// 1-3 padding + 4 (default) + 4 (npairs) + 8 * npairs
				yield 12 + ( switchNode.keys.size() * 8 );
			}

			// MULTIANEWARRAY: 4 bytes
			case AbstractInsnNode.MULTIANEWARRAY_INSN -> 4;

			// Labels and line numbers: no bytecode
			case AbstractInsnNode.LABEL, AbstractInsnNode.LINE, AbstractInsnNode.FRAME -> 0;

			// Default: assume 3 bytes for safety
			default -> 3;
		};
	}

	/**
	 * --------------------------------------------------------------------------
	 * Method Splitting
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Process a method's instructions and split if necessary.
	 * Returns modified instruction list with sub-method calls if splitting occurred.
	 *
	 * @param nodes         The original instruction list
	 * @param methodName    The base name for generating sub-method names
	 * @param parameterType The parameter type (typically IBoxContext)
	 * @param returnType    The return type
	 *
	 * @return Processed instruction list (may include sub-method calls)
	 */
	public List<AbstractInsnNode> processMethod(
	    List<AbstractInsnNode> nodes,
	    String methodName,
	    Type parameterType,
	    Type returnType ) {

		int estimatedSize = estimateBytecodeSize( nodes );

		// If under the limit, return unchanged
		if ( estimatedSize < BYTECODE_SIZE_LIMIT ) {
			return nodes;
		}

		// Split the method into segments
		List<MethodSegment> segments = splitIntoSegments( nodes, methodName );

		// Generate sub-methods and call sites
		return generateSplitMethod( segments, methodName, parameterType, returnType );
	}

	/**
	 * Split instructions into segments at DividerNode boundaries.
	 * Ensures that labels and their references stay within the same segment.
	 *
	 * @param nodes      The instruction list to split
	 * @param methodName The base method name for logging
	 *
	 * @return List of method segments
	 */
	private List<MethodSegment> splitIntoSegments( List<AbstractInsnNode> nodes, String methodName ) {
		// First, find all safe split points (DividerNodes that don't break label references)
		List<Integer> safeSplitPoints = findSafeSplitPoints( nodes );

		// If no safe split points found, we can't split safely - return single segment
		if ( safeSplitPoints.isEmpty() ) {
			List<AbstractInsnNode> allNodes = nodes.stream()
			    .filter( n -> ! ( n instanceof DividerNode ) )
			    .collect( java.util.stream.Collectors.toList() );
			return List.of( new MethodSegment( allNodes, estimateBytecodeSize( allNodes ), containsFlowControl( allNodes ) ) );
		}

		List<MethodSegment>	segments		= new ArrayList<>();
		int					currentStart	= 0;
		int					currentSize		= 0;

		for ( int i = 0; i < nodes.size(); i++ ) {
			AbstractInsnNode	node		= nodes.get( i );
			int					nodeSize	= estimateInstructionSize( node );
			currentSize += nodeSize;

			// Check if this is a safe split point and we should split
			if ( safeSplitPoints.contains( i ) && currentSize >= BYTECODE_SIZE_LIMIT / 2 ) {
				// Create segment from currentStart to i (exclusive of DividerNode)
				List<AbstractInsnNode> segmentNodes = new ArrayList<>();
				for ( int j = currentStart; j < i; j++ ) {
					AbstractInsnNode n = nodes.get( j );
					if ( ! ( n instanceof DividerNode ) ) {
						segmentNodes.add( n );
					}
				}
				if ( !segmentNodes.isEmpty() ) {
					segments.add( new MethodSegment( segmentNodes, currentSize, containsFlowControl( segmentNodes ) ) );
				}
				currentStart	= i + 1; // Skip the DividerNode
				currentSize		= 0;
			}
		}

		// Add final segment
		List<AbstractInsnNode> finalSegment = new ArrayList<>();
		for ( int j = currentStart; j < nodes.size(); j++ ) {
			AbstractInsnNode n = nodes.get( j );
			if ( ! ( n instanceof DividerNode ) ) {
				finalSegment.add( n );
			}
		}
		if ( !finalSegment.isEmpty() ) {
			segments.add( new MethodSegment( finalSegment, currentSize, containsFlowControl( finalSegment ) ) );
		}

		return segments;
	}

	/**
	 * Find safe split points where we can split without breaking label references.
	 * A DividerNode is safe to split at if no jump instruction before it references
	 * a label after it, and no jump instruction after it references a label before it.
	 *
	 * @param nodes The instruction list
	 *
	 * @return List of indices where it's safe to split (at DividerNodes)
	 */
	private List<Integer> findSafeSplitPoints( List<AbstractInsnNode> nodes ) {
		// Collect all label positions and their references
		java.util.Map<LabelNode, Integer>	labelPositions	= new java.util.HashMap<>();
		java.util.Map<Integer, LabelNode>	jumpReferences	= new java.util.HashMap<>();

		for ( int i = 0; i < nodes.size(); i++ ) {
			AbstractInsnNode node = nodes.get( i );
			if ( node instanceof LabelNode labelNode ) {
				labelPositions.put( labelNode, i );
			} else if ( node instanceof JumpInsnNode jumpNode ) {
				jumpReferences.put( i, jumpNode.label );
			} else if ( node instanceof TableSwitchInsnNode switchNode ) {
				// TableSwitch has multiple labels
				// Mark as unsafe to split near switch statements
			} else if ( node instanceof LookupSwitchInsnNode switchNode ) {
				// LookupSwitch has multiple labels
				// Mark as unsafe to split near switch statements
			}
		}

		// Find DividerNodes that are safe to split at
		List<Integer> safeSplitPoints = new ArrayList<>();

		for ( int i = 0; i < nodes.size(); i++ ) {
			if ( ! ( nodes.get( i ) instanceof DividerNode ) ) {
				continue;
			}

			boolean isSafe = true;

			// Check if any jump before this point references a label after this point
			for ( var entry : jumpReferences.entrySet() ) {
				int			jumpPos		= entry.getKey();
				LabelNode	targetLabel	= entry.getValue();
				Integer		labelPos	= labelPositions.get( targetLabel );

				if ( labelPos == null ) {
					// Label not in this method - might be external, skip
					continue;
				}

				// If jump is before split point and label is after, or vice versa, not safe
				if ( ( jumpPos < i && labelPos >= i ) || ( jumpPos >= i && labelPos < i ) ) {
					isSafe = false;
					break;
				}
			}

			if ( isSafe ) {
				safeSplitPoints.add( i );
			}
		}

		return safeSplitPoints;
	}

	/**
	 * Check if a segment contains flow control (return, break, continue)
	 * that might need to propagate to the parent method.
	 *
	 * @param nodes The instruction segment
	 *
	 * @return true if flow control is present
	 */
	private boolean containsFlowControl( List<AbstractInsnNode> nodes ) {
		for ( AbstractInsnNode node : nodes ) {
			if ( node instanceof InsnNode ) {
				int opcode = node.getOpcode();
				// RETURN opcodes
				if ( opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN ) {
					return true;
				}
			}
			// Could also check for calls to FlowControlResult.of* methods
		}
		return false;
	}

	/**
	 * Generate the split method with sub-method calls.
	 *
	 * @param segments      The method segments
	 * @param methodName    Base method name
	 * @param parameterType Parameter type
	 * @param returnType    Return type
	 *
	 * @return Instruction list for the main method
	 */
	private List<AbstractInsnNode> generateSplitMethod(
	    List<MethodSegment> segments,
	    String methodName,
	    Type parameterType,
	    Type returnType ) {

		List<AbstractInsnNode> result = new ArrayList<>();

		for ( int i = 0; i < segments.size(); i++ ) {
			MethodSegment	segment	= segments.get( i );
			String			subName	= generateSubMethodName( methodName, i );
			boolean			isLast	= ( i == segments.size() - 1 );

			// Create the sub-method
			createSubMethod( subName, segment.nodes(), parameterType );

			// Generate the call to the sub-method
			result.addAll( generateSubMethodCall( subName, parameterType, segment.hasFlowControl(), isLast ) );
		}

		return result;
	}

	/**
	 * Generate a unique sub-method name
	 */
	private String generateSubMethodName( String baseName, int index ) {
		return "_split_" + baseName + "_" + index + "_" + this.subMethodCounter.incrementAndGet();
	}

	/**
	 * Create a sub-method containing the given instructions.
	 * Sub-methods return FlowControlResult to signal flow control.
	 *
	 * @param name          The sub-method name
	 * @param nodes         The instructions for the sub-method body
	 * @param parameterType The parameter type
	 */
	private void createSubMethod( String name, List<AbstractInsnNode> nodes, Type parameterType ) {
		// Sub-methods return FlowControlResult to handle flow control propagation
		Type returnType = Type.getType( FlowControlResult.class );

		AsmHelper.methodWithContextAndClassLocator(
		    this.classNode,
		    name,
		    parameterType,
		    returnType,
		    false,
		    this.transpiler,
		    false,
		    () -> {
			    // Wrap the nodes to return FlowControlResult.NORMAL_RESULT at the end
			    List<AbstractInsnNode> wrapped = new ArrayList<>( nodes );

			    // Remove any trailing POP that would discard our result
			    while ( !wrapped.isEmpty() && wrapped.get( wrapped.size() - 1 ).getOpcode() == Opcodes.POP ) {
				    wrapped.remove( wrapped.size() - 1 );
			    }

			    // Add normal result return
			    wrapped.add( new FieldInsnNode(
			        Opcodes.GETSTATIC,
			        Type.getInternalName( FlowControlResult.class ),
			        "NORMAL_RESULT",
			        Type.getDescriptor( FlowControlResult.class )
			    ) );

			    return wrapped;
		    }
		);
	}

	/**
	 * Generate instructions to call a sub-method and handle flow control.
	 *
	 * @param subMethodName  The sub-method to call
	 * @param parameterType  The parameter type
	 * @param hasFlowControl Whether the sub-method may return flow control
	 * @param isLast         Whether this is the last segment
	 *
	 * @return Instructions for calling the sub-method
	 */
	private List<AbstractInsnNode> generateSubMethodCall(
	    String subMethodName,
	    Type parameterType,
	    boolean hasFlowControl,
	    boolean isLast ) {

		List<AbstractInsnNode>	nodes		= new ArrayList<>();
		Type					resultType	= Type.getType( FlowControlResult.class );

		// Load this
		nodes.add( new VarInsnNode( Opcodes.ALOAD, 0 ) );

		// Load context parameter
		nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );

		// Invoke the sub-method
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKEVIRTUAL,
		    this.mainType.getInternalName(),
		    subMethodName,
		    Type.getMethodDescriptor( resultType, parameterType ),
		    false
		) );

		// If this segment might have flow control, we need to check and propagate
		if ( hasFlowControl ) {
			nodes.addAll( generateFlowControlCheck( isLast ) );
		} else if ( !isLast ) {
			// Just pop the result if no flow control and not last
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return nodes;
	}

	/**
	 * Generate instructions to check FlowControlResult and propagate if needed.
	 *
	 * @param isLast Whether this is the last segment
	 *
	 * @return Instructions for flow control checking
	 */
	private List<AbstractInsnNode> generateFlowControlCheck( boolean isLast ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();

		// DUP the result
		nodes.add( new InsnNode( Opcodes.DUP ) );

		// Call isNormal()
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( FlowControlResult.class ),
		    "isNormal",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false
		) );

		LabelNode continueLabel = new LabelNode();

		// If normal (true), jump to continue
		nodes.add( new JumpInsnNode( Opcodes.IFNE, continueLabel ) );

		// Not normal - return the FlowControlResult to propagate
		nodes.add( new InsnNode( Opcodes.ARETURN ) );

		// Continue label
		nodes.add( continueLabel );

		// Pop the result since we're continuing normally
		if ( !isLast ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return nodes;
	}

	/**
	 * Record representing a method segment
	 */
	private record MethodSegment( List<AbstractInsnNode> nodes, int estimatedSize, boolean hasFlowControl ) {

	}
}
