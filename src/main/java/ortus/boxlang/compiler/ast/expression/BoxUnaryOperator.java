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

public enum BoxUnaryOperator {

	Plus,
	Minus,
	PrePlusPlus,
	PreMinusMinus,
	PostPlusPlus,
	PostMinusMinus,
	Not,
	BitwiseComplement;

	public String getSymbol() {
		switch ( this ) {
			case Plus :
				return "+";
			case Minus :
				return "-";
			case PrePlusPlus :
				return "++";
			case PreMinusMinus :
				return "--";
			case PostPlusPlus :
				return "++";
			case PostMinusMinus :
				return "--";
			case Not :
				return "!";
			case BitwiseComplement :
				return "b~";
			default :
				return "";
		}
	}

	public Boolean isPre() {
		switch ( this ) {
			case Plus :
				return true;
			case Minus :
				return true;
			case PrePlusPlus :
				return true;
			case PreMinusMinus :
				return true;
			case PostPlusPlus :
				return false;
			case PostMinusMinus :
				return false;
			case Not :
				return true;
			case BitwiseComplement :
				return true;
			default :
				return true;
		}
	}
}
