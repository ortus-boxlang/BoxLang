package ortus.boxlang.compiler.toolchain;

import java.util.List;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.parser.SQLParser;
import ortus.boxlang.parser.antlr.SQLGrammar.ParseContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Select_stmtContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Sql_stmtContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Sql_stmt_listContext;
import ortus.boxlang.parser.antlr.SQLGrammarBaseVisitor;

/**
 * This class is responsible for creating the SQL AST from the ANTLR generated parse tree.
 */
public class SQLVisitor extends SQLGrammarBaseVisitor<BoxNode> {

	private final SQLParser tools;

	public SQLVisitor( SQLParser tools ) {
		this.tools = tools;
	}

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	@Override
	public BoxNode visitParse( ParseContext ctx ) {
		List<Sql_stmt_listContext>	statements	= ctx.sql_stmt_list();
		var							pos			= tools.getPosition( ctx );

		if ( statements.isEmpty() ) {
			tools.reportError( "No SQL statements found in query of query.", pos );
		}
		if ( statements.size() > 1 ) {
			tools.reportError( "Only one SQL statement is allowed per query of query.", pos );
		}

		return visit( statements.get( 0 ) );
	}

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	@Override
	public BoxNode visitSql_stmt_list( Sql_stmt_listContext ctx ) {
		List<Sql_stmtContext>	statements	= ctx.sql_stmt();
		var						pos			= tools.getPosition( ctx );

		if ( statements.isEmpty() ) {
			tools.reportError( "No SQL statements found in query of query.", pos );
		}
		if ( statements.size() > 1 ) {
			tools.reportError( "Only one SQL statement is allowed per query of query.", pos );
		}

		Sql_stmtContext statement = statements.get( 0 );
		if ( statement.select_stmt() == null ) {
			tools.reportError( "Only SELECT statements are allowed in query of query.", pos );
		}

		return visit( statement );
	}

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	@Override
	public BoxNode visitSelect_stmt( Select_stmtContext ctx ) {
		var pos = tools.getPosition( ctx );

		return null;
	}

}