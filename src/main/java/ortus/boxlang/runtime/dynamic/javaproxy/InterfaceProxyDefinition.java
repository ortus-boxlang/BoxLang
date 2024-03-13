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
package ortus.boxlang.runtime.dynamic.javaproxy;

import java.lang.reflect.Method;
import java.util.List;

/**
 * I represent the definition of an interface proxy
 * 
 * @param name       The name of the proxy (MD5 hash of the interfaces)
 * @param methods    The methods that the proxy should implement
 * @param interfaces The interfaces names for debugging
 */
public record InterfaceProxyDefinition( String name, List<Method> methods, List<String> interfaces ) {

}