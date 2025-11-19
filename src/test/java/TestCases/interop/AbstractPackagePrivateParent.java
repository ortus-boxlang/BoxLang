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

/**
 * Package-private abstract class with public static fields.
 * This simulates the real-world scenario where a package-private parent class
 * contains public static constants that need to be accessible through a public subclass.
 */
abstract class AbstractPackagePrivateParent {

	public static final String	PARENT_CONSTANT_STRING	= "ParentValue";
	public static final int		PARENT_CONSTANT_INT		= 42;
	public static final boolean	PARENT_CONSTANT_BOOLEAN	= true;

	protected String			protectedField			= "protected";

	public abstract String getType();
}
