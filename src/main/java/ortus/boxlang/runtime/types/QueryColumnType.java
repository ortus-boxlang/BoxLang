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
	OBJECT,
	OTHER;

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
}