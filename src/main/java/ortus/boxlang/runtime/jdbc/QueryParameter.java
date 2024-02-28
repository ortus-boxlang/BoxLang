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

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.ListUtil;

public class QueryParameter {

	private final Object			value;
	private final QueryColumnType	type;
	private final Integer			maxLength;
	private final Integer			scale;

	public QueryParameter( IStruct param ) {
		String sqltype = ( String ) param.getOrDefault( Key.cfsqltype, "VARCHAR" );
		if ( ( Boolean ) param.getOrDefault( Key.nulls, false ) ) {
			sqltype = "NULL";
		}

		Object v = param.get( Key.value );
		if ( ( Boolean ) param.getOrDefault( Key.list, false ) ) {
			v		= ListUtil.asList( ( String ) v, ( String ) param.getOrDefault( Key.separator, "," ) );
			sqltype	= "ARRAY";
		}

		this.value		= v;
		this.type		= QueryColumnType.fromString( sqltype.replace( "CF_SQL_", "" ) );
		this.maxLength	= param.getAsInteger( Key.maxLength );
		this.scale		= param.getAsInteger( Key.scale );
	}

	public QueryParameter( Object value ) {
		this( Struct.of( "value", value ) );
	}

	public Object getValue() {
		return value;
	}

	public int getSqlTypeAsInt() {
		return this.type.sqlType;
	}

	public Integer getScaleOrLength() {
		return switch ( type ) {
			case DOUBLE, DECIMAL -> this.scale;
			default -> this.maxLength;
		};
	}

}
