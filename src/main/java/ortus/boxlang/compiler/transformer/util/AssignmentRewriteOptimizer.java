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
package ortus.boxlang.compiler.transformer.util;

import java.util.ArrayList;
import java.util.List;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;

/**
 * Shared assignment rewrite utilities for assignment transformers.
 */
public final class AssignmentRewriteOptimizer {

	private AssignmentRewriteOptimizer() {
	}

	/**
	 * Rewrites explicit self-assignment patterns such as {@code target = target + value}
	 * into their compound assignment forms where possible.
	 *
	 * @param assignment assignment node to inspect and optionally rewrite
	 */
	public static void optimizeCompoundAssignmentPatterns( BoxAssignment assignment ) {
		if ( !isSimpleEqualAssignmentCandidate( assignment ) ) {
			return;
		}

		if ( optimizeConcatEqualsPatternInternal( assignment ) ) {
			return;
		}

		if ( optimizeBinaryEqualsPattern( assignment, BoxBinaryOperator.Plus, BoxAssignmentOperator.PlusEqual ) ) {
			return;
		}

		if ( optimizeBinaryEqualsPattern( assignment, BoxBinaryOperator.Minus, BoxAssignmentOperator.MinusEqual ) ) {
			return;
		}

		if ( optimizeBinaryEqualsPattern( assignment, BoxBinaryOperator.Star, BoxAssignmentOperator.StarEqual ) ) {
			return;
		}

		if ( optimizeBinaryEqualsPattern( assignment, BoxBinaryOperator.Slash, BoxAssignmentOperator.SlashEqual ) ) {
			return;
		}

		optimizeBinaryEqualsPattern( assignment, BoxBinaryOperator.Mod, BoxAssignmentOperator.ModEqual );
	}

	/**
	 * Rewrites explicit concat assignment patterns of the form {@code target = target & ...}
	 * into {@link BoxAssignmentOperator#ConcatEqual} on the same assignment node.
	 *
	 * @param assignment assignment node to inspect and optionally rewrite
	 */
	public static void optimizeConcatEqualsPattern( BoxAssignment assignment ) {
		if ( !isSimpleEqualAssignmentCandidate( assignment ) ) {
			return;
		}

		optimizeConcatEqualsPatternInternal( assignment );
	}

	/**
	 * Returns true when the assignment can participate in explicit-self-assignment rewrite checks.
	 *
	 * @param assignment assignment node to evaluate
	 *
	 * @return {@code true} when assignment is plain equals with no modifiers and a non-null RHS
	 */
	private static boolean isSimpleEqualAssignmentCandidate( BoxAssignment assignment ) {
		return assignment.getOp() == BoxAssignmentOperator.Equal && assignment.getModifiers().isEmpty() && assignment.getRight() != null;
	}

	/**
	 * Rewrites explicit concat assignment patterns and reports whether a rewrite occurred.
	 *
	 * @param assignment assignment node to inspect and optionally rewrite
	 *
	 * @return {@code true} when assignment was rewritten to {@link BoxAssignmentOperator#ConcatEqual}
	 */
	private static boolean optimizeConcatEqualsPatternInternal( BoxAssignment assignment ) {
		if ( ! ( assignment.getRight() instanceof BoxStringConcat concat ) ) {
			return false;
		}

		List<BoxExpression> values = concat.getValues();
		if ( values == null || values.size() < 2 ) {
			return false;
		}

		if ( !isSupportedConcatAssignmentTarget( assignment.getLeft() ) || !sameReferenceTarget( assignment.getLeft(), values.get( 0 ) ) ) {
			return false;
		}

		BoxExpression newRight;
		if ( values.size() == 2 ) {
			newRight = values.get( 1 );
		} else {
			newRight = new BoxStringConcat( new ArrayList<>( values.subList( 1, values.size() ) ), concat.getPosition(), concat.getSourceText() );
		}

		assignment.setOp( BoxAssignmentOperator.ConcatEqual );
		assignment.setRight( newRight );
		return true;
	}

	/**
	 * Rewrites explicit binary self-assignment patterns of the form {@code target = target <op> value}
	 * into the provided compound assignment operator.
	 *
	 * @param assignment  assignment node to inspect and optionally rewrite
	 * @param operator    expected binary operator in the right-hand expression
	 * @param replacement replacement assignment operator when pattern matches
	 *
	 * @return {@code true} when the assignment was rewritten
	 */
	private static boolean optimizeBinaryEqualsPattern( BoxAssignment assignment, BoxBinaryOperator operator, BoxAssignmentOperator replacement ) {
		if ( ! ( assignment.getRight() instanceof BoxBinaryOperation binaryOperation ) ) {
			return false;
		}

		if ( binaryOperation.getOperator() != operator ) {
			return false;
		}

		if ( !isSupportedConcatAssignmentTarget( assignment.getLeft() ) || !sameReferenceTarget( assignment.getLeft(), binaryOperation.getLeft() ) ) {
			return false;
		}

		assignment.setOp( replacement );
		assignment.setRight( binaryOperation.getRight() );
		return true;
	}

	/**
	 * Determines whether an expression can participate in concat-assignment rewrite matching.
	 *
	 * @param expression assignment target expression
	 *
	 * @return {@code true} when target is an identifier, dot access, or array access
	 */
	private static boolean isSupportedConcatAssignmentTarget( BoxExpression expression ) {
		return expression instanceof BoxIdentifier || expression instanceof BoxDotAccess || expression instanceof BoxArrayAccess;
	}

	/**
	 * Performs structural equivalence checks for assignment targets used by concat rewrite matching.
	 *
	 * @param left  assignment left-hand side target
	 * @param right first concat operand candidate
	 *
	 * @return {@code true} if both expressions reference the same target shape
	 */
	private static boolean sameReferenceTarget( BoxExpression left, BoxExpression right ) {
		if ( left instanceof BoxScope leftScope && right instanceof BoxScope rightScope ) {
			return leftScope.getName().equalsIgnoreCase( rightScope.getName() );
		}

		if ( left instanceof BoxIdentifier leftId && right instanceof BoxIdentifier rightId ) {
			return leftId.getName().equalsIgnoreCase( rightId.getName() );
		}

		if ( left instanceof BoxDotAccess leftDot && right instanceof BoxDotAccess rightDot ) {
			if ( leftDot.isSafe() != rightDot.isSafe() ) {
				return false;
			}
			return sameReferenceTarget( leftDot.getContext(), rightDot.getContext() )
			    && sameReferenceTarget( leftDot.getAccess(), rightDot.getAccess() );
		}

		if ( left instanceof BoxArrayAccess leftArray && right instanceof BoxArrayAccess rightArray ) {
			if ( leftArray.isSafe() != rightArray.isSafe() ) {
				return false;
			}
			return sameReferenceTarget( leftArray.getContext(), rightArray.getContext() )
			    && sameReferenceTarget( leftArray.getAccess(), rightArray.getAccess() );
		}

		if ( left instanceof BoxIntegerLiteral leftInt && right instanceof BoxIntegerLiteral rightInt ) {
			return leftInt.getValue().equals( rightInt.getValue() );
		}

		if ( left instanceof BoxStringLiteral leftString && right instanceof BoxStringLiteral rightString ) {
			return leftString.getValue().equals( rightString.getValue() );
		}

		return false;
	}
}