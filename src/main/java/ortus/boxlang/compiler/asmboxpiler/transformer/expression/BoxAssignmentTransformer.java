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
package ortus.boxlang.compiler.asmboxpiler.transformer.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringPattern;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxObjectDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxObjectDestructuringPattern;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.ArrayDestructurer;
import ortus.boxlang.runtime.dynamic.ObjectDestructurer;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.operators.Divide;
import ortus.boxlang.runtime.operators.Minus;
import ortus.boxlang.runtime.operators.Modulus;
import ortus.boxlang.runtime.operators.Multiply;
import ortus.boxlang.runtime.operators.Plus;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxAssignmentTransformer extends AbstractTransformer {

	public BoxAssignmentTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxAssignment			assignment	= ( BoxAssignment ) node;
		List<AbstractInsnNode>	nodes		= null;

		if ( assignment.getOp() == null ) {
			if ( ! ( assignment.getLeft() instanceof BoxIdentifier ) ) {
				throw new ExpressionException( "You cannot declare a variable using " + assignment.getLeft().getClass().getSimpleName(),
				    assignment.getPosition(),
				    assignment.getSourceText() );
			}

			nodes = assignNullValue( ( BoxIdentifier ) assignment.getLeft() );
		} else if ( assignment.getOp() == BoxAssignmentOperator.Equal ) {
			List<AbstractInsnNode> jRight = transpiler.transform( assignment.getRight(), TransformerContext.NONE, ReturnValueContext.VALUE );
			if ( assignment.getLeft() instanceof BoxObjectDestructuringPattern pattern ) {
				nodes = transformDestructuringEquals( pattern, jRight, assignment.getModifiers() );
			} else if ( assignment.getLeft() instanceof BoxArrayDestructuringPattern pattern ) {
				nodes = transformArrayDestructuringEquals( pattern, jRight, assignment.getModifiers() );
			} else {
				nodes = transformEquals( assignment.getLeft(), jRight, assignment.getOp(), assignment.getModifiers() );
			}
		} else {
			nodes = transformCompoundEquals( assignment );
		}

		if ( returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return AsmHelper.addLineNumberLabels( nodes, node );
	}

	public List<AbstractInsnNode> transformEquals( BoxExpression left, List<AbstractInsnNode> jRight, BoxAssignmentOperator op,
	    List<BoxAssignmentModifier> modifiers ) throws IllegalStateException {
		boolean							hasVar			= hasVar( modifiers );
		boolean							hasStatic		= hasStatic( modifiers );
		boolean							hasFinal		= hasFinal( modifiers );
		String							mustBeScopeName	= null;
		Optional<MethodContextTracker>	tracker			= transpiler.getCurrentMethodContextTracker();

		// "#arguments.scope#.#arguments.propertyName#" = arguments.propertyValue;
		if ( left instanceof BoxStringInterpolation || left instanceof BoxStringLiteral ) {
			if ( hasVar ) {
				throw new ExpressionException( "You cannot use the [var] keyword with a quoted string on the left hand side of your assignment",
				    left.getPosition(), left.getSourceText() );
			}
			if ( hasStatic ) {
				throw new ExpressionException( "You cannot use the [static] keyword with a quoted string on the left hand side of your assignment",
				    left.getPosition(), left.getSourceText() );
			}
			/*
			 * ExpressionInterpreter.setVariable(
			 * ${contextName},
			 * StringCaster.cast( ${left} ),
			 * ${right}
			 * );
			 */
			List<AbstractInsnNode> nodes = new ArrayList<>();
			tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );

			nodes.addAll( transpiler.transform( left, null, ReturnValueContext.VALUE_OR_NULL ) );

			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( StringCaster.class ),
			    "cast",
			    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
			    false ) );

			nodes.addAll( jRight );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ExpressionInterpreter.class ),
			    "setVariable",
			    Type.getMethodDescriptor( Type.getType( Object.class ),
			        Type.getType( IBoxContext.class ),
			        Type.getType( String.class ),
			        Type.getType( Object.class ) ),
			    false ) );

			return nodes;
		}

		List<List<AbstractInsnNode>>	accessKeys		= new ArrayList<>();
		BoxExpression					furthestLeft	= left;

		while ( furthestLeft instanceof BoxAccess currentObjectAccess ) {
			// DotAccess just uses the string directly, array access allows any expression
			if ( currentObjectAccess instanceof BoxDotAccess dotAccess ) {
				if ( dotAccess.getAccess() instanceof BoxIdentifier id ) {
					accessKeys.add( 0, transpiler.createKey( id.getName() ) );
				} else if ( dotAccess.getAccess() instanceof BoxIntegerLiteral intl ) {
					accessKeys.add( 0, transpiler.createKey( intl.getValue() ) );
				} else {
					throw new ExpressionException(
					    "Unexpected element [" + currentObjectAccess.getAccess().getClass().getSimpleName() + "] in dot access expression.",
					    currentObjectAccess.getAccess().getPosition(), currentObjectAccess.getAccess().getSourceText() );
				}
			} else {
				accessKeys.add( 0, transpiler.createKey( currentObjectAccess.getAccess() ) );
			}
			furthestLeft = currentObjectAccess.getContext();
		}

		// CF parses `var["foo"] = "bar"` as a `var` modifier + array literal LHS.
		// Treat it as assignment to identifier `var` with bracket access key(s).
		if ( hasVar && furthestLeft instanceof BoxArrayLiteral arrayLiteral ) {
			hasVar = false;
			for ( BoxExpression value : arrayLiteral.getValues() ) {
				accessKeys.add( transpiler.createKey( value ) );
			}
			furthestLeft = new BoxIdentifier( "var", null, null );
		}

		if ( hasStatic && hasVar ) {
			throw new ExpressionException( "You cannot use the [var] and [static] keywords together", left.getPosition(), left.getSourceText() );
		}

		// If this assignment was var foo = 1, then we need into insert the scope as the furthest left and shift the key
		if ( hasVar ) {
			mustBeScopeName = "local";
			// This is for the edge case of
			// var variables = 5
			// or
			// var variables.foo = 5
			// in which case it's not really a scope but just an identifier
			// I'd rather do this check when building the AST but the parse tree is more of a pain to deal with
			if ( furthestLeft instanceof BoxScope scope ) {
				accessKeys.add( 0, transpiler.createKey( scope.getName() ) );
			} else if ( furthestLeft instanceof BoxIdentifier id ) {
				accessKeys.add( 0, transpiler.createKey( id.getName() ) );
			} else {
				throw new ExpressionException( "You cannot use the [var] keyword before " + furthestLeft.getClass().getSimpleName(), furthestLeft.getPosition(),
				    furthestLeft.getSourceText() );
			}
			furthestLeft = new BoxIdentifier( "local", null, null );
		}

		// If this assignment was static foo = 1, then we need into insert the scope as the furthest left and shift the key
		if ( hasStatic ) {
			mustBeScopeName = "static";
			// This is for the edge case of
			// static variables = 5
			// or
			// static variables.foo = 5
			// in which case it's not really a scope but just an identifier
			// I'd rather do this check when building the AST but the parse tree is more of a pain to deal with
			if ( furthestLeft instanceof BoxScope scope ) {
				accessKeys.add( 0, transpiler.createKey( scope.getName() ) );
			} else if ( furthestLeft instanceof BoxIdentifier id ) {
				accessKeys.add( 0, transpiler.createKey( id.getName() ) );
			} else {
				throw new ExpressionException( "You cannot use the [static] keyword before " + furthestLeft.getClass().getSimpleName(),
				    furthestLeft.getPosition(),
				    furthestLeft.getSourceText() );
			}
			furthestLeft = new BoxIdentifier( "static", null, null );
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();
		if ( furthestLeft instanceof BoxIdentifier id ) {
			// imported.foo = 5 is ok, but imported = 5 is not
			if ( left instanceof BoxIdentifier idl && transpiler.matchesImport( idl.getName() )
			    && transpiler.getProperty( "sourceType" ).toLowerCase().startsWith( "box" ) ) {
				throw new ExpressionException( "You cannot assign a variable with the same name as an import: [" + idl.getName() + "]",
				    idl.getPosition(), idl.getSourceText() );
			}

			/*
			 * Referencer.setDeep(
			 * ${contextName},
			 * hasFinal,
			 * mustBeScope
			 * ${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ),
			 * ${right}
			 * ${accessKeys});
			 */
			tracker.ifPresent( t -> {
				nodes.addAll( t.loadCurrentContext() );
			} );

			nodes.add( new FieldInsnNode( Opcodes.GETSTATIC, Type.getInternalName( Boolean.class ), hasFinal ? "TRUE" : "FALSE",
			    Type.getDescriptor( Boolean.class ) ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( Boolean.class ),
			    "booleanValue",
			    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
			    false ) );

			if ( mustBeScopeName != null ) {
				nodes.addAll( transpiler.createKey( mustBeScopeName ) );
			} else {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			}

			Class<?>	baseObjectClass;
			// If id is an imported class name, load the class directly instead of searching scopes for it
			boolean		isBoxSyntax	= transpiler.getProperty( "sourceType" ).toLowerCase().startsWith( "box" );
			if ( transpiler.matchesImport( id.getName() ) && isBoxSyntax ) {
				baseObjectClass = Object.class;
				nodes.addAll( AsmHelper.loadClass( transpiler, id ) );
			} else {
				// Otherwise, search for varible in scopes
				baseObjectClass = IBoxContext.ScopeSearchResult.class;
				tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );
				nodes.addAll( transpiler.createKey( id.getName() ) );
				tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
				    Type.getInternalName( IBoxContext.class ),
				    "getDefaultAssignmentScope",
				    Type.getMethodDescriptor( Type.getType( IScope.class ) ),
				    true ) );
				nodes.add( new LdcInsnNode( 1 ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
				    Type.getInternalName( IBoxContext.class ),
				    "scopeFindNearby",
				    Type.getMethodDescriptor( Type.getType( IBoxContext.ScopeSearchResult.class ), Type.getType( Key.class ), Type.getType( IScope.class ),
				        Type.BOOLEAN_TYPE ),
				    true ) );
			}

			nodes.addAll( jRight );

			nodes.addAll( AsmHelper.array( Type.getType( Key.class ), accessKeys ) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( Referencer.class ),
			    "setDeep",
			    Type.getMethodDescriptor( Type.getType( Object.class ),
			        Type.getType( IBoxContext.class ),
			        Type.BOOLEAN_TYPE,
			        Type.getType( Key.class ),
			        Type.getType( baseObjectClass ),
			        Type.getType( Object.class ),
			        Type.getType( Key[].class ) ),
			    false ) );

		} else {
			if ( accessKeys.size() == 0 && ! ( left instanceof BoxScope ) ) {
				throw new ExpressionException( "You cannot assign a value to " + left.getClass().getSimpleName(), left.getPosition(), left.getSourceText() );
			}
			/*
			 * Referencer.setDeep(
			 * ${contextName},
			 * hasFinal
			 * mustBeScope
			 * ${furthestLeft},
			 * ${right},
			 * ${accessKeys})
			 */
			tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );

			nodes.add( new FieldInsnNode( Opcodes.GETSTATIC, Type.getInternalName( Boolean.class ), hasFinal ? "TRUE" : "FALSE",
			    Type.getDescriptor( Boolean.class ) ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( Boolean.class ),
			    "booleanValue",
			    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
			    false ) );

			if ( mustBeScopeName != null ) {
				nodes.addAll( transpiler.createKey( mustBeScopeName ) );
			} else {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			}

			nodes.addAll( transpiler.transform( furthestLeft, TransformerContext.NONE, ReturnValueContext.VALUE ) );

			nodes.addAll( jRight );

			nodes.addAll( AsmHelper.array( Type.getType( Key.class ), accessKeys ) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( Referencer.class ),
			    "setDeep",
			    Type.getMethodDescriptor( Type.getType( Object.class ),
			        Type.getType( IBoxContext.class ),
			        Type.BOOLEAN_TYPE,
			        Type.getType( Key.class ),
			        Type.getType( Object.class ),
			        Type.getType( Object.class ),
			        Type.getType( Key[].class ) ),
			    false ) );
		}

		return nodes;
	}

	private List<AbstractInsnNode> transformDestructuringEquals( BoxObjectDestructuringPattern pattern, List<AbstractInsnNode> jRight,
	    List<BoxAssignmentModifier> modifiers ) {
		boolean							hasVar			= hasVar( modifiers );
		boolean							hasStatic		= hasStatic( modifiers );
		boolean							hasFinal		= hasFinal( modifiers );
		boolean							isDeclaration	= !modifiers.isEmpty();
		String							mustBeScopeName	= null;
		Optional<MethodContextTracker>	tracker			= transpiler.getCurrentMethodContextTracker();

		if ( hasStatic && hasVar ) {
			throw new ExpressionException( "You cannot use the [var] and [static] keywords together", pattern.getPosition(), pattern.getSourceText() );
		}

		if ( hasVar ) {
			mustBeScopeName = "local";
		} else if ( hasStatic ) {
			mustBeScopeName = "static";
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();
		tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );
		nodes.addAll( jRight );

		nodes.add( new LdcInsnNode( hasFinal ? 1 : 0 ) );

		if ( mustBeScopeName != null ) {
			nodes.addAll( transpiler.createKey( mustBeScopeName ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.addAll( buildDestructuringBindingsArrayNodes( pattern.getBindings(), isDeclaration ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ObjectDestructurer.class ),
		    "destructure",
		    Type.getMethodDescriptor(
		        Type.getType( Object.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.BOOLEAN_TYPE,
		        Type.getType( Key.class ),
		        Type.getType( ObjectDestructurer.Binding[].class )
		    ),
		    false ) );

		return nodes;
	}

	private List<AbstractInsnNode> transformArrayDestructuringEquals( BoxArrayDestructuringPattern pattern, List<AbstractInsnNode> jRight,
	    List<BoxAssignmentModifier> modifiers ) {
		boolean							hasVar			= hasVar( modifiers );
		boolean							hasStatic		= hasStatic( modifiers );
		boolean							hasFinal		= hasFinal( modifiers );
		boolean							isDeclaration	= !modifiers.isEmpty();
		String							mustBeScopeName	= null;
		Optional<MethodContextTracker>	tracker			= transpiler.getCurrentMethodContextTracker();

		if ( hasStatic && hasVar ) {
			throw new ExpressionException( "You cannot use the [var] and [static] keywords together", pattern.getPosition(), pattern.getSourceText() );
		}

		if ( hasVar ) {
			mustBeScopeName = "local";
		} else if ( hasStatic ) {
			mustBeScopeName = "static";
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();
		tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );
		nodes.addAll( jRight );

		nodes.add( new LdcInsnNode( hasFinal ? 1 : 0 ) );

		if ( mustBeScopeName != null ) {
			nodes.addAll( transpiler.createKey( mustBeScopeName ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.addAll( buildArrayDestructuringBindingsArrayNodes( pattern.getBindings(), isDeclaration ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ArrayDestructurer.class ),
		    "destructure",
		    Type.getMethodDescriptor(
		        Type.getType( Object.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.BOOLEAN_TYPE,
		        Type.getType( Key.class ),
		        Type.getType( ArrayDestructurer.Binding[].class )
		    ),
		    false ) );

		return nodes;
	}

	private List<AbstractInsnNode> buildDestructuringBindingsArrayNodes( List<BoxObjectDestructuringBinding> bindings, boolean isDeclaration ) {
		return AsmHelper.array( Type.getType( ObjectDestructurer.Binding.class ), bindings,
		    ( binding, i ) -> buildDestructuringBindingNodes( binding, isDeclaration ) );
	}

	private List<AbstractInsnNode> buildDestructuringBindingNodes( BoxObjectDestructuringBinding binding, boolean isDeclaration ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		if ( binding.isRest() ) {
			nodes.addAll( buildDestructuringTargetNodes( binding.getTarget(), isDeclaration ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ObjectDestructurer.class ),
			    "rest",
			    Type.getMethodDescriptor( Type.getType( ObjectDestructurer.Binding.class ), Type.getType( ObjectDestructurer.Target.class ) ),
			    false ) );
			return nodes;
		}

		nodes.add( new LdcInsnNode( extractDestructuringKeyName( binding ) ) );

		if ( binding.getTarget() != null ) {
			nodes.addAll( buildDestructuringTargetNodes( binding.getTarget(), isDeclaration ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		if ( binding.getPattern() != null ) {
			nodes.addAll( buildDestructuringBindingsArrayNodes( binding.getPattern().getBindings(), isDeclaration ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		if ( binding.getDefaultValue() != null ) {
			nodes.addAll( AsmHelper.generateArgumentProducerLambda( transpiler,
			    () -> transpiler.transform( binding.getDefaultValue(), TransformerContext.NONE, ReturnValueContext.VALUE ) ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ObjectDestructurer.class ),
		    "binding",
		    Type.getMethodDescriptor(
		        Type.getType( ObjectDestructurer.Binding.class ),
		        Type.getType( String.class ),
		        Type.getType( ObjectDestructurer.Target.class ),
		        Type.getType( ObjectDestructurer.Binding[].class ),
		        Type.getType( Function.class )
		    ),
		    false ) );

		return nodes;
	}

	private List<AbstractInsnNode> buildArrayDestructuringBindingsArrayNodes( List<BoxArrayDestructuringBinding> bindings, boolean isDeclaration ) {
		return AsmHelper.array( Type.getType( ArrayDestructurer.Binding.class ), bindings,
		    ( binding, i ) -> buildArrayDestructuringBindingNodes( binding, isDeclaration ) );
	}

	private List<AbstractInsnNode> buildArrayDestructuringBindingNodes( BoxArrayDestructuringBinding binding, boolean isDeclaration ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		if ( binding.isRest() ) {
			nodes.addAll( buildArrayDestructuringTargetNodes( binding.getTarget(), isDeclaration ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ArrayDestructurer.class ),
			    "rest",
			    Type.getMethodDescriptor( Type.getType( ArrayDestructurer.Binding.class ), Type.getType( ArrayDestructurer.Target.class ) ),
			    false ) );
			return nodes;
		}

		if ( binding.getTarget() != null ) {
			nodes.addAll( buildArrayDestructuringTargetNodes( binding.getTarget(), isDeclaration ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		if ( binding.getPattern() != null ) {
			nodes.addAll( buildArrayDestructuringBindingsArrayNodes( binding.getPattern().getBindings(), isDeclaration ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		if ( binding.getDefaultValue() != null ) {
			nodes.addAll( AsmHelper.generateArgumentProducerLambda( transpiler,
			    () -> transpiler.transform( binding.getDefaultValue(), TransformerContext.NONE, ReturnValueContext.VALUE ) ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ArrayDestructurer.class ),
		    "binding",
		    Type.getMethodDescriptor(
		        Type.getType( ArrayDestructurer.Binding.class ),
		        Type.getType( ArrayDestructurer.Target.class ),
		        Type.getType( ArrayDestructurer.Binding[].class ),
		        Type.getType( Function.class )
		    ),
		    false ) );

		return nodes;
	}

	private List<AbstractInsnNode> buildDestructuringTargetNodes( BoxExpression target, boolean isDeclaration ) {
		DestructuringTargetDescriptor	descriptor	= describeDestructuringTarget( target, isDeclaration );
		List<AbstractInsnNode>			nodes		= new ArrayList<>();
		nodes.add( new LdcInsnNode( descriptor.scoped ? 1 : 0 ) );
		nodes.addAll( AsmHelper.array( Type.getType( String.class ), descriptor.path, ( segment, i ) -> List.of( new LdcInsnNode( segment ) ) ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ObjectDestructurer.class ),
		    "target",
		    Type.getMethodDescriptor(
		        Type.getType( ObjectDestructurer.Target.class ),
		        Type.BOOLEAN_TYPE,
		        Type.getType( String[].class )
		    ),
		    false ) );
		return nodes;
	}

	private List<AbstractInsnNode> buildArrayDestructuringTargetNodes( BoxExpression target, boolean isDeclaration ) {
		DestructuringTargetDescriptor	descriptor	= describeDestructuringTarget( target, isDeclaration );
		List<AbstractInsnNode>			nodes		= new ArrayList<>();
		nodes.add( new LdcInsnNode( descriptor.scoped ? 1 : 0 ) );
		nodes.addAll( AsmHelper.array( Type.getType( String.class ), descriptor.path, ( segment, i ) -> List.of( new LdcInsnNode( segment ) ) ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ArrayDestructurer.class ),
		    "target",
		    Type.getMethodDescriptor(
		        Type.getType( ArrayDestructurer.Target.class ),
		        Type.BOOLEAN_TYPE,
		        Type.getType( String[].class )
		    ),
		    false ) );
		return nodes;
	}

	private DestructuringTargetDescriptor describeDestructuringTarget( BoxExpression target, boolean isDeclaration ) {
		if ( target instanceof BoxIdentifier id ) {
			return new DestructuringTargetDescriptor( false, List.of( id.getName() ) );
		}
		if ( target instanceof BoxScope scope ) {
			return new DestructuringTargetDescriptor( false, List.of( scope.getName() ) );
		}
		if ( target instanceof BoxDotAccess dotAccess ) {
			List<String>	segments	= new ArrayList<>();
			BoxExpression	current		= dotAccess;
			while ( current instanceof BoxDotAccess dot ) {
				if ( dot.isSafe() ) {
					throw new ExpressionException( "Destructuring targets cannot use safe navigation.", dot.getPosition(), dot.getSourceText() );
				}
				if ( ! ( dot.getAccess() instanceof BoxIdentifier id ) ) {
					throw new ExpressionException(
					    "Destructuring targets only support identifier path segments.",
					    dot.getAccess().getPosition(),
					    dot.getAccess().getSourceText() );
				}
				segments.add( 0, id.getName() );
				current = dot.getContext();
			}
			String scopeName;
			if ( current instanceof BoxScope scope ) {
				scopeName = scope.getName();
			} else if ( current instanceof BoxIdentifier id && isExplicitDestructuringScope( id.getName() ) ) {
				scopeName = id.getName();
			} else {
				throw new ExpressionException(
				    "Destructuring dotted targets must start with an explicit scope.",
				    target.getPosition(),
				    target.getSourceText() );
			}
			segments.add( 0, scopeName );
			if ( isDeclaration ) {
				throw new ExpressionException(
				    "Scoped targets are not allowed in var/final/static destructuring declarations.",
				    target.getPosition(),
				    target.getSourceText() );
			}
			return new DestructuringTargetDescriptor( true, segments );
		}

		throw new ExpressionException(
		    "Unsupported destructuring target [" + target.getClass().getSimpleName() + "]",
		    target.getPosition(),
		    target.getSourceText() );
	}

	private String extractDestructuringKeyName( BoxObjectDestructuringBinding binding ) {
		BoxExpression key = binding.getKey();
		if ( key instanceof BoxIdentifier id ) {
			return id.getName();
		}
		if ( key instanceof BoxStringLiteral str ) {
			return str.getValue();
		}
		if ( key instanceof BoxIntegerLiteral integer ) {
			return integer.getValue();
		}
		if ( key instanceof BoxFQN fqn ) {
			return fqn.getValue();
		}
		if ( key instanceof BoxScope scope ) {
			return scope.getName();
		}

		throw new ExpressionException(
		    "Unsupported destructuring key [" + key.getClass().getSimpleName() + "]",
		    binding.getPosition(),
		    binding.getSourceText() );
	}

	private static class DestructuringTargetDescriptor {

		final boolean		scoped;
		final List<String>	path;

		DestructuringTargetDescriptor( boolean scoped, List<String> path ) {
			this.scoped	= scoped;
			this.path	= path;
		}
	}

	private boolean isExplicitDestructuringScope( String scopeName ) {
		return switch ( scopeName.toLowerCase() ) {
			case "application", "arguments", "cgi", "client", "cookie", "form", "local", "request", "server", "session", "static", "this", "thread",
			    "url", "variables" -> true;
			default -> false;
		};
	}

	private List<AbstractInsnNode> transformCompoundEquals( BoxAssignment assigment ) throws IllegalStateException {
		// Note any var keyword is completley ignored in this code path!

		Optional<MethodContextTracker>	tracker	= transpiler.getCurrentMethodContextTracker();
		List<AbstractInsnNode>			nodes	= new ArrayList<>();
		List<AbstractInsnNode>			right	= transpiler.transform( assigment.getRight(), TransformerContext.NONE, ReturnValueContext.VALUE );

		/*
		 * ${operation}.invoke(${contextName},
		 * ${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ).scope(),
		 * ${accessKey},
		 * ${right})
		 */

		tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );

		if ( assigment.getLeft() instanceof BoxIdentifier id ) {
			List<AbstractInsnNode> accessKey = transpiler.createKey( id.getName() );

			tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );

			nodes.addAll( accessKey );

			tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "getDefaultAssignmentScope",
			    Type.getMethodDescriptor( Type.getType( IScope.class ) ),
			    true ) );
			nodes.add( new LdcInsnNode( 1 ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "scopeFindNearby",
			    Type.getMethodDescriptor( Type.getType( IBoxContext.ScopeSearchResult.class ),
			        Type.getType( Key.class ),
			        Type.getType( IScope.class ), Type.BOOLEAN_TYPE ),
			    true ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( IBoxContext.ScopeSearchResult.class ),
			    "scope",
			    Type.getMethodDescriptor( Type.getType( IReferenceable.class ) ),
			    false ) );

			nodes.addAll( accessKey );
		} else if ( assigment.getLeft() instanceof BoxAccess objectAccess ) {
			nodes.addAll( transpiler.transform( objectAccess.getContext(), TransformerContext.NONE, ReturnValueContext.VALUE_OR_NULL ) );

			List<AbstractInsnNode> accessKey;
			// DotAccess just uses the string directly, array access allows any expression
			if ( objectAccess instanceof BoxDotAccess dotAccess ) {
				if ( dotAccess.getAccess() instanceof BoxIdentifier id ) {
					accessKey = transpiler.createKey( id.getName() );
				} else if ( dotAccess.getAccess() instanceof BoxIntegerLiteral intl ) {
					accessKey = transpiler.createKey( intl.getValue() );
				} else {
					throw new ExpressionException(
					    "Unexpected element [" + dotAccess.getAccess().getClass().getSimpleName() + "] in dot access expression.",
					    dotAccess.getAccess().getPosition(), dotAccess.getAccess().getSourceText() );
				}
			} else {
				accessKey = transpiler.createKey( objectAccess.getAccess() );
			}
			nodes.addAll( accessKey );
		} else {
			throw new ExpressionException( "You cannot assign a value to " + assigment.getLeft().getClass().getSimpleName(), assigment.getPosition(),
			    assigment.getSourceText() );
		}

		nodes.addAll( right );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( getMethodCallTemplate( assigment ) ),
		    "invoke",
		    Type.getMethodDescriptor( Type.getType( assigment.getOp() == BoxAssignmentOperator.ConcatEqual ? String.class : Number.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.getType( Key.class ),
		        Type.getType( Object.class ) ),
		    false ) );

		return nodes;
	}

	private boolean hasVar( List<BoxAssignmentModifier> modifiers ) {
		return modifiers.stream().anyMatch( it -> it == BoxAssignmentModifier.VAR );
	}

	private boolean hasStatic( List<BoxAssignmentModifier> modifiers ) {
		return modifiers.stream().anyMatch( it -> it == BoxAssignmentModifier.STATIC );
	}

	private boolean hasFinal( List<BoxAssignmentModifier> modifiers ) {
		return modifiers.stream().anyMatch( it -> it == BoxAssignmentModifier.FINAL );
	}

	private Class<?> getMethodCallTemplate( BoxAssignment assignment ) {
		BoxAssignmentOperator operator = assignment.getOp();
		return switch ( operator ) {
			case PlusEqual -> Plus.class;
			case MinusEqual -> Minus.class;
			case StarEqual -> Multiply.class;
			case SlashEqual -> Divide.class;
			case ModEqual -> Modulus.class;
			case ConcatEqual -> Concat.class;
			default -> throw new ExpressionException( "Unknown assingment operator " + operator.toString(), assignment.getPosition(),
			    assignment.getSourceText() );
		};
	}

	private List<AbstractInsnNode> assignNullValue( BoxIdentifier name ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		transpiler.getCurrentMethodContextTracker().ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );

		nodes.add( new InsnNode( Opcodes.DUP ) );
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( LocalScope.class ),
		    "name",
		    Type.getDescriptor( Key.class ) ) );
		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		nodes.add( new LdcInsnNode( 1 ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "scopeFindNearby",
		    Type.getMethodDescriptor( Type.getType( IBoxContext.ScopeSearchResult.class ),
		        Type.getType( Key.class ),
		        Type.getType( IScope.class ),
		        Type.BOOLEAN_TYPE ),
		    true ) );

		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		nodes.addAll( AsmHelper.array( Type.getType( Key.class ), List.of( transpiler.createKey( ( name.getName() ) ) ) ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( Referencer.class ),
		    "setDeep",
		    Type.getMethodDescriptor( Type.getType( Object.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( IBoxContext.ScopeSearchResult.class ),
		        Type.getType( Object.class ),
		        Type.getType( Key[].class ) ),
		    false ) );

		return nodes;
	}

}
