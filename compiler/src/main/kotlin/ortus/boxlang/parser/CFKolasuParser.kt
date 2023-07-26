package ortus.boxlang.parser

import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.parsing.ANTLRTokenFactory
import com.strumenta.kolasu.parsing.KolasuANTLRToken
import com.strumenta.kolasu.parsing.KolasuParser
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.TokenStream
import java.io.InputStream
import java.nio.charset.Charset


class CFKolasuParser : KolasuParser<CFScript, CFParser, CFParser.ScriptContext, KolasuANTLRToken>(ANTLRTokenFactory()) {
    override fun createANTLRLexer(inputStream: InputStream, charset: Charset): Lexer {
        return CFLexer(CharStreams.fromStream(inputStream))
    }

    override fun createANTLRLexer(charStream: CharStream): Lexer {
        return CFLexer(charStream)
    }

    override fun createANTLRParser(tokenStream: TokenStream): CFParser {
        return CFParser(tokenStream)
    }

    override fun parse(code: String, considerPosition: Boolean, measureLexingTime: Boolean): ParsingResult<CFScript> {
        return super.parse(code, considerPosition, measureLexingTime)
    }

    override fun parseTreeToAst(
        parseTreeRoot: CFParser.ScriptContext,
        considerPosition: Boolean,
        issues: MutableList<Issue>,
        source: Source?
    ): CFScript? {
        return parseTreeRoot.toAst()
    }
}
