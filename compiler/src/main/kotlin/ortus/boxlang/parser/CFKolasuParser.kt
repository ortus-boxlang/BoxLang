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


class CFKolasuParser : KolasuParser<BoxScript, CFParser, CFParser.ScriptContext, KolasuANTLRToken>(ANTLRTokenFactory()) {
	override fun createANTLRLexer(inputStream: InputStream, charset: Charset): Lexer {
		return CFLexer(CharStreams.fromStream(inputStream.withoutBOM()))
	}

	override fun createANTLRLexer(charStream: CharStream): Lexer {
		return CFLexer(charStream)
	}

	override fun createANTLRParser(tokenStream: TokenStream): CFParser {
		return CFParser(tokenStream)
	}

	override fun parse(file: File, charset: Charset, considerPosition: Boolean, measureLexingTime: Boolean): ParsingResult<BoxScript> {
		return parse(file.inputStreamWithoutBOM(), charset, considerPosition, measureLexingTime, FileSource(file))
	}

	override fun parse(inputStream: InputStream, charset: Charset, considerPosition: Boolean, measureLexingTime: Boolean, source: Source?): ParsingResult<BoxScript> {
		return super.parse(inputStream.withoutBOM(), charset, considerPosition, measureLexingTime, source)
	}

	override fun parseTreeToAst(
		parseTreeRoot: CFParser.ScriptContext,
		considerPosition: Boolean,
		issues: MutableList<Issue>,
		source: Source?
	): BoxScript? {
		return parseTreeRoot.toAst()
	}
}
