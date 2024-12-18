package ortus.boxlang.compiler.toolchain;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.select.SQLJoin;
import ortus.boxlang.compiler.ast.sql.select.SQLJoinType;
import ortus.boxlang.compiler.ast.sql.select.SQLResultColumn;
import ortus.boxlang.compiler.ast.sql.select.SQLSelect;
import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.SQLTableSubQuery;
import ortus.boxlang.compiler.ast.sql.select.SQLTableVariable;
import ortus.boxlang.compiler.ast.sql.select.SQLUnion;
import ortus.boxlang.compiler.ast.sql.select.SQLUnionType;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLCase;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLCaseWhenThen;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLColumn;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLCountFunction;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLFunction;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLOrderBy;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLParam;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLParenthesis;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLStarExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLBooleanLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNullLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNumberLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLStringLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLBetweenOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLBinaryOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLBinaryOperator;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLInOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLInSubQueryOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLUnaryOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLUnaryOperator;
import ortus.boxlang.compiler.parser.SQLParser;
import ortus.boxlang.parser.antlr.SQLGrammar;
import ortus.boxlang.parser.antlr.SQLGrammar.Case_exprContext;
import ortus.boxlang.parser.antlr.SQLGrammar.ExprContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Literal_valueContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Ordering_termContext;
import ortus.boxlang.parser.antlr.SQLGrammar.ParseContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Result_columnContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Select_coreContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Select_stmtContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Sql_stmtContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Sql_stmt_listContext;
import ortus.boxlang.parser.antlr.SQLGrammar.SubqueryContext;
import ortus.boxlang.parser.antlr.SQLGrammar.TableContext;
import ortus.boxlang.parser.antlr.SQLGrammar.Table_or_subqueryContext;
import ortus.boxlang.parser.antlr.SQLGrammarBaseVisitor;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.QueryColumnType;

/**
 * This class is responsible for creating the SQL AST from the ANTLR generated parse tree.
 */
public class SQLVisitor extends SQLGrammarBaseVisitor<BoxNode> {

	private final SQLParser	tools;
	private int				bindCount	= 0;
	private int				tableIndex	= 0;

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
		List<SQLUnion>		unions		= null;
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

		if ( ctx.union() != null && !ctx.union().isEmpty() ) {
			int numCols = select.getResultColumns().size();
			unions = new ArrayList<SQLUnion>();
			int numSelect = 2;
			for ( var unionCtx : ctx.union() ) {
				SQLUnionType unionType = unionCtx.ALL_() != null ? SQLUnionType.ALL : SQLUnionType.DISTINCT;
				if ( unionType == SQLUnionType.ALL && unionCtx.DISTINCT_() != null ) {
					tools.reportError( "Cannot have both ALL and DISTINCT in a UNION", tools.getPosition( unionCtx ) );
				}
				var unionSelect = ( SQLSelect ) visit( unionCtx.select_core() );
				if ( unionSelect.getResultColumns().size() != numCols ) {
					tools.reportError(
					    "All SELECT statements in a UNION must have the same number of columns.  Select number " + numSelect + " has "
					        + unionSelect.getResultColumns().size() + " column(s) but the original select has " + numCols + " column(s).",
					    tools.getPosition( unionCtx ) );
				}
				unions.add( new SQLUnion( unionSelect, unionType, tools.getPosition( unionCtx ), tools.getSourceText( unionCtx ) ) );
				numSelect++;
			}

		}

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
		var							pos				= tools.getPosition( ctx );
		var							src				= tools.getSourceText( ctx );

		boolean						distinct		= ctx.DISTINCT_() != null;
		SQLNumberLiteral			limit			= null;
		List<SQLResultColumn>		resultColumns	= null;
		SQLTable					table			= null;
		List<SQLJoin>				joins			= null;
		SQLExpression				where			= null;
		List<SQLExpression>			groupBys		= null;
		SQLExpression				having			= null;

