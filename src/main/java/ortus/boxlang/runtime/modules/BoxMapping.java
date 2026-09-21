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
package ortus.boxlang.runtime.modules;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to define a module mapping within a {@link BoxModule} declaration.
 *
 * Supports two forms:
 * <ul>
 * <li><strong>String shorthand</strong> — use {@code @BoxMapping("myMapping")} for simple
 * name overrides.</li>
 * <li><strong>Struct form</strong> — use {@code @BoxMapping(name = "custom", external = true)}
 * for full mapping configuration.</li>
 * </ul>
 *
 * When used as a {@link BoxModule#mapping() module mapping}, the {@link #value()} shorthand
 * sets the mapping name. When used as a {@link BoxModule#publicMapping() public mapping},
 * the shorthand sets the public folder name relative to the module root.
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.15.0
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( {} )
public @interface BoxMapping {

	/** Shorthand string form (e.g. {@code @BoxMapping("myMapping")}). */
	String value() default "";

	/** Mapping name (struct form). */
	String name() default "";

	/** Relative path from the module root (struct form). */
	String path() default "";

	/** Whether to prepend the {@code /bxModules/} prefix. */
	boolean usePrefix() default true;

	/** Whether the mapping is publicly visible. */
	boolean external() default false;

}
