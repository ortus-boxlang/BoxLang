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
package TestCases.phase3;

/**
 * A test target class with overloaded methods for validating that BoxLang
 * generates correct Java method stubs when extending a Java class.
 */
public class JavaOverloadTarget {

	public String doSomething( String s ) {
		return "string:" + s;
	}

	public String doSomething( int n ) {
		return "int:" + n;
	}

	public String doSomething( String s, int n ) {
		return "both:" + s + ":" + n;
	}

	public int getCount() {
		return 42;
	}

	public String getName() {
		return "base";
	}

	/**
	 * This method should NOT be overridden — used to verify we only generate stubs
	 * for methods that have a matching UDF.
	 */
	public String untouched() {
		return "original";
	}

	/**
	 * Returns a boolean primitive — used to test that the fallback annotation path
	 * preserves lowercase primitive types.
	 */
	public boolean isActive() {
		return false;
	}
}
