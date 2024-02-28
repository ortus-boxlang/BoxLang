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

import java.sql.Types;

public enum QueryColumnType {

	INTEGER( Types.INTEGER ),
	BIGINT( Types.BIGINT ),
	DOUBLE( Types.DOUBLE ),
	DECIMAL( Types.DECIMAL ),
	VARCHAR( Types.VARCHAR ),
	BINARY( Types.BINARY ),
	BIT( Types.BIT ),
	TIME( Types.TIME ),
	DATE( Types.DATE ),
	TIMESTAMP( Types.TIMESTAMP ),
	OBJECT( Types.JAVA_OBJECT ),
	OTHER( Types.OTHER ),
	NULL( Types.NULL );

	public final int sqlType;

	QueryColumnType( int sqlType ) {
		this.sqlType = sqlType;
	}

	public static QueryColumnType fromString( String type ) {
		type = type.toLowerCase();
		// TODO: handle other types
		switch ( type ) {
			case "integer" :
				return INTEGER;
			case "bigint" :
				return BIGINT;
			case "double" :
			case "numeric" :
				return DOUBLE;
			case "decimal" :
				return DECIMAL;
			case "varchar" :
			case "string" :
				return VARCHAR;
			case "binary" :
				return BINARY;
			case "bit" :
				return BIT;
			case "time" :
				return TIME;
			case "date" :
				return DATE;
			case "timestamp" :
				return TIMESTAMP;
			case "object" :
				return OBJECT;
			case "other" :
				return OTHER;
			default :
				throw new RuntimeException( "Unknown QueryColumnType: " + type );
		}
	}

	/**
	 * Acquire a QueryColumnType from a SQL type. Useful for assembling Query objects from JDBC result sets.
	 *
	 * @param type The SQL type to convert.
	 *
	 * @return The QueryColumnType corresponding to the SQL type.
	 */
	public static QueryColumnType fromSQLType( int type ) {
		switch ( type ) {
			case Types.ARRAY :
				return OTHER;
			case Types.BIGINT :
				return BIGINT;
			case Types.BINARY :
				return BINARY;
			case Types.BIT :
				return BIT;
			case Types.BLOB :
				return OBJECT;
			case Types.BOOLEAN :
				return BIT;
			case Types.CHAR :
				return VARCHAR;
			case Types.CLOB :
				return OBJECT;
			case Types.DATALINK :
				return OTHER;
			case Types.DATE :
				return DATE;
			case Types.DECIMAL :
				return DECIMAL;
			case Types.DISTINCT :
				return OTHER;
			case Types.DOUBLE :
				return DOUBLE;
			case Types.FLOAT :
				return DOUBLE;
			case Types.INTEGER :
				return INTEGER;
			case Types.JAVA_OBJECT :
				return OBJECT;
			case Types.LONGNVARCHAR :
				return VARCHAR;
			case Types.LONGVARBINARY :
				return BINARY;
			case Types.LONGVARCHAR :
				return VARCHAR;
			case Types.NCHAR :
				return VARCHAR;
			case Types.NCLOB :
				return OBJECT;
			case Types.NULL :
				return OTHER;
			case Types.NUMERIC :
				return DOUBLE;
			case Types.NVARCHAR :
				return VARCHAR;
			case Types.OTHER :
				return OTHER;
			case Types.REAL :
				return DOUBLE;
			case Types.REF :
				return OTHER;
			case Types.REF_CURSOR :
				return OTHER;
			case Types.ROWID :
				return OTHER;
			case Types.SMALLINT :
				return INTEGER;
			case Types.SQLXML :
				return OTHER;
			case Types.STRUCT :
				return OTHER;
			case Types.TIME :
				return TIME;
			case Types.TIME_WITH_TIMEZONE :
				return TIME;
			case Types.TIMESTAMP :
				return TIMESTAMP;
			case Types.TIMESTAMP_WITH_TIMEZONE :
				return TIMESTAMP;
			case Types.TINYINT :
				return INTEGER;
			case Types.VARBINARY :
				return BINARY;
			case Types.VARCHAR :
				return VARCHAR;
			default :
				return OTHER;
		}
	}
}