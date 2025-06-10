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
package ortus.boxlang.runtime.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark and configure cache provider implementations in BoxLang.
 *
 * This annotation provides metadata about cache providers, including their capabilities,
 * identification, and grouping information. It should be applied to classes that implement
 * cache functionality within the BoxLang runtime.
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.0.0
 *
 * @see ortus.boxlang.runtime.cache
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
public @interface BoxCache {

	// The registration alias for the cache provider
	String alias() default "";

	// A human short description of the cache provider
	String description() default "";

	// If the cache supports distributed caching
	boolean distributed() default false;

	// If the cache supports distributed locking
	boolean distributedLocking() default false;

	// An array of tags that can be used to group cache providers
	String[] tags() default {};

}
