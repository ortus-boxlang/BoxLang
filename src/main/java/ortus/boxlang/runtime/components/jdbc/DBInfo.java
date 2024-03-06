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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.validation.Validator;
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

	Logger log = LoggerFactory.getLogger( DBInfo.class );

	/**
	 * Enumeration of all possible `type` attribute values.
	 */
	private enum DBInfoType {

		COLUMNS,
		DBNAMES,
		TABLES,
		FOREIGNKEYS,
		INDEX,
		PROCEDURES,
		VERSION;

		public static DBInfoType fromString( String type ) {
			return DBInfoType.valueOf( type.trim().toUpperCase() );
		}
	}

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
		    new Attribute( Key.table, "string" ),
		    new Attribute( Key.pattern, "string" ),
		    new Attribute( Key.dbname, "string" ),
			// @TODO: Implement
			// new Attribute( Key.username, "string" ),
			// new Attribute( Key.password, "string" )
			// NOTE: Lucee also supports a `filter` attribute, but ONLY for `type="tables"`. IMO, this should be handled by a query filter instead.
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
		// @TODO: Add a DataSourceManager to the runtime. For now, we'll manually construct one.
		// DataSourceManager manager = context.getRuntime().getDataSourceManager();
		DataSourceManager	manager		= DataSourceManager.getInstance();
		DataSource			datasource	= attributes.containsKey( Key.datasource )
		    ? manager.getDataSource( Key.of( attributes.getAsString( Key.datasource ) ) )
		    : manager.getDefaultDataSource();
		if ( datasource == null ) {
			if ( attributes.containsKey( Key.datasource ) ) {
				throw new DatabaseException( String.format( "Datasource not found for string name [%s]", attributes.getAsString( Key.datasource ) ) );
			} else {
				throw new DatabaseException(
				    "Default datasource not found; You must supply a `datasource` attribute or define a default datasource in Application.bx"
				);
			}
		}
		String	databaseName	= attributes.getAsString( Key.dbname );
		String	tableNameLookup	= attributes.getAsString( Key.table );
		if ( tableNameLookup == null ) {
			tableNameLookup = attributes.getAsString( Key.pattern );
		}

		DBInfoType type = DBInfoType.fromString( attributes.getAsString( Key.type ) );

		try ( Connection conn = datasource.getConnection(); ) {
			DatabaseMetaData databaseMetadata = conn.getMetaData();
			// Lucee compat: Default to the database name set on the connection (provided by the datasource config).
			databaseName	= databaseName != null ? databaseName : getDatabaseNameFromConnection( conn );

			// Lucee compat: Pull table name and schema name from a dot-delimited string, like "mySchema.tblUsers"
			tableNameLookup	= normalizeTableNameCasing( databaseMetadata, tableNameLookup );
			String	tableName	= parseTableName( tableNameLookup );
			String	schema		= parseSchemaFromTableName( tableNameLookup );
			Query	result		= ( switch ( type ) {
									case DBNAMES -> getDbNames( databaseMetadata );
									case VERSION -> getVersion( databaseMetadata );
									case COLUMNS -> getColumnsForTable( databaseMetadata, databaseName, schema, tableName );
									case TABLES -> getTables( databaseMetadata, databaseName, schema, tableName );
									case FOREIGNKEYS -> getForeignKeys( databaseMetadata, databaseName, schema, tableName );
									case INDEX -> getIndexes( databaseMetadata, databaseName, schema, tableName );
									case PROCEDURES -> getProcedures( databaseMetadata, databaseName, schema, tableName );
								} );
			ExpressionInterpreter.setVariable( context, attributes.getAsString( Key._NAME ), result );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Unable to read " + attributes.getAsString( Key.type ) + " metadata", e );
		}
		// @TODO: Return null???
		return DEFAULT_RETURN;
	}

	/**
	 * Build a Query object of database names for both SCHEMA and CATALOG types.
	 *
	 * @param datasource DataSource on which to pull database names
	 *
	 * @return A Query object where each row represents a catalog name or schema name within this datasource.
	 */
	private Query getDbNames( DatabaseMetaData databaseMetadata ) throws SQLException {
		Query result = new Query();
		result.addColumn( Key.of( "DBNAME" ), QueryColumnType.VARCHAR );
		result.addColumn( Key.of( "type" ), QueryColumnType.VARCHAR );
		try ( ResultSet catalogs = databaseMetadata.getCatalogs() ) {
			while ( catalogs.next() ) {
				result.addRow( new Object[] { catalogs.getObject( 1 ), "CATALOG" } );
			}
		}
		try ( ResultSet schemas = databaseMetadata.getSchemas(); ) {
			while ( schemas.next() ) {
				result.addRow( new Object[] { schemas.getObject( 1 ), "SCHEMA" } );
			}
		}
		// @TODO: Support `pattern` attribute here, like Lucee does, to filter the results.
		return result;
	}

	/**
	 * Build a Query object with the database version info.
	 * For historical reasons, the first six columns must match the historical column names.
	 *
	 * @param datasource Datasource on which to check version info.
	 *
	 * @return A single-row Query object populated with JDBC driver version info.
	 */
	private Query getVersion( DatabaseMetaData databaseMetadata ) throws SQLException {
		Query result = new Query();
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
	}

	/**
	 * Retrieve column metadata for a given table name.
	 *
	 * @TODO: Add support for Lucee's custom foreign key and primary key fields.
	 *
	 * @param datasource   Datasource on which the table resides.
	 * @param databaseName Name of the database to check for tables. If not provided, the database name from the connection will be used.
	 * @param tableName    Optional pattern to filter table names by. Can use wildcards or any `LIKE`-compatible pattern such as `tbl_%`. Can use
	 *                     `schemaName.tableName` syntax to additionally filter by schema.
	 *
	 * @return Query object where each row represents a column on the given table.
	 */
	private Query getColumnsForTable( DatabaseMetaData databaseMetadata, String databaseName, String schema, String tableName ) throws SQLException {
		Query result = new Query();
		try ( ResultSet resultSet = databaseMetadata.getColumns( databaseName, schema, tableName, null ) ) {
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			buildQueryColumns( result, resultSetMetaData );

			while ( resultSet.next() ) {
				IStruct	row					= buildQueryRow( resultSet, resultSetMetaData );
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
			if ( result.isEmpty() && ( !databaseMetadata.getTables( null, schema, tableName, null ).next() ) ) {
				throw new DatabaseException( String.format( "Table not found for pattern [%s] on schema [%s]", tableName, schema ) );
			}
		}
		return result;
	}

	/**
	 * Retrieve table metadata, optionally filtering by a table name LIKE pattern.
	 *
	 * @param datasource   Datasource on which the table resides.
	 * @param databaseName Name of the database to check for tables. If not provided, the database name from the connection will be used.
	 * @param tableName    Optional pattern to filter table names by. Can use wildcards or any `LIKE`-compatible pattern such as `tbl_%`. Can use
	 *                     `schemaName.tableName` syntax to additionally filter by schema.
	 *
	 * @return Query object where each row represents a table in the provided database.
	 */
	private Query getTables( DatabaseMetaData databaseMetadata, String databaseName, String schema, String tableName ) throws SQLException {
		Query result = new Query();

		try ( ResultSet resultSet = databaseMetadata.getTables( databaseName, schema, tableName, null ) ) {
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			buildQueryColumns( result, resultSetMetaData );

			while ( resultSet.next() ) {
				IStruct row = buildQueryRow( resultSet, resultSetMetaData );
				result.addRow( row );
			}
		}
		return result;
	}

	/**
	 * Retrieve foreign keys referencing the specified table from the specified (or datasource-configured) database.
	 *
	 * @param datasource   Datasource to connect on.
	 * @param databaseName Name of the database to filter by. If not provided, the database name from the connection will be used.
	 * @param tableName    Table name on which to retrieve foreign keys. Can use `schemaName.tableName` syntax to additionally filter by schema.
	 *
	 * @return Query object where each row represents a foreign key on the specified table.
	 */
	private Query getForeignKeys( DatabaseMetaData databaseMetadata, String databaseName, String schema, String tableName ) throws SQLException {
		Query result = new Query();
		try ( ResultSet resultSet = databaseMetadata.getExportedKeys( databaseName, schema, tableName ) ) {
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			buildQueryColumns( result, resultSetMetaData );

			while ( resultSet.next() ) {
				IStruct row = buildQueryRow( resultSet, resultSetMetaData );
				result.addRow( row );
			}
			if ( result.isEmpty() && ( !databaseMetadata.getTables( null, schema, tableName, null ).next() ) ) {
				throw new DatabaseException( String.format( "Table not found for pattern [%s] on schema [%s]", tableName, schema ) );
			}
		}
		return result;
	}

	/**
	 * Retrieve indices on the specified table from the specified (or datasource-configured) database.
	 *
	 * @param datasource   Datasource to connect on.
	 * @param databaseName Name of the database to filter by. If not provided, the database name from the connection will be used.
	 * @param tableName    Table name on which to retrieve indices. Can use `schemaName.tableName` syntax to additionally filter by schema.
	 *
	 * @return Query object where each row represents an index on the specified table.
	 */
	private Query getIndexes( DatabaseMetaData databaseMetadata, String databaseName, String schema, String tableName ) throws SQLException {
		Query result = new Query();
		try ( ResultSet resultSet = databaseMetadata.getIndexInfo( databaseName, schema, tableName, false, true ) ) {
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			buildQueryColumns( result, resultSetMetaData );

			while ( resultSet.next() ) {
				IStruct	row				= buildQueryRow( resultSet, resultSetMetaData );
				// type
				Integer	indexType		= row.getAsInteger( Key.type );
				String	stringIndexType	= switch ( indexType ) {
											case 0 -> "Table Statistic";
											case 1 -> "Clustered Index";
											case 2 -> "Hashed Index";
											case 3 -> "Other Index";
											default -> row.getAsString( Key.type );
										};
				row.put( Key.type, stringIndexType );

				// Lucee compat: Lucee actually converts the "CARDINALITY" value to a Double here. Do we care??
				result.addRow( row );
			}
			if ( result.isEmpty() && ( !databaseMetadata.getTables( null, schema, tableName, null ).next() ) ) {
				throw new DatabaseException( String.format( "Table not found for pattern [%s] on schema [%s]", tableName, schema ) );
			}
		}
		return result;
	}

	/**
	 * Retrieve procedures from the specified (or datasource-configured) database.
	 *
	 * @param datasource   Datasource to connect on.
	 * @param databaseName Name of the database to filter by. If not provided, the database name from the connection will be used.
	 * @param schema       Optional schema name to filter by.
	 * @param tableName    Optional pattern to filter table names by. Can use wildcards or any `LIKE`-compatible pattern such as `tbl_%`.
	 *
	 * @return Query object where each row represents an index on the specified table.
	 */
	private Query getProcedures( DatabaseMetaData databaseMetadata, String databaseName, String schema, String tableName ) throws SQLException {
		Query result = new Query();
		try ( ResultSet resultSet = databaseMetadata.getProcedures( databaseName, schema, tableName ) ) {
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			buildQueryColumns( result, resultSetMetaData );

			while ( resultSet.next() ) {
				IStruct row = buildQueryRow( resultSet, resultSetMetaData );
				result.addRow( row );
			}
		}
		return result;
	}

	/**
	 * Acquire a Struct instance for populating a Query row from a ResultSet.
	 *
	 * @param resultSet         JDBC ResultSet from which to read row data.
	 * @param resultSetMetaData JDBC ResultSetMetaData used for column names.
	 *
	 * @throws SQLException
	 */
	private IStruct buildQueryRow( ResultSet resultSet, ResultSetMetaData resultSetMetaData ) throws SQLException {
		int		columnCount	= resultSetMetaData.getColumnCount();
		Struct	row			= new Struct( IStruct.TYPES.LINKED );
		for ( int i = 1; i <= columnCount; i++ ) {
			row.put(
			    Key.of( resultSetMetaData.getColumnLabel( i ) ),
			    resultSet.getObject( i )
			);
		}
		return row;
	}

	/**
	 * Append columns to a Query object based on the column names and types in a ResultSet.
	 *
	 * @param result            Result Query object to add columns to.
	 * @param resultSetMetaData JDBC ResultSetMetaData used for column names.
	 *
	 * @throws SQLException
	 */
	private void buildQueryColumns( Query result, ResultSetMetaData resultSetMetaData ) throws SQLException {
		int columnCount = resultSetMetaData.getColumnCount();
		// The column count starts from 1
		for ( int i = 1; i <= columnCount; i++ ) {
			result.addColumn(
			    Key.of( resultSetMetaData.getColumnLabel( i ) ),
			    QueryColumnType.fromSQLType( resultSetMetaData.getColumnType( i ) )
			);
		}
	}

	/**
	 * Extract the table name from a dot-delimited string, or return the input string if no period is present.
	 *
	 * @param tableName Table name to parse. Can be a table name like "tblUsers", or a dot-delimited string like "mySchema.tblUsers".
	 *
	 * @return The table name portion of the input string, or null if tableName is null.
	 */
	private String parseTableName( String tableName ) {
		if ( tableName != null && tableName.contains( "." ) ) {
			return tableName.split( "\\." )[ 0 ];
		}
		return tableName;
	}

	/**
	 * Extract the schema name from a dot-delimited string.
	 *
	 * @param tableName Table name to parse. Can be a table name like "tblUsers", or a dot-delimited string like "mySchema.tblUsers".
	 *
	 * @return The schema name portion of the input string IF the input is non-null and contains a period. Else null.
	 */
	private String parseSchemaFromTableName( String tableName ) {
		if ( tableName != null && tableName.contains( "." ) ) {
			return tableName.split( "\\." )[ 1 ];
		}
		return null;
	}

	/**
	 * Correct the schema or table name identifier case based on what the database vendor claims to store identifiers as - lower or upper case.
	 *
	 * @param metaData      The DatabaseMetaData object from which to read identifier casing preferences.
	 * @param tableOrSchema The table or schema name to normalize, or even a dot-delimited string like "mySchema.tblUsers".
	 *
	 * @throws SQLException
	 */
	private String normalizeTableNameCasing( DatabaseMetaData metaData, String tableOrSchema ) throws SQLException {
		if ( tableOrSchema == null ) {
			return null;
		}
		if ( metaData.storesLowerCaseIdentifiers() )
			return tableOrSchema.toLowerCase();
		if ( metaData.storesUpperCaseIdentifiers() )
			return tableOrSchema.toUpperCase();
		return tableOrSchema;
	}

	private String getDatabaseNameFromConnection( Connection conn ) {
		try {
			return conn.getCatalog();
		} catch ( SQLException e ) {
			log.warn( "Unable to read database name from connection", e );
			return null;
		}
	}
}
