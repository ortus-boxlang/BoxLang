
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

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class QueryColumnTypeTest {;


	@DisplayName( "Test fromString() constructor against all known cf_sql parameter types" )
	@Test
	void testFromStringConstructor() {
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "ARRAY" ) );
		assertEquals( QueryColumnType.BIGINT, QueryColumnType.fromString( "BIGINT" ) );
		assertEquals( QueryColumnType.BINARY, QueryColumnType.fromString( "BINARY" ) );
		assertEquals( QueryColumnType.BIT, QueryColumnType.fromString( "BIT" ) );
		assertEquals( QueryColumnType.OBJECT, QueryColumnType.fromString( "BLOB" ) );
		assertEquals( QueryColumnType.CHAR, QueryColumnType.fromString( "CHAR" ) );
		assertEquals( QueryColumnType.OBJECT, QueryColumnType.fromString( "CLOB" ) );
		assertEquals( QueryColumnType.DATE, QueryColumnType.fromString( "DATE" ) );
		assertEquals( QueryColumnType.DECIMAL, QueryColumnType.fromString( "DECIMAL" ) );
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "DISTINCT" ) );
		assertEquals( QueryColumnType.DOUBLE, QueryColumnType.fromString( "DOUBLE" ) );
		assertEquals( QueryColumnType.DOUBLE, QueryColumnType.fromString( "FLOAT" ) );
		assertEquals( QueryColumnType.CHAR, QueryColumnType.fromString( "IDSTAMP" ) );
		assertEquals( QueryColumnType.INTEGER, QueryColumnType.fromString( "INTEGER" ) );
		assertEquals( QueryColumnType.BINARY, QueryColumnType.fromString( "LONGVARBINARY" ) );
		assertEquals( QueryColumnType.VARCHAR, QueryColumnType.fromString( "LONGNVARCHAR" ) );
		assertEquals( QueryColumnType.VARCHAR, QueryColumnType.fromString( "LONGVARCHAR" ) );
		assertEquals( QueryColumnType.DOUBLE, QueryColumnType.fromString( "MONEY" ) );
		assertEquals( QueryColumnType.DOUBLE, QueryColumnType.fromString( "MONEY4" ) );
		assertEquals( QueryColumnType.CHAR, QueryColumnType.fromString( "NCHAR" ) );
		assertEquals( QueryColumnType.OBJECT, QueryColumnType.fromString( "NCLOB" ) );
		assertEquals( QueryColumnType.NULL, QueryColumnType.fromString( "NULL" ) );
		assertEquals( QueryColumnType.DOUBLE, QueryColumnType.fromString( "NUMERIC" ) );
		assertEquals( QueryColumnType.VARCHAR, QueryColumnType.fromString( "NVARCHAR" ) );
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "OTHER" ) );
		assertEquals( QueryColumnType.DOUBLE, QueryColumnType.fromString( "REAL" ) );
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "REFCURSOR" ) );
		assertEquals( QueryColumnType.INTEGER, QueryColumnType.fromString( "SMALLINT" ) );
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "STRUCT" ) );
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "SQLXML" ) );
		assertEquals( QueryColumnType.TIME, QueryColumnType.fromString( "TIME" ) );
		assertEquals( QueryColumnType.TIMESTAMP, QueryColumnType.fromString( "TIMESTAMP" ) );
		assertEquals( QueryColumnType.INTEGER, QueryColumnType.fromString( "TINYINT" ) );
		assertEquals( QueryColumnType.BINARY, QueryColumnType.fromString( "VARBINARY" ) );
		assertEquals( QueryColumnType.VARCHAR, QueryColumnType.fromString( "VARCHAR" ) );
	}

}
