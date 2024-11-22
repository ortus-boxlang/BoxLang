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
package ortus.boxlang.compiler.ast.sql.select.expression.operation;

public enum SQLBinaryOperator {

	PLUS,
	MINUS,
	MULTIPLY,
	DIVIDE,
	MODULO,
	EQUAL,
	NOTEQUAL,
	LESSTHAN,
	LESSTHANOREQUAL,
	GREATERTHAN,
	GREATERTHANOREQUAL,
	AND,
	OR;

	public String getSymbol() {
		switch ( this ) {
			case PLUS :
				return "+";
			case MINUS :
				return "-";
			case MULTIPLY :
				return "*";
			case DIVIDE :
				return "/";
			case MODULO :
				return "%";
			case EQUAL :
				return "=";
			case NOTEQUAL :
				return "!=";
			case LESSTHAN :
				return "<";
			case LESSTHANOREQUAL :
				return "<=";
			case GREATERTHAN :
				return ">";
			case GREATERTHANOREQUAL :
				return ">=";
			case AND :
				return "AND";
			case OR :
				return "OR";
			default :
				return "";
		}
	}

}
