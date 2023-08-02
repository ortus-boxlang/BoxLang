package ortus.boxlang.parser

import com.strumenta.kolasu.model.ReferenceByName
import org.antlr.v4.runtime.ParserRuleContext

private fun ParserRuleContext.astConversionNotImplemented() = NotImplementedError(
	"""
		|AST conversion not implemented
		|node: ${this.javaClass.simpleName}
		|with children: ${this.children.joinToString(separator = "${System.lineSeparator()}\t") { "${it.javaClass.simpleName}" }}
		|for: <<
		|${this.text}
		|>>
	""".trimMargin("|")
)

// TODO: partially implemented
fun CFParser.ScriptContext.toAst(): CFScript {
	val statements = mutableListOf<Statement>()

	if (this.component() != null)
		statements += this.component().toAst()

	this.functionOrStatement().forEach { statements += it.toAst() }

	return CFScript(statements)
}

fun CFParser.FunctionOrStatementContext.toAst(): Statement {
	return when {
		this.function() != null -> this.function().toAst()
		this.statement() != null -> this.statement().toAst()
		else -> throw this.astConversionNotImplemented()
	}
}

fun CFParser.StatementContext.toAst(): Statement {
	return when {
		this.simpleStatement() != null -> this.simpleStatement().toAst()
		this.if_() != null -> this.if_().toAst()
		else -> throw this.astConversionNotImplemented()
	}
}

fun CFParser.IfContext.toAst() = IfStatement(
	condition = this.expression().toAst(),
	body = this.ifStmtBlock?.toAst() ?: listOf(this.ifStmt.toAst()),
	elseStatement = this.elseStmtBlock?.toAst() ?: this.elseStmt?.let { listOf(it.toAst()) }
)

fun CFParser.StatementBlockContext.toAst() = this.statement().map { it.toAst() }

fun CFParser.SimpleStatementContext.toAst(): Statement {
	return when {
		this.assignment() != null -> this.assignment().toAst()
		this.methodInvokation() != null -> MethodInvokationStatement(this.methodInvokation().toAst())
		else -> throw this.astConversionNotImplemented()
	}
}

fun CFParser.AssignmentContext.toAst() = Assignment(
	left = this.assignmentLeft().toAst(),
	right = this.assignmentRight().toAst()
)

fun CFParser.AssignmentLeftContext.toAst(): Expression {
	return when {
		this.assignmentLeft() == null -> this.accessExpression().toAst()
		else -> throw this.astConversionNotImplemented()
	}
}

fun CFParser.AssignmentRightContext.toAst() = this.expression().toAst()

fun CFParser.ExpressionContext.toAst(): Expression {
	return when {
		this.literalExpression() != null -> this.literalExpression().toAst()
		this.accessExpression() != null -> this.accessExpression().toAst()
		this.objectExpression() != null -> this.objectExpression().toAst()
		this.methodInvokation() != null -> this.methodInvokation().toAst()
		this.EQ() != null ||
			this.GT() != null ||
			this.GTE() != null ||
			this.LT() != null ||
			this.LTE() != null ||
			this.NEQ() != null -> ComparisonExpression(
			left = this.expression()[0].toAst(),
			right = this.expression()[1].toAst(),
			op = this.EQ()?.let { ComparisonOperator.Equal } ?: this.GT()?.let { ComparisonOperator.GreaterThan }
			?: this.GTE()?.let { ComparisonOperator.GreaterEqualsThan }
			?: this.LT()?.let { ComparisonOperator.LessThan } ?: this.LTE()?.let { ComparisonOperator.LessEqualThan }
			?: this.NEQ()?.let { ComparisonOperator.NotEqual } ?: throw this.astConversionNotImplemented()
		)

		this.AMPERSAND() != null -> BinaryExpression(
			left = this.expression()[0].toAst(),
			right = this.expression()[1].toAst(),
			op = this.AMPERSAND()?.let { BinaryOperator.Concat } ?: throw this.astConversionNotImplemented()
		)

		else -> throw this.astConversionNotImplemented()
	}
}

