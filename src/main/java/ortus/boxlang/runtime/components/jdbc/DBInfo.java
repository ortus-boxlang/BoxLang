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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = false )
public class DBInfo extends Component {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger( DBInfo.class );

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
		        Validator.valueOneOf( "columns", "dbnames", "tables", "foreignkeys", "index", "procedures", "version" ),
		        Validator.valueRequires( "columns", Key.table ),
		        Validator.valueRequires( "foreignkeys", Key.table ),
		        Validator.valueRequires( "index", Key.table )
		    ) ),
		    new Attribute( Key._NAME, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY
		    ) ),
		    new Attribute( Key.datasource, "string" ),
		    new Attribute( Key.table, "string" ),
		    new Attribute( Key.pattern, "string" ),
		    new Attribute( Key.dbname, "string" ),
		    new Attribute( Key.filter, "string" ),

		    // We probably will not implement these. We can remove later if we decide not to.
		    new Attribute( Key.username, "string", Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.password, "string", Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		};
	}

	/**
	 * Retrieve database metadata for a given datasource. This can include: column metadata, database names, table names, foreign keys, index info,
	 * stored procedures, and version info.
	 * <p>
	 * Please note that the <code>type</code> attribute is required, and the <code>name</code> attribute is required to store the result.
	 *
	 * @attribute.type Type of metadata to retrieve. One of: `columns`, `dbnames`, `tables`, `foreignkeys`, `index`, `procedures`, or `version`.
	 *
	 * @attribute.name Name of the variable to which the result will be assigned. Required.
	 *
	 * @attribute.table Table name for which to retrieve metadata. Required for `columns`, `foreignkeys`, and `index` types.
	 *
	 * @attribute.datasource Name of the datasource to check metadata on. If not provided, the default datasource will be used.
	 *
	 * @attribute.filter This is a string value that must match a table type in your database implementation. Each database is different.
	 *                   Some common filter types are:
	 *                   <ul>
	 *                   <li>TABLE - This is the default value and will return only tables.</li>
	 *                   <li>VIEW - This will return only views.</li>
	 *                   <li>SYSTEM TABLE - This will return only system tables.</li>
	 *                   <li>GLOBAL TEMPORARY - This will return only global temporary tables.</li>
	 *                   <li>LOCAL TEMPORARY - This will return only local temporary tables.</li>
	 *                   <li>ALIAS - This will return only aliases.</li>
	 *                   <li>SYNONYM - This will return only synonyms.</li>
	 *                   </ul>
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IJDBCCapableContext	jdbcContext			= context.getParentOfType( IJDBCCapableContext.class );
		ConnectionManager	connectionManager	= jdbcContext.getConnectionManager();

		// Prep arguments
		DataSource			datasource			= attributes.containsKey( Key.datasource )
		    ? connectionManager.getDatasourceOrThrow( Key.of( attributes.getAsString( Key.datasource ) ) )
		    : connectionManager.getDefaultDatasourceOrThrow();
		String				tableNameLookup		= attributes.getAsString( Key.table );
		if ( tableNameLookup == null ) {
			tableNameLookup = attributes.getAsString( Key.pattern );
		}
		DBInfoType	type	= DBInfoType.fromString( attributes.getAsString( Key.type ) );
		String		filter	= attributes.getAsString( Key.filter );

		// If the filter is set, the type must be "tables"
		if ( filter != null && !filter.isEmpty() && !type.equals( DBInfoType.TABLES ) ) {
			throw new BoxValidationException( "The 'filter' attribute can only be used with the 'tables' type" );
		}

		// Now that we have the datasource, we can get the connection and metadata by type
		try ( Connection conn = datasource.getConnection(); ) {
			DatabaseMetaData databaseMetadata = conn.getMetaData();
			tableNameLookup = normalizeTableNameCasing( databaseMetadata, tableNameLookup );
			String databaseName = attributes.getAsString( Key.dbname );

			if ( databaseName == null ) {
				// Specify database name in a dot-delimited string, i.e. "mDB.mySchema.tblUsers".
				databaseName = parseDatabaseFromTableName( tableNameLookup );
			}

			if ( databaseName == null ) {
				// Default to the database name set on the connection (provided by the datasource config).
				databaseName = getDatabaseNameFromConnection( conn );
			}

			String	schema		= parseSchemaFromTableName( tableNameLookup );
			String	tableName	= parseTableName( tableNameLookup );

			// If the filter is not null and not empty, validate it at runtime against the valid database metadata filter types
			if ( filter != null && !filter.isEmpty() ) {
				validateFilter( filter, databaseMetadata.getTableTypes() );
			}

			Query result = ( switch ( type ) {
				case DBNAMES -> getDbNames( databaseMetadata );
				case VERSION -> getVersion( databaseMetadata );
				case COLUMNS -> getColumnsForTable( databaseMetadata, databaseName, schema, tableName );
				case TABLES -> getTables( databaseMetadata, databaseName, schema, tableName, filter );
				case FOREIGNKEYS -> getForeignKeys( databaseMetadata, databaseName, schema, tableName );
				case INDEX -> getIndexes( databaseMetadata, databaseName, schema, tableName );
				case PROCEDURES -> getProcedures( databaseMetadata, databaseName, schema, tableName );
			} );
			ExpressionInterpreter.setVariable( context, attributes.getAsString( Key._NAME ), result );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Unable to read " + attributes.getAsString( Key.type ) + " metadata", e );
		}
		return DEFAULT_RETURN;
	}

	/**
	 * Validate the filter attribute against the valid database metadata filter types.
	 *
	 * @param filter     The filter value to validate.
	 * @param tableTypes ResultSet of valid table types from the database metadata.
	 *
	 * @throws BoxValidationException If the filter value is not found in the table types ResultSet.
	 */
	private void validateFilter( String filter, ResultSet tableTypes ) {
		List<String> allowedTypes = new ArrayList<>();

		try ( tableTypes ) {
			allowedTypes = new ArrayList<>();
			boolean validType = false;

			while ( tableTypes.next() ) {
				String type = tableTypes.getString( 1 );
				allowedTypes.add( type );
				if ( type.equals( filter ) ) {
					validType = true;
					break;
				}
			}

			if ( !validType ) {
				throw new BoxValidationException(
				    String.format( "Invalid [dbinfo] type=table filter [%s]. Supported table types are %s.", filter, String.join( ", ", allowedTypes ) )
				);
			}
		} catch ( SQLException e ) {
			throw new DatabaseException( "Error retrieving table types.", e );
		}
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
	 * @param datasource   Datasource on which the table resides.
	 * @param databaseName Name of the database to check for tables. If not provided, the database name from the connection will be used.
	 * @param tableName    Optional pattern to filter table names by. Can use wildcards or any `LIKE`-compatible pattern such as `tbl_%`. Can use
	 *                     `schemaName.tableName` syntax to additionally filter by schema.
	 *
	 * @return Query object where each row represents a column on the given table.
	 */
	private Query getColumnsForTable( DatabaseMetaData databaseMetadata, String databaseName, String schema, String tableName ) throws SQLException {

		// logger.trace( "getColumnsForTable: databaseName: {}, schema: {}, tableName: {}", databaseName, schema, tableName );

		Query result = new Query();
		try ( ResultSet resultSet = databaseMetadata.getColumns( databaseName, schema, tableName, null ) ) {
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			buildQueryColumns( result, resultSetMetaData );

			result.addColumn( Key.of( "IS_PRIMARYKEY" ), QueryColumnType.BIT );
			result.addColumn( Key.of( "IS_FOREIGNKEY" ), QueryColumnType.BIT );
			result.addColumn( Key.of( "REFERENCED_PRIMARYKEY" ), QueryColumnType.VARCHAR );
			result.addColumn( Key.of( "REFERENCED_PRIMARYKEY_TABLE" ), QueryColumnType.VARCHAR );

			// Cache primary key and foreign key info for each table to avoid repeated queries unless this is a multi-table result set.
			// i.e. if the catalog, schema, and table are the same, we can reuse the cached key info for each column.
			Map<String, List<String>>						primaryKeyCache	= new HashMap<>();
			Map<String, Map<String, Map<String, String>>>	foreignKeyCache	= new HashMap<>();
			while ( resultSet.next() ) {
				IStruct								row					= buildQueryRow( resultSet, resultSetMetaData );

				// These can be null
				String								columnCatalog		= resultSet.getString( "TABLE_CAT" ) == null
				    ? ""
				    : resultSet.getString( "TABLE_CAT" );
				String								columnSchema		= resultSet.getString( "TABLE_SCHEM" ) == null
				    ? ""
				    : resultSet.getString( "TABLE_SCHEM" );

				// This one is never null
				String								columnTable			= resultSet.getString( "TABLE_NAME" );
				String								lookupHash			= String.format( "%d.%d.%d", columnCatalog.hashCode(), columnSchema.hashCode(),
				    columnTable.hashCode() );

				List<String>						primaryKeys			= primaryKeyCache.computeIfAbsent(
				    lookupHash,
				    k -> getPrimaryKeys( databaseMetadata, columnCatalog, columnSchema, columnTable )
				);
				Map<String, Map<String, String>>	foreignKeys			= foreignKeyCache.computeIfAbsent(
				    lookupHash,
				    k -> getForeignKeysAsMap( databaseMetadata, columnCatalog, columnSchema, columnTable )
				);
				boolean								isPrimaryKey		= primaryKeys.contains( row.getAsString( Key.of( "COLUMN_NAME" ) ) );
				boolean								isForeignKey		= foreignKeys.containsKey( row.getAsString( Key.of( "COLUMN_NAME" ) ) );
				String								referencedKeyColumn	= "N/A";
				String								referencedKeyTable	= "N/A";

				if ( isForeignKey ) {
					Map<String, String> fkey = foreignKeys.get( row.getAsString( Key.of( "COLUMN_NAME" ) ) );
					referencedKeyColumn	= fkey.get( "PKCOLUMN_NAME" );
					referencedKeyTable	= fkey.get( "PKTABLE_NAME" );
				}

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
	 * @param filter       Optional filter to apply to the table type. Can be `TABLE`, `VIEW`, `SYSTEM TABLE`, `GLOBAL TEMPORARY`, `LOCAL TEMPORARY`,
	 *
	 * @return Query object where each row represents a table in the provided database.
	 */
	private Query getTables(
	    DatabaseMetaData databaseMetadata,
	    String databaseName,
	    String schema,
	    String tableName,
	    String filter ) throws SQLException {
		Query result = new Query();

		try ( ResultSet resultSet = databaseMetadata.getTables( databaseName, schema, tableName, filter == null ? null : new String[] { filter } ) ) {
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
		int	columnCount		= resultSetMetaData.getColumnCount();
		int	emptyCounter	= 0;
		// The column count starts from 1
		for ( int i = 1; i <= columnCount; i++ ) {
			String label = resultSetMetaData.getColumnLabel( i );
			if ( label.isBlank() ) {
				label = "column_" + ( emptyCounter++ );
			}
			result.addColumn(
			    Key.of( label ),
			    QueryColumnType.fromSQLType( resultSetMetaData.getColumnType( i ) )
			);
		}
	}

	/**
	 * Extract the database name from a dot-delimited string, or return null if no period is present.
	 *
	 * @param tableName Table name to parse which MAY include a database and schema name like "myDB.mySchema.tblUsers".
	 *
	 * @return The database name portion of the input string, or null.
	 */
	private String parseDatabaseFromTableName( String tableName ) {
		if ( tableName != null && tableName.contains( "." ) ) {
			String[] parts = tableName.split( "\\." );
			return parts.length == 3 ? parts[ 0 ] : null;
		}
		return null;
	}

	/**
	 * Extract the table name from a dot-delimited string, or return the input string if no period is present.
	 *
	 * @param tableName Table name to parse. Can be a table name like "tblUsers", or a dot-delimited string like "myDB.mySchema.tblUsers" or simply "mySchema.tblUsers".
	 *
	 * @return The table name portion of the input string, or null if tableName is null.
	 */
	private String parseTableName( String tableName ) {
		if ( tableName != null && tableName.contains( "." ) ) {
			// myDB.mySchema.tblUsers or mySchema.tblUsers
			return tableName.substring( tableName.lastIndexOf( '.' ) + 1 );
		}
		// tblUsers
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
			String[] parts = tableName.split( "\\." );
			return parts.length == 3
			    // myDB.mySchema.tblUsers
			    ? parts[ 1 ]
			    // mySchema.tblUsers
			    : parts[ 0 ];
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

	/**
	 * Retrieve the primary keys for a given catalog/schema/table combo and return them as a list of strings.
	 *
	 * @param metadata Database metadata object to use for querying, i.e. connection.getMetaData().
	 * @param catalog  Catalog name to filter by.
	 * @param schema   Schema name to filter by.
	 * @param table    Table name to filter by.
	 */
	private List<String> getPrimaryKeys( DatabaseMetaData metadata, String catalog, String schema, String table ) {
		List<String> temp = new ArrayList<>();
		try ( ResultSet keys = metadata.getPrimaryKeys( catalog, schema, table ) ) {
			while ( keys.next() ) {
				temp.add( keys.getString( "COLUMN_NAME" ) );
			}
		} catch ( SQLException e ) {
			logger.error( "Unable to read foreign key info for table [{}], schema [{}], and catalog [{}]", table, schema, catalog, e );
			throw new BoxRuntimeException( "Unable to read foreign key info", e );
		}
		return temp;
	}

	/**
	 * Retrieve the foreign keys for a given catalog/schema/table combo and return them as a map of column names to primary key column names and table names.
	 *
	 * @param metadata Database metadata object to use for querying, i.e. connection.getMetaData().
	 * @param catalog  Catalog name to filter by.
	 * @param schema   Schema name to filter by.
	 * @param table    Table name to filter by.
	 */
	private Map<String, Map<String, String>> getForeignKeysAsMap( DatabaseMetaData metadata, String catalog, String schema, String table ) {
		Map<String, Map<String, String>> temp = new HashMap<>();
		try ( ResultSet keys = metadata.getImportedKeys( catalog, schema, table ) ) {
			while ( keys.next() ) {
				temp.put( keys.getString( "FKCOLUMN_NAME" ), Map.of(
				    "PKCOLUMN_NAME", keys.getString( "PKCOLUMN_NAME" ),
				    "PKTABLE_NAME", keys.getString( "PKTABLE_NAME" )
				) );
			}
		} catch ( SQLException e ) {
			logger.error( "Unable to read foreign key info for table [{}], schema [{}], and catalog [{}]", table, schema, catalog, e );
			throw new BoxRuntimeException( "Unable to read foreign key info", e );
		}
		return temp;
	}

	private String getDatabaseNameFromConnection( Connection conn ) {
		try {
			return conn.getCatalog();
		} catch ( SQLException e ) {
			logger.warn( "Unable to read database name from connection", e );
			return null;
		}
	}
}
