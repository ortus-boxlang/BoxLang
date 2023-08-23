package ortus.boxlang.java

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseResult
import com.github.javaparser.ast.*
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.ModuleDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.processNodesOfType
import ortus.boxlang.parser.*
import java.io.File
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.pathString
import com.github.javaparser.ast.expr.Expression as JExpression


private val javaParser = JavaParser()

private fun parseJava(code: String): ParseResult<CompilationUnit> {
	val result = javaParser.parse(code)
	System.err.println(
		result.problems.joinToString(separator = System.lineSeparator()) { problem ->
			"${problem.message}${System.lineSeparator()}\t${problem.location.map { "${it.begin.range.map { ":${it.begin.line}:${it.begin.column}" }.orElse("")}" }.orElse("")}"
		}
	)
	return result
}

class BoxToJavaMapper(
	private val boxAstRoot: BoxScript,
	private val originalFile: File
) {
	fun toJava(): CompilationUnit =
		if (boxAstRoot.body.any { it !is BoxComponent }) {
			SingleScriptTemplate(
				boxAstRoot.body.filter { it !is BoxComponent },
				originalFile,
				"<JSON AST>",
				boxAstRoot
			).toJava()
		} else {
			CompilationUnit()
		}
}

class SingleScriptTemplate(
	private val scriptStatements: List<BoxStatement>,
	private val originalFile: File,
	private val jsonBoxAst: String? = null,
	private val boxAstRoot: BoxScript
) {
	private val className = originalFile.name.replace(Regex("""(.*)\.([^.]+)"""), """$1\$$2""") ?: "MockTemplate"
	private val fileName = originalFile.name
	private val fileExtension = originalFile.extension
	private val fileFolderPath = originalFile.parentFile?.toPath()?.toRealPath()?.pathString ?: ""
	private val packageName = Path(fileFolderPath).toRealPath().pathString.replace(File.separator, ".").replace(Regex("^."), "")
	private val lastModifiedTimestamp = Date(originalFile.lastModified()).toString()
	private val compiledOnTimestamp = Date().toString()
	private val templateAst = parseJava("""
			// Auto package creation according to file path on disk
			package $packageName;

			import ortus.boxlang.runtime.BoxRuntime;
			import ortus.boxlang.runtime.context.IBoxContext;

			// BoxLang Auto Imports
			import ortus.boxlang.runtime.dynamic.BaseTemplate;
			import ortus.boxlang.runtime.dynamic.Referencer;
			import ortus.boxlang.runtime.interop.DynamicObject;
			import ortus.boxlang.runtime.loader.ClassLocator;
			import ortus.boxlang.runtime.operators.*;
			import ortus.boxlang.runtime.scopes.Key;
			import ortus.boxlang.runtime.scopes.IScope;

			// Classes Auto-Imported on all Templates and Classes by BoxLang
			import java.time.LocalDateTime;
			import java.time.Instant;
			import java.lang.System;
			import java.lang.String;
			import java.lang.Character;
			import java.lang.Boolean;
			import java.lang.Double;
			import java.lang.Integer;

			public class $className extends BaseTemplate {

				// Auto-Generated Singleton Helpers
				private static $className instance;

				public $className() {
					this.name         = "$fileName";
					this.extension    = "$fileExtension";
					this.path         = "$fileFolderPath";
					// this.lastModified = "$lastModifiedTimestamp";
					// this.compiledOn   = "$compiledOnTimestamp";
					// this.ast = ???
				}

				public static synchronized $className getInstance() {
					if ( instance == null ) {
						instance = new $className();
					}
					return instance;
				}

				/**
				 * Each template must implement the invoke() method which executes the template
				 *
				 * @param context The execution context requesting the execution
				 */
				public void invoke( IBoxContext context ) throws Throwable {
					// Reference to the variables scope
					IScope variablesScope = context.getScopeLocal( Key.of( "variables" ) );

					ClassLocator JavaLoader = ClassLocator.getInstance();
				}

				public static void main( String[] args ) {
					// This is the main method, it will be invoked when the template is executed
					// You can use this
					// Get a runtime going
					BoxRuntime.startup( true );

					try {
						BoxRuntime.executeTemplate( $className.getInstance() );
					} catch ( Throwable e ) {
						e.printStackTrace();
						System.exit( 1 );
					}

					// Bye bye! Ciao Bella!
					BoxRuntime.shutdown();
				}
			}
		""".trimIndent())

	fun toJava(): CompilationUnit {
		// TODO: handle "orElseThrow" cases
		val cu = templateAst.result.orElseThrow().findCompilationUnit().orElseThrow()
		val invokeMethodBody = templateAst.result.orElseThrow()
			.findCompilationUnit().orElseThrow()
			.getClassByName(className).orElseThrow()
			.getMethodsByName("invoke")[0]
			.body.orElseThrow()

		// Add statements to the invoke method
		scriptStatements
			.map { it.toJava(cu) }
			.forEach { invokeMethodBody.addStatement(it) }

		// CFML/Boxlang AST generation code
		val cfmlAst = generationCode(boxAstRoot, cu)
		// TODO: inject this into the AST

		return cu
	}

	private fun generationCode(boxAst: BoxScript, cu: CompilationUnit) = ObjectCreationExpr(
		null,
		useTypeAndAddImport(BoxScript::class.java.name, cu),
		NodeList(
			ObjectCreationExpr(
				null,
				ClassOrInterfaceType("ArrayList"),
				NodeList(
					ObjectCreationExpr(
						null,
						ClassOrInterfaceType("BoxFunctionDefinition"),
						NodeList()
					)
				)
			)
		)
	)
}

