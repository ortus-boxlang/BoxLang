package ortus.boxlang.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseResult
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.comments.Comment
import com.strumenta.kolasu.parsing.ParsingResult
import junit.framework.TestCase.assertEquals
import org.junit.Ignore
import org.junit.Test
import ortus.boxlang.java.BoxToJavaMapper
import ortus.boxlang.java.toJava
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*
import javax.tools.ToolProvider
import kotlin.io.path.*


class TestBoxlangToJavaAST : BaseTest() {

	private val testsBaseFolder = Path("../examples/cf_to_java")
	private val javaParser = JavaParser()
	private val cfParser = CFLanguageParser()

	private fun retrieveTestCasesFolders(): List<Path> =
		testsBaseFolder.listDirectoryEntries()
			.filter(Path::isDirectory)

	private fun retrieveCfCase(path: Path) = path.listDirectoryEntries()
		.first { !it.isDirectory() && it.extension in arrayOf("cfc", "cfm") }
		.toFile()

	private fun retrieveJavaCase(path: Path) = path.listDirectoryEntries()
		.first { !it.isDirectory() && it.extension == "java" }
		.toFile()

	@Ignore
	@Test
	fun helloWorldTest() {
		val packageName = "c_drive.projects.examples"
		val cfFile = Path(testsBaseFolder.pathString, "HelloWorld", "HelloWorld.cfm").toFile()
		val cfmlParseResult = cfmlToBoxlang(cfFile)
		check(cfmlParseResult.correct) { "Cannot correctly parse the CF file: ${cfFile.absolutePath}" }
		checkNotNull(cfmlParseResult.root) { "Something may be wrong with the CF to Boxlang conversion: ${cfFile.absolutePath}" }

		val javaFile = Path(testsBaseFolder.pathString, "HelloWorld", "HelloWorld.java").toFile()
		val javaParseResult = parseJava(javaFile)
		check(javaParseResult.isSuccessful) { "The Java file seems incorrect ${javaFile.absolutePath}" }
		check(javaParseResult.result.isPresent) { "The Java file parsing did not produce a result ${javaFile.absolutePath}" }

		val boxToJavaMapper = BoxToJavaMapper(cfmlParseResult.root!!, cfFile)
		val boxlangToJava = boxToJavaMapper.toJava()
		val expectedJavaAst = javaParseResult.result.orElseThrow()
		expectedJavaAst.walk(Comment::class.java) { it.remove() }
		expectedJavaAst.walk {
			it.setComment(null)
		}
		assertASTEqual(expectedJavaAst, boxlangToJava) { "" }
	}

	@Test
	fun runHelloWorldTest() {

		// Parsing CFML to Boxlang AST
		val cfFile = Path(testsBaseFolder.pathString, "HelloWorld", "HelloWorld.cfm").toFile()
		val cfmlParseResult = cfParser.source(cfFile).parse()
		check(cfmlParseResult.correct) { "Cannot correctly parse the CF file: ${cfFile.absolutePath}" }
		checkNotNull(cfmlParseResult.root) { "Something may be wrong with the CF to Boxlang conversion: ${cfFile.absolutePath}" }

		// Converting Boxlang AST to Java
		val javaAST = BoxToJavaMapper(cfmlParseResult.root!!, cfFile).toJava()

		// Compiling Java and executing the invoke() method
		val printStream = PrintStream(ByteArrayOutputStream())
		System.setOut(printStream)
		generateByteCode(
			packageName = javaAST.packageDeclaration.orElseThrow().nameAsString,
			className = javaAST.getClassByName("HelloWorld\$cfm").orElseThrow().nameAsString,
			code = javaAST.toString()
		)

		// Testing stdout against the "Hello world" string
		assertEquals("Hello world", printStream.toString())
	}

	@Test
	fun templateTest() {
		val cfmlFile = Path(testsBaseFolder.pathString, "HelloWorld", "HelloWorld.cfm").toFile()
		val className = cfmlFile.name.replace(Regex("""(.*)\.([^.]+)"""), """$1\$$2""") ?: "MockTemplate"
		val fileName = cfmlFile.name
		val fileExtension = cfmlFile.extension
		val fileFolderPath = cfmlFile.parentFile?.toPath()?.toRealPath()?.pathString ?: ""
		val packageName = Path(fileFolderPath).toRealPath().pathString.replace(File.separator, ".").replace(Regex("^."), "")
		val lastModifiedTimestamp = Date(cfmlFile.lastModified()).toString()
		val compiledOnTimestamp = Date().toString()

		val templateCode = """
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
					System.out.println("I am in!");

					// Reference to the variables scope
					IScope variablesScope = context.getScopeLocal( Key.of( "variables" ) );

					ClassLocator JavaLoader = ClassLocator.getInstance();
				}
			}
		""".trimIndent()

		val templateAst = parseJava(templateCode)
		templateAst.problems.forEach { println(it) }

		val cu = templateAst.result.orElseThrow().findCompilationUnit().orElseThrow()
		val cfmlAst = CFMLKolasuParser().parse(cfmlFile.readText())
		val cfscriptJavaStatements = cfmlAst.root!!.body.map { it.toJava(cu) }

		val invokeMethodBody = templateAst.result.orElseThrow()
			.findCompilationUnit().orElseThrow()
			.getClassByName(className).orElseThrow()
			.getMethodsByName("invoke")[0]
			.body.orElseThrow()

		// Convert Box statements to Java
		cfscriptJavaStatements.forEach { invokeMethodBody.addStatement(it) }

		val cfmlJava = cu.toString()

		generateByteCode(packageName, className, cfmlJava)
	}

