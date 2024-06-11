/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.types;

import java.io.Serializable;
import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

public class QueryColumn implements IReferenceable, Serializable {

	/**
	 * The name of the column
	 */
	private Key					name;

	/**
	 * The type of the column
	 */
	private QueryColumnType		type;

	/**
	 * The query this column is a part of
	 */
	private transient Query		query;

	/**
	 * Keep in sync if columns are added or removed
	 */
	private int					index;

	/**
	 * Metadata object
	 */
	public transient BoxMeta	$bx;

	/**
	 * Serial version UID
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * Add new column to query
	 *
	 * @param name  column name
	 * @param type  column type
	 * @param query query
	 * @param index column index (0-based)
	 */
	public QueryColumn( Key name, QueryColumnType type, Query query, int index ) {
		this.name	= name;
		this.type	= type;
		this.query	= query;
		this.index	= index;
	}

	/**
	 * Get the metadata object for this column
	 *
	 * @return metadata object
	 */
	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			// TODO: create query column meta object.
			// getMetaData() in CF returns struct of
			/*
			 * IsCaseSensitive NO
			 * Name colName
			 * TypeName OBJECT
			 */
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;
	}

	public Key getName() {
		return name;
	}

	public QueryColumnType getType() {
		return type;
	}

	public Query getQuery() {
		return query;
	}

	public int getIndex() {
		return index;
	}

	// Convenience methods

	/**
	 * Set the value of a cell in this column
	 *
	 * @param row   The row to set, 0-based index
	 * @param value The value to set
	 *
	 * @return This QueryColumn
	 */
	public QueryColumn setCell( int row, Object value ) {
		query.validateRow( row );
		query.getData().get( row )[ index ] = value;
		return this;
	}

	/**
	 * Get the value of a cell in this column
	 *
	 * @param row The row to get, 0-based index
	 *
	 * @return The value of the cell
	 */
	public Object getCell( int row ) {
		// Does full null support change this?
		if ( query.isEmpty() ) {
			return "";
		}
		return this.query.getData().get( row )[ index ];
	}

	/**
	 * Get all data in a column as a Java Object[]
	 * Data is copied, so re-assignments into the array will not be reflected in the query.
	 * Mutating a complex object in the array will be reflected in the query.
	 *
	 * @return array of column data
	 */
	public Object[] getColumnData() {
		return query.getColumnData( name );
	}

	/**
	 * Get all data in a column as an BoxLang Array
	 * Data is copied, so re-assignments into the array will not be reflected in the query.
	 * Mutating a complex object in the array will be reflected in the query.
	 *
	 * @return array of column data
	 */
	public Array getColumnDataAsArray() {
		return query.getColumnDataAsArray( name );
	}

	public static int getIntFromKey( Key key, boolean safe ) {
		Integer index;

		// If key is int, use it directly
		if ( key instanceof IntKey intKey ) {
			index = intKey.getIntValue();
		} else {
			// If key is not an int, we must attempt to cast it
			CastAttempt<Double> indexAtt = DoubleCaster.attempt( key.getName() );
			if ( !indexAtt.wasSuccessful() ) {
				if ( safe ) {
					return -1;
				}
				throw new BoxRuntimeException( String.format(
				    "Query column cannot be assigned with key %s", key.getName()
				) );
			}
			Double dIndex = indexAtt.get();
			index = dIndex.intValue();
			// Dissallow non-integer indexes foo[1.5]
			if ( index.doubleValue() != dIndex ) {
				if ( safe ) {
					return -1;
				}
				throw new BoxRuntimeException( String.format(
				    "Query column index [%s] is invalid.  Index must be an integer.", dIndex
				) );
			}
		}
		return index;
	}

	/***************************
	 * IReferencable implementation
	 ****************************/

	@Override
	public Object dereference( IBoxContext context, Key name, Boolean safe ) {

		// Special check for $bx
		/*
		 * if ( name.equals( BoxMeta.key ) ) {
		 * return getBoxMeta();
		 * }
		 */

		// Check if the key is numeric
		int index = getIntFromKey( name, true );
		// If dereferncing a query column with a number like qry.col[1], then we ALWAYS get the value from that row
		if ( index > 0 ) {
			return getCell( index - 1 );
		}

		// If dereferncing a query column with a NON number like qry.col["key"], then we get the value at the "current" row and dererence it
		return Referencer.get( context, getCell( query.getRowFromContext( context ) ), name, safe );

	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		// qry.col.method() will ALWAYS get the value from the current row and call the method on that cell value
		// Unlike Lucee/Adobe, we'll never call the method on the query column itself
		return DynamicInteropService.invoke( context, getCell( query.getRowFromContext( context ) ), name.getName(), safe, positionalArguments );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		// qry.col.method() will ALWAYS get the value from the current row and call the method on that cell value
		// Unlike Lucee/Adobe, we'll never call the method on the query column itself
		return DynamicInteropService.invoke( context, getCell( query.getRowFromContext( context ) ), name.getName(), safe, namedArguments );
	}

	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {

		// Check if the key is numeric
		int index = getIntFromKey( name, true );
		// If assign a query column with a number like qry.col[1]='new value', then we ALWAYS get the value from that row
		if ( index > 0 ) {
			setCell( index - 1, value );
			return value;
		}

		// If dereferencing a query column with a NON number like qry.col["key"]="new value",
		// then we get the value at the "current" row and assign it (perhaps it's struct etc)
		Referencer.set( context, getCell( query.getRowFromContext( context ) ), name, value );
		return value;
	}

}
