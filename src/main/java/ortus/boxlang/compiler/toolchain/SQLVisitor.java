package ortus.boxlang.compiler.toolchain;

import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.sql.select.SQLJoin;
import ortus.boxlang.compiler.ast.sql.select.SQLResultColumn;
import ortus.boxlang.compiler.ast.sql.select.SQLSelect;
import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLColumn;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLOrderBy;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLStarExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLBooleanLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNullLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNumberLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLStringLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLBinaryOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLBinaryOperator;
import ortus.boxlang.compiler.parser.SQLParser;
import ortus.boxlang.parser.antlr.SQLGrammar.ExprContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Literal_valueContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Ordering_termContext;
import ortus.boxlang.parser.antlr.SQLGrammar.ParseContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Result_columnContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Select_coreContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Select_stmtContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Sql_stmtContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Sql_stmt_listContext;
import ortus.boxlang.parser.antlr.SQLGrammar.TableContext;
import ortus.boxlang.parser.antlr.SQLGrammarBaseVisitor;
import ortus.boxlang.runtime.scopes.Key;

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
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );

		SQLSelect			select		= null;
		List<SQLSelect>		unions		= null;
		List<SQLOrderBy>	orderBys	= null;
		SQLNumberLiteral	limit		= null;

		select = ( SQLSelect ) visit( ctx.select_core() );
		final SQLSelect finalSelect = select;

		if ( ctx.order_by_stmt() != null ) {
			orderBys = ctx.order_by_stmt().ordering_term().stream().map( term -> visitOrdering_term( term, finalSelect.getTable(), finalSelect.getJoins() ) )
			    .toList();
		}

		// Limit after the order by, applies to all unions
		if ( ctx.limit_stmt() != null ) {
			limit = NUMERIC_LITERAL( ctx.limit_stmt().NUMERIC_LITERAL() );
		}

		// TODO: handle unions

		return new SQLSelectStatement( select, unions, orderBys, limit, pos, src );
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
	public SQLSelect visitSelect_core( Select_coreContext ctx ) {
		var						pos				= tools.getPosition( ctx );
		var						src				= tools.getSourceText( ctx );

		boolean					distinct		= ctx.DISTINCT_() != null;
		SQLNumberLiteral		limit			= null;
		List<SQLResultColumn>	resultColumns	= null;
		SQLTable				table			= null;
		List<SQLJoin>			joins			= null;
		SQLExpression			where			= null;
		List<SQLExpression>		groupBys		= null;
		SQLExpression			having			= null;

		// limit before order by, can have one per unioned table
		if ( ctx.limit_stmt() != null ) {
			limit = NUMERIC_LITERAL( ctx.limit_stmt().NUMERIC_LITERAL() );
		}
		// each
		if ( ctx.top() != null ) {
			limit = NUMERIC_LITERAL( ctx.top().NUMERIC_LITERAL() );
		}

		if ( !ctx.table().isEmpty() ) {
			var firstTable = ctx.table().get( 0 );
			table = ( SQLTable ) visit( firstTable );
		}

		if ( ctx.whereExpr != null ) {
			where = visitExpr( ctx.whereExpr, table, joins );
		}

		// TODO: group by

		// TODO: having

		// TODO: handle additional tables as joins

		// TODO: handle joins

		// Do this after all joins above so we know the tables available to us
		final SQLTable		finalTable	= table;
		final List<SQLJoin>	finalJoins	= joins;
		resultColumns = ctx.result_column().stream().map( col -> visitResult_column( col, finalTable, finalJoins ) ).toList();

		var	result	= new SQLSelect( distinct, resultColumns, table, joins, where, groupBys, having, limit, pos, src );
		var	cols	= result.getDescendantsOfType( SQLColumn.class, c -> c.getTable() == null );
		if ( cols.size() > 0 ) {
			if ( joins != null && !joins.isEmpty() ) {
				tools.reportError( "Column reference must have table prefix to disambiguate.", pos );
			} else {
				if ( table == null ) {
					tools.reportError( "This QoQ has column references, but there is no table!", pos );
				} else {
					cols.forEach( c -> c.setTable( finalTable ) );
				}
			}
		}
		return result;
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
	public SQLTable visitTable( TableContext ctx ) {
		var		pos		= tools.getPosition( ctx );
		var		src		= tools.getSourceText( ctx );
		String	schema	= null;
		String	name	= ctx.table_name().getText();
		String	alias	= null;

		if ( ctx.schema_name() != null ) {
			schema = ctx.schema_name().getText();
		}

		if ( ctx.table_alias() != null ) {
			alias = ctx.table_alias().getText();
		}

		return new SQLTable( schema, name, alias, pos, src );
	}

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	public SQLResultColumn visitResult_column( Result_columnContext ctx, SQLTable table, List<SQLJoin> joins ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );
		String			alias	= null;
		SQLExpression	expression;

		if ( ctx.column_alias() != null ) {
			alias = ctx.column_alias().getText();
		}

		if ( ctx.STAR() != null ) {
			SQLTable tableRef = null;
			// if we have tableName.* or tAlias.* then we need to find the table reference
			if ( ctx.table_name() != null ) {
				String tableName = ctx.table_name().getText();
				tableRef = findTableRef( table, joins, tableName );
				// If we didn't find the table reference then error
				if ( tableRef == null ) {
					tools.reportError( "Table reference not found for " + src, pos );
				}
			}
			expression = new SQLStarExpression( tableRef, pos, src );
		} else {
			expression = visitExpr( ctx.expr(), table, joins );
		}

		return new SQLResultColumn( expression, alias, 0, pos, src );
	}

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	public SQLOrderBy visitOrdering_term( Ordering_termContext ctx, SQLTable table, List<SQLJoin> joins ) {
		var		pos			= tools.getPosition( ctx );
		var		src			= tools.getSourceText( ctx );
		boolean	ascending	= true;

		if ( ctx.asc_desc() != null ) {
			ascending = ctx.asc_desc().DESC_() != null;
		}

		return new SQLOrderBy( visitExpr( ctx.expr(), table, joins ), ascending, pos, src );
	}

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	public SQLExpression visitExpr( ExprContext ctx, SQLTable table, List<SQLJoin> joins ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );

		if ( ctx.column_name() != null ) {
			SQLTable tableRef = null;
			// if we have tableName.* or tAlias.* then we need to find the table reference
			if ( ctx.table_name() != null ) {
				String tableName = ctx.table_name().getText();
				tableRef = findTableRef( table, joins, tableName );
				// If we didn't find the table reference then error
				if ( tableRef == null ) {
					tools.reportError( "Table reference not found for " + src, pos );
				}
			}
			return new SQLColumn( tableRef, ctx.column_name().getText(), pos, src );
		} else if ( ctx.literal_value() != null ) {
			return ( SQLExpression ) visit( ctx.literal_value() );
		} else if ( ctx.EQ() != null || ctx.ASSIGN() != null ) {
			return new SQLBinaryOperation( visitExpr( ctx.expr( 0 ), table, joins ), visitExpr( ctx.expr( 1 ), table, joins ), SQLBinaryOperator.EQUAL, pos,
			    src );
		} else {
			throw new UnsupportedOperationException( "Unimplemented expression: " + src );
		}
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
	public SQLExpression visitLiteral_value( Literal_valueContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );

		if ( ctx.NULL_() != null ) {
			return new SQLNullLiteral( pos, src );
		} else if ( ctx.NUMERIC_LITERAL() != null ) {
			return NUMERIC_LITERAL( ctx.NUMERIC_LITERAL() );
		} else if ( ctx.TRUE_() != null ) {
			return new SQLBooleanLiteral( true, pos, src );
		} else if ( ctx.FALSE_() != null ) {
			return new SQLBooleanLiteral( false, pos, src );
		} else if ( ctx.STRING_LITERAL() != null ) {
			String str = ctx.STRING_LITERAL().getText();
			// strip quote chars
			str	= str.substring( 1, str.length() - 1 );
			// unescape `''` inside string
			str	= str.replace( "''", "'" );
			return new SQLStringLiteral( str, pos, src );
		} else {
			throw new UnsupportedOperationException( "Unimplemented literal expression: " + src );
		}
	}

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	public SQLNumberLiteral NUMERIC_LITERAL( TerminalNode ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= ctx.getText();

		// TODO: handle different types (int, long, double, bigdecimal)
		return new SQLNumberLiteral( Double.parseDouble( src ), pos, src );
	}

	private SQLTable findTableRef( SQLTable table, List<SQLJoin> joins, String tableName ) {
		Key tableNameKey = Key.of( tableName );
		if ( table.isCalled( tableNameKey ) ) {
			return table;
		} else if ( joins != null ) {
			for ( SQLJoin join : joins ) {
				if ( join.getTable().isCalled( tableNameKey ) ) {
					return join.getTable();
				}
			}
		}
		return null;
	}

}