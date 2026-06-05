
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
import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class QueryColumnTypeTest {
	;

	@DisplayName( "Test fromString() constructor against all known cf_sql parameter types" )
	@Test
	void testFromStringConstructor() {
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "ARRAY" ) );
		assertEquals( QueryColumnType.BIGINT, QueryColumnType.fromString( "BIGINT" ) );
		assertEquals( QueryColumnType.BINARY, QueryColumnType.fromString( "BINARY" ) );
		assertEquals( QueryColumnType.BIT, QueryColumnType.fromString( "BIT" ) );
		assertEquals( QueryColumnType.BLOB, QueryColumnType.fromString( "BLOB" ) );
		assertEquals( QueryColumnType.CHAR, QueryColumnType.fromString( "CHAR" ) );
		assertEquals( QueryColumnType.CLOB, QueryColumnType.fromString( "CLOB" ) );
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
		assertEquals( QueryColumnType.CLOB, QueryColumnType.fromString( "NCLOB" ) );
		assertEquals( QueryColumnType.NULL, QueryColumnType.fromString( "NULL" ) );
		assertEquals( QueryColumnType.DOUBLE, QueryColumnType.fromString( "NUMERIC" ) );
		assertEquals( QueryColumnType.VARCHAR, QueryColumnType.fromString( "NVARCHAR" ) );
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "OTHER" ) );
		assertEquals( QueryColumnType.DOUBLE, QueryColumnType.fromString( "REAL" ) );
		assertEquals( QueryColumnType.REFCURSOR, QueryColumnType.fromString( "REFCURSOR" ) );
		assertEquals( QueryColumnType.INTEGER, QueryColumnType.fromString( "SMALLINT" ) );
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "STRUCT" ) );
		assertEquals( QueryColumnType.OTHER, QueryColumnType.fromString( "SQLXML" ) );
		assertEquals( QueryColumnType.TIME, QueryColumnType.fromString( "TIME" ) );
		assertEquals( QueryColumnType.TIMESTAMP, QueryColumnType.fromString( "TIMESTAMP" ) );
		assertEquals( QueryColumnType.INTEGER, QueryColumnType.fromString( "TINYINT" ) );
		assertEquals( QueryColumnType.BINARY, QueryColumnType.fromString( "VARBINARY" ) );
		assertEquals( QueryColumnType.VARCHAR, QueryColumnType.fromString( "VARCHAR" ) );
	}

	@DisplayName( "toSQLType() returns null for empty arrays" )
	@Test
	void testToSQLTypeEmptyArray() {
		Array emptyArray = new Array();

		// Empty arrays should return null for all SQL types
		assertNull( QueryColumnType.toSQLType( QueryColumnType.INTEGER, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.BIGINT, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.DOUBLE, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.DECIMAL, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.CHAR, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.VARCHAR, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.BOOLEAN, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.DATE, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.TIME, emptyArray, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.TIMESTAMP, emptyArray, null, null ) );
	}

	@DisplayName( "toSQLType() returns null for null values" )
	@Test
	void testToSQLTypeNull() {
		// Null values should return null for all SQL types
		assertNull( QueryColumnType.toSQLType( QueryColumnType.INTEGER, null, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.VARCHAR, null, null, null ) );
		assertNull( QueryColumnType.toSQLType( QueryColumnType.BOOLEAN, null, null, null ) );
	}

	@DisplayName( "toSQLType() handles non-empty values correctly" )
	@Test
	void testToSQLTypeNonEmptyValues() {
		// Non-empty values should still work correctly
		assertEquals( 42, QueryColumnType.toSQLType( QueryColumnType.INTEGER, 42, null, null ) );
		assertEquals( "test", QueryColumnType.toSQLType( QueryColumnType.VARCHAR, "test", null, null ) );
		assertEquals( true, QueryColumnType.toSQLType( QueryColumnType.BOOLEAN, true, null, null ) );
	}

}