		Table_or_subqueryContext	firstTable;
		if ( !ctx.table_or_subquery().isEmpty() ) {
			firstTable	= ctx.table_or_subquery().get( 0 );
			table		= ( SQLTable ) visit( firstTable );

			if ( ctx.table_or_subquery().size() > 1 ) {
				// from table1, table2 is treated as a join with no `on` clause
				joins = new ArrayList<SQLJoin>();
				for ( int i = 1; i < ctx.table_or_subquery().size(); i++ ) {
					var			tableCtx	= ctx.table_or_subquery().get( i );
					SQLTable	joinTable	= ( SQLTable ) visit( tableCtx );
					joins.add( new SQLJoin( SQLJoinType.FULL, joinTable, null, tools.getPosition( tableCtx ), tools.getSourceText( tableCtx ) ) );
				}
			}
		} else if ( ctx.join_clause() != null ) {
			firstTable	= ctx.join_clause().table_or_subquery();
			table		= ( SQLTable ) visit( firstTable );
			joins		= buildJoins( ctx.join_clause(), table );
		}

		// limit before order by, can have one per unioned table
		if ( ctx.limit_stmt() != null ) {
			limit = NUMERIC_LITERAL( ctx.limit_stmt().NUMERIC_LITERAL() );
		}

		// each select can have a top
		if ( ctx.top() != null ) {
			limit = NUMERIC_LITERAL( ctx.top().NUMERIC_LITERAL() );
		}

		if ( ctx.whereExpr != null ) {
			where = visitExpr( ctx.whereExpr, table, joins );
		}

		// group by
		if ( ctx.groupByExpr != null && !ctx.groupByExpr.isEmpty() ) {
			SQLTable		finalTable	= table;
			List<SQLJoin>	finalJoins	= joins;
			groupBys = ctx.groupByExpr.stream().map( expr -> visitExpr( expr, finalTable, finalJoins ) ).toList();
		}

		// having
		if ( ctx.havingExpr != null ) {
			having = visitExpr( ctx.havingExpr, table, joins );
		}

		// Do this after all joins above so we know the tables available to us
		final SQLTable		finalTable	= table;
		final List<SQLJoin>	finalJoins	= joins;
		resultColumns = ctx.result_column().stream().map( col -> visitResult_column( col, finalTable, finalJoins ) ).toList();

		var	result	= new SQLSelect( distinct, resultColumns, table, joins, where, groupBys, having, limit, pos, src );

		var	cols	= result.getDescendantsOfType( SQLColumn.class, c -> c.getTable() == null );
		if ( cols.size() > 0 ) {
			if ( table == null && joins == null ) {
				// Only report an error if there are no issues already
				// because this can get tripped up by parsing that simply failed too soon and didn't finish
				if ( tools.issues.size() == 0 ) {
					tools.reportError( "This QoQ has column references, but there is no table!", pos );
				}
			} else if ( joins == null ) {
				// If there is only one table, we know what it is now
				cols.forEach( c -> c.setTable( finalTable ) );
			}
		}

