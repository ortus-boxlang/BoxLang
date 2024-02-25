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
		    new Attribute( Key.table, "string" ),
		    new Attribute( Key.datasource, "string" )
		};
	}

	/**
	 * Retrieve database metadata for a given datasource
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
		    ? manager.getDatasource( attributes.getAsKey( Key.datasource ) )
		    : manager.getDefaultDatasource();
		Query				result		= null;
		switch ( DBInfoType.fromString( attributes.getAsString( Key.type ) ) ) {
			// case COLUMNS :
			// result = getColumns( context, attributes );
			// case DBNAMES :
			// result = getDbNames( context );
			// case TABLES :
			// result = getTables( context );
			// case FOREIGNKEYS :
			// result = getForeignKeys( context, attributes );
			// case INDEX :
			// result = getIndex( context, attributes );
			// case PROCEDURES :
			// result = getProcedures( context );
			case VERSION :
				result = getVersion( datasource );
		}
		ExpressionInterpreter.setVariable( context, attributes.getAsString( Key._NAME ), result );
		// @TODO: Return null???
		return DEFAULT_RETURN;
	}

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

	private enum DBInfoType {

	    // COLUMNS,
	    // DBNAMES,
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
