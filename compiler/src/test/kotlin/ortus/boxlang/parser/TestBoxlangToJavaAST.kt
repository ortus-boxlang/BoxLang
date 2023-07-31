package ortus.boxlang.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import org.junit.Test
import ortus.boxlang.java.toJava
import java.io.File
import kotlin.test.assertEquals


class TestBoxlangToJavaAST : BaseTest() {

	private val javaParser = JavaParser()
	private val cfParser = CFLanguageParser()

	@Test
	fun dummyBoxlangToASTTest() {
		val file = File("../examples/cf/Test.cfc")
		val parseResult = cfParser.parse(file)
		require(parseResult.correct)
		requireNotNull(parseResult.root)

		val actualAst = parseResult.root!!.toJava()
		val expectedAst = javaParser.parse(File("../examples/cf/Test.java")).result.get()
		assertASTEqual(expectedAst, actualAst)
	}

	private fun assertASTEqual(expected: CompilationUnit, actual: CompilationUnit) = assertEquals(expected.toString(), actual.toString())

	private fun assertCodeEqual(expected: String, actual: String) = assertASTEqual(javaParser.parse(expected).result.get(), javaParser.parse(actual).result.get())
}
