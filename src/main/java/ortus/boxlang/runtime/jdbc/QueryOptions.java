package ortus.boxlang.runtime.jdbc;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import javax.annotation.Nullable;
import java.sql.Connection;

public class QueryOptions {

	private static final DataSourceManager	manager	= DataSourceManager.getInstance();

	private DataSource						datasource;
	private IStruct							options;
	private @Nullable String				resultVariableName;
	private String							returnType;
	private String							columnKey;
	private String							username;
	private String							password;
	private Integer							queryTimeout;
	private Long							maxRows;

	public QueryOptions( IStruct options ) {
		this.options = options;
		determineDataSource();
		determineReturnType();
		this.resultVariableName	= options.getAsString( Key.result );
		this.username			= options.getAsString( Key.username );
		this.password			= options.getAsString( Key.password );
		this.queryTimeout		= options.getAsInteger( Key.queryTimeout );
		this.maxRows			= options.getAsLong( Key.maxRows );
		if ( this.maxRows == null ) {
			this.maxRows = -1L;
		}
	}

	public DataSource getDataSource() {
		return this.datasource;
	}

	public Connection getConnnection() {
		if ( wantsUsernameAndPassword() ) {
			return getDataSource().getConnection( getUsername(), getPassword() );
		} else {
			return getDataSource().getConnection();
		}
	}

	private boolean wantsUsernameAndPassword() {
		return this.username != null;
	}

	private String getUsername() {
		return this.username;
	}

	private String getPassword() {
		return this.password;
	}

	public boolean wantsResultStruct() {
		return this.resultVariableName != null;
	}

	public @Nullable String getResultVariableName() {
		return this.resultVariableName;
	}

	public Object castAsReturnType( ExecutedQuery query ) {
		return switch ( this.returnType ) {
			case "query" -> query.getResults();
			case "array" -> query.getResultsAsArray();
			case "struct" -> query.getResultsAsStruct( this.columnKey );
			default -> throw new BoxRuntimeException( "Unknown return type: " + returnType );
		};
	}

	private void determineDataSource() {
		if ( this.options.containsKey( "datasource" ) ) {
			Object					datasourceObject	= this.options.get( Key.datasource );
			CastAttempt<IStruct>	datasourceAsStruct	= StructCaster.attempt( datasourceObject );
			if ( datasourceAsStruct.wasSuccessful() ) {
				this.datasource = DataSource.fromDataSourceStruct( datasourceAsStruct.getOrFail() );
			} else {
				CastAttempt<String>	datasourceAsString	= StringCaster.attempt( datasourceObject );
				String				datasourceName		= datasourceAsString.getOrFail();
				this.datasource = manager.getDataSource( Key.of( datasourceName ) );
				if ( this.datasource == null ) {
					throw new BoxRuntimeException( "No [" + datasourceName + "] datasource defined." );
				}
			}
		} else {
			this.datasource = manager.getDefaultDataSource();
			if ( this.datasource == null ) {
				throw new BoxRuntimeException(
				    "No default datasource has been defined. Either register a default datasource or provide a datasource name in the query options." );
			}
		}
	}

	private void determineReturnType() {
		Object				returnTypeObject	= options.get( Key.returnType );
		CastAttempt<String>	returnTypeAsString	= StringCaster.attempt( returnTypeObject );
		String				returnTypeString	= returnTypeAsString.getOrDefault( "query" );

		switch ( returnTypeString ) {
			case "query", "array" -> this.returnType = returnTypeString;
			case "struct" -> {
				this.columnKey = options.getAsString( Key.columnKey );
				if ( this.columnKey == null ) {
					throw new BoxRuntimeException( "You must defined a `columnKey` option when using `returnType: struct`." );
				}
				this.returnType = "struct";
			}
			default -> throw new BoxRuntimeException( "Unknown return type: " + returnTypeString );
		}
	}

	public Integer getQueryTimeout() {
		return queryTimeout;
	}

	public Long getMaxRows() {
		return maxRows;
	}
}
