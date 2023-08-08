package ortus.boxlang.parser

import com.strumenta.kolasu.testing.assertASTsAreEqual
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

	@Test
	fun testScope() {
		val code = "variables.greeting = \"foo\""
		val parser = CFKolasuParser()
		val result = parser.parseStatement(code)
		val expected = BoxAssignment(
			left = BoxObjectAccessExpression(
				access = BoxIdentifier("greeting"),
				context = BoxVariablesScopeExpression()
			),
			right = BoxStringLiteral("foo")
		)
		assertASTsAreEqual(
			expected = expected,
			result
		)
	}

	@Test
	fun testScopeAccessAsDictionary() {
		val code = "variables['foo'] = \"bar\""
		val parser = CFKolasuParser()
		val result = parser.parseStatement(code)
		val expected = BoxAssignment(
			left = BoxObjectAccessExpression(
				access = BoxStringLiteral("foo"),
				context = BoxVariablesScopeExpression()
			),
			right = BoxStringLiteral("bar")
		)
		assertASTsAreEqual(
			expected = expected,
			result
		)
	}

	@Test
	fun testScopeReadingValue() {
		val code = "foo = variables['bar']"
		val parser = CFKolasuParser()
		val result = parser.parseStatement(code)
		val expected = BoxAssignment(
			left = BoxIdentifier("foo"),
			right = BoxObjectAccessExpression(
				access = BoxStringLiteral("bar"),
				context = BoxVariablesScopeExpression()
			)
		)
		assertASTsAreEqual(
			expected = expected,
			result
		)
	}

	@Test
	fun testArrayAccess() {
		val code = "ar[1][2] = ray[3]"
		val parser = CFKolasuParser()
		val result = parser.parseStatement(code)
		val expected = BoxAssignment(
			left = BoxArrayAccessExpression(
				index = BoxIntegerLiteral("2"),
				context = BoxArrayAccessExpression(
					index = BoxIntegerLiteral("1"),
					context = BoxIdentifier("ar")
				)
			),
			right = BoxArrayAccessExpression(
				index = BoxIntegerLiteral("3"),
				context = BoxIdentifier("ray")
			)
		)
		assertASTsAreEqual(
			expected = expected,
			result
		)
	}

	@Test
	fun testObjectAccessExpression() {
		val code = "variables.a.b.c = x.y.z"
		val parser = CFKolasuParser()
		val result = parser.parseStatement(code)
		val expected = BoxAssignment(
			left = BoxObjectAccessExpression(
				context = BoxVariablesScopeExpression(),
				access = BoxObjectAccessExpression(
					context = BoxIdentifier("a"),
					access = BoxObjectAccessExpression(
						context = BoxIdentifier("b"),
						access = BoxIdentifier("c"))
				)
			),
			right = BoxObjectAccessExpression(
				context = BoxIdentifier("x"),
				access = BoxObjectAccessExpression(
					context = BoxIdentifier("y"),
					access = BoxIdentifier("z")
				)
			)
		)
		assertASTsAreEqual(
			expected = expected,
			result
		)
	}
}
