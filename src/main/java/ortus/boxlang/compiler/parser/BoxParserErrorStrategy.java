package ortus.boxlang.compiler.parser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.IntervalSet;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;

import java.util.*;
import java.util.stream.Collectors;

public class BoxParserErrorStrategy extends ParserErrorStrategy {

	private static final Set<Character>			vowels				= new HashSet<>( Arrays.asList( 'a', 'e', 'i', 'o', 'u' ) );

	/**
	 * What to display in place of a specific token. If there is no entry for a token, the lexer spec name will be used as is.
	 */
	private static final Map<Integer, String>	tokenTranslation	= new HashMap<>() {

																		{
																			put( BoxScriptGrammar.AMPAMP, "Operator" );
																			put( BoxScriptGrammar.AMPERSAND, "Operator" );
																			put( BoxScriptGrammar.AND, "Operator" );
																			put( BoxScriptGrammar.ARROW, "->" );
																			put( BoxScriptGrammar.ARROW_RIGHT, "=>" );
																			put( BoxScriptGrammar.AT, "Annotation" );
																			put( BoxScriptGrammar.BACKSLASH, "Operator" );
																			put( BoxScriptGrammar.BANG, "Operator" );
																			put( BoxScriptGrammar.BANGEQUAL, "Operator" );
																			put( BoxScriptGrammar.BITWISE_AND, "Operator" );
																			put( BoxScriptGrammar.BITWISE_COMPLEMENT, "Operator" );
																			put( BoxScriptGrammar.BITWISE_OR, "Operator" );
																			put( BoxScriptGrammar.BITWISE_SIGNED_LEFT_SHIFT, "Operator" );
																			put( BoxScriptGrammar.BITWISE_SIGNED_RIGHT_SHIFT, "Operator" );
																			put( BoxScriptGrammar.BITWISE_UNSIGNED_RIGHT_SHIFT, "Operator" );
																			put( BoxScriptGrammar.BITWISE_XOR, "Operator" );
																			put( BoxScriptGrammar.CASE, "Case clause" );
																			put( BoxScriptGrammar.CASTAS, "Expression" );
																			put( BoxScriptGrammar.CATCH, "Catch clause" );
																			put( BoxScriptGrammar.CLASS, "Class definition" );
																			put( BoxScriptGrammar.CLOSE_QUOTE, "String literal close" );
																			put( BoxScriptGrammar.CLOSE_SQUOTE, "String literal close" );
																			put( BoxScriptGrammar.COLON, "Operator" );
																			put( BoxScriptGrammar.COLONCOLON, "Static accessor" );
																			put( BoxScriptGrammar.COMMA, "','" );
																			put( BoxScriptGrammar.COMPONENT_ISLAND_END, "Component island close" );
																			put( BoxScriptGrammar.COMPONENT_ISLAND_START, "Component island" );
																			put( BoxScriptGrammar.CONCATEQUAL, "Operator" );
																			put( BoxScriptGrammar.CONTAIN, "Operator" );
																			put( BoxScriptGrammar.CONTAINS, "Operator" );
																			put( BoxScriptGrammar.CONTINUE, "Statement" );
																			put( BoxScriptGrammar.DEFAULT, "Expression" );
																			put( BoxScriptGrammar.DO, "Statement" );
																			put( BoxScriptGrammar.DOES, "Statement" );
																			put( BoxScriptGrammar.DOT, "'.' accessor" );
																			put( BoxScriptGrammar.DOT_FLOAT_LITERAL, "Number" );
																			put( BoxScriptGrammar.ELIF, "Elif clause" );
																			put( BoxScriptGrammar.ELSE, "Else clause" );
																			put( BoxScriptGrammar.ELVIS, "Operator" );
																			put( BoxScriptGrammar.EQ, "Operator" );
																			put( BoxScriptGrammar.EQEQ, "Operator" );
																			put( BoxScriptGrammar.EQUAL, "Operator" );
																			put( BoxScriptGrammar.EQUALSIGN, "=" );
																			put( BoxScriptGrammar.EQV, "Operator" );
																			put( BoxScriptGrammar.FALSE, "Expression" );
																			put( BoxScriptGrammar.FINAL, "Access modifier" );
																			put( BoxScriptGrammar.FINALLY, "Finally clause" );
																			put( BoxScriptGrammar.FLOAT_LITERAL, "Number" );
																			put( BoxScriptGrammar.FOR, "Statement" );
																			put( BoxScriptGrammar.FUNCTION, "Function declaration" );
																			put( BoxScriptGrammar.GE, "Operator" );
																			put( BoxScriptGrammar.GREATER, "Operator" );
																			put( BoxScriptGrammar.GT, "Operator" );
																			put( BoxScriptGrammar.GTE, "Operator" );
																			put( BoxScriptGrammar.GTESIGN, "Operator" );
																			put( BoxScriptGrammar.GTSIGN, "Operator" );
																			put( BoxScriptGrammar.HASHHASH, "## expression" );
																			put( BoxScriptGrammar.ICHAR, "# expression" );
																			put( BoxScriptGrammar.IDENTIFIER, "Identifier" );
																			put( BoxScriptGrammar.IF, "Statement" );
																			put( BoxScriptGrammar.IMP, "Operator" );
																			put( BoxScriptGrammar.IMPORT, "Statement" );
																			put( BoxScriptGrammar.IN, "in" );
																			put( BoxScriptGrammar.INCLUDE, "Statement" );
																			put( BoxScriptGrammar.INSTANCEOF, "Expression" );
																			put( BoxScriptGrammar.INTEGER_LITERAL, "Number" );
																			put( BoxScriptGrammar.INTERFACE, "Interface declaration" );
																			put( BoxScriptGrammar.IS, "Operator" );
																			put( BoxScriptGrammar.JAVA, "Statement" );
																			put( BoxScriptGrammar.LBRACE, "'}'" );
																			put( BoxScriptGrammar.LBRACKET, "'['" );
																			put( BoxScriptGrammar.LE, "Operator" );
																			put( BoxScriptGrammar.LESS, "Expression" );
																			put( BoxScriptGrammar.LESSTHANGREATERTHAN, "Operator" );
																			put( BoxScriptGrammar.LPAREN, "'('" );
																			put( BoxScriptGrammar.LT, "Operator" );
																			put( BoxScriptGrammar.LTE, "Operator" );
																			put( BoxScriptGrammar.LTESIGN, "Operator" );
																			put( BoxScriptGrammar.LTSIGN, "Operator" );
																			put( BoxScriptGrammar.MESSAGE, "Statement" );
																			put( BoxScriptGrammar.MINUS, "Operator" );
																			put( BoxScriptGrammar.MINUSEQUAL, "Operator" );
																			put( BoxScriptGrammar.MINUSMINUS, "Operator" );
																			put( BoxScriptGrammar.MOD, "Operator" );
																			put( BoxScriptGrammar.MODEQUAL, "Operator" );
																			put( BoxScriptGrammar.NEQ, "Operator" );
																			put( BoxScriptGrammar.NEW, "Statement" );
																			put( BoxScriptGrammar.NUMERIC, "Type specifier" );
																			put( BoxScriptGrammar.OPEN_QUOTE, "String literal" );
																			put( BoxScriptGrammar.OR, "Operator" );
																			put( BoxScriptGrammar.PACKAGE, "Statement" );
																			put( BoxScriptGrammar.PARAM, "Component declaration" );
																			put( BoxScriptGrammar.PERCENT, "Operator" );
																			put( BoxScriptGrammar.PIPE, "Operator" );
																			put( BoxScriptGrammar.PIPEPIPE, "Operator" );
																			put( BoxScriptGrammar.PLUS, "Operator" );
																			put( BoxScriptGrammar.PLUSEQUAL, "Operator" );
																			put( BoxScriptGrammar.PLUSPLUS, "Operator" );
																			put( BoxScriptGrammar.POWER, "Operator" );
																			put( BoxScriptGrammar.PRIVATE, "Access modifier" );
																			put( BoxScriptGrammar.PROPERTY, "Property declaration" );
																			put( BoxScriptGrammar.PUBLIC, "Access modifier" );
																			put( BoxScriptGrammar.QM, "Operator" );
																			put( BoxScriptGrammar.QUERY, "Statement" );
																			put( BoxScriptGrammar.RBRACE, "'}'" );
																			put( BoxScriptGrammar.RBRACKET, "']'" );
																			put( BoxScriptGrammar.REMOTE, "Statement" );
																			put( BoxScriptGrammar.REQUEST, "Statement" );
																			put( BoxScriptGrammar.RETHROW, "Statement" );
																			put( BoxScriptGrammar.RETURN, "Statement" );
																			put( BoxScriptGrammar.RPAREN, "')'" );
																			put( BoxScriptGrammar.SEMICOLON, "Operator" );
																			put( BoxScriptGrammar.SERVER, "Statement" );
																			put( BoxScriptGrammar.SETTING, "Statement" );
																			put( BoxScriptGrammar.SHASHHASH, "## expression" );
																			put( BoxScriptGrammar.SLASH, "Operator" );
																			put( BoxScriptGrammar.SLASHEQUAL, "Operator" );
																			put( BoxScriptGrammar.STAR, "Operator" );
																			put( BoxScriptGrammar.STAREQUAL, "Operator" );
																			put( BoxScriptGrammar.STATIC, "Access modifier" );
																			put( BoxScriptGrammar.STRING, "Type specifier" );
																			put( BoxScriptGrammar.STRING_LITERAL, "String literal" );
																			put( BoxScriptGrammar.STRUCT, "Struct declaration" );
																			put( BoxScriptGrammar.SWITCH, "Statement" );
																			put( BoxScriptGrammar.TEQ, "Operator" );
																			put( BoxScriptGrammar.THAN, "Expression" );
																			put( BoxScriptGrammar.THROW, "Statement" );
																			put( BoxScriptGrammar.TRY, "Statement" );
																			put( BoxScriptGrammar.TYPE, "Statement" );
																			put( BoxScriptGrammar.VAR, "Statement" );
																			put( BoxScriptGrammar.VARIABLES, "Expression" );
																			put( BoxScriptGrammar.WHEN, "Statement" );
																			put( BoxScriptGrammar.WHILE, "Statement" );
																			put( BoxScriptGrammar.XOR, "Expression" );
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
																			put( "testExpression", "mickey mouse expression" );
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
	private static final IntervalSet			keywordIDENTIFIERS	= new IntervalSet( BoxScriptGrammar.ABSTRACT, BoxScriptGrammar.AND, BoxScriptGrammar.ANY,
	    BoxScriptGrammar.ARRAY, BoxScriptGrammar.AS, BoxScriptGrammar.ASSERT, BoxScriptGrammar.BOOLEAN, BoxScriptGrammar.BREAK, BoxScriptGrammar.CASE,
	    BoxScriptGrammar.CASTAS, BoxScriptGrammar.CATCH, BoxScriptGrammar.CLASS, BoxScriptGrammar.CONTAIN, BoxScriptGrammar.CONTAINS, BoxScriptGrammar.CONTINUE,
	    BoxScriptGrammar.DEFAULT, BoxScriptGrammar.DO, BoxScriptGrammar.DOES, BoxScriptGrammar.ELIF, BoxScriptGrammar.ELSE, BoxScriptGrammar.EQ,
	    BoxScriptGrammar.EQUAL, BoxScriptGrammar.EQV, BoxScriptGrammar.FALSE, BoxScriptGrammar.FINAL, BoxScriptGrammar.FINALLY, BoxScriptGrammar.FOR,
	    BoxScriptGrammar.FUNCTION, BoxScriptGrammar.GE, BoxScriptGrammar.GREATER, BoxScriptGrammar.GT, BoxScriptGrammar.GTE, BoxScriptGrammar.IF,
	    BoxScriptGrammar.IMP, BoxScriptGrammar.IMPORT, BoxScriptGrammar.IN, BoxScriptGrammar.INCLUDE, BoxScriptGrammar.INSTANCEOF, BoxScriptGrammar.INTERFACE,
	    BoxScriptGrammar.IS, BoxScriptGrammar.JAVA, BoxScriptGrammar.LE, BoxScriptGrammar.LESS, BoxScriptGrammar.LT, BoxScriptGrammar.LTE,
	    BoxScriptGrammar.MESSAGE, BoxScriptGrammar.MOD, BoxScriptGrammar.NEQ, BoxScriptGrammar.NEW, BoxScriptGrammar.NOT, BoxScriptGrammar.NULL,
	    BoxScriptGrammar.NUMERIC, BoxScriptGrammar.OR, BoxScriptGrammar.PACKAGE, BoxScriptGrammar.PARAM, BoxScriptGrammar.PRIVATE, BoxScriptGrammar.PROPERTY,
	    BoxScriptGrammar.PUBLIC, BoxScriptGrammar.QUERY, BoxScriptGrammar.REMOTE, BoxScriptGrammar.REQUEST, BoxScriptGrammar.REQUIRED, BoxScriptGrammar.RETHROW,
	    BoxScriptGrammar.RETURN, BoxScriptGrammar.SERVER, BoxScriptGrammar.SETTING, BoxScriptGrammar.STATIC, BoxScriptGrammar.STRING, BoxScriptGrammar.STRUCT,
	    BoxScriptGrammar.THAN, BoxScriptGrammar.THROW, BoxScriptGrammar.TO, BoxScriptGrammar.TRUE, BoxScriptGrammar.TRY, BoxScriptGrammar.TYPE,
	    BoxScriptGrammar.VAR, BoxScriptGrammar.VARIABLES, BoxScriptGrammar.WHEN, BoxScriptGrammar.WHILE, BoxScriptGrammar.XOR );

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
		IntervalSet	expect					= expected.contains( BoxScriptGrammar.IDENTIFIER ) ? removeIdKeywords( expected ) : expected;

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