package ortus.boxlang.runtime.interop;

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

import java.math.BigDecimal;

/**
 * For the life of me, I can't find a JDK class that has overloaded methods which accept the same number of arguments which can both be casted to the same type
 * whose result is different based on choosing the correct overloaded method based on the incoming type. I'm creating this just for the test. *
 */
public class TestClassOverloadedMethods {

	static public String go( char p ) {
		return "char";
	}

	static public String go( String p ) {
		return "String";
	}

	static public String go( Integer p ) {
		return "Integer boxed";
	}

	static public String go( long p ) {
		return "long";
	}

	static public String go( BigDecimal p ) {
		return "BigDecimal";
	}

}
