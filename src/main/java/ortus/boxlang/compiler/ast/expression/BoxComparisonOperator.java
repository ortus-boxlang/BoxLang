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
package ortus.boxlang.compiler.ast.expression;

public enum BoxComparisonOperator {

	Equal,
	GreaterThan,
	GreaterThanEquals,
	LessThan,
	LessThanEquals,
	NotEqual,
	Contains,
	TEqual;

	public String getSymbol() {
		switch ( this ) {
			case Equal :
				return "==";
			case GreaterThan :
				return ">";
			case GreaterThanEquals :
				return ">=";
			case LessThan :
				return "<";
			case LessThanEquals :
				return "<=";
			case NotEqual :
				return "!=";
			case Contains :
				return "contains";
			case TEqual :
				return "===";
			default :
				return "";
		}
	}
}
