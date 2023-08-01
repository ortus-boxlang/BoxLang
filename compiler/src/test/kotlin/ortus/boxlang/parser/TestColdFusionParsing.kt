package ortus.boxlang.parser

import com.strumenta.kolasu.parsing.ParsingResult
import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class TestColdFusionParsing : BaseTest() {

	private fun testCfDirectory(path: String) {
		val cfLanguageParser = CFLanguageParser()
		val cfExtensions = setOf("cfc") // TODO: add "cfm"
		val files = scanForFiles(path, cfExtensions, exclude = listOf())
		val errors = hashMapOf<String, Int>()
		var ok = 0
		var ignored: MutableList<String> = mutableListOf()
		var cfmlCount = 0
		var cfscriptCount = 0
		var unknownCount = 0
		var totalLines = 0

		println("+----------+----------+----------")
		// TODO really?
		System.out.format("| %-8s | %-8s | %s%n", "TYPE", "ISSUES", "FILE")
		println("+----------+----------+----------")

		files.forEach { file ->
			totalLines += file.readLines().size

			val unknownLanguageResult = cfLanguageParser.parseFirstStage(file)
			var type = "unknown"

			if (unknownLanguageResult.isCFScriptParsable()) {
				type = "CFScript"
				cfscriptCount++
			} else if (unknownLanguageResult.isCFMLParsable()) {
				type = "CFML"
				cfmlCount++
			} else {
				unknownCount++
			}

			val result = unknownLanguageResult.asCFScript().orCFML()

			// TODO: for now, ignore file with actual, expected errors
			if (file.name == "BaseSpec.cfc") {
				ignored += "Ignoring file://${file.absolutePath}${System.lineSeparator()}\t" +
					"There is a missing ',' in a statement"
			}
//			else if (file.name == "CoverageReporterTest.cfc" && result.issues[0].position?.start?.line == 77 ||
//				file.name == "BeforeAllFailures.cfc"
//			) {
//				ignored += "Ignoring file://${file.absolutePath}${System.lineSeparator()}\t" +
//					"There is a missing ';' in a statement"
//			}
			else if (result.correct) {
				ok++
			} else {
				// really?
				System.out.format("| %-8s | %8d | %s%n", type, result.issues.size, "file://${file.absolutePath}")
				errors[file.absolutePath] = result.issues.size
			}
		}

		println("+----------+----------+----------")

		ignored.forEach { println(it); println() }

		println("CFML: $cfmlCount")
		println("CFScript: $cfscriptCount")
		println("Unknown: $unknownCount")
		println("OK: ${ok}/${files.size}")
		println("Ignored: ${ignored.size}")
		println("Errors: ${errors.keys.size}")
		println("Total lines: $totalLines")
		println("Total errors: ${errors.values.sum()}")

		assertTrue(errors.isEmpty())
	}

	@Test
	fun testCfTestBox() {
		testCfDirectory(testboxDirectory)
	}

	@Ignore
	@Test
	fun testCfContentBox() {
		testCfDirectory(contentboxDirectory)
	}

	@Ignore
	@Test
	fun testCfColdBox() {
		testCfDirectory(coldboxDirectory)
	}

	private fun testCfAst(file: File): ParsingResult<CFScript> {
		val result = CFLanguageParser().parse(file)
		assertTrue(result.correct, result.issues.joinToString(System.lineSeparator()))
		return result
	}

	@Test
	fun testCfTestBoxAstCoverageGenerator() {
		val file = Path(testboxDirectory, "system/coverage/data/CoverageGenerator.cfc").toFile()
		val result = testCfAst(file)
		assertEquals(listOf("configure", "beginCapture", "endCapture", "generateData", "isPathAllowed"), (result.root!!.body[0] as Component).functions.map { it.identifier })
	}

	@Test
	fun testCfTestBoxAstMockGenerator() {
		val file = Path(testboxDirectory, "system/mockutils/MockGenerator.cfc").toFile()
		val result = testCfAst(file)
		assertEquals("init", (result.root!!.body[0] as Component).functions[0].identifier)
		assertEquals(listOf("init", "generate", "outputQuotedValue", "writeStub", "removeStub", "generateCFC", "generateMethodsFromMD", "\$include"), (result.root!!.body[0] as Component).functions.map { it.identifier })
	}

	@Test
	fun testCfExamplesHelloWorld() {
		val file = File("../examples/cf_to_java/HelloWorld/HelloWorld.cfm")
		val firstStageParseResult = CFLanguageParser().parseFirstStage(file)
			.asCFML()
			.orCFScript()
		assert(firstStageParseResult.correct) { "First stage parsing is not correct: ${file.absolutePath}" }
		assertNotNull(firstStageParseResult.root) { "Parse tree root node is null: ${file.absolutePath}" }

		val result = CFLanguageParser().parse(file)
		assert(result.correct) { "Parsing is not correct: ${file.absolutePath}" }
		assertNotNull(result.root) { "AST root node is null: ${file.absolutePath}" }
	}
}
