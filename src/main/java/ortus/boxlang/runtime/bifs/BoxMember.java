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
package ortus.boxlang.runtime.bifs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ortus.boxlang.runtime.types.BoxLangType;

@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Repeatable( BoxMembers.class )
public @interface BoxMember {

	BoxLangType type();

	// If not provided, the name will be the name of the BIF with the BoxType replaced. So arrayAppend() would be append()
	String name() default "";

	// If not provided, the argument will be the first argument of the BIF
	String objectArgument() default "";
}
