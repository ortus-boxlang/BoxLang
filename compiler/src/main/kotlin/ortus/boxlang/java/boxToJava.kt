package ortus.boxlang.java

import com.github.javaparser.ast.*
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.ModuleDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.VoidType
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.processNodesOfType
import ortus.boxlang.parser.*


class BoxToJavaMapper(
	private val boxAstRoot: BoxScript,
	private val fileName: String? = null
) {
	private val cu = CompilationUnit()

	fun toJava(): CompilationUnit =
		if (boxAstRoot.body.any { it !is BoxComponent })
			SingleScriptTemplate(
				boxAstRoot.body.filter { it !is BoxComponent },
				cu,
				fileName
			).toJava()
		else
			cu
}

class SingleScriptTemplate(
	private val scriptStatements: List<BoxStatement>,
	private val cu: CompilationUnit,
	private val fileName: String? = null
) {
	inner class ExecutionContextType : ClassOrInterfaceType("ExecutionContext") {
		init {
			cu.addImport("ortus.boxlang.runtime.ExecutionContext")
		}
	}

	private val executionContextType = ExecutionContextType()
	private val iTemplateType = ClassOrInterfaceType("ITemplate")
		.apply { cu.addImport("ortus.boxlang.runtime.dynamic.ITemplate") }
	private val iScopeType = ClassOrInterfaceType("IScope")
		.apply { cu.addImport("ortus.boxlang.runtime.scopes.IScope") }
	private val executionContextParameter = Parameter(executionContextType, "context")
	private val invokeMethodDeclaration = MethodDeclaration()
		.apply { name = SimpleName("invoke") }
		.apply {
			setBody(
				BlockStmt().apply {
					addStatement(
						ExpressionStmt(AssignExpr(
							VariableDeclarationExpr(iScopeType, "variablesScope"),
							MethodCallExpr(NameExpr("context"), "getVariablesScope"),
							AssignExpr.Operator.ASSIGN
						)))
				}
			)
		}
		.apply { addModifier(Modifier.Keyword.PUBLIC) }
		.apply { parameters = NodeList(executionContextParameter) }
		.apply { type = VoidType() }
		.apply { addThrownException(Throwable::class.java) }
	private val classDefinition = ClassOrInterfaceDeclaration()
		.apply { addMember(invokeMethodDeclaration) }
		.apply { fileName?.let { setName(it.replace(Regex("""(.*)\.([^.]+)"""), """$1\$$2""")) } }
		.apply { addModifier(Modifier.Keyword.PUBLIC) }
		.apply { addImplementedType(iTemplateType) }

	fun toJava(): CompilationUnit {
		scriptStatements.forEach {
			invokeMethodDeclaration.body.orElseThrow().addStatement(it.toJava())
		}
		return cu
			.apply { addType(classDefinition) }
	}
}

fun BoxScript.toJava(): com.github.javaparser.ast.CompilationUnit {
	val packageDeclaration = PackageDeclaration()

	val imports = NodeList<ImportDeclaration>()

	val statements = NodeList<TypeDeclaration<*>>()

	// When we have statements at the root level that are not contained in a Component
	// we want to define a Java class and map those statements
	// into an invoke() method
	//
	statements += this.body
		.filter { it !is BoxComponent }
		.takeIf { it.isNotEmpty() }
		?.fold(
			initial = BlockStmt(),
			operation = { block, statement -> block.apply { this.addStatement(statement.toJava()) } }
		)
		?.let { block ->
			ClassOrInterfaceDeclaration()
				.apply {
					this.addMethod("invoke")
						.apply { this.setBody(block) }
				}
		}

	// When Components are defined
	//
	this.processNodesOfType(
		BoxComponent::class.java,
		{ component -> statements.add(component.toJava()) }
	)

	val module = ModuleDeclaration()
	return com.github.javaparser.ast.CompilationUnit(packageDeclaration, imports, statements, null)
}

fun BoxComponent.toJava(): ClassOrInterfaceDeclaration {
	val classDeclaration = ClassOrInterfaceDeclaration()
	if (!this.identifier.isNullOrBlank())
		classDeclaration.name = SimpleName(this.identifier)
	this.functions.forEach {
		classDeclaration.addMethod(it.name)
	}
	return classDeclaration
}

