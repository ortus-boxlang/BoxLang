package ortus.boxlang.compiler.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.IntervalSet;

import ortus.boxlang.parser.antlr.BoxGrammar;

public class BoxParserErrorStrategy extends ParserErrorStrategy {

	private static final Set<Character>			vowels				= new HashSet<>( Arrays.asList( 'a', 'e', 'i', 'o', 'u' ) );

	/**
	 * What to display in place of a specific token. If there is no entry for a token, the lexer spec name will be used as is.
	 */
	private static final Map<Integer, String>	tokenTranslation	= new HashMap<>() {

																		{
																			put( BoxGrammar.AMPAMP, "Operator" );
																			put( BoxGrammar.AMPERSAND, "Operator" );
																			put( BoxGrammar.AND, "Operator" );
																			put( BoxGrammar.ARROW, "->" );
																			put( BoxGrammar.ARROW_RIGHT, "=>" );
																			put( BoxGrammar.AT, "Annotation" );
																			put( BoxGrammar.BACKSLASH, "Operator" );
																			put( BoxGrammar.BANG, "Operator" );
																			put( BoxGrammar.BANGEQUAL, "Operator" );
																			put( BoxGrammar.BITWISE_AND, "Operator" );
																			put( BoxGrammar.BITWISE_COMPLEMENT, "Operator" );
																			put( BoxGrammar.BITWISE_OR, "Operator" );
																			put( BoxGrammar.BITWISE_SIGNED_LEFT_SHIFT, "Operator" );
																			put( BoxGrammar.BITWISE_SIGNED_RIGHT_SHIFT, "Operator" );
																			put( BoxGrammar.BITWISE_UNSIGNED_RIGHT_SHIFT, "Operator" );
																			put( BoxGrammar.BITWISE_XOR, "Operator" );
																			put( BoxGrammar.CASE, "Case clause" );
																			put( BoxGrammar.CASTAS, "Expression" );
																			put( BoxGrammar.CATCH, "Catch clause" );
																			put( BoxGrammar.CLASS, "Class definition" );
																			put( BoxGrammar.CLOSE_QUOTE, "String literal close" );
																			put( BoxGrammar.CLOSE_SQUOTE, "String literal close" );
																			put( BoxGrammar.COLON, "Operator" );
																			put( BoxGrammar.COLONCOLON, "Static accessor" );
																			put( BoxGrammar.COMMA, "','" );
																			put( BoxGrammar.COMPONENT_ISLAND_END, "Component island close" );
																			put( BoxGrammar.COMPONENT_ISLAND_START, "Component island" );
																			put( BoxGrammar.CONCATEQUAL, "Operator" );
																			put( BoxGrammar.CONTAIN, "Operator" );
																			put( BoxGrammar.CONTAINS, "Operator" );
																			put( BoxGrammar.CONTINUE, "Statement" );
																			put( BoxGrammar.DEFAULT, "Expression" );
																			put( BoxGrammar.DO, "Statement" );
																			put( BoxGrammar.DOES, "Statement" );
																			put( BoxGrammar.DOT, "'.' accessor" );
																			put( BoxGrammar.DOT_FLOAT_LITERAL, "Number" );
																			put( BoxGrammar.ELSE, "Else clause" );
																			put( BoxGrammar.ELVIS, "Operator" );
																			put( BoxGrammar.EQ, "Operator" );
																			put( BoxGrammar.EQEQ, "Operator" );
																			put( BoxGrammar.EQUAL, "Operator" );
																			put( BoxGrammar.EQUALSIGN, "=" );
																			put( BoxGrammar.EQV, "Operator" );
																			put( BoxGrammar.FALSE, "Expression" );
																			put( BoxGrammar.FINAL, "Access modifier" );
																			put( BoxGrammar.FINALLY, "Finally clause" );
																			put( BoxGrammar.FLOAT_LITERAL, "Number" );
																			put( BoxGrammar.FOR, "Statement" );
																			put( BoxGrammar.FUNCTION, "Function declaration" );
																			put( BoxGrammar.GE, "Operator" );
																			put( BoxGrammar.GREATER, "Operator" );
																			put( BoxGrammar.GT, "Operator" );
																			put( BoxGrammar.GTE, "Operator" );
																			put( BoxGrammar.GTESIGN, "Operator" );
																			put( BoxGrammar.GTSIGN, "Operator" );
																			put( BoxGrammar.HASHHASH, "## expression" );
																			put( BoxGrammar.ICHAR, "# expression" );
																			put( BoxGrammar.IDENTIFIER, "Identifier" );
																			put( BoxGrammar.IF, "Statement" );
																			put( BoxGrammar.IMP, "Operator" );
																			put( BoxGrammar.IMPORT, "Statement" );
																			put( BoxGrammar.IN, "in" );
																			put( BoxGrammar.INCLUDE, "Statement" );
																			put( BoxGrammar.INSTANCEOF, "Expression" );
																			put( BoxGrammar.INTEGER_LITERAL, "Number" );
																			put( BoxGrammar.INTERFACE, "Interface declaration" );
																			put( BoxGrammar.IS, "Operator" );
																			put( BoxGrammar.JAVA, "Statement" );
																			put( BoxGrammar.LBRACE, "'}'" );
																			put( BoxGrammar.LBRACKET, "'['" );
																			put( BoxGrammar.LE, "Operator" );
																			put( BoxGrammar.LESS, "Expression" );
																			put( BoxGrammar.LESSTHANGREATERTHAN, "Operator" );
																			put( BoxGrammar.LPAREN, "'('" );
																			put( BoxGrammar.LT, "Operator" );
																			put( BoxGrammar.LTE, "Operator" );
																			put( BoxGrammar.LTESIGN, "Operator" );
																			put( BoxGrammar.LTSIGN, "Operator" );
																			put( BoxGrammar.MINUS, "Operator" );
																			put( BoxGrammar.MINUSEQUAL, "Operator" );
																			put( BoxGrammar.MINUSMINUS, "Operator" );
																			put( BoxGrammar.MOD, "Operator" );
																			put( BoxGrammar.MODEQUAL, "Operator" );
																			put( BoxGrammar.NEQ, "Operator" );
																			put( BoxGrammar.NEW, "Statement" );
																			put( BoxGrammar.OPEN_QUOTE, "String literal" );
																			put( BoxGrammar.OR, "Operator" );
																			put( BoxGrammar.PACKAGE, "Statement" );
																			put( BoxGrammar.PARAM, "Component declaration" );
																			put( BoxGrammar.PERCENT, "Operator" );
																			put( BoxGrammar.PIPE, "Operator" );
																			put( BoxGrammar.PIPEPIPE, "Operator" );
																			put( BoxGrammar.PLUS, "Operator" );
																			put( BoxGrammar.PLUSEQUAL, "Operator" );
																			put( BoxGrammar.PLUSPLUS, "Operator" );
																			put( BoxGrammar.POWER, "Operator" );
																			put( BoxGrammar.PRIVATE, "Access modifier" );
																			put( BoxGrammar.PROPERTY, "Property declaration" );
																			put( BoxGrammar.PUBLIC, "Access modifier" );
																			put( BoxGrammar.QM, "Operator" );
																			put( BoxGrammar.RBRACE, "'}'" );
																			put( BoxGrammar.RBRACKET, "']'" );
																			put( BoxGrammar.REMOTE, "Statement" );
																			put( BoxGrammar.RETHROW, "Statement" );
																			put( BoxGrammar.RETURN, "Statement" );
																			put( BoxGrammar.RPAREN, "')'" );
																			put( BoxGrammar.SEMICOLON, "Operator" );
																			put( BoxGrammar.SHASHHASH, "## expression" );
																			put( BoxGrammar.SLASH, "Operator" );
																			put( BoxGrammar.SLASHEQUAL, "Operator" );
																			put( BoxGrammar.STAR, "Operator" );
																			put( BoxGrammar.STAREQUAL, "Operator" );
																			put( BoxGrammar.STATIC, "Access modifier" );
																			put( BoxGrammar.STRING_LITERAL, "String literal" );
																			put( BoxGrammar.SWITCH, "Statement" );
																			put( BoxGrammar.TEQ, "Operator" );
																			put( BoxGrammar.THAN, "Expression" );
																			put( BoxGrammar.THROW, "Statement" );
																			put( BoxGrammar.TRY, "Statement" );
																			put( BoxGrammar.VAR, "Statement" );
																			put( BoxGrammar.WHEN, "Statement" );
																			put( BoxGrammar.WHILE, "Statement" );
																			put( BoxGrammar.XOR, "Expression" );
																		}
																	};