private fun useTypeAndAddImport(fqn: String, cu: CompilationUnit? = null): ClassOrInterfaceType {
	cu?.addImport(fqn)
	return ClassOrInterfaceType(fqn.substring(fqn.lastIndexOf(".") + 1))
}

sealed class ScopeNameExpr(name: String) : NameExpr(name) {
	fun constructSetExpression(fieldName: String, value: JExpression) = ScopeSetExpression(this, fieldName, value)
	fun constructGetExpression(fieldName: String) = ScopeGetExpression(this, fieldName)
}

class VariablesScopeNameExpr : ScopeNameExpr("variablesScope")

class ScopeSetExpression(
	scopeNameExpression: ScopeNameExpr,
	fieldName: String,
	value: JExpression
) : MethodCallExpr(scopeNameExpression, "put", NodeList(fieldName.toKeyOf(), value))

class ScopeGetExpression(
	val scopeNameExpression: ScopeNameExpr,
	private val fieldName: String
) : MethodCallExpr(scopeNameExpression, "get", NodeList(fieldName.toKeyOf())) {
	fun toSetExpression(value: JExpression) = ScopeSetExpression(
		scopeNameExpression,
		fieldName,
		value)
}

class ScopeFindLocalMethodCall(key: String) : MethodCallExpr(
	NameExpr("context"),
	"scopeFindLocal",
	NodeList(key.toKeyOf())
)

class GetScopeLocalMethodCall(key: String) : MethodCallExpr(
	NameExpr("context"),
	"getScopeLocal",
	NodeList(key.toKeyOf())
)

class ReferencerGetExpression(
	scopesList: MutableList<JExpression> = mutableListOf()
) : MethodCallExpr(
	NameExpr("Referencer"),
	"get",
	NodeList(scopesList.map { if (it is NameExpr) it.toKeyOf() else it } + BooleanLiteralExpr(false))
) {
	fun addLastScope(scope: JExpression) = arguments.addLast(transform(scope))
	fun addFirstScope(scope: JExpression) = arguments.addFirst(transform(scope))
	private fun transform(expression: JExpression) =
		if (expression is NameExpr)
			expression.toKeyOf()
		else
			expression
}

class ReferencerGetAndInvokeExpression(
	methodName: String,
	arguments: List<JExpression> = listOf(),
	scope: JExpression? = null
) : MethodCallExpr(
	NameExpr("Referencer"),
	"getAndInvoke",
	NodeList(
		if (scope is NameExpr) scope.toKeyOf() else scope,
//		StringLiteralExpr(methodName),
		methodName.toKeyOf(),
		ArrayCreationExpr(
			ClassOrInterfaceType("Object"),
			NodeList(ArrayCreationLevel()),
			ArrayInitializerExpr(NodeList(arguments))
		),
		BooleanLiteralExpr(false)
	)
)

