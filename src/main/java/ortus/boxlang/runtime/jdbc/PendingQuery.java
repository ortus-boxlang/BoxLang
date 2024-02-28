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
package ortus.boxlang.runtime.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.util.ListUtil;

public class PendingQuery {

	public String				sql;
	public String				originalSql;
	public List<QueryParameter>	parameters;

	public PendingQuery( String sql, List<QueryParameter> parameters, String originalSql ) {
		this.sql			= sql;
		this.originalSql	= originalSql;
		this.parameters		= parameters;
	}

	public PendingQuery( String sql, List<QueryParameter> parameters ) {
		this( sql, parameters, sql );
	}

	public PendingQuery( String sql, Array parameters ) {
		this( sql, parameters.stream().map( QueryParameter::new ).collect( Collectors.toList() ) );
	}

	public static PendingQuery fromStructParameters( String sql, IStruct parameters ) {
		List<QueryParameter>	params	= new ArrayList<>();
		Pattern					pattern	= Pattern.compile( ":\\w+" );
		Matcher					matcher	= pattern.matcher( sql );
		while ( matcher.find() ) {
			String paramName = matcher.group();
			paramName = paramName.substring( 1 );
			Object paramValue = parameters.get( paramName );
			if ( paramValue == null ) {
				throw new BoxRuntimeException( "Missing param in query: [" + paramName + "]. SQL: " + sql );
			}
			params.add( new QueryParameter( paramValue ) );
		}
		return new PendingQuery( matcher.replaceAll( "?" ), params, sql );
	}

	public List<Object> getParameterValues() {
		return this.parameters.stream().map( QueryParameter::getValue ).collect( Collectors.toList() );
	}

	public ExecutedQuery execute( Connection conn ) {
		try ( PreparedStatement statement = conn.prepareStatement( this.sql, Statement.RETURN_GENERATED_KEYS ) ) {
			// The param index starts from 1
			for ( int i = 1; i <= this.parameters.size(); i++ ) {
				QueryParameter	param			= this.parameters.get( i - 1 );
				Integer			scaleOrLength	= param.getScaleOrLength();
				if ( scaleOrLength == null ) {
					statement.setObject( i, param.getValue(), param.getSqlTypeAsInt() );
				} else {
					statement.setObject( i, param.getValue(), param.getSqlTypeAsInt(), scaleOrLength );
				}
			}
			long	startTick	= System.currentTimeMillis();
			boolean	hasResults	= statement.execute();
			long	endTick		= System.currentTimeMillis();
			return new ExecutedQuery(
			    this,
			    statement,
			    endTick - startTick,
			    hasResults
			);
		} catch ( SQLException e ) {
			throw new DatabaseException(
			    e.getMessage(),
			    "detail message",
			    String.valueOf( e.getErrorCode() ),
			    e.getSQLState(),
			    originalSql,
			    null, // queryError
			    ListUtil.asString( Array.fromList( this.getParameterValues() ), "," ), // where
			    e
			);
		}
	}

}