private fun CFParser.MethodInvokationContext.toAst() = MethodInvokationExpression(
	methodName = ReferenceByName(this.functionInvokation().identifier().text),
	arguments = this.functionInvokation().argumentList()?.argument()?.map { it.toAst() } ?: emptyList(),
	obj = when {
		this.objectExpression() != null -> this.objectExpression().toAst()
		this.accessExpression() != null -> this.accessExpression().toAst()
		else -> throw this.astConversionNotImplemented()
	}
)

private fun CFParser.LiteralExpressionContext.toAst(): LiteralExpression {
	return when {
		this.stringLiteral() != null -> this.stringLiteral().toAst()
		else -> throw this.astConversionNotImplemented()
	}
}

fun CFParser.ObjectExpressionContext.toAst(): AccessExpression {
	return when {
		this.functionInvokation() != null -> this.functionInvokation().toAst()
		this.identifier() != null -> ObjectAccessExpression(
			access = this.identifier().toAst()
		)

		else -> throw this.astConversionNotImplemented()
	}
}

private fun CFParser.FunctionInvokationContext.toAst() = FunctionInvokationExpression(
	name = ReferenceByName(name = this.identifier().text),
	arguments = this.argumentList()?.argument()?.map { it.toAst() } ?: emptyList()
)

private fun CFParser.ArgumentContext.toAst(): Expression {
	// TODO: consider the other possible expressions
	return this.expression()[0].toAst()
}

fun CFParser.AccessExpressionContext.toAst(): AccessExpression {
	return when {
		this.identifier() != null -> ObjectAccessExpression(
			access = this.identifier().toAst()
		)

		this.arrayAccess() != null -> this.arrayAccess().toAst()
		this.objectExpression() != null -> ObjectAccessExpression(
			access = this.accessExpression().toAst(),
			context = this.objectExpression().toAst()
		)

		else -> throw this.astConversionNotImplemented()
	}
}

private fun CFParser.IdentifierContext.toAst() = Identifier(
	name = this.text,
	scope = this.scope()?.text?.let { ReferenceByName(it) }
)

private fun CFParser.ScopeContext.toAst(): ScopeExpression {
	return when {
		this.VARIABLES() != null -> VariablesScopeExpression(this.text)
		else -> throw this.astConversionNotImplemented()
	}
}

fun CFParser.ArrayAccessContext.toAst(): AccessExpression {
	val context = when {
		this.identifier() != null -> ObjectAccessExpression(access = this.identifier().toAst())
		this.arrayAccess() != null -> this.arrayAccess().toAst()
		else -> throw this.astConversionNotImplemented()
	}
	return when {
		this.arrayAccessIndex().stringLiteral() != null -> ObjectAccessExpression(
			access = this.arrayAccessIndex().stringLiteral().toAst(),
			context = context
		)

		this.arrayAccessIndex().integerLiteral() != null -> ArrayAccessExpression(
			index = this.arrayAccessIndex().integerLiteral().toAst(),
			context = context
		)

		else -> throw this.astConversionNotImplemented()
	}
}

private fun CFParser.IntegerLiteralContext.toAst() = IntegerLiteral(value = this.text)

fun CFParser.StringLiteralContext.toAst(): StringLiteral {
	val value = StringBuffer()
	this.children.forEach { fragment ->
		when (fragment) {
			OPEN_QUOTE(), CLOSE_QUOTE() -> {}
			else -> value.append(fragment.text)
		}
	}
	return StringLiteral(value.toString())
}

// TODO: partially implemented
fun CFParser.ComponentContext.toAst() = Component(
	identifier = this.identifier()?.text ?: "",
	functions = this.functionOrStatement()
		.filter { it.function() != null }
		.map { it.function().toAst() }
)

// TODO: partially implemented
fun CFParser.FunctionContext.toAst() = FunctionDefinition(
	name = this.functionSignature().identifier().text,
	returnType = this.functionSignature().returnType()?.text ?: ""
)