fun String.toKeyOf() = MethodCallExpr(
	useTypeAndAddImport("ortus.boxlang.runtime.scopes.Key").nameAsExpression,
	"of",
	NodeList(StringLiteralExpr(this))
)

fun NameExpr.toKeyOf() = this.nameAsString.toKeyOf()

fun NameExpr.toScopeFindLocal() = ScopeFindLocalMethodCall(nameAsString)

fun BoxScript.toJava(cu: CompilationUnit): com.github.javaparser.ast.CompilationUnit {
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
			operation = { block, statement -> block.apply { this.addStatement(statement.toJava(cu)) } }
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
		{ component -> statements.add(component.toJava(cu)) }
	)

	val module = ModuleDeclaration()
	return CompilationUnit(packageDeclaration, imports, statements, null)
}

fun BoxComponent.toJava(cu: CompilationUnit): ClassOrInterfaceDeclaration {
	val classDeclaration = ClassOrInterfaceDeclaration()
	if (!this.identifier.isNullOrBlank())
		classDeclaration.name = SimpleName(this.identifier)
	this.functions.forEach {
		classDeclaration.addMethod(it.name)
	}
	return classDeclaration
}

fun BoxStatement.toJava(cu: CompilationUnit): Statement = when (this) {
	is BoxAssignment -> this.toJava(cu)
	is BoxIfStatement -> this.toJava(cu)
	is BoxExpressionStatement -> this.toJava(cu)
	else -> throw this.notImplemented()
}

fun BoxExpressionStatement.toJava(cu: CompilationUnit): ExpressionStmt = ExpressionStmt(
	this.expression.toJava(cu)
)

fun BoxObjectAccessExpression.toJava(cu: CompilationUnit): JExpression {
	val scope = this.context.toJava(cu)

	return when (scope) {
		is ScopeNameExpr -> scope.constructGetExpression(
			when (this.access) {
				is BoxIdentifier -> this.access.name
				is BoxStringLiteral -> this.access.value
				else -> throw this.notImplemented()
			})

		is ReferencerGetExpression -> scope.apply { addFirstScope(scope) }

		else -> {
			ReferencerGetExpression(mutableListOf(scope, when (this.access) {
				is BoxIdentifier -> NameExpr(this.access.name)
				else -> this.access.toJava(cu)
			}))
		}
	}
}

fun FunctionInvokationExpression.toJava(cu: CompilationUnit): MethodCallExpr {
	/* TODO: move this toJava() definition inside the SingleScriptTemplate
		so that it knows about "context"
	*/
	val arguments = this.arguments.map { it.toJava(cu) }.toTypedArray()

	return if (this.name.name == "createObject" && arguments.size == 2) {
		MethodCallExpr(
			when {
				arguments[0] is StringLiteralExpr &&
					arguments[0].asStringLiteralExpr().value == "java" -> NameExpr("JavaLoader")

				else -> throw notImplemented()
			},
			"load",
			NodeList(mutableListOf<JExpression>(NameExpr("context")) + arguments.toList().subList(1, arguments.size))
		)
	} else {
		MethodCallExpr(this.name.name, *arguments)
	}
}

fun BoxAssignment.toJava(cu: CompilationUnit): ExpressionStmt {
	val assignmentLeftExpression = this.left.toJava(cu)
	val assignmentRightExpression = this.right.toJava(cu)

	val possiblyScopeExpression = when (assignmentLeftExpression) {
		is FieldAccessExpr -> assignmentLeftExpression.scope
		is ScopeGetExpression -> assignmentLeftExpression.scopeNameExpression
		else -> null
	}

	return ExpressionStmt(
		if (possiblyScopeExpression is ScopeNameExpr) {
			when (assignmentLeftExpression) {
				is FieldAccessExpr -> possiblyScopeExpression.constructSetExpression(
					assignmentLeftExpression.nameAsString,
					assignmentRightExpression
				)

				is ScopeGetExpression -> assignmentLeftExpression.toSetExpression(assignmentRightExpression)

				else -> throw notImplemented()
			}
		} else {
			AssignExpr(
				assignmentLeftExpression,
				assignmentRightExpression,
				AssignExpr.Operator.ASSIGN
			)
		}
	)
}