	/**
	 * A translation of rule names to more human-readable names. If there is no entry for a rule, the rule name will be skipped
	 * in message construction, so that we can keep the error message as concise as possible without losing context.
	 * <p>
	 * Note that duplicates will be removed from the error message.
	 * </p>
	 */
	private static final Map<String, String>	ruleTranslation		= new HashMap<>() {

																		{
																			put( "boxClass", "box class" );
																			put( "identifier", "identifier" );
																			put( "script", "box script" );
																			put( "testExpression", "expression" );
																			put( "importStatement", "import statement" );
																			put( "classBody", "class body" );
																			put( "staticInitializer", "static initializer" );
																			put( "interface", "interface definition" );
																			put( "function", "function definition" );
																			put( "modifier", "access modifier" );
																			put( "returnType", "access modifier" );
																			put( "functionParamList", "function parameter" );
																			put( "functionParam", "function parameter" );
																			put( "preAnnotation", "annotation" );
																			put( "arrayLiteral", "array literal" );
																			put( "postAnnotation", "annotation" );
																			put( "annotation", "annotation" );
																			put( "property", "property definition" );
																			put( "not", "not statement" );
																			put( "component", "component declaration" );
																			put( "param", "param declaration" );
																			put( "if", "if statement" );
																			put( "for", "for statement" );
																			put( "do", "do statement" );
																			put( "while", "while statement" );
																			put( "assert", "assert statement" );
																			put( "break", "break statement" );
																			put( "continue", "continue statement" );
																			put( "return", "return statement" );
																			put( "throw", "throw statement" );
																			put( "switch", "switch statement" );
																			put( "case", "case clause" );
																			put( "componentIsland", "component island" );
																			put( "try", "try statement" );
																			put( "catches", "catch clause" );
																			put( "finally", "finally clause" );
																			put( "stringLiteral", "string literal" );
																			put( "structExpression", "struct expression" );
																			put( "new", "new statement" );
																			put( "expression", "expression" );
																			put( "el2", "expression" );
																			put( "anonymousFunction", "lambda or closure function" );
																		}
																	};
	/**
	 * Keywords that are also identifiers and can be excluded from the expected set when IDENTIFIER is expected
	 * as otherwise it just clutters the error message with all the keywords that can also be identifiers.
	 */
	private static final IntervalSet			keywordIDENTIFIERS	= new IntervalSet( BoxGrammar.ABSTRACT, BoxGrammar.AND,
	    BoxGrammar.AS, BoxGrammar.ASSERT, BoxGrammar.BREAK, BoxGrammar.CASE,
	    BoxGrammar.CASTAS, BoxGrammar.CATCH, BoxGrammar.CLASS, BoxGrammar.CONTAIN, BoxGrammar.CONTAINS, BoxGrammar.CONTINUE,
	    BoxGrammar.DEFAULT, BoxGrammar.DO, BoxGrammar.DOES, BoxGrammar.ELSE, BoxGrammar.EQ,
	    BoxGrammar.EQUAL, BoxGrammar.EQV, BoxGrammar.FALSE, BoxGrammar.FINAL, BoxGrammar.FINALLY, BoxGrammar.FOR,
	    BoxGrammar.FUNCTION, BoxGrammar.GE, BoxGrammar.GREATER, BoxGrammar.GT, BoxGrammar.GTE, BoxGrammar.IF,
	    BoxGrammar.IMP, BoxGrammar.IMPORT, BoxGrammar.IN, BoxGrammar.INCLUDE, BoxGrammar.INSTANCEOF, BoxGrammar.INTERFACE,
	    BoxGrammar.IS, BoxGrammar.JAVA, BoxGrammar.LE, BoxGrammar.LESS, BoxGrammar.LT, BoxGrammar.LTE,
	    BoxGrammar.MOD, BoxGrammar.NEQ, BoxGrammar.NEW, BoxGrammar.NOT, BoxGrammar.NULL,
	    BoxGrammar.OR, BoxGrammar.PACKAGE, BoxGrammar.PARAM, BoxGrammar.PRIVATE, BoxGrammar.PROPERTY,
	    BoxGrammar.PUBLIC, BoxGrammar.REMOTE, BoxGrammar.REQUIRED, BoxGrammar.RETHROW,
	    BoxGrammar.RETURN, BoxGrammar.STATIC,
	    BoxGrammar.THAN, BoxGrammar.THROW, BoxGrammar.TO, BoxGrammar.TRUE, BoxGrammar.TRY,
	    BoxGrammar.VAR, BoxGrammar.WHEN, BoxGrammar.WHILE, BoxGrammar.XOR );

