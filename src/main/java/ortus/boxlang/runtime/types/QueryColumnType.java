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

	INTEGER,
	BIGINT,
	DOUBLE,
	DECIMAL,
	VARCHAR,
	BINARY,
	BIT,
	TIME,
	DATE,
	TIMESTAMP,
	OBJECT;

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
				return OBJECT;
			case Types.BIGINT :
				return BIGINT;
			case Types.BINARY :
				return BINARY;
			case Types.BIT :
				return BIT;
			case Types.BLOB :
				return OBJECT;
			case Types.CLOB :
				return OBJECT;
			case Types.NCLOB :
				return OBJECT;
			case Types.DATALINK :
				return OBJECT;
			case Types.DATE :
				return DATE;
			case Types.DECIMAL :
				return DECIMAL;
			case Types.DISTINCT :
				return OBJECT;
			case Types.DOUBLE :
			case Types.NUMERIC :
				return DOUBLE;
			case Types.FLOAT :
				return DOUBLE;
			case Types.INTEGER :
				return INTEGER;
			case Types.JAVA_OBJECT :
				return OBJECT;
			case Types.NULL :
				return OBJECT;
			case Types.OTHER :
				return OBJECT;
			case Types.REF :
				return OBJECT;
			case Types.STRUCT :
				return OBJECT;
			case Types.TIME :
				return TIME;
			case Types.TIMESTAMP :
				return TIMESTAMP;
			case Types.VARCHAR :
				return VARCHAR;
			// @TODO: Implement these return types.
			// case Types.BOOLEAN:
			// return BOOLEAN;
			// case Types.CHAR:
			// return CHAR;
			// case Types.NCHAR:
			// return NCHAR;
			// case Types.LONGVARBINARY:
			// return LONGVARBINARY;
			// case Types.LONGVARCHAR:
			// return LONGVARCHAR;
			// case Types.REAL:
			// return REAL;
			// case Types.TINYINT:
			// return TINYINT;
			// case Types.VARBINARY:
			// return VARBINARY;
			// case Types.NVARCHAR:
			// return NVARCHAR;
			// case Types.SQLXML:
			// return SQLXML;
			// case Types.SMALLINT:
			// return SMALLINT;
			default :
				return VARCHAR;
		}
	}
}