package ortus.boxlang.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import junit.framework.TestCase.assertEquals
import org.junit.Test
import ortus.boxlang.java.toJava
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

	@Test
	fun cfToJavaTestSet() {
		val errors = mutableListOf<AssertionError>()

		retrieveTestCasesFolders().forEach { testFolder ->
			val cfFile = retrieveCfCase(testFolder)
			val javaFile = retrieveJavaCase(testFolder)
			val cfParseResult = CFLanguageParser().parse(cfFile)
			check(cfParseResult.correct) { "Parse failed: file://${cfFile.absolutePath}" }
			checkNotNull(cfParseResult.root) { "Parse result has no root: file://${cfFile.absolutePath}" }
			try {
				assertASTEqual(
					javaParser.parse(javaFile).result.get(),
					cfParseResult.root!!.toJava(),
					testFolder.pathString
				)
			} catch (e: AssertionError) {
				errors.add(e)
			}
		}

		assertEquals(0, errors.size, "Errors: ${errors.size}")
	}

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
		val file = Path(testsBaseFolder.pathString, "HelloWorld", "HelloWorld.cfm").toFile()
		val parseResult = cfParser.source(file).parse()
		require(parseResult.correct)
		requireNotNull(parseResult.root)

		val actualAst = parseResult.root!!.toJava()
		val expectedAst = javaParser.parse(
			Path(
				testsBaseFolder.pathString,
				"HelloWorld",
				"HelloWorld.java"
			).toFile()
		).result.get()
		assertASTEqual(expectedAst, actualAst)
	}

	private fun assertASTEqual(expected: CompilationUnit, actual: CompilationUnit, message: String = "") =
		assertEquals(message, expected.toString(), actual.toString())

	private fun assertCodeEqual(expected: String, actual: String) =
		assertASTEqual(javaParser.parse(expected).result.get(), javaParser.parse(actual).result.get())
}
