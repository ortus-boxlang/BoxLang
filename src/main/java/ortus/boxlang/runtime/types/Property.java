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
package ortus.boxlang.runtime.types;

import ortus.boxlang.runtime.scopes.Key;

/**
 * Represents a class property
 *
 * @param name          The name of the property
 * @param type          The type of the argument
 * @param defaultValue  The default value of the argument
 * @param annotations   Annotations for the property
 * @param documentation Documentation for the property
 *
 */
public record Property( Key name, String type, Object defaultValue, Struct annotations, Struct documentation ) {

}
