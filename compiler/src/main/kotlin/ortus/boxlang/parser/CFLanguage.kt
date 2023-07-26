package ortus.boxlang.parser;

import com.strumenta.kolasu.parsing.FirstStageParsingResult
import com.strumenta.kolasu.parsing.ParsingResult
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ErrorNodeImpl
import java.io.File
import java.lang.IllegalArgumentException

// Usage:
// CFLanguageParser()
//      .parseFirstStage(file)
//      .asCFML()
//      .orCFScript()
//
// CFLanguageParser()
//      .parseFirstStage(file)
//      .asCFML()
//      .result
//
class CFLanguageParser {
    open inner class UnknownCFLanguageFirstStageParser(private val file: File) {
        private lateinit var cfscriptParseResult: FirstStageParsingResult<CFParser.ScriptContext>
        private lateinit var cfmlParseResult: FirstStageParsingResult<CFMLParser.HtmlDocumentContext>

        fun asCFScript() : CFLanguageFirstStageParsingResult<CFParser.ScriptContext> {
            if (!this::cfscriptParseResult.isInitialized)
                cfscriptParseResult = cfParser.parseFirstStage(file)
            return CFLanguageFirstStageParsingResult(file, cfscriptParseResult)
        }

        fun asCFML() : CFLanguageFirstStageParsingResult<CFMLParser.HtmlDocumentContext> {
            if (!this::cfmlParseResult.isInitialized)
                cfmlParseResult = cfmlParser.parseFirstStage(file)
            return CFLanguageFirstStageParsingResult(file, cfmlParseResult)
        }

        private fun isParsable(result: CFLanguageFirstStageParsingResult<out ParserRuleContext>) =
            result.result.root?.children?.any { it !is ErrorNodeImpl } ?: false
        fun isCFScriptParsable() = isParsable(asCFScript())
        fun isCFMLParsable() = isParsable(asCFML())
    }

    inner class CFLanguageFirstStageParsingResult<C : ParserRuleContext>(
        file: File,
        val result: FirstStageParsingResult<C>
    ) : UnknownCFLanguageFirstStageParser(file) {
        fun orCFScript() = if (isCFMLParsable()) this.result else asCFScript().result
        fun orCFML() = if (isCFScriptParsable()) this.result else asCFML().result
    }

    private val cfParser = CFKolasuParser()
    private val cfmlParser = CFMLKolasuParser()

    fun parseFirstStage(file: File) = UnknownCFLanguageFirstStageParser(file)

    fun parse(file: File) : ParsingResult<CFScript> {
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
