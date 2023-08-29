package ortus.boxlang.parser;

import com.strumenta.kolasu.parsing.FirstStageParsingResult
import com.strumenta.kolasu.parsing.ParsingResult
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNodeImpl
import ortus.boxlang.parser.CFMLParser.HtmlDocumentContext
import ortus.boxlang.parser.CFParser.ScriptContext
import java.io.File

enum class CFLanguage {
	CFML,
	CFScript;

	fun knownExtensions() =
		if (this == CFML)
			setOf("cfml", "cfm", "cfc")
		else if (this == CFScript)
			setOf("cfc")
		else
			throw IllegalStateException("Unknown value: $this")
}

/*
	Usage:

	CFLanguageParser()
		.parseFirstStage(file)
		.asCFML()
		.orCFScript()

	CFLanguageParser()
		.parseFirstStage(file)
		.asCFML()
		.result

	CFLanguageParser()
		.parse(file)

	CFLanguageParser()
		.source(file)
		.parseFirstStage()

	CFLanguageParser()
		.source(code)
		.parseFirstStage()

	CFLanguageParser()
		.source(file)
		.parse()

	CFLanguageParser()
		.source(file)
		.isCFML()		// .isCFScript()
 */
class CFLanguageParser {
	open inner class UnknownCFLanguageCodeParser(private val code: String) {

		private lateinit var detectedLanguage: CFLanguage
		private lateinit var cfFirstStageParseResult: FirstStageParsingResult<ScriptContext>
		private lateinit var cfmlFirstStageParseResult: FirstStageParsingResult<HtmlDocumentContext>
		private lateinit var parseResult: ParsingResult<BoxScript>

		private fun getCfParseResult(): FirstStageParsingResult<ScriptContext> {
			if (!this::cfFirstStageParseResult.isInitialized)
				cfFirstStageParseResult = cfParser.parseFirstStage(code)
			return cfFirstStageParseResult
		}

		private fun getCfmlParseResult(): FirstStageParsingResult<HtmlDocumentContext> {
			if (!this::cfmlFirstStageParseResult.isInitialized)
				cfmlFirstStageParseResult = cfmlParser.parseFirstStage(code)
			return cfmlFirstStageParseResult
		}

		private fun getParseResult(): ParsingResult<BoxScript> {
			if (!this::parseResult.isInitialized)
				parseResult = when (detectLanguage()) {
					CFLanguage.CFML -> cfmlParser.parse(code)
					CFLanguage.CFScript -> cfParser.parse(code)
				}
			return parseResult
		}

		protected open fun detectLanguage(): CFLanguage {
			if (!this::detectedLanguage.isInitialized) {
				detectedLanguage = code.lineSequence()
					.firstOrNull { it.contains("component") }
					.let {
						if (it == null) {
							when {
								isParsable(getCfParseResult()) -> CFLanguage.CFScript
								isParsable(getCfmlParseResult()) -> CFLanguage.CFML
								else -> throw IllegalStateException("Unknown language")
							}
						} else if (it.contains("<cfcomponent")) {
							CFLanguage.CFML
						} else {
							CFLanguage.CFScript
						}
					}
			}
			return detectedLanguage
		}

		fun parseFirstStage(): FirstStageParsingResult<out ParserRuleContext> = when (detectLanguage()) {
			CFLanguage.CFML -> getCfmlParseResult()
			CFLanguage.CFScript -> getCfParseResult()
		}

		fun parse(): ParsingResult<BoxScript> = getParseResult()

		fun isCFML() = detectLanguage() == CFLanguage.CFML
		fun isCFScript() = detectLanguage() == CFLanguage.CFScript
		fun language() = detectLanguage()

		private fun isParsable(result: FirstStageParsingResult<out ParserRuleContext>) =
			result.root?.children?.any { it !is ErrorNodeImpl } ?: false
	}

	open inner class UnknownCFLanguageFileParser(
		private val file: File
	) : UnknownCFLanguageCodeParser(
		code = file.inputStreamWithoutBOM().bufferedReader().use { it.readText() }
	) {

		override fun detectLanguage(): CFLanguage {
			check(file.exists()) { "File does not exists! ${file.absolutePath}" }
			check(file.isFile) { "${file.absolutePath} is not a file" }

			var a = 0
			if (CFLanguage.CFScript.knownExtensions().contains(file.extension))
				a += 1
			if (CFLanguage.CFML.knownExtensions().contains(file.extension))
				a += 2
			return when (a) {
				0, 3 -> super.detectLanguage()
				1 -> CFLanguage.CFScript
				2 -> CFLanguage.CFML
				else -> throw java.lang.IllegalStateException()
			}
		}
	}

	@Deprecated("")
	open inner class UnknownCFLanguageFirstStageParser(private val file: File) {
		private lateinit var cfscriptParseResult: FirstStageParsingResult<CFParser.ScriptContext>
		private lateinit var cfmlParseResult: FirstStageParsingResult<CFMLParser.HtmlDocumentContext>

		fun asCFScript(): CFLanguageFirstStageParsingResult<CFParser.ScriptContext> {
			if (!this::cfscriptParseResult.isInitialized)
				cfscriptParseResult = cfParser.parseFirstStage(file.inputStreamWithoutBOM())
			return CFLanguageFirstStageParsingResult(file, cfscriptParseResult)
		}

		fun asCFML(): CFLanguageFirstStageParsingResult<CFMLParser.HtmlDocumentContext> {
			if (!this::cfmlParseResult.isInitialized)
				cfmlParseResult = cfmlParser.parseFirstStage(file.inputStreamWithoutBOM())
			return CFLanguageFirstStageParsingResult(file, cfmlParseResult)
		}

		private fun isParsable(result: CFLanguageFirstStageParsingResult<out ParserRuleContext>) =
			result.result.root?.children?.any { it !is ErrorNodeImpl } ?: false

		fun isCFScriptParsable() = isParsable(asCFScript())
		fun isCFMLParsable() = isParsable(asCFML())
	}

	@Deprecated("")
	inner class CFLanguageFirstStageParsingResult<C : ParserRuleContext>(
		file: File,
		val result: FirstStageParsingResult<C>
	) : UnknownCFLanguageFirstStageParser(file) {
		fun orCFScript() = if (isCFMLParsable()) this.result else asCFScript().result
		fun orCFML() = if (isCFScriptParsable()) this.result else asCFML().result
	}

	private val cfParser = CFKolasuParser()
	private val cfmlParser = CFMLKolasuParser()

	fun source(file: File) = UnknownCFLanguageFileParser(file)
	fun source(code: String) = UnknownCFLanguageCodeParser(code)

	@Deprecated("")
	fun parseFirstStage(file: File) = UnknownCFLanguageFirstStageParser(file)

	@Deprecated("")
	fun parse(file: File): ParsingResult<BoxScript> {
		// FIXME: this approach will perform the same first-stage parsing twice, either in A) or B) below
		val firstStageResult = parseFirstStage(file)
		return if (firstStageResult.isCFScriptParsable())
			cfParser.parse(file) // A)
		else if (firstStageResult.isCFMLParsable())
			cfmlParser.parse(file) // B)
		else
			throw IllegalArgumentException("Unknown CF language type for file: ${file.absolutePath}")
	}
}
