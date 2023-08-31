package ortus.boxlang.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseResult
import com.github.javaparser.ast.CompilationUnit
import com.strumenta.kolasu.parsing.ParsingResult
import junit.framework.TestCase.assertEquals
import org.junit.Ignore
import org.junit.Test
import ortus.boxlang.java.BoxToJavaMapper
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
	fun javaCodeComparisonForHelloWorld() {

		// Parsing CFML to Boxlang AST
		val cfFile = Path(testsBaseFolder.pathString, "HelloWorld", "HelloWorld.cfm").toFile()
		val cfmlParseResult = cfParser.source(cfFile).parse()
		check(cfmlParseResult.correct) { "Cannot correctly parse the CF file: ${cfFile.absolutePath}" }
		checkNotNull(cfmlParseResult.root) { "Something may be wrong with the CF to Boxlang conversion: ${cfFile.absolutePath}" }

		// Converting Boxlang AST to Java
		val javaAST = BoxToJavaMapper(cfmlParseResult.root!!, cfFile).toJava()

		// Comparing the generated Java code with the expected one
		assertASTEqual(
			expected = parseJava(
				Path(testsBaseFolder.pathString, "HelloWorld", "HelloWorld.java").toFile()
			).result.orElseThrow(),
			actual = javaAST
		) { "" }
	}

	@Test
	fun compileAndRunHelloWorld() {

		// Parsing CFML to Boxlang AST
		val cfFile = Path(testsBaseFolder.pathString, "HelloWorld", "HelloWorld.cfm").toFile()
		val cfmlParseResult = cfParser.source(cfFile).parse()
		check(cfmlParseResult.correct) { "Cannot correctly parse the CF file: ${cfFile.absolutePath}" }
		checkNotNull(cfmlParseResult.root) { "Something may be wrong with the CF to Boxlang conversion: ${cfFile.absolutePath}" }

		println("Compiling: ${cfFile.absolutePath}")
		// Converting Boxlang AST to Java
		val javaAST = BoxToJavaMapper(cfmlParseResult.root!!, cfFile).toJava()


		val rootDirectory = File("build/cfml_to_java")
		val javaFile = File(
			Path(
				rootDirectory.absolutePath,
				javaAST.packageDeclaration.orElseThrow().nameAsString.replace(".", File.separator)
			).toFile(),
			javaAST.getClassByName("HelloWorld\$cfm").orElseThrow().nameAsString + ".java"
		)
		println("Compiling: ${javaFile.absolutePath}")
		println("Running: /home/madytyoo/IdeaProjects/boxlang/compiler/build/cfml_to_java/home/madytyoo/IdeaProjects/boxlang/examples/cf_to_java/HelloWorld/HelloWorld\$cfm.class")
		// Compiling Java and executing the invoke() method
		val outputStream = ByteArrayOutputStream()
		val printStream = PrintStream(outputStream)
		System.setOut(printStream)


		runClassFromCode(
			packageName = javaAST.packageDeclaration.orElseThrow().nameAsString,
			className = javaAST.getClassByName("HelloWorld\$cfm").orElseThrow().nameAsString,
			code = javaAST.toString()
		)

		// Testing stdout against the "Hello world" string
		assertEquals("Hello world${System.lineSeparator()}", outputStream.toString())
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
			.getStatement(2).asExpressionStmt()
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

	private fun runClassFromCode(packageName: String, className: String, code: String) {
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
		val mainMethod = cls.getDeclaredMethod("main", Array<String>::class.java)

		// Run the main method
		mainMethod.invoke(instance, arrayOf<String>())
	}
}
