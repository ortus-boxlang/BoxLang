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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.RegexBuilder;

/**
 * Represents a parameter to a SQL query created via QueryExecute or the Query component.
 */
public class QueryParameter {

	/**
	 * The parameter name.
	 */
	private final String			name;

	/**
	 * The parameter value.
	 */
	private final Object			value;

	/**
	 * The SQL type of the parameter. Defaults to `VARCHAR`.
	 */
	private final QueryColumnType	type;

	/**
	 * Unmodified, uncasted SQL type of the parameter as a string. (With the cf_sql_ prefix removed.)
	 */
	private final String			sqltype;

	/**
	 * The maximum length of the parameter. Defaults to `null`.
	 */
	private final Integer			maxLength;

	/**
	 * The scale of the parameter, used only on `double` and `decimal` types. Defaults to `null`.
	 */
	private final Integer			scale;

	/**
	 * If true, forces the parameter value to be `null`.
	 */
	private final boolean			isNullParam;

	/**
	 * If true, this is a list parameter and the `value` should be converted from a delimited list to an array.
	 */
	private final boolean			isListParam;

	/**
	 * Construct a new QueryParameter from a given IStruct of parameters.
	 * <p>
	 * The IStruct may contain the following properties:
	 * <ul>
	 * <li>`value` - The value of the parameter.</li>
	 * <li>`sqltype` - String SQL type of the parameter. Defaults to `VARCHAR`.</li>
	 * <li>`nulls` - Whether the parameter can be null. Defaults to `false`.</li>
	 * <li>`list` - Boolean indicating whether the parameter is a list. Defaults to `false`.</li>
	 * <li>`separator` - The separator for the list. Defaults to `,`.</li>
	 * <li>`maxLength` - The maximum length of the parameter. Defaults to `null`.</li>
	 * <li>`scale` - The scale of the parameter, used only on `double` and `decimal` types. Defaults to `null`.</li>
	 */
	private QueryParameter( String name, IStruct param ) {
		String sqltype = ( String ) param.getOrDefault( Key.sqltype, param.getOrDefault( Key.type, "VARCHAR" ) );
		// allow nulls and null
		this.isNullParam	= BooleanCaster.cast( param.getOrDefault( Key.nulls, param.getOrDefault( Key.nulls2, false ) ) );
		this.isListParam	= BooleanCaster.cast( param.getOrDefault( Key.list, false ) );
		String	separator	= StringCaster.cast( param.getOrDefault( Key.separator, "," ) );

		Object	v			= param.get( Key.value );
		if ( this.isListParam ) {
			if ( v instanceof Array ) {
				// do nothing?
			} else {
				v = ListUtil.asList( ( String ) v, ( String ) param.getOrDefault( Key.separator, separator ) );
			}
		}

		this.name		= name;
		this.value		= this.isNullParam ? null : v;
		this.sqltype	= RegexBuilder.of( sqltype, RegexBuilder.CF_SQL ).replaceAllAndGet( "" ).toUpperCase().trim();
		this.type		= QueryColumnType.fromString( this.sqltype );
		this.maxLength	= param.getAsInteger( Key.maxLength );
		this.scale		= param.getAsInteger( Key.scale );
	}

	/**
	 * Construct a new QueryParameter from a given value.
	 * <p>
	 * If the value is an IStruct, it will be used as the construction arguments to {@link QueryParameter#QueryParameter(IStruct)}. Otherwise, the QueryParameter will be constructed with the value as the `value` property of the IStruct, and no sqltype,
	 * null, list, or maxLength/scale properties.
	 */
	public static QueryParameter fromAny( Object value ) {
		return QueryParameter.fromAny( null, value );
	}

	/**
	 * Construct a new QueryParameter from a given name and value.
	 * <p>
	 * If the value is an IStruct, it will be used as the construction arguments to {@link QueryParameter#QueryParameter(IStruct)}. Otherwise, the QueryParameter will be constructed with the value as the `value`
	 * property of the IStruct, and no sqltype,
	 * null, list, or maxLength/scale properties.
	 * 
	 * @param name  The parameter name. Null is completely valid.
	 * @param value The parameter value.
	 */
	public static QueryParameter fromAny( String name, Object value ) {
		CastAttempt<IStruct> castAsStruct = StructCaster.attempt( value );
		if ( castAsStruct.wasSuccessful() ) {
			return new QueryParameter( name, castAsStruct.getOrFail() );
		}
		return new QueryParameter( name, Struct.of( "value", value ) );
	}

	/**
	 * Retrieve the parameter name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Is this a list parameter?
	 */
	public boolean isListParam() {
		return this.isListParam;
	}

	/**
	 * Is this parameter specifically typed as 'null'?
	 */
	public boolean isNullParam() {
		return this.isNullParam;
	}

	/**
	 * Retrieve the parameter value.
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Retrieve the value casted to the declared SQL type of the parameter..
	 */
	public Object toSQLType( IBoxContext context ) {
		if ( this.value == null ) {
			return null;
		}
		return QueryColumnType.toSQLType( this.type, this.value, context );
	}

	/**
	 * Retrieve the QueryColumnType of the parameter.
	 */
	public QueryColumnType getType() {
		return this.type;
	}

	/**
	 * Retrieve the SQL type as an ordinal from {@link java.sql.Types}.
	 * <p>
	 * For example, {@link java.sql.Types#VARCHAR} is 12.
	 */
	public int getSqlTypeAsInt() {
		return this.type.sqlType;
	}

	/**
	 * Retrieve the `scale` or `maxLength` of the parameter.
	 * <p>
	 * For {@link QueryColumnType#DOUBLE} and {@link QueryColumnType#DECIMAL}, this is the `scale`. For all other types, this is the `maxLength` property.
	 */
	public Integer getScaleOrLength() {
		return switch ( type ) {
			case DOUBLE, DECIMAL -> this.scale;
			default -> this.maxLength;
		};
	}

	/**
	 * Returns whether this parameter has a null value override.
	 * <p>
	 * For example:
	 * <code>&lt;bx:queryparam null="true" /&gt;</code>
	 */
	public boolean isNull() {
		return this.isNullParam;
	}

}