fun BoxIfStatement.toJava(cu: CompilationUnit): IfStmt = IfStmt(
	this.condition.toJava(cu),
	BlockStmt(NodeList(this.body.map { it.toJava(cu) })),
	this.elseStatement?.let { statement -> BlockStmt(NodeList(statement.map { it.toJava(cu) })) }
)

fun BoxExpression.toJava(cu: CompilationUnit): JExpression = when (this) {
	is BoxObjectAccessExpression -> this.toJava(cu)
	is BoxIdentifier -> this.toJava(cu)
	is FunctionInvokationExpression -> this.toJava(cu)
	is BoxStringLiteral -> this.toJava(cu)
	is BoxIntegerLiteral -> this.toJava(cu)
	is BoxMethodInvokationExpression -> this.toJava(cu)
	is BoxComparisonExpression -> this.toJava(cu)
	is BoxBinaryExpression -> this.toJava(cu)
	is BoxVariablesScopeExpression -> this.toJava(cu)
	is BoxArrayAccessExpression -> this.toJava(cu)
	else -> throw this.notImplemented()
}

fun BoxArrayAccessExpression.toJava(cu: CompilationUnit): JExpression = when (val index = this.index.toJava(cu)) {
	is StringLiteralExpr -> FieldAccessExpr(this.context.toJava(cu), index.value) // TODO: variables scope case
	else -> ArrayAccessExpr(this.context.toJava(cu), index)
}

fun BoxBinaryExpression.toJava(cu: CompilationUnit) = MethodCallExpr(
	TypeExpr(useTypeAndAddImport("ortus.boxlang.runtime.operators.Concat", cu)),
	"invoke",
	NodeList(
//		NameExpr("context"),
		this.left.toJava(cu).let {
			if (it.isNameExpr)
				it.asNameExpr().toKeyOf()
			else
				it
		},
		this.right.toJava(cu)
	)
)

fun BoxComparisonExpression.toJava(cu: CompilationUnit) = MethodCallExpr(
	when (this.op) {
		BoxComparisonOperator.Equal -> TypeExpr(useTypeAndAddImport("ortus.boxlang.runtime.operators.EqualsEquals", cu))
		else -> throw notImplemented()
	},
	"invoke",
//	NodeList(NameExpr("context"), this.left.toJava(cu), this.right.toJava(cu))
	NodeList(this.left.toJava(cu), this.right.toJava(cu))
)

fun BoxMethodInvokationExpression.toJava(cu: CompilationUnit): MethodCallExpr {
	val scope = this.obj.toJava(cu)

	return if (this.methodName.name == "init") {
		MethodCallExpr(
			scope,
			"invokeConstructor",
			NodeList(ArrayCreationExpr(
				ClassOrInterfaceType("Object"),
				NodeList(ArrayCreationLevel()),
				ArrayInitializerExpr(NodeList(this.arguments.map { it.toJava(cu) }))
			))
		)
	} else {
		ReferencerGetAndInvokeExpression(
			this.methodName.name,
			this.arguments.map { it.toJava(cu) },
			scope
		)
	}
}

fun BoxVariablesScopeExpression.toJava(cu: CompilationUnit) = VariablesScopeNameExpr()

fun BoxIdentifier.toJava(cu: CompilationUnit): JExpression = NameExpr(this.name).toScopeFindLocal()

fun BoxStringLiteral.toJava(cu: CompilationUnit): StringLiteralExpr = StringLiteralExpr(this.value)

fun BoxIntegerLiteral.toJava(cu: CompilationUnit): IntegerLiteralExpr = IntegerLiteralExpr(this.value)


fun BoxNode.notImplemented() = NotImplementedError(
	"""
		${this.javaClass.simpleName} with children:
		${this.children.map { it.javaClass.simpleName }}
	""".trimIndent()
)