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

public enum BoxBinaryOperator {

	Contains,
	CastAs,

	NotContains,
	Plus,
	Minus,
	Star,
	Slash,
	Power,
	Backslash,
	Xor,
	Mod,
	InstanceOf,
	Elvis,
	And,
	Or,
	Equivalence,
	Implies,

	BitwiseAnd,
	BitwiseOr,
	BitwiseXor,
	BitwiseSignedLeftShift,
	BitwiseSignedRightShift,
	BitwiseUnsignedRightShift;

	public String getSymbol() {
		switch ( this ) {
			case Contains :
				return "contains";
			case CastAs :
				return "castAs";
			case NotContains :
				return "not contains";
			case Plus :
				return "+";
			case Minus :
				return "-";
			case Star :
				return "*";
			case Slash :
				return "/";
			case Power :
				return "^";
			case Backslash :
				return "\\";
			case Xor :
				return "xor";
			case Mod :
				return "%";
			case InstanceOf :
				return "instanceOf";
			case Elvis :
				return "?:";
			case And :
				return "&&";
			case Or :
				return "||";
			case Equivalence :
				return "==";
			case Implies :
				return "imp";
			case BitwiseAnd :
				return "b&";
			case BitwiseOr :
				return "b|";
			case BitwiseXor :
				return "b^";
			case BitwiseSignedLeftShift :
				return "b<<";
			case BitwiseSignedRightShift :
				return "b>>";
			case BitwiseUnsignedRightShift :
				return "b>>>";
			default :
				return "";
		}
	}
}
