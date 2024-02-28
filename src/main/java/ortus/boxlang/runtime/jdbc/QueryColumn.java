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

import java.sql.Types;

import ortus.boxlang.runtime.types.QueryColumnType;

public class QueryColumn {

	public String	name;
	public int		type;

	public QueryColumn( String name, int type ) {
		this.name	= name;
		this.type	= type;
	}

	public QueryColumnType getTypeAsQueryColumnType() {
		switch ( this.type ) {
			case Types.ARRAY :
				return QueryColumnType.OTHER;
			case Types.BIGINT :
				return QueryColumnType.BIGINT;
			case Types.BINARY :
				return QueryColumnType.BINARY;
			case Types.BIT :
				return QueryColumnType.BIT;
			case Types.BLOB :
				return QueryColumnType.OBJECT;
			case Types.BOOLEAN :
				return QueryColumnType.BIT;
			case Types.CHAR :
				return QueryColumnType.VARCHAR;
			case Types.CLOB :
				return QueryColumnType.OBJECT;
			case Types.DATALINK :
				return QueryColumnType.OTHER;
			case Types.DATE :
				return QueryColumnType.DATE;
			case Types.DECIMAL :
				return QueryColumnType.DECIMAL;
			case Types.DISTINCT :
				return QueryColumnType.OTHER;
			case Types.DOUBLE :
				return QueryColumnType.DOUBLE;
			case Types.FLOAT :
				return QueryColumnType.DOUBLE;
			case Types.INTEGER :
				return QueryColumnType.INTEGER;
			case Types.JAVA_OBJECT :
				return QueryColumnType.OBJECT;
			case Types.LONGNVARCHAR :
				return QueryColumnType.VARCHAR;
			case Types.LONGVARBINARY :
				return QueryColumnType.BINARY;
			case Types.LONGVARCHAR :
				return QueryColumnType.VARCHAR;
			case Types.NCHAR :
				return QueryColumnType.VARCHAR;
			case Types.NCLOB :
				return QueryColumnType.OBJECT;
			case Types.NULL :
				return QueryColumnType.OTHER;
			case Types.NUMERIC :
				return QueryColumnType.DOUBLE;
			case Types.NVARCHAR :
				return QueryColumnType.VARCHAR;
			case Types.OTHER :
				return QueryColumnType.OTHER;
			case Types.REAL :
				return QueryColumnType.DOUBLE;
			case Types.REF :
				return QueryColumnType.OTHER;
			case Types.REF_CURSOR :
				return QueryColumnType.OTHER;
			case Types.ROWID :
				return QueryColumnType.OTHER;
			case Types.SMALLINT :
				return QueryColumnType.INTEGER;
			case Types.SQLXML :
				return QueryColumnType.OTHER;
			case Types.STRUCT :
				return QueryColumnType.OTHER;
			case Types.TIME :
				return QueryColumnType.TIME;
			case Types.TIME_WITH_TIMEZONE :
				return QueryColumnType.TIME;
			case Types.TIMESTAMP :
				return QueryColumnType.TIMESTAMP;
			case Types.TIMESTAMP_WITH_TIMEZONE :
				return QueryColumnType.TIMESTAMP;
			case Types.TINYINT :
				return QueryColumnType.INTEGER;
			case Types.VARBINARY :
				return QueryColumnType.BINARY;
			case Types.VARCHAR :
				return QueryColumnType.VARCHAR;
			default :
				return QueryColumnType.OTHER;
		}
	}

}
