/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.jdbc.qoq;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.sql.select.SQLResultColumn;
import ortus.boxlang.compiler.ast.sql.select.SQLSelect;
import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLColumn;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.parser.SQLParser;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.util.FRTransService;

/**
 * I handle executing query of queries
 */
public class QoQService {

	/**
	 * The transaction service used to track subtransactions
	 */
	private static final FRTransService frTransService = FRTransService.getInstance( true );

	public static SQLNode parseSQL( String sql ) {
		DynamicObject	trans	= frTransService.startTransaction( "BL QoQ Parse", "" );
		SQLParser		parser	= new SQLParser();
		ParsingResult	result;

		try {
			result = parser.parse( sql );
		} finally {
			frTransService.endTransaction( trans );
		}

		if ( !result.isCorrect() ) {
			throw new ParseException( result.getIssues(), sql );
		}
		IStruct data = Struct.of(
		    "file", null,
		    "result", result
		);
		BoxRuntime.getInstance().announce( "onParse", data );

		return ( SQLNode ) result.getRoot();
	}

	public static Query executeSelect( IBoxContext context, SQLSelectStatement selectStatement, QoQStatement statement ) throws SQLException {
		Map<SQLTable, Query>	tableLookup	= new LinkedHashMap<SQLTable, Query>();
		boolean					hasTable	= selectStatement.getSelect().getTable() != null;
		Query					source		= null;

		// TODO: Process all joins
		if ( hasTable ) {
			String tableVarName = selectStatement.getSelect().getTable().getVariableName();
			source = getSourceQuery( context, tableVarName );
			tableLookup.put( selectStatement.getSelect().getTable(), source );
		}

		Map<Key, TypedResultColumn>	resultColumns	= calculateResultColumns( selectStatement, tableLookup );

		Query						target			= buildTargetQuery( resultColumns );

		// Process one select
		// TODO: refactor this out
		SQLSelect					select			= selectStatement.getSelect();

		Long						thisSelectLimit	= select.getLimitValue();
		// This boolean expression will be used to filter the records we keep
		SQLExpression				where			= select.getWhere();
		boolean						canEarlyLimit	= selectStatement.getOrderBys() == null;

		// Just selecting out literal values
		if ( !hasTable ) {
			Object[] values = new Object[ resultColumns.size() ];
			for ( Key key : resultColumns.keySet() ) {
				SQLResultColumn	resultColumn	= resultColumns.get( key ).resultColumn;
				Object			value			= resultColumn.getExpression().evaluate( tableLookup, 1 );
				values[ resultColumn.getOrdinalPosition() - 1 ] = value;
			}
			target.addRow( values );
			return target;
		}

		// 1-based index!
		for ( int i = 1; i <= source.size(); i++ ) {
			// enforce top/limit for this select. This would be a "top N" clause in the select or a "limit N" clause BEFORE the order by, which
			// could exist or all selects in a union.
			if ( canEarlyLimit && thisSelectLimit > -1 && i > thisSelectLimit ) {
				break;
			}
			// Evaluate the where expression
			if ( where == null || ( Boolean ) where.evaluate( tableLookup, i ) ) {
				Object[] values = new Object[ resultColumns.size() ];
				for ( Key key : resultColumns.keySet() ) {
					SQLResultColumn	resultColumn	= resultColumns.get( key ).resultColumn;
					Object			value			= resultColumn.getExpression().evaluate( tableLookup, i );
					values[ resultColumn.getOrdinalPosition() - 1 ] = value;
				}
				target.addRow( values );
			}
		}

		// TODO: Sort the query

		// This is the maxRows in the query options. It takes priority.
		Long overallSelectLimit = statement.getLargeMaxRows();
		// If that wasn't set, use the limit clause AFTER the order by (which could apply at the end of a union)
		if ( overallSelectLimit == -1 ) {
			overallSelectLimit = selectStatement.getLimitValue();
		}

		// If we have a limit for the final select, apply it here.
		if ( overallSelectLimit > -1 ) {
			target.truncate( overallSelectLimit );
		}
		return target;
	}

	/**
	 * Build the target query based on the result columns
	 * 
	 * @param resultColumns the result columns
	 * 
	 * @return the target query
	 */
	private static Query buildTargetQuery( Map<Key, TypedResultColumn> resultColumns ) {
		Query target = new Query();
		for ( Key key : resultColumns.keySet() ) {
			target.addColumn( key, resultColumns.get( key ).type );
		}
		return target;
	}

	private static Map<Key, TypedResultColumn> calculateResultColumns( SQLSelectStatement select, Map<SQLTable, Query> tableLookup ) {
		Map<Key, TypedResultColumn> resultColumns = new LinkedHashMap<Key, TypedResultColumn>();
		for ( SQLResultColumn resultColumn : select.getSelect().getResultColumns() ) {
			// For *, expand all columns in the query
			if ( resultColumn.isStarExpression() ) {
				// TODO: when we add joins, handle looking up the correct table reference based on resultColumn.getTable()
				var thisTable = tableLookup.get( select.getSelect().getTable() );
				for ( Key key : thisTable.getColumns().keySet() ) {
					resultColumns.put( key,
					    TypedResultColumn.of(
					        thisTable.getColumns().get( key ).getType(),
					        new SQLResultColumn(
					            new SQLColumn( select.getSelect().getTable(), key.getName(), null, null ),
					            null,
					            resultColumns.size() + 1,
					            null,
					            null
					        )
					    )
					);
				}
				// Non-star columns are named after the column, or given a column_0, column_1, etc name
			} else {
				resultColumns.put( resultColumn.getResultColumnName(),
				    TypedResultColumn.of( resultColumn.getExpression().getType( tableLookup ), resultColumn ) );
			}
		}
		return resultColumns;
	}

	private static Query getSourceQuery( IBoxContext context, String tableVarName ) {
		Object oSource = ExpressionInterpreter.getVariable( context, tableVarName, false );
		if ( oSource instanceof Query qSource ) {
			return qSource;
		} else {
			throw new DatabaseException( "The QoQ table name [" + tableVarName + "] cannot be found as a variable." );
		}
	}

	public record TypedResultColumn( QueryColumnType type, SQLResultColumn resultColumn ) {

		public static TypedResultColumn of( QueryColumnType type, SQLResultColumn resultColumn ) {
			return new TypedResultColumn( type, resultColumn );
		}
	}

}
