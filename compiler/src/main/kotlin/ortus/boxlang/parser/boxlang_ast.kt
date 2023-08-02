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

data class FunctionDefinition(
	override val name: String,
	val returnType: String,
	val parameters: List<String> = emptyList(),
	val body: List<Statement> = emptyList()
) : Statement(), Named

data class MethodDefinition(
	override val name: String,
	val returnType: String,
	val parameters: List<String> = emptyList(),
	val body: List<Statement> = emptyList()
) : Statement(), Named

data class Component(
	val identifier: String,
	val functions: List<FunctionDefinition>
) : Statement()

interface ExpressionType
sealed class Expression(var type: ExpressionType? = null) : BoxNode()

sealed class LiteralExpression : Expression()
data class IntegerLiteral(val value: String) : LiteralExpression()
data class FloatLiteral(val value: String) : LiteralExpression()
data class StringLiteral(val value: String) : LiteralExpression()
data class BooleanLiteral(val value: String) : LiteralExpression()

data class Identifier(
	val scope: ReferenceByName<ScopeExpression>?,
	override val name: String
) : Expression(), Named

enum class BinaryOperator {
	Concat
}

enum class ComparisonOperator {
	Equal,
	GreaterThan,
	GreaterEqualsThan,
	LessThan,
	LessEqualThan,
	NotEqual
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

sealed class ScopeExpression(
	override val name: String
) : Expression(), Named

data class VariablesScopeExpression(
	override val name: String
) : ScopeExpression(name)

sealed class InvokationExpression : AccessExpression()

data class FunctionInvokationExpression(
	val name: ReferenceByName<FunctionDefinition>,
	val arguments: List<Expression>
) : InvokationExpression()

data class MethodInvokationStatement(
	val invokation: MethodInvokationExpression
) : Statement()

data class MethodInvokationExpression(
	val methodName: ReferenceByName<MethodDefinition>,
	val obj: AccessExpression,
	val arguments: List<Expression>
) : InvokationExpression()

sealed class AccessExpression : Expression()

data class ArrayAccessExpression(
	val context: AccessExpression,
	val index: Expression
) : AccessExpression()

data class ObjectAccessExpression(
	val context: AccessExpression? = null,
	val access: Expression
) : AccessExpression()

data class IfStatement(
	val condition: Expression,
	val body: List<Statement>,
	val elseStatement: List<Statement>?
) : Statement()
