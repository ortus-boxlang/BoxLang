package ortus.boxlang.parser

import com.strumenta.kolasu.model.FileSource
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
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

class CFMLKolasuParser : KolasuParser<CFScript, CFMLParser, CFMLParser.HtmlDocumentContext, KolasuANTLRToken>(ANTLRTokenFactory()) {
	override fun createANTLRLexer(inputStream: InputStream, charset: Charset): Lexer {
		return CFMLLexer(CharStreams.fromStream(inputStream.withoutBOM()))
	}

	override fun createANTLRLexer(charStream: CharStream): Lexer {
		return CFMLLexer(charStream)
	}

	override fun createANTLRParser(tokenStream: TokenStream): CFMLParser {
		return CFMLParser(tokenStream)
	}

	override fun parse(file: File, charset: Charset, considerPosition: Boolean, measureLexingTime: Boolean): ParsingResult<CFScript> {
		return parse(file.inputStreamWithoutBOM(), charset, considerPosition, measureLexingTime, FileSource(file))
	}

	override fun parse(inputStream: InputStream, charset: Charset, considerPosition: Boolean, measureLexingTime: Boolean, source: Source?): ParsingResult<CFScript> {
		return super.parse(inputStream.withoutBOM(), charset, considerPosition, measureLexingTime, source)
	}

	override fun parseTreeToAst(
		parseTreeRoot: CFMLParser.HtmlDocumentContext,
		considerPosition: Boolean,
		issues: MutableList<Issue>,
		source: Source?
	): CFScript? {
		return parseTreeRoot.toAst()
	}
}
