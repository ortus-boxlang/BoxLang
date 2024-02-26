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
package ortus.boxlang.runtime.components.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.validators.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

@BoxComponent( allowsBody = false )
public class DBInfo extends Component {

	/**
	 * Constructor
	 */
	public DBInfo() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.type, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "columns", "dbnames", "tables", "foreignkeys", "index", "procedures", "version" )
			// @TODO: Figure out why the `valueRequires` validator s requiring the `table` argument regardless of the `type` value!
			// Validator.valueRequires( "columns", Key.table ),
			// Validator.valueRequires( "foreignkeys", Key.table ),
			// Validator.valueRequires( "index", Key.table )
		    ) ),
		    new Attribute( Key._NAME, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY
		    ) ),
		    new Attribute( Key.datasource, "string" ),
			// @TODO: Implement
			// new Attribute( Key.table, "string" ),
			// new Attribute( Key.pattern, "string" ),
			// new Attribute( Key.dbname, "string" ),
			// new Attribute( Key.username, "string" ),
			// new Attribute( Key.password, "string" )
		};
	}

	/**
	 * Retrieve database metadata for a given datasource.
	 * <p>
	 *
	 * @attribute.type Type of metadata to retrieve. One of: `columns`, `dbnames`, `tables`, `foreignkeys`, `index`, `procedures`, or `version`.
	 *
	 * @attribute.name Name of the variable to which the result will be assigned. Required.
	 *
	 * @attribute.table Table name for which to retrieve metadata. Required for `columns`, `foreignkeys`, and `index` types.
	 *
	 * @attribute.datasource Name of the datasource to check metadata on. If not provided, the default datasource will be used.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		// @TODO: Add a DatasourceManager to the runtime. For now, we'll manually construct one.
		// DataSourceManager manager = context.getRuntime().getDatasourceManager();
		DataSourceManager	manager		= DataSourceManager.getInstance();
		DataSource			datasource	= attributes.containsKey( Key.datasource )
		    ? manager.getDatasource( Key.of( attributes.getAsString( Key.datasource ) ) )
		    : manager.getDefaultDatasource();
		if ( datasource == null ) {
			if ( attributes.containsKey( Key.datasource ) ) {
				throw new DatabaseException( String.format( "Datasource not found for string name [%s]", attributes.getAsString( Key.datasource ) ) );
			} else {
				throw new DatabaseException(
				    "Default datasource not found; You must supply a `datasource` attribute or define a default datasource in Application.bx"
				);
			}
		}
		Query result = null;
		switch ( DBInfoType.fromString( attributes.getAsString( Key.type ) ) ) {
			case DBNAMES :
				result = getDbNames( datasource );
				break;
			case VERSION :
				result = getVersion( datasource );
				break;
			case COLUMNS :
				result = getColumnsForTable( datasource, attributes.getAsString( Key.table ) );
				break;
			// @TODO: Implement remaining types.
			// case TABLES :
			// result = getTables( context );
			// case FOREIGNKEYS :
			// result = getForeignKeys( context, attributes );
			// case INDEX :
			// result = getIndex( context, attributes );
			// case PROCEDURES :
			// result = getProcedures( context );
		}
		ExpressionInterpreter.setVariable( context, attributes.getAsString( Key._NAME ), result );
		// @TODO: Return null???
		return DEFAULT_RETURN;
	}

	/**
	 * Build a Query object of database names for both SCHEMA and CATALOG types.
	 *
	 * @param datasource Datasource on which to pull database names
	 *
	 * @return A Query object where each row represents a catalog name or schema name within this datasource.
	 */
	private Query getDbNames( DataSource datasource ) {
		try ( Connection conn = datasource.getConnection(); ) {
			Query result = new Query();
			result.addColumn( Key.of( "DBNAME" ), QueryColumnType.VARCHAR );
			result.addColumn( Key.of( "type" ), QueryColumnType.VARCHAR );
			try ( ResultSet catalogs = conn.getMetaData().getCatalogs() ) {
				while ( catalogs.next() ) {
					result.addRow( new Object[] { catalogs.getObject( 1 ), "CATALOG" } );
				}
			}
			try ( ResultSet schemas = conn.getMetaData().getSchemas(); ) {
				while ( schemas.next() ) {
					result.addRow( new Object[] { schemas.getObject( 1 ), "SCHEMA" } );
				}
			}
			// @TODO: Support `pattern` attribute here, like Lucee does, to filter the results.
			return result;
		} catch ( SQLException e ) {
			throw new DatabaseException( "Unable to read database names", e );
		}
	}

	/**
	 * Build a Query object with the database version info.
	 * For historical reasons, the first six columns must match the historical column names.
	 *
	 * @param datasource Datasource on which to check version info.
	 *
	 * @return A single-row Query object populated with JDBC driver version info.
	 */
	private Query getVersion( DataSource datasource ) {
		try ( Connection conn = datasource.getConnection(); ) {
			Query				result				= new Query();
			DatabaseMetaData	databaseMetadata	= conn.getMetaData();
			result.addColumn( Key.of( "DATABASE_PRODUCTNAME" ), QueryColumnType.VARCHAR, new Object[] {
			    databaseMetadata.getDatabaseProductName()
			} );
			result.addColumn( Key.of( "DATABASE_VERSION" ), QueryColumnType.VARCHAR, new Object[] {
			    databaseMetadata.getDatabaseProductVersion()
			} );
			result.addColumn( Key.of( "DRIVER_NAME" ), QueryColumnType.VARCHAR, new Object[] {
			    databaseMetadata.getDriverName()
			} );
			result.addColumn( Key.of( "DRIVER_VERSION" ), QueryColumnType.VARCHAR, new Object[] {
			    databaseMetadata.getDriverVersion()
			} );
			result.addColumn( Key.of( "JDBC_MAJOR_VERSION" ), QueryColumnType.VARCHAR, new Object[] {
			    Double.valueOf( databaseMetadata.getJDBCMajorVersion() )
			} );
			result.addColumn( Key.of( "JDBC_MINOR_VERSION" ), QueryColumnType.DOUBLE, new Object[] {
			    Double.valueOf( databaseMetadata.getJDBCMinorVersion() )
			} );
			return result;
		} catch ( SQLException e ) {
			throw new DatabaseException( "Unable to read database version info", e );
		}
	}

	/**
	 * Retrieve column metadata for a given table name.
	 *
	 * @param datasource Datasource on which the table resides.
	 *
	 * @return Query object where each row represents a column on the given table.
	 */
	private Query getColumnsForTable( DataSource datasource, String tableName ) {
		try ( Connection conn = datasource.getConnection(); ) {
			Query				result				= new Query();
			DatabaseMetaData	databaseMetadata	= conn.getMetaData();

			try ( ResultSet resultSet = databaseMetadata.getColumns( null, null, tableName, null ) ) {
				ResultSetMetaData	resultSetMetaData	= resultSet.getMetaData();
				int					columnCount			= resultSetMetaData.getColumnCount();

				// The column count starts from 1
				for ( int i = 1; i <= columnCount; i++ ) {
					result.addColumn(
					    Key.of( resultSetMetaData.getColumnLabel( i ) ),
					    QueryColumnType.fromSQLType( resultSetMetaData.getColumnType( i ) )
					);
				}

				while ( resultSet.next() ) {
					Struct row = new Struct( IStruct.TYPES.LINKED );
					for ( int i = 1; i <= columnCount; i++ ) {
						row.put(
						    Key.of( resultSetMetaData.getColumnLabel( i ) ),
						    resultSet.getObject( i )
						);
					}
					// @TODO: Implement primary key detection and columns
					boolean	isPrimaryKey		= false;
					// @TODO: Implement foreign key detection and columns
					boolean	isForeignKey		= false;
					String	referencedKeyColumn	= "N/A";
					String	referencedKeyTable	= "N/A";
					row.put( Key.of( "IS_PRIMARYKEY" ), isPrimaryKey );
					row.put( Key.of( "IS_FOREIGNKEY" ), isForeignKey );
					row.put( Key.of( "REFERENCED_PRIMARYKEY" ), referencedKeyColumn );
					row.put( Key.of( "REFERENCED_PRIMARYKEY_TABLE" ), referencedKeyTable );
					result.addRow( row );
				}
				if ( result.isEmpty() && ( !databaseMetadata.getTables( null, null, tableName, null ).next() ) ) {
					throw new DatabaseException( String.format( "Table not found for pattern [%s] ", tableName ) );
				}
			}
			return result;
		} catch ( SQLException e ) {
			throw new DatabaseException( "Unable to read column metadata", e );
		}
	}

	/**
	 * Enumeration of all possible `type` attribute values.
	 */
	private enum DBInfoType {

		COLUMNS,
		DBNAMES,
	    // TABLES,
	    // FOREIGNKEYS,
	    // INDEX,
	    // PROCEDURES,
		VERSION;

		public static DBInfoType fromString( String type ) {
			return DBInfoType.valueOf( type.trim().toUpperCase() );
		}
	}
}