	/**
	 * Generates an explanatory message concerning where we were in the parse when the error occurred. It will
	 * eliminate duplicates from the message, and will use the ruleTranslation map to provide more human-readable
	 * descriptions of the rules. Consecutive duplicates are removed so that we do not generate things like:
	 * in an expression, in an expression, in an expression, in an expression, in an expression, in an expression,
	 * you get the point...
	 *
	 * @param recognizer the ANTLR generated recognizer in use. As this instance of the parser strategy is
	 *                   specific to the BoxLang grammar, we could safely cast this to a BoxParser instance if
	 *                   it were ever useful.
	 * @param e          the exception that caused the error, which is used to traverse the call stack and identify
	 *                   ANTLR rules of interest in message generation.
	 *
	 * @return a string message that explains where the error occurred in the parse, which the ErrorListener will receive
	 */
	@Override
	protected String generateMessage( Parser recognizer, RecognitionException e ) {
		List<String> messages = new ArrayList<>();

		for ( StackTraceElement traceElement : e.getStackTrace() ) {
			String	methodName			= traceElement.getMethodName();

			String	translatedMessage	= ruleTranslation.get( methodName );

			if ( translatedMessage != null ) {
				boolean shouldAppend = methodName.equals( "boxClass" ) || methodName.equals( "interface" ) || methodName.equals( "script" ) ? messages.isEmpty()
				    : messages.isEmpty() || !messages.getLast().equals( translatedMessage );

				if ( shouldAppend ) {
					messages.add( translatedMessage );

				}
			}
		}

		if ( !messages.isEmpty() ) {
			String initialMessage = "while parsing " + articleFor( messages.getFirst() ) + " " + messages.getFirst();

			return messages.stream().skip( 1 ).reduce( initialMessage, ( acc, message ) -> acc + " in " + articleFor( message ) + " " + message );

		}
		return "";
	}

