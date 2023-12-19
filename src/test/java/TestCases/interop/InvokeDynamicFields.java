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
package TestCases.interop;

public class InvokeDynamicFields {

	public static final String	HELLO			= "Hello World";
	public static final int		MY_PRIMITIVE	= 42;

	public String				name			= "luis";

	public InvokeDynamicFields() {
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName( String name ) {
		this.name = name;
	}

	public Boolean hasName() {
		return this.name != null;
	}

	public String hello() {
		return "Hello";
	}

	public String hello( String name ) {
		return "Hello " + name;
	}

	public String hello( String name, int test ) {
		return "Hello " + name + test;
	}

	public Long getNow() {
		return System.currentTimeMillis();
	}

}
