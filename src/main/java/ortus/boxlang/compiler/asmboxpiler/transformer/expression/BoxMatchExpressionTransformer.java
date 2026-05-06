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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxMatchArrayPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchBindingPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchCase;
import ortus.boxlang.compiler.ast.expression.BoxMatchConstructorPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchExpression;
import ortus.boxlang.compiler.ast.expression.BoxMatchLiteralPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchObjectPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchWildcardPattern;
import ortus.boxlang.compiler.ast.expression.BoxObjectDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.MatchExpression.ArrayBinding;
import ortus.boxlang.runtime.dynamic.MatchExpression.Case;
import ortus.boxlang.runtime.dynamic.MatchExpression.ObjectBinding;
import ortus.boxlang.runtime.dynamic.MatchExpression.Pattern;
import ortus.boxlang.runtime.dynamic.MatchExpression.Target;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxMatchExpressionTransformer extends AbstractTransformer {

	public BoxMatchExpressionTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxMatchExpression				matchExpression	= ( BoxMatchExpression ) node;
		Optional<MethodContextTracker>	tracker			= transpiler.getCurrentMethodContextTracker();
		List<AbstractInsnNode>			nodes			= new ArrayList<>();

		tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );
		nodes.addAll( transpiler.transform( matchExpression.getSubject(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
		nodes.addAll( AsmHelper.array( Type.getType( Case.class ), matchExpression.getCases(), ( matchCase, index ) -> buildCaseNodes( matchCase ) ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
		    "invoke",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IBoxContext.class ), Type.getType( Object.class ),
		        Type.getType( Case[].class ) ),
		    false ) );

		if ( returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return AsmHelper.addLineNumberLabels( nodes, node );
	}

	private List<AbstractInsnNode> buildCaseNodes( BoxMatchCase matchCase ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.addAll( buildPatternNodes( matchCase.getPattern() ) );
		if ( matchCase.getGuard() != null ) {
			nodes.addAll( AsmHelper.getDefaultExpression( ( ortus.boxlang.compiler.asmboxpiler.AsmTranspiler ) transpiler, matchCase.getGuard() ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}
		nodes.addAll( AsmHelper.getDefaultExpression( ( ortus.boxlang.compiler.asmboxpiler.AsmTranspiler ) transpiler, matchCase.getBody() ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
		    "matchCase",
		    Type.getMethodDescriptor( Type.getType( Case.class ), Type.getType( Pattern.class ), Type.getType( DefaultExpression.class ),
		        Type.getType( DefaultExpression.class ) ),
		    false ) );
		return nodes;
	}

	private List<AbstractInsnNode> buildPatternNodes( BoxMatchPattern pattern ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		if ( pattern instanceof BoxMatchLiteralPattern literalPattern ) {
			nodes.addAll( transpiler.transform( literalPattern.getValue(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
			    "literal",
			    Type.getMethodDescriptor( Type.getType( Pattern.class ), Type.getType( Object.class ) ),
			    false ) );
			return nodes;
		}
		if ( pattern instanceof BoxMatchWildcardPattern ) {
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
			    "wildcard",
			    Type.getMethodDescriptor( Type.getType( Pattern.class ) ),
			    false ) );
			return nodes;
		}
		if ( pattern instanceof BoxMatchBindingPattern bindingPattern ) {
			nodes.addAll( buildTargetNodes( bindingPattern.getBinding(), false ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
			    "binding",
			    Type.getMethodDescriptor( Type.getType( Pattern.class ), Type.getType( Target.class ) ),
			    false ) );
			return nodes;
		}
		if ( pattern instanceof BoxMatchConstructorPattern constructorPattern ) {
			nodes.add( new LdcInsnNode( constructorPattern.getLabel().getName() ) );
			nodes
			    .addAll( AsmHelper.array( Type.getType( Pattern.class ), constructorPattern.getPatterns(), ( nested, index ) -> buildPatternNodes( nested ) ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
			    "constructor",
			    Type.getMethodDescriptor( Type.getType( Pattern.class ), Type.getType( String.class ), Type.getType( Pattern[].class ) ),
			    false ) );
			return nodes;
		}
		if ( pattern instanceof BoxMatchObjectPattern objectPattern ) {
			nodes.addAll( AsmHelper.array( Type.getType( ObjectBinding.class ), objectPattern.getPattern().getBindings(),
			    ( binding, index ) -> buildObjectBindingNodes( binding ) ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
			    "object",
			    Type.getMethodDescriptor( Type.getType( Pattern.class ), Type.getType( ObjectBinding[].class ) ),
			    false ) );
			return nodes;
		}
		if ( pattern instanceof BoxMatchArrayPattern arrayPattern ) {
			nodes.addAll( AsmHelper.array( Type.getType( ArrayBinding.class ), arrayPattern.getPattern().getBindings(),
			    ( binding, index ) -> buildArrayBindingNodes( binding ) ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
			    "array",
			    Type.getMethodDescriptor( Type.getType( Pattern.class ), Type.getType( ArrayBinding[].class ) ),
			    false ) );
			return nodes;
		}
		throw new ExpressionException( "Unsupported match pattern [" + pattern.getClass().getSimpleName() + "]", pattern.getPosition(),
		    pattern.getSourceText() );
	}

	private List<AbstractInsnNode> buildObjectBindingNodes( BoxObjectDestructuringBinding binding ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		if ( binding.isRest() ) {
			nodes.addAll( buildTargetNodes( binding.getTarget(), false ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
			    "objectRest",
			    Type.getMethodDescriptor( Type.getType( ObjectBinding.class ), Type.getType( Target.class ) ),
			    false ) );
			return nodes;
		}

		nodes.add( new LdcInsnNode( extractObjectKeyName( binding ) ) );
		if ( binding.getTarget() != null ) {
			nodes.addAll( buildTargetNodes( binding.getTarget(), false ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}
		if ( binding.getPattern() != null ) {
			nodes.addAll( AsmHelper.array( Type.getType( ObjectBinding.class ), binding.getPattern().getBindings(),
			    ( nested, index ) -> buildObjectBindingNodes( nested ) ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}
		if ( binding.getDefaultValue() != null ) {
			nodes.addAll( AsmHelper.getDefaultExpression( ( ortus.boxlang.compiler.asmboxpiler.AsmTranspiler ) transpiler, binding.getDefaultValue() ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
		    "objectBinding",
		    Type.getMethodDescriptor( Type.getType( ObjectBinding.class ), Type.getType( String.class ), Type.getType( Target.class ),
		        Type.getType( ObjectBinding[].class ), Type.getType( DefaultExpression.class ) ),
		    false ) );
		return nodes;
	}

	private List<AbstractInsnNode> buildArrayBindingNodes( BoxArrayDestructuringBinding binding ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		if ( binding.isRest() ) {
			nodes.addAll( buildTargetNodes( binding.getTarget(), false ) );
			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
			    "arrayRest",
			    Type.getMethodDescriptor( Type.getType( ArrayBinding.class ), Type.getType( Target.class ) ),
			    false ) );
			return nodes;
		}

		if ( binding.getTarget() != null ) {
			nodes.addAll( buildTargetNodes( binding.getTarget(), false ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}
		if ( binding.getPattern() != null ) {
			nodes.addAll( AsmHelper.array( Type.getType( ArrayBinding.class ), binding.getPattern().getBindings(),
			    ( nested, index ) -> buildArrayBindingNodes( nested ) ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}
		if ( binding.getDefaultValue() != null ) {
			nodes.addAll( AsmHelper.getDefaultExpression( ( ortus.boxlang.compiler.asmboxpiler.AsmTranspiler ) transpiler, binding.getDefaultValue() ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
		    "arrayBinding",
		    Type.getMethodDescriptor( Type.getType( ArrayBinding.class ), Type.getType( Target.class ), Type.getType( ArrayBinding[].class ),
		        Type.getType( DefaultExpression.class ) ),
		    false ) );
		return nodes;
	}

	private List<AbstractInsnNode> buildTargetNodes( BoxExpression target, boolean isDeclaration ) {
		DestructuringTargetDescriptor	descriptor	= describeTarget( target, isDeclaration );
		List<AbstractInsnNode>			nodes		= new ArrayList<>();
		nodes.add( new LdcInsnNode( descriptor.scoped ? 1 : 0 ) );
		nodes.addAll( AsmHelper.array( Type.getType( String.class ), descriptor.path, ( segment, index ) -> List.of( new LdcInsnNode( segment ) ) ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ortus.boxlang.runtime.dynamic.MatchExpression.class ),
		    "target",
		    Type.getMethodDescriptor( Type.getType( Target.class ), Type.BOOLEAN_TYPE, Type.getType( String[].class ) ),
		    false ) );
		return nodes;
	}

	private DestructuringTargetDescriptor describeTarget( BoxExpression target, boolean isDeclaration ) {
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
			} else if ( current instanceof BoxIdentifier id && isExplicitScope( id.getName() ) ) {
				scopeName = id.getName();
			} else {
				throw new ExpressionException(
				    "Destructuring dotted targets must start with an explicit scope.",
				    target.getPosition(),
				    target.getSourceText() );
			}
			if ( isDeclaration ) {
				throw new ExpressionException(
				    "Scoped targets are not allowed in var/final/static destructuring declarations.",
				    target.getPosition(),
				    target.getSourceText() );
			}
			segments.add( 0, scopeName );
			return new DestructuringTargetDescriptor( true, segments );
		}
		throw new ExpressionException(
		    "Unsupported destructuring target [" + target.getClass().getSimpleName() + "]",
		    target.getPosition(),
		    target.getSourceText() );
	}

	private String extractObjectKeyName( BoxObjectDestructuringBinding binding ) {
		if ( binding.getKey() instanceof BoxIdentifier id ) {
			return id.getName();
		}
		if ( binding.getKey() instanceof BoxStringLiteral stringLiteral ) {
			return stringLiteral.getValue();
		}
		if ( binding.getKey() instanceof BoxIntegerLiteral integerLiteral ) {
			return integerLiteral.getValue();
		}
		if ( binding.getKey() instanceof BoxFQN fqn ) {
			return fqn.getValue();
		}
		if ( binding.getKey() instanceof BoxScope scope ) {
			return scope.getName();
		}
		throw new ExpressionException( "Unsupported destructuring key [" + binding.getKey().getClass().getSimpleName() + "]", binding.getPosition(),
		    binding.getSourceText() );
	}

	private boolean isExplicitScope( String scopeName ) {
		return switch ( scopeName.toLowerCase() ) {
			case "application", "arguments", "cgi", "client", "cookie", "form", "local", "request", "server", "session", "static", "this", "thread", "url", "variables" -> true;
			default -> false;
		};
	}

	private static class DestructuringTargetDescriptor {

		private final boolean		scoped;
		private final List<String>	path;

		private DestructuringTargetDescriptor( boolean scoped, List<String> path ) {
			this.scoped	= scoped;
			this.path	= path;
		}
	}
}