	private String articleFor( String word ) {
		return ( word != null && !word.isEmpty() && vowels.contains( Character.toLowerCase( word.charAt( 0 ) ) ) ) ? "an" : "a";
	}

	/**
	 * Builds the expected message when a syntax error results in a token that breaks teh syntax and we know which tokens would
	 * have made sense at that point.
	 *
	 * @param recognizer The BoxParserGrammar instance in use
	 * @param expected   an IntervalSet of the expected tokens at this point in the parse
	 * 
	 * @return the generated message without duplicates or naming every possible keyword that is an identifier possibility
	 */
	@Override
	protected String buildExpectedMessage( Parser recognizer, IntervalSet expected ) {
		IntervalSet	expect					= expected.contains( BoxGrammar.IDENTIFIER ) ? removeIdKeywords( expected ) : expected;

		Set<String>	uniqueExpectedTokens	= expect.toList().stream()
		    .map( tokenId -> tokenTranslation.getOrDefault( tokenId, recognizer.getVocabulary().getDisplayName( tokenId ).toLowerCase() ) )
		    .collect( Collectors.toSet() );

		if ( uniqueExpectedTokens.size() <= 12 ) {
			return uniqueExpectedTokens.stream().sorted( capitalizedSort ).collect( Collectors.joining( ", " ) );

		}
		return uniqueExpectedTokens.stream().sorted( capitalizedSort ).limit( 12 ).collect( Collectors.joining( ", " ) ) + "...";
	}

	private IntervalSet removeIdKeywords( IntervalSet set ) {
		return set.subtract( keywordIDENTIFIERS );
	}
}