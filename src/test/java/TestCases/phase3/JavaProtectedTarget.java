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
 * A target class with a protected method, for validating that a BoxLang UDF can override
 * a protected Java method (the SPI hook pattern, e.g. JGit's JschConfigSessionFactory#configure).
 */
public class JavaProtectedTarget {

	/**
	 * Protected hook, overriding it from a BoxLang subclass must work
	 */
	protected String greet() {
		return "hello from parent";
	}

	/**
	 * Public entry point which calls the protected hook, mirroring how Java libraries
	 * invoke their overridable protected methods.
	 */
	public String callGreet() {
		return greet();
	}

}
