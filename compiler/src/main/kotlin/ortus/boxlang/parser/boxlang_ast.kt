package ortus.boxlang.parser

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName

sealed class BoxNode : Node()

data class BoxScript(
	val body: List<BoxStatement>
) : Node()

sealed class BoxStatement : BoxNode()

data class BoxAssignment(
	val left: BoxExpression,
	val right: BoxExpression
) : BoxStatement()

data class BoxFunctionDefinition(
	override val name: String,
	val returnType: String,
	val parameters: List<String> = emptyList(),
	val body: List<BoxStatement> = emptyList()
) : BoxStatement(), Named

data class BoxMethodDefinition(
	override val name: String,
	val returnType: String,
	val parameters: List<String> = emptyList(),
	val body: List<BoxStatement> = emptyList()
) : BoxStatement(), Named

data class BoxComponent(
	val identifier: String,
	val functions: List<BoxFunctionDefinition>
) : BoxStatement()

interface BoxExpressionType
sealed class BoxExpression(var type: BoxExpressionType? = null) : BoxNode()

sealed class BoxLiteralExpression : BoxExpression()
data class BoxIntegerLiteral(val value: String) : BoxLiteralExpression()
data class BoxFloatLiteral(val value: String) : BoxLiteralExpression()
data class BoxStringLiteral(val value: String) : BoxLiteralExpression()
data class BoxBooleanLiteral(val value: String) : BoxLiteralExpression()

data class BoxIdentifier(
	override val name: String
) : BoxExpression(), Named

enum class BoxBinaryOperator {
	Concat
}

enum class BoxComparisonOperator {
	Equal,
	GreaterThan,
	GreaterEqualsThan,
	LessThan,
	LessEqualThan,
	NotEqual
}

data class BoxBinaryExpression(
	var left: BoxExpression,
	var op: BoxBinaryOperator,
	var right: BoxExpression
) : BoxExpression()

data class BoxComparisonExpression(
	var left: BoxExpression,
	var op: BoxComparisonOperator,
	var right: BoxExpression
) : BoxExpression()

sealed class BoxScopeExpression : BoxExpression()

class BoxVariablesScopeExpression : BoxScopeExpression()

sealed class BoxInvokationExpression : BoxAccessExpression()

data class FunctionInvokationExpression(
	val name: ReferenceByName<BoxFunctionDefinition>,
	val arguments: List<BoxExpression>
) : BoxInvokationExpression()

data class BoxMethodInvokationStatement(
	val invokation: BoxMethodInvokationExpression
) : BoxStatement()

data class BoxMethodInvokationExpression(
	val methodName: ReferenceByName<BoxMethodDefinition>,
	val obj: BoxExpression,
	val arguments: List<BoxExpression>
) : BoxInvokationExpression()

sealed class BoxAccessExpression : BoxExpression()

data class BoxArrayAccessExpression(
	val context: BoxAccessExpression,
	val index: BoxExpression
) : BoxAccessExpression()

data class BoxObjectAccessExpression(
	val context: BoxExpression? = null,
	val access: BoxExpression
) : BoxAccessExpression()

data class BoxIfStatement(
	val condition: BoxExpression,
	val body: List<BoxStatement>,
	val elseStatement: List<BoxStatement>?
) : BoxStatement()