fun BoxStatement.toJava(): Statement = when (this) {
	is BoxAssignment -> this.toJava()
	is BoxIfStatement -> this.toJava()
	is BoxMethodInvokationStatement -> this.toJava()
	else -> throw this.notImplemented()
}

fun BoxMethodInvokationStatement.toJava(): ExpressionStmt = ExpressionStmt(
	this.invokation.toJava()
)

fun BoxObjectAccessExpression.toJava(): Expression = when {
	this.context == null -> this.access.toJava()
	this.context != null -> FieldAccessExpr(
		this.context.toJava(),
		when (this.access) {
			is BoxIdentifier -> this.access.name
			is BoxStringLiteral -> this.access.value
			is BoxObjectAccessExpression -> when {
				this.access.context == null && this.access.access is BoxIdentifier -> this.access.access.name
				else -> throw this.notImplemented()
			}

			else -> throw this.notImplemented()
		})

	else -> throw this.notImplemented()
}

fun FunctionInvokationExpression.toJava(): MethodCallExpr {
	val arguments = this.arguments.map { it.toJava() }.toTypedArray()
	return MethodCallExpr(this.name.name, *arguments)
}

fun BoxAssignment.toJava(): ExpressionStmt = ExpressionStmt(
	AssignExpr(
		this.left.toJava(),
		this.right.toJava(),
		AssignExpr.Operator.ASSIGN
	)
)

fun BoxIfStatement.toJava(): IfStmt = IfStmt(
	this.condition.toJava(),
	BlockStmt(NodeList(this.body.map { it.toJava() })),
	BlockStmt(NodeList(this.elseStatement?.map { it.toJava() } ?: emptyList<Statement>()))
)

fun BoxExpression.toJava(): Expression = when (this) {
	is BoxObjectAccessExpression -> this.toJava()
	is BoxIdentifier -> this.toJava()
	is FunctionInvokationExpression -> this.toJava()
	is BoxStringLiteral -> this.toJava()
	is BoxMethodInvokationExpression -> this.toJava()
	is BoxComparisonExpression -> this.toJava()
	is BoxBinaryExpression -> this.toJava()
	else -> throw this.notImplemented()
}

fun BoxBinaryExpression.toJava(): BinaryExpr = BinaryExpr(
	this.left.toJava(),
	this.right.toJava(),
	when (this.op) {
		BoxBinaryOperator.Concat -> BinaryExpr.Operator.PLUS
	}
)

fun BoxComparisonExpression.toJava(): BinaryExpr = BinaryExpr(
	this.left.toJava(),
	this.right.toJava(),
	when (this.op) {
		BoxComparisonOperator.Equal -> BinaryExpr.Operator.EQUALS
		BoxComparisonOperator.GreaterThan -> BinaryExpr.Operator.GREATER
		BoxComparisonOperator.GreaterEqualsThan -> BinaryExpr.Operator.GREATER_EQUALS
		BoxComparisonOperator.LessThan -> BinaryExpr.Operator.LESS
		BoxComparisonOperator.LessEqualThan -> BinaryExpr.Operator.LESS_EQUALS
		BoxComparisonOperator.NotEqual -> BinaryExpr.Operator.NOT_EQUALS
	}
)

fun BoxMethodInvokationExpression.toJava(): MethodCallExpr = MethodCallExpr(
	this.obj.toJava(),
	this.methodName.name,
	NodeList(this.arguments.map { it.toJava() })
)

fun BoxIdentifier.toJava(): Expression = when {
	this.scope == null -> NameExpr(this.name)
	this.scope != null -> FieldAccessExpr(NameExpr(this.scope.name), this.name)
	else -> throw this.notImplemented()
}

fun BoxStringLiteral.toJava(): StringLiteralExpr = StringLiteralExpr(this.value)

fun BoxNode.notImplemented() = NotImplementedError(
	"""
		${this.javaClass.simpleName} with children:
		${this.children.map { it.javaClass.simpleName }}
	""".trimIndent()
)