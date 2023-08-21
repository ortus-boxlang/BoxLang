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
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertEquals


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
	fun cfToJavaTestSet() {
		val errors = mutableListOf<AssertionError>()

		retrieveTestCasesFolders().forEach { testFolder ->
			val cfFile = retrieveCfCase(testFolder)
			val javaFile = retrieveJavaCase(testFolder)
			val cfParseResult = CFLanguageParser().parse(cfFile)
			check(cfParseResult.correct) { "Parse failed: file://${cfFile.absolutePath}" }
			checkNotNull(cfParseResult.root) { "Parse result has no root: file://${cfFile.absolutePath}" }
			val actualJavaAst = javaParser.parse(javaFile).result.get()
				.walk(Comment::class.java) { TODO() }

			try {
				assertASTEqual(
					javaParser.parse(javaFile).result.get(),
					cfParseResult.root!!.toJava()
				) { testFolder.pathString }
			} catch (e: AssertionError) {
				errors.add(e)
			}
		}

		assertEquals(0, errors.size, "Errors: ${errors.size}")
	}

	@Ignore
	@Test
	fun dummyBoxlangToASTTest() {
		val file = Path(testsBaseFolder.pathString, "Test", "Test.cfc").toFile()
		val parseResult = cfParser.source(file).parse()
		require(parseResult.correct)
		requireNotNull(parseResult.root)

		val actualAst = parseResult.root!!.toJava()
		val expectedAst = javaParser.parse(
			Path(
				testsBaseFolder.pathString,
				"Test",
				"Test.java"
			).toFile()
		).result.get()
		assertASTEqual(expectedAst, actualAst)
	}

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

		val boxToJavaMapper = BoxToJavaMapper(cfmlParseResult.root!!, cfFile, packageName)
		val boxlangToJava = boxToJavaMapper.toJava()
		val expectedJavaAst = javaParseResult.result.orElseThrow()
		expectedJavaAst.walk(Comment::class.java) { it.remove() }
		expectedJavaAst.walk {
			it.setComment(null)
		}
		assertASTEqual(expectedJavaAst, boxlangToJava) { "" }
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
}
