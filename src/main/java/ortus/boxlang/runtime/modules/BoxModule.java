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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to declare module metadata on a Java {@link IModuleConfig} implementation.
 *
 * All members are optional; defaults mirror the standard BoxLang module conventions.
 *
 * <pre>{@code
 * &#64;BoxModule( version = "2.5.0", author = "Ortus Solutions", dependencies = { "bx-derby",
 *     "bx-redis" }, mapping = @BoxMapping( "myMapping" ), publicMapping = @BoxMapping( "www" ) )
 * public class MyModule implements IModuleConfig {
 * }
 * }</pre>
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.15.0
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
public @interface BoxModule {

	/**
	 * The module name. Consulted for jar-based Java modules, where the convention default is the
	 * jar file's base name. For folder-based modules the folder name (or {@code box.json}'s
	 * {@code boxlang.moduleName}) always wins.
	 */
	String name() default "";

	/** The version of the module. */
	String version() default "1.0.0";

	/** The author of the module. */
	String author() default "";

	/** A description of the module. */
	String description() default "";

	/** The web URL of the module. */
	String webURL() default "";

	/** Whether the module is enabled for activation. */
	boolean enabled() default true;

	/** Module activation dependencies (module names). */
	String[] dependencies() default {};

	/** The module mapping. Use {@code @BoxMapping("name")} for a string shorthand, or {@code @BoxMapping(name = "...", ...)} for full configuration. */
	BoxMapping mapping() default @BoxMapping;

	/** The public convention mapping. Use {@code @BoxMapping("www")} for a string shorthand, or {@code @BoxMapping(name = "...", external = true)} for full configuration. */
	BoxMapping publicMapping() default @BoxMapping;

}
