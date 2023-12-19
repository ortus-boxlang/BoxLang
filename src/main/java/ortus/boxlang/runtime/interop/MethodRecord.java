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
package ortus.boxlang.runtime.interop;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * This immutable record represents an executable method handle and it's metadata.
 * This record is the one that is cached in the {@link DynamicObject#methodCache} map.
 *
 * @param methodName    The name of the method
 * @param method        The method representation
 * @param methodHandle  The method handle to use for invocation
 * @param isStatic      Whether the method is static or not
 * @param argumentCount The number of arguments the method takes
 */
public record MethodRecord(
    String methodName,
    Method method,
    MethodHandle methodHandle,
    boolean isStatic,
    int argumentCount ) {
    // A beautiful java record of our method handle
}
