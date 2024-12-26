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
package ortus.boxlang.compiler.ast.sql.select.expression;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.SQLTableVariable;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL column reference
 */
public class SQLColumn extends SQLExpression {

	private final static Set<QueryColumnType>	numericTypes	= Set.of( QueryColumnType.BIGINT, QueryColumnType.DECIMAL, QueryColumnType.DOUBLE,
	    QueryColumnType.INTEGER, QueryColumnType.BIT );

	private SQLTable							table;

	private Key									name;

	// THIS DATA IS SPECIFIC TO THE EXECUTION. IF WE START CACHING THIS AST, REWORK THIS TO BE STORED ELSEWHERE
	private List<Object[]>						data			= null;
	private int									tableIndex		= -1;
	private int									colIndex		= -1;
	private QueryColumnType						type			= null;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLColumn( SQLTable table, String name, Position position, String sourceText ) {
		super( position, sourceText );
		setName( name );
		setTable( table );
	}

	/**
	 * Get the name of the function
	 *
	 * @return the name of the function
	 */
	public Key getName() {
		return name;
	}

	/**
	 * Set the name of the function
	 *
	 * @param name the name of the function
	 */
	public void setName( String name ) {
		this.name = Key.of( name );
	}

	/**
	 * Get the table (may be null if there is no alias)
	 */
	public SQLTable getTable() {
		return table;
	}

	/**
	 * Get the table, performing runtime lookup if necessary
	 */
	public SQLTable getTableFinal( QoQSelectExecution QoQExec ) {
		var t = getTable();
		if ( t != null ) {
			return t;
		}
		// Abmiguity, we need to find the table
		var tables = QoQExec.getTableLookup().entrySet();
		for ( var tableSet : tables ) {
			if ( tableSet.getValue().getColumns().containsKey( name ) ) {
				return tableSet.getKey();
			}
		}
		throw new BoxRuntimeException( "Column " + name + " is ambiguous and not found in any table." );
	}

	/**
	 * Get the table, performing runtime lookup if necessary
	 */
	public void ensureData( QoQSelectExecution QoQExec ) {
		if ( data != null ) {
			return;
		}

		synchronized ( this ) {
			var t = getTable();
			if ( data != null ) {
				return;
			}
			if ( t == null ) {
				// Abmiguity, we need to find the table
				var tables = QoQExec.getTableLookup().entrySet();
				for ( var tableSet : tables ) {
					if ( tableSet.getValue().getColumns().containsKey( name ) ) {
						t = tableSet.getKey();
					}
				}
				if ( t == null ) {
					throw new BoxRuntimeException( "Column " + name + " is ambiguous and not found in any table." );
				}
			}
			tableIndex = t.getIndex();
			Query		table	= QoQExec.getTableLookup().get( t );
			// Cache this data for future use
			QueryColumn	column	= table.getColumns().get( name );
			if ( column == null ) {
				throw new BoxRuntimeException(
				    "Column " + name + " not found in table " + ( t instanceof SQLTableVariable tv ? tv.getName() : t.getAlias() ) );
			}
			colIndex	= column.getIndex();
			type		= column.getType();
			data		= table.getData();
		}
	}

	/**
	 * Set the table
	 */
	public void setTable( SQLTable table ) {
		// This node has no parent/child relationship, it's just a reference
		this.table = table;
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( QoQSelectExecution QoQExec ) {
		ensureData( QoQExec );
		return type;
		// return QoQExec.getTableLookup().get( getTableFinal( QoQExec ) ).getColumns().get( name ).getType();
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQSelectExecution QoQExec, int[] intersection ) {
		ensureData( QoQExec );
		int rowNum = intersection[ tableIndex ];

		// This means an outer join matched nothing
		if ( rowNum == 0 ) {
			return null;
		}

		return data.get( intersection[ tableIndex ] - 1 )[ colIndex ];
		/*
		 * var tableFinal = getTableFinal( QoQExec );
		 * // System.out.println( "getting SQL column: " + name.getName() + " from table: " + tableFinal.getName() + " with index: " + tableFinal.getIndex() );
		 * // System.out.println( "intersection: " + Arrays.toString( intersection ) );
		 * int rowNum = intersection[ tableFinal.getIndex() ];
		 * // This means an outer join matched nothing
		 * if ( rowNum == 0 ) {
		 * return null;
		 * }
		 * return QoQExec.getTableLookup().get( tableFinal ).getCell( name, rowNum - 1 );
		 */
	}

	/**
	 * Evaluate the expression aginst a partition of data
	 */
	public Object evaluateAggregate( QoQSelectExecution QoQExec, List<int[]> intersections ) {
		if ( intersections.isEmpty() ) {
			return null;
		}
		return evaluate( QoQExec, intersections.get( 0 ) );
	}

	/**
	 * Runtime check if the expression evaluates to a boolean value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a boolean value
	 */
	public boolean isBoolean( QoQSelectExecution QoQExec ) {
		return getType( QoQExec ) == QueryColumnType.BIT;
	}

	/**
	 * Runtime check if the expression evaluates to a numeric value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a numeric value
	 */
	public boolean isNumeric( QoQSelectExecution QoQExec ) {
		return numericTypes.contains( getType( QoQExec ) );
	}

	@Override
	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	@Override
	public BoxNode accept( ReplacingBoxVisitor v ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "name", name.getName() );
		if ( table != null ) {
			map.put( "table", table.toMap() );
		} else {
			map.put( "table", null );
		}
		return map;
	}

}
