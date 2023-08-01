package ortus.boxlang.parser

import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class TestColdFusionParsing : BaseTest() {

	private fun testCfDirectory(path: String) {
		val cfLanguageParser = CFLanguageParser()
		val cfExtensions = setOf("cfc", "cfm", "cfml")
		val files = scanForFiles(path, cfExtensions, exclude = listOf())
		val errors = hashMapOf<String, Int>()
		var ok = 0
		var ignored: MutableList<String> = mutableListOf()
		var cfmlCount = 0
		var cfscriptCount = 0
		var totalLines = 0

		println("+----------+----------+----------")
		// TODO really?
		System.out.format("| %-8s | %-8s | %s%n", "TYPE", "ISSUES", "FILE")
		println("+----------+----------+----------")

		files.forEach { file ->
			totalLines += file.readLines().size

			val parser = cfLanguageParser.source(file)

			when (parser.language()) {
				CFLanguage.CFML -> cfmlCount++
				CFLanguage.CFScript -> cfscriptCount++
			}

			val result = parser.parseFirstStage()

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
				System.out.format("| %-8s | %8d | %s%n", parser.language(), result.issues.size, "file://${file.absolutePath}")
				errors[file.absolutePath] = result.issues.size
			}
		}

		println("+----------+----------+----------")

		ignored.forEach { println(it); println() }

		println("CFML: $cfmlCount")
		println("CFScript: $cfscriptCount")
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

	@Test
	fun testHelloWorld() {
		val file = File("../examples/cf_to_java/HelloWorld/HelloWorld.cfm")
		val firstStageParseResult = CFLanguageParser().source(file).parseFirstStage()
		assert(firstStageParseResult.correct) { "First stage parsing is not correct: ${file.absolutePath}" }
		assertNotNull(firstStageParseResult.root) { "Parse tree root node is null: ${file.absolutePath}" }
	}
}
