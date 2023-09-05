package ortus.boxlang.parser

import com.strumenta.kolasu.parsing.ParsingResult
import org.junit.Test
import java.io.File
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class TestColdFusionToBoxlang : BaseTest() {

	@Test
	fun testHelloWorld() {
		val file = Path(baseExampleDirectory,"/cf_to_java/HelloWorld/HelloWorld.cfm").toFile()
		val result = CFLanguageParser().parse(file)
		assert(result.correct) { "Parsing is not correct: ${file.absolutePath}" }
		assertNotNull(result.root) { "AST root node is null: ${file.absolutePath}" }
	}

	@Test
	fun testTestBoxCoverageGenerator() {
		val file = Path(testboxDirectory, "system/coverage/data/CoverageGenerator.cfc").toFile()
		val result = testCfAst(file)
		assertEquals(listOf("configure", "beginCapture", "endCapture", "generateData", "isPathAllowed"), (result.root!!.body[0] as BoxComponent).functions.map { it.name })
	}

	@Test
	fun testTestBoxMockGenerator() {
		val file = Path(testboxDirectory, "system/mockutils/MockGenerator.cfc").toFile()
		val result = testCfAst(file)
		assertEquals("init", (result.root!!.body[0] as BoxComponent).functions[0].name)
		assertEquals(listOf("init", "generate", "outputQuotedValue", "writeStub", "removeStub", "generateCFC", "generateMethodsFromMD", "\$include"), (result.root!!.body[0] as BoxComponent).functions.map { it.name })
	}

	private fun testCfAst(file: File): ParsingResult<BoxScript> {
		val result = CFLanguageParser().parse(file)
		assertTrue(result.correct, result.issues.joinToString(System.lineSeparator()))
		return result
	}
}