	@Test
	fun dictionaryLikeObjectAccess() {
		val cfml = """
			<cfscript>
				foo['bar'] = 3;
			</cfscript>
		""".trimIndent()
		val cfmlParseResult = CFMLKolasuParser().parse(cfml)
		check(cfmlParseResult.correct)
		checkNotNull(cfmlParseResult.root)

		val javaAst = BoxToJavaMapper(cfmlParseResult.root!!, File("test.cfm")).toJava()
		val expr = javaAst
			.getClassByName("test\$cfm").orElseThrow()
			.getMethodsByName("invoke")
			.first()
			.body.orElseThrow()
			.getStatement(1).asExpressionStmt()
			.expression.asAssignExpr()
			.target
		assert(expr.isFieldAccessExpr) { expr::class.java }
	}

	private fun assertASTEqual(expected: CompilationUnit, actual: CompilationUnit, message: () -> String = { "" }) =
		assertEquals(message.invoke(), expected.toString(), actual.toString())

	private fun assertCodeEqual(expected: String, actual: String) =
		assertASTEqual(javaParser.parse(expected).result.get(), javaParser.parse(actual).result.get())

	private fun cfmlToBoxlang(file: File): ParsingResult<BoxScript> = cfParser.source(file).parse()
	private fun parseJava(file: File): ParseResult<CompilationUnit> {
		val result = javaParser.parse(file)
		System.err.println(
			result.problems.joinToString(separator = System.lineSeparator()) { problem ->
				"${problem.message}${System.lineSeparator()}\tat file://${file.absolutePath}${problem.location.map { "${it.begin.range.map { ":${it.begin.line}:${it.begin.column}" }.orElse("")}" }.orElse("")}"
			}
		)
		return result
	}

	private fun parseJava(code: String): ParseResult<CompilationUnit> {
		val result = javaParser.parse(code)
		System.err.println(
			result.problems.joinToString(separator = System.lineSeparator()) { problem ->
				"${problem.message}${System.lineSeparator()}\t${problem.location.map { "${it.begin.range.map { ":${it.begin.line}:${it.begin.column}" }.orElse("")}" }.orElse("")}"
			}
		)
		return result
	}

	private fun generateByteCode(packageName: String, className: String, code: String) {
		// see: https://stackoverflow.com/questions/2946338/how-do-i-programmatically-compile-and-instantiate-a-java-class

		// Write source to file
		val rootDirectory = File("build/cfml_to_java")
		val javaFile = File(
			Path(
				rootDirectory.absolutePath,
				packageName.replace(".", File.separator)
			).toFile(),
			"$className.java"
		)

		javaFile.parentFile.mkdirs()
		javaFile.writeText(code)

		// Compile
		val runtimeRoot = "../runtime/build/classes/java/main"
		val compiler = ToolProvider.getSystemJavaCompiler()
		compiler.run(null, null, null,
			"-cp",
			"${System.getProperty("java.class.path")}${File.pathSeparator}$runtimeRoot",
			javaFile.path
		)

		// Load and instantiate compiled class
		val classLoader = URLClassLoader.newInstance(arrayOf(
			rootDirectory.toURI().toURL(),
			File(runtimeRoot).toURI().toURL()
		))
		val cls = Class.forName("$packageName.$className", true, classLoader)
		val instance = cls.getDeclaredConstructor().newInstance()
		val iboxClass = Class.forName("ortus.boxlang.runtime.context.IBoxContext", true, classLoader)
		val iboxContextInstance = Class.forName("ortus.boxlang.runtime.context.TemplateBoxContext", true, classLoader)
			.getDeclaredConstructor().newInstance()
		val invokeMethod = instance.javaClass.getDeclaredMethod("invoke", iboxClass)
		invokeMethod.invoke(instance, iboxContextInstance)
	}
}