		return result;
	}

	public List<SQLJoin> buildJoins( SQLGrammar.Join_clauseContext ctx, SQLTable table ) {
		var joins = new ArrayList<SQLJoin>();
		for ( var joinCtx : ctx.join() ) {
			var			pos			= tools.getPosition( joinCtx );
			var			src			= tools.getSourceText( joinCtx );
			var			joinTable	= ( SQLTable ) visit( joinCtx.table_or_subquery() );
			var			typeCtx		= joinCtx.join_operator();
			boolean		hasOn		= joinCtx.join_constraint() != null;
			String		joinText	= tools.getSourceText( typeCtx );
			// If left, right, full, or cross are not specified, we default to inner
			SQLJoinType	type		= SQLJoinType.INNER;
			if ( typeCtx.LEFT_() != null ) {
				if ( !hasOn ) {
					tools.reportError( "[" + joinText + "] must have an ON clause", tools.getPosition( typeCtx ) );
				}
				type = SQLJoinType.LEFT;
			} else if ( typeCtx.RIGHT_() != null ) {
				if ( !hasOn ) {
					tools.reportError( "[" + joinText + "] must have an ON clause", tools.getPosition( typeCtx ) );
				}
				type = SQLJoinType.RIGHT;
			} else if ( typeCtx.FULL_() != null ) {
				if ( !hasOn ) {
					tools.reportError( "[" + joinText + "] must have an ON clause", tools.getPosition( typeCtx ) );
				}
				type = SQLJoinType.FULL;
			} else if ( typeCtx.CROSS_() != null ) {
				if ( hasOn ) {
					tools.reportError( "[" + joinText + "] cannot have an ON clause", tools.getPosition( typeCtx ) );
				}
				type = SQLJoinType.CROSS;
			}
			// Leave expression null here. I need to get the JOIN into the list of joins FIRST so the expression
			// visitors can correctly match the table names. Well add the expression later
			SQLJoin thisJoin = new SQLJoin( type, joinTable, null, pos, src );
			joins.add( thisJoin );
			if ( hasOn ) {
				thisJoin.setOn( visitExpr( joinCtx.join_constraint().expr(), table, joins ) );
			}
		}
		return joins;
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
	public SQLTable visitTable_or_subquery( Table_or_subqueryContext ctx ) {
		if ( ctx.table() != null ) {
			return visitTable( ctx.table() );
		} else {
			return visitSubquery( ctx.subquery() );
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
	public SQLTableVariable visitTable( TableContext ctx ) {
		var		pos		= tools.getPosition( ctx );
		var		src		= tools.getSourceText( ctx );
		String	schema	= null;
		String	name	= unwrapBracket( ctx.table_name().getText() );
		String	alias	= null;

		if ( ctx.schema_name() != null ) {
			schema = unwrapBracket( ctx.schema_name().getText() );
		}

		if ( ctx.table_alias() != null ) {
			alias = unwrapBracket( ctx.table_alias().getText() );
		}

		return new SQLTableVariable( schema, name, alias, tableIndex++, pos, src );
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
	public SQLTableSubQuery visitSubquery( SubqueryContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		SQLSelectStatement	select	= ( SQLSelectStatement ) new SQLVisitor( tools ).visit( ctx.select_stmt() );

		return new SQLTableSubQuery( select, ctx.table_alias().getText(), tableIndex++, pos, src );
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
			alias = unwrapBracket( ctx.column_alias().getText() );
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
			ascending = ctx.asc_desc().DESC_() == null;
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
				String tableName = unwrapBracket( ctx.table_name().getText() );
				tableRef = findTableRef( table, joins, tableName );
				// If we didn't find the table reference then error
				if ( tableRef == null ) {
					tools.reportError( "Table reference not found for " + src, pos );
				}
			}
			return new SQLColumn( tableRef, unwrapBracket( ctx.column_name().getText() ), pos, src );
		} else if ( ctx.literal_value() != null ) {
			return ( SQLExpression ) visit( ctx.literal_value() );
		} else if ( ctx.EQ() != null || ctx.ASSIGN() != null || ctx.IS_() != null ) {
			// IS vs IS NOT
			SQLBinaryOperator operator = ctx.NOT_() != null ? SQLBinaryOperator.NOTEQUAL : SQLBinaryOperator.EQUAL;
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), operator, pos, src, table, joins );
		} else if ( ctx.BETWEEN_() != null ) {
			return new SQLBetweenOperation( visitExpr( ctx.expr( 0 ), table, joins ), visitExpr( ctx.expr( 1 ), table, joins ),
			    visitExpr( ctx.expr( 2 ), table, joins ), ctx.NOT_() != null, pos, src );
		} else if ( ctx.AND_() != null ) {
			// Needs to run AFTER between checks
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.AND, pos, src, table, joins );
		} else if ( ctx.OR_() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.OR, pos, src, table, joins );
		} else if ( ctx.CAST_() != null || ctx.CONVERT_() != null ) {
			// CAST( expr AS type )
			Key					functionName	= ctx.CONVERT_() != null ? Key.convert : Key.cast;
			List<SQLExpression>	arguments		= new ArrayList<SQLExpression>();

			// Add expr as first arg
			arguments.add( visitExpr( ctx.expr( 0 ), table, joins ) );

			// Add type as second arg
			// We allow both varchar or 'varchar'
			SQLStringLiteral type;
			if ( ctx.STRING_LITERAL() != null ) {
				type = processStringLiteral( ctx.STRING_LITERAL() );
			} else {
				type = new SQLStringLiteral( unwrapBracket( ctx.name().getText() ), tools.getPosition( ctx.name() ), tools.getSourceText( ctx.name() ) );
			}
			// validate the type here
			try {
				QueryColumnType.fromString( type.getValue() );
			} catch ( IllegalArgumentException e ) {
				tools.reportError( "Invalid type for " + functionName.getName() + ": " + type.getValue(), pos );
			}
			arguments.add( type );

			return new SQLFunction( functionName, arguments, pos, src );
		} else if ( ctx.function_name() != null ) {
			Key					functionName	= Key.of( unwrapBracket( ctx.function_name().getText() ) );
			boolean				hasDistinct		= ctx.DISTINCT_() != null;
			List<SQLExpression>	arguments		= new ArrayList<SQLExpression>();
			if ( ctx.STAR() != null ) {
				arguments.add( new SQLStarExpression( null, pos, src ) );
			} else {
				arguments = ctx.expr().stream().map( e -> visitExpr( e, table, joins ) ).toList();
			}
			if ( functionName.equals( Key.count ) ) {
				return new SQLCountFunction( functionName, arguments, hasDistinct, pos, src );
			} else {
				return new SQLFunction( functionName, arguments, pos, src );
			}
		} else if ( ctx.BIND_PARAMETER() != null ) {
			int		index	= bindCount++;
			String	name	= null;
			if ( ctx.BIND_PARAMETER().getText().startsWith( ":" ) ) {
				name = ctx.BIND_PARAMETER().getText().substring( 1 );
			}
			return new SQLParam( name, index, pos, src );
		} else if ( ctx.IN_() != null ) {
			SQLExpression expr = visitExpr( ctx.expr( 0 ), table, joins );
			if ( ctx.subquery_no_alias() != null ) {
				SQLSelectStatement subquery = ( SQLSelectStatement ) new SQLVisitor( tools ).visit( ctx.subquery_no_alias().select_stmt() );
				if ( subquery.getSelect().getResultColumns().size() != 1 ) {
					tools.reportError( "Subquery in IN clause must return exactly one column", pos );
				}
				return new SQLInSubQueryOperation( expr, subquery, ctx.NOT_() != null, pos, src );
			} else {
				List<SQLExpression> values = ctx.expr().stream().skip( 1 ).map( e -> visitExpr( e, table, joins ) ).toList();
				return new SQLInOperation( expr, values, ctx.NOT_() != null, pos, src );
			}
		} else if ( ctx.LIKE_() != null ) {
			SQLBinaryOperator	op		= ctx.NOT_() != null ? SQLBinaryOperator.NOTLIKE : SQLBinaryOperator.LIKE;
			SQLExpression		escape	= null;
			if ( ctx.ESCAPE_() != null ) {
				escape = visitExpr( ctx.expr( 2 ), table, joins );
			}
			return new SQLBinaryOperation( visitExpr( ctx.expr( 0 ), table, joins ), visitExpr( ctx.expr( 1 ), table, joins ), op, escape, pos, src );
		} else if ( ctx.PIPE2() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.CONCAT, pos, src, table, joins );
		} else if ( ctx.STAR() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.MULTIPLY, pos, src, table, joins );
		} else if ( ctx.DIV() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.DIVIDE, pos, src, table, joins );
		} else if ( ctx.MOD() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.MODULO, pos, src, table, joins );
		} else if ( ctx.PLUS() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.PLUS, pos, src, table, joins );
		} else if ( ctx.MINUS() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.MINUS, pos, src, table, joins );
		} else if ( ctx.LT() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.LESSTHAN, pos, src, table, joins );
		} else if ( ctx.LT_EQ() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.LESSTHANOREQUAL, pos, src, table, joins );
		} else if ( ctx.GT() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.GREATERTHAN, pos, src, table, joins );
		} else if ( ctx.GT_EQ() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.GREATERTHANOREQUAL, pos, src, table, joins );
		} else if ( ctx.NOT_EQ1() != null || ctx.NOT_EQ2() != null ) {
			return binarySimple( ctx.expr( 0 ), ctx.expr( 1 ), SQLBinaryOperator.NOTEQUAL, pos, src, table, joins );
		} else if ( ctx.OPEN_PAR() != null ) {
			// Needs to run AFTER function and IN checks
			return new SQLParenthesis( visitExpr( ctx.expr( 0 ), table, joins ), pos, src );
		} else if ( ctx.case_expr() != null ) {
			return visitCase( ctx.case_expr(), table, joins );
		} else if ( ctx.unary_operator() != null ) {
			SQLUnaryOperator op;
			if ( ctx.unary_operator().BANG() != null ) {
				op = SQLUnaryOperator.NOT;
			} else if ( ctx.unary_operator().MINUS() != null ) {
				op = SQLUnaryOperator.MINUS;
			} else if ( ctx.unary_operator().PLUS() != null ) {
				op = SQLUnaryOperator.PLUS;
			} else {
				throw new UnsupportedOperationException( "Unimplemented unary operator: " + ctx.unary_operator().getText() );
			}
			return new SQLUnaryOperation( visitExpr( ctx.expr( 0 ), table, joins ), op, pos, src );
		} else {
			throw new UnsupportedOperationException( "Unimplemented expression: " + src );
		}
	}

	private SQLExpression visitCase( Case_exprContext ctx, SQLTable table, List<SQLJoin> joins ) {
		var						pos				= tools.getPosition( ctx );
		var						src				= tools.getSourceText( ctx );

		SQLExpression			inputExpression	= null;
		List<SQLCaseWhenThen>	whenThens		= new ArrayList<SQLCaseWhenThen>();
		SQLExpression			elseExpression	= null;

		if ( ctx.initial_expr != null ) {
			inputExpression = visitExpr( ctx.initial_expr, table, joins );
		}

		if ( ctx.else_expr != null ) {
			elseExpression = visitExpr( ctx.else_expr, table, joins );
		}

		for ( var whenThenCtx : ctx.case_when_then() ) {
			SQLExpression	when	= visitExpr( whenThenCtx.when_expr, table, joins );
			SQLExpression	then	= visitExpr( whenThenCtx.then_expr, table, joins );
			whenThens.add( new SQLCaseWhenThen( when, then, tools.getPosition( whenThenCtx ), tools.getSourceText( whenThenCtx ) ) );
		}

		return new SQLCase( inputExpression, whenThens, elseExpression, pos, src );
	}

	/**
	 * Unwrap the brackets from the table or column name
	 * 
	 * @param text the text to unwrap
	 * 
	 * @return the unwrapped text
	 */
	private String unwrapBracket( String text ) {
		if ( text.startsWith( "[" ) && text.endsWith( "]" ) ) {
			return text.substring( 1, text.length() - 1 );
		}
		return text;
	}

	private SQLExpression binarySimple( ExprContext left, ExprContext right, SQLBinaryOperator op, Position pos, String src, SQLTable table,
	    List<SQLJoin> joins ) {
		return new SQLBinaryOperation( visitExpr( left, table, joins ), visitExpr( right, table, joins ), op, pos, src );
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
			return processStringLiteral( ctx.STRING_LITERAL() );
		} else {
			throw new UnsupportedOperationException( "Unimplemented literal expression: " + src );
		}
	}

	private SQLStringLiteral processStringLiteral( TerminalNode ctx ) {
		var		pos	= tools.getPosition( ctx );

		String	str	= ctx.getText();
		// strip quote chars
		str	= str.substring( 1, str.length() - 1 );
		// unescape `''` inside string
		str	= str.replace( "''", "'" );
		return new SQLStringLiteral( str, pos, str );
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