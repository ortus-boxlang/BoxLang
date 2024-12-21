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

/**
 * Represents a column type in a Query object.
 */
public enum QueryColumnType {

	BIGINT( Types.BIGINT ),
	BINARY( Types.BINARY ),
	BOOLEAN( Types.BOOLEAN ),
	BIT( Types.BIT ),
	CHAR( Types.CHAR ),
	DATE( Types.DATE ),
	DECIMAL( Types.DECIMAL ),
	DOUBLE( Types.DOUBLE ),
	INTEGER( Types.INTEGER ),
	NULL( Types.NULL ),
	OBJECT( Types.JAVA_OBJECT ),
	OTHER( Types.OTHER ),
	TIME( Types.TIME ),
	TIMESTAMP( Types.TIMESTAMP ),
	VARCHAR( Types.VARCHAR );

	public final int sqlType;

	QueryColumnType( int sqlType ) {
		this.sqlType = sqlType;
	}

	/**
	 * Create a new QueryColumnType from a string value.
	 */
	public static QueryColumnType fromString( String type ) {
		type = type.toLowerCase();

		switch ( type ) {
			case "array" :
			case "refcursor" :
			case "struct" :
			case "sqlxml" :
				return OTHER;
			case "bigint" :
				return BIGINT;
			case "binary" :
			case "varbinary" :
			case "longvarbinary" :
				return BINARY;
			case "blob" :
			case "clob" :
			case "nclob" :
				return OBJECT;
			case "bit" :
				return BIT;
			case "boolean" :
			case "bool" :
				return BOOLEAN;
			case "nchar" :
			case "char" :
				return CHAR;
			case "date" :
				return DATE;
			case "distinct" :
				return OTHER;
			case "decimal" :
				return DECIMAL;
			case "real" :
			case "money" :
			case "money4" :
			case "float" :
			case "double" :
			case "numeric" :
			case "number" :
				return DOUBLE;
			case "idstamp" :
				return CHAR;
			case "tinyint" :
			case "smallint" :
			case "integer" :
			case "int" :
				return INTEGER;
			case "nvarchar" :
			case "longvarchar" :
			case "longnvarchar" :
				return VARCHAR;
			case "object" :
				return OBJECT;
			case "other" :
				return OTHER;
			case "time" :
				return TIME;
			case "timestamp" :
				return TIMESTAMP;
			case "varchar" :
			case "string" :
				return VARCHAR;
			case "null" :
				return NULL;
			default :
				throw new IllegalArgumentException( "Unknown QueryColumnType: " + type );
		}
	}

	/**
	 * Retrieve this QueryColumnType as a string value.
	 */
	public String toString() {
		switch ( this ) {
			case INTEGER :
				return "integer";
			case BIGINT :
				return "bigint";
			case DOUBLE :
				return "numeric";
			case DECIMAL :
				return "decimal";
			case CHAR :
				return "string";
			case VARCHAR :
				return "string";
			case BINARY :
				return "binary";
			case BIT :
				return "bit";
			case TIME :
				return "time";
			case DATE :
				return "date";
			case TIMESTAMP :
				return "timestamp";
			case OBJECT :
				return "object";
			case OTHER :
				return "other";
			case NULL :
				return "null";
			case BOOLEAN :
				return "boolean";
			default :
				throw new IllegalArgumentException( "Unknown QueryColumnType: " + this );
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
				return BOOLEAN;
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
				return NULL;
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

	/**
	 * Does this represent a type that can be treated as a string?
	 * 
	 * @param type The type to check.
	 * 
	 * @return true if the type can be treated as a string.
	 */
	public static boolean isStringType( QueryColumnType type ) {
		return type == VARCHAR || type == CHAR || type == TIME;
	}
}
