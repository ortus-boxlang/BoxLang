package ortus.boxlang.parser

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName

sealed class BoxNode : Node()

data class CFScript(
	val body: List<Statement>
) : Node()

sealed class Statement : BoxNode()

data class Assignment(
	val left: Expression,
	val right: Expression
) : Statement()

data class Function(
	val identifier: String,
	val returnType: String,
	val parameters: List<String> = emptyList(),
	val body: List<Statement> = emptyList()
) : BoxNode()

data class Component(
	val identifier: String,
	val functions: List<Function>
) : Statement()

interface ExpressionType
sealed class Expression(var type: ExpressionType? = null) : BoxNode()

data class IntegerLiteral(val value: String) : Expression()
data class FloatLiteral(val value: String) : Expression()
data class StringLiteral(val value: String) : Expression()
data class BooleanLiteral(val value: String) : Expression()

data class Identifier(override val name: String) : Expression(), Named

enum class BinaryOperator {
	Concat
}

enum class ComparisonOperator {
	Equals
}

data class BinaryExpression(
	var left: Expression,
	var op: BinaryOperator,
	var right: Expression
) : Expression()

data class ComparisonExpression(
	var left: Expression,
	var op: ComparisonOperator,
	var right: Expression
) : Expression()

data class ScopeVariablesExpression(
	val key: String
) : Expression()

data class FunctionInvokationExpression(
	val name: ReferenceByName<Identifier>,
	val arguments: List<Expression>
) : Expression()


