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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxMatchAndPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchArrayPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchBindingPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchCase;
import ortus.boxlang.compiler.ast.expression.BoxMatchConstructorPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchExpression;
import ortus.boxlang.compiler.ast.expression.BoxMatchLiteralPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchNotPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchObjectPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchOrPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchPredicatePattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchRangePattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchTypePattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchWildcardPattern;
import ortus.boxlang.compiler.ast.expression.BoxObjectDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxYield;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxMatchExpressionTransformer extends AbstractTransformer {

	public BoxMatchExpressionTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxMatchExpression	matchExpression	= ( BoxMatchExpression ) node;
		Map<String, String>	values			= new HashMap<>();
		values.put( "contextName", transpiler.peekContextName() );
		values.put( "subject", transpiler.transform( matchExpression.getSubject(), TransformerContext.RIGHT ).toString() );
		values.put( "cases", buildCasesExpression( matchExpression.getCases() ) );
		return parseExpression( "ortus.boxlang.runtime.dynamic.MatchExpression.invoke(${contextName}, ${subject}, ${cases})", values );
	}

	private String buildCasesExpression( List<BoxMatchCase> matchCases ) {
		return "new ortus.boxlang.runtime.dynamic.MatchExpression.Case[] { "
		    + matchCases.stream().map( this::buildCaseExpression ).collect( Collectors.joining( ", " ) )
		    + " }";
	}

	private String buildCaseExpression( BoxMatchCase matchCase ) {
		String	guard	= matchCase.getGuard() == null ? "null" : buildLambdaExpression( matchCase.getGuard() );
		String	body	= buildLambdaExpression( matchCase.getBody() );
		return "ortus.boxlang.runtime.dynamic.MatchExpression.matchCase("
		    + buildPatternExpression( matchCase.getPattern() ) + ", "
		    + guard + ", "
		    + body
		    + ")";
	}

	private String buildPatternExpression( BoxMatchPattern pattern ) {
		if ( pattern instanceof BoxMatchLiteralPattern literalPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.literal("
			    + transpiler.transform( literalPattern.getValue(), TransformerContext.RIGHT )
			    + ")";
		}
		if ( pattern instanceof BoxMatchWildcardPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.wildcard()";
		}
		if ( pattern instanceof BoxMatchBindingPattern bindingPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.binding("
			    + buildTargetExpression( bindingPattern.getBinding(), false )
			    + ")";
		}
		if ( pattern instanceof BoxMatchConstructorPattern constructorPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.constructor("
			    + quoteJavaString( constructorPattern.getLabel().getName() ) + ", "
			    + buildNestedPatternsExpression( constructorPattern.getPatterns() )
			    + ")";
		}
		if ( pattern instanceof BoxMatchObjectPattern objectPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.object("
			    + buildObjectBindingsExpression( objectPattern.getPattern().getBindings() )
			    + ")";
		}
		if ( pattern instanceof BoxMatchOrPattern orPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.or("
			    + buildNestedPatternsExpression( orPattern.getPatterns() )
			    + ")";
		}
		if ( pattern instanceof BoxMatchAndPattern andPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.and("
			    + buildNestedPatternsExpression( andPattern.getPatterns() )
			    + ")";
		}
		if ( pattern instanceof BoxMatchNotPattern notPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.not("
			    + buildPatternExpression( notPattern.getPattern() )
			    + ")";
		}
		if ( pattern instanceof BoxMatchPredicatePattern predicatePattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.predicate("
			    + buildLambdaExpression( predicatePattern.getPredicate() )
			    + ")";
		}
		if ( pattern instanceof BoxMatchRangePattern rangePattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.range("
			    + transpiler.transform( rangePattern.getFrom(), TransformerContext.RIGHT ) + ", "
			    + transpiler.transform( rangePattern.getTo(), TransformerContext.RIGHT )
			    + ")";
		}
		if ( pattern instanceof BoxMatchTypePattern typePattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.type("
			    + buildTypeNamesExpression( typePattern.getTypes() ) + ", "
			    + buildTargetExpression( typePattern.getBinding(), false )
			    + ")";
		}
		if ( pattern instanceof BoxMatchArrayPattern arrayPattern ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.array("
			    + buildArrayBindingsExpression( arrayPattern.getPattern().getBindings() )
			    + ")";
		}
		throw new ExpressionException( "Unsupported match pattern [" + pattern.getClass().getSimpleName() + "]", pattern.getPosition(),
		    pattern.getSourceText() );
	}

	private String buildTypeNamesExpression( List<String> types ) {
		if ( types.isEmpty() ) {
			return "new String[] {}";
		}
		return "new String[] { "
		    + types.stream().map( this::quoteJavaString ).collect( Collectors.joining( ", " ) )
		    + " }";
	}

	private String buildNestedPatternsExpression( List<BoxMatchPattern> patterns ) {
		if ( patterns.isEmpty() ) {
			return "new ortus.boxlang.runtime.dynamic.MatchExpression.Pattern[] {}";
		}
		return "new ortus.boxlang.runtime.dynamic.MatchExpression.Pattern[] { "
		    + patterns.stream().map( this::buildPatternExpression ).collect( Collectors.joining( ", " ) )
		    + " }";
	}

	private String buildObjectBindingsExpression( List<BoxObjectDestructuringBinding> bindings ) {
		if ( bindings.isEmpty() ) {
			return "new ortus.boxlang.runtime.dynamic.MatchExpression.ObjectBinding[] {}";
		}
		return "new ortus.boxlang.runtime.dynamic.MatchExpression.ObjectBinding[] { "
		    + bindings.stream().map( this::buildObjectBindingExpression ).collect( Collectors.joining( ", " ) )
		    + " }";
	}

	private String buildObjectBindingExpression( BoxObjectDestructuringBinding binding ) {
		if ( binding.isRest() ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.objectRest("
			    + buildTargetExpression( binding.getTarget(), false )
			    + ")";
		}

		String	sourceKey		= quoteJavaString( extractObjectKeyName( binding ) );
		String	target			= binding.getTarget() == null ? "null" : buildTargetExpression( binding.getTarget(), false );
		String	nested			= binding.getPattern() == null ? "null" : buildObjectBindingsExpression( binding.getPattern().getBindings() );
		String	defaultValue	= binding.getDefaultValue() == null ? "null" : buildLambdaExpression( binding.getDefaultValue() );

		return "ortus.boxlang.runtime.dynamic.MatchExpression.objectBinding("
		    + sourceKey + ", "
		    + target + ", "
		    + nested + ", "
		    + defaultValue
		    + ")";
	}

	private String buildArrayBindingsExpression( List<BoxArrayDestructuringBinding> bindings ) {
		if ( bindings.isEmpty() ) {
			return "new ortus.boxlang.runtime.dynamic.MatchExpression.ArrayBinding[] {}";
		}
		return "new ortus.boxlang.runtime.dynamic.MatchExpression.ArrayBinding[] { "
		    + bindings.stream().map( this::buildArrayBindingExpression ).collect( Collectors.joining( ", " ) )
		    + " }";
	}

	private String buildArrayBindingExpression( BoxArrayDestructuringBinding binding ) {
		if ( binding.isRest() ) {
			return "ortus.boxlang.runtime.dynamic.MatchExpression.arrayRest("
			    + buildTargetExpression( binding.getTarget(), false )
			    + ")";
		}

		String	target			= binding.getTarget() == null ? "null" : buildTargetExpression( binding.getTarget(), false );
		String	nested			= binding.getPattern() == null ? "null" : buildArrayBindingsExpression( binding.getPattern().getBindings() );
		String	defaultValue	= binding.getDefaultValue() == null ? "null" : buildLambdaExpression( binding.getDefaultValue() );

		return "ortus.boxlang.runtime.dynamic.MatchExpression.arrayBinding("
		    + target + ", "
		    + nested + ", "
		    + defaultValue
		    + ")";
	}

	private String buildLambdaExpression( BoxExpression expression ) {
		String contextName = "matchContext" + transpiler.incrementAndGetLambdaContextCounter();
		transpiler.pushContextName( contextName );
		String expressionSource = transpiler.transform( expression, TransformerContext.RIGHT ).toString();
		transpiler.popContextName();
		return "(" + contextName + ") -> " + expressionSource;
	}

	private String buildLambdaExpression( BoxStatement body ) {
		if ( body instanceof BoxExpressionStatement expressionStatement ) {
			return buildLambdaExpression( expressionStatement.getExpression() );
		}

		String contextName = "matchContext" + transpiler.incrementAndGetLambdaContextCounter();
		transpiler.pushContextName( contextName );
		BlockStmt lambdaBody = buildLambdaBlock( body );
		transpiler.popContextName();
		return "(" + contextName + ") -> " + lambdaBody;
	}

	private BlockStmt buildLambdaBlock( BoxStatement body ) {
		if ( body instanceof BoxStatementBlock statementBlock ) {
			return buildLambdaBlock( statementBlock );
		}

		throw new ExpressionException( "Unsupported match case body [" + body.getClass().getSimpleName() + "]", body );
	}

	private BlockStmt buildLambdaBlock( BoxStatementBlock statementBlock ) {
		if ( statementBlock.getBody().isEmpty() ) {
			throw new ExpressionException( "Match block bodies must end with an expression or yield statement.", statementBlock );
		}

		BoxStatement	lastStatement	= statementBlock.getBody().getLast();
		BlockStmt		lambdaBody		= new BlockStmt();
		for ( int i = 0; i < statementBlock.getBody().size() - 1; i++ ) {
			appendStatement( lambdaBody, transpiler.transform( statementBlock.getBody().get( i ) ) );
		}
		if ( lastStatement instanceof BoxExpressionStatement finalExpression ) {
			Expression returnExpression = ( Expression ) transpiler.transform( finalExpression.getExpression(), TransformerContext.RIGHT );
			lambdaBody.addStatement( new ReturnStmt( returnExpression ) );
			return lambdaBody;
		}
		if ( lastStatement instanceof BoxYield ) {
			appendStatement( lambdaBody, transpiler.transform( lastStatement ) );
			return lambdaBody;
		}

		throw new ExpressionException( "Match block bodies must end with an expression or yield statement.", statementBlock );
	}

	private void appendStatement( BlockStmt target, Node javaNode ) {
		if ( javaNode instanceof BlockStmt blockStmt ) {
			blockStmt.getStatements().forEach( target::addStatement );
			return;
		}
		target.addStatement( ( Statement ) javaNode );
	}

	private String buildTargetExpression( BoxExpression target, boolean isDeclaration ) {
		DestructuringTargetDescriptor descriptor = describeTarget( target, isDeclaration );
		return "ortus.boxlang.runtime.dynamic.MatchExpression.target("
		    + ( descriptor.scoped ? "true" : "false" ) + ", "
		    + descriptor.path.stream().map( this::quoteJavaString ).collect( Collectors.joining( ", " ) )
		    + ")";
	}

	private DestructuringTargetDescriptor describeTarget( BoxExpression target, boolean isDeclaration ) {
		if ( target instanceof BoxIdentifier id ) {
			return new DestructuringTargetDescriptor( false, List.of( id.getName() ) );
		}
		if ( target instanceof BoxScope scope ) {
			return new DestructuringTargetDescriptor( false, List.of( scope.getName() ) );
		}
		if ( target instanceof BoxDotAccess dotAccess ) {
			return describeDotTarget( dotAccess, isDeclaration );
		}
		throw new ExpressionException(
		    "Unsupported destructuring target [" + target.getClass().getSimpleName() + "]",
		    target.getPosition(),
		    target.getSourceText() );
	}

	private DestructuringTargetDescriptor describeDotTarget( BoxDotAccess dotAccess, boolean isDeclaration ) {
		List<String>	segments	= new java.util.ArrayList<>();
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
			    dotAccess.getPosition(),
			    dotAccess.getSourceText() );
		}

		if ( isDeclaration ) {
			throw new ExpressionException(
			    "Scoped targets are not allowed in var/final/static destructuring declarations.",
			    dotAccess.getPosition(),
			    dotAccess.getSourceText() );
		}

		segments.add( 0, scopeName );
		return new DestructuringTargetDescriptor( true, segments );
	}

	private String extractObjectKeyName( BoxObjectDestructuringBinding binding ) {
		BoxExpression key = binding.getKey();
		if ( key instanceof BoxIdentifier id ) {
			return id.getName();
		}
		if ( key instanceof BoxStringLiteral stringLiteral ) {
			return stringLiteral.getValue();
		}
		if ( key instanceof BoxIntegerLiteral integerLiteral ) {
			return integerLiteral.getValue();
		}
		if ( key instanceof BoxFQN fqn ) {
			return fqn.getValue();
		}
		if ( key instanceof BoxScope scope ) {
			return scope.getName();
		}
		throw new ExpressionException( "Unsupported destructuring key [" + key.getClass().getSimpleName() + "]", binding.getPosition(),
		    binding.getSourceText() );
	}

	private boolean isExplicitScope( String scopeName ) {
		return switch ( scopeName.toLowerCase() ) {
			case "application", "arguments", "cgi", "client", "cookie", "form", "local", "request", "server", "session", "static", "this", "thread", "url", "variables" -> true;
			default -> false;
		};
	}

	private String quoteJavaString( String value ) {
		return "\""
		    + value
		        .replace( "\\", "\\\\" )
		        .replace( "\"", "\\\"" )
		        .replace( "\n", "\\n" )
		        .replace( "\r", "\\r" )
		        .replace( "\t", "\\t" )
		    + "\"";
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