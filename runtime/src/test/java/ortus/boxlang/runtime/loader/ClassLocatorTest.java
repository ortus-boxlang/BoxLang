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
package ortus.boxlang.runtime.loader;

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.TemplateBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;

import static com.google.common.truth.Truth.assertThat;

public class ClassLocatorTest {

	@DisplayName( "It can load native Java classes" )
	@Test
	public void testCanLoadJavaClasses() throws Throwable {
		ClassLocator	locator		= ClassLocator.getInstance( "" );
		String			targetClass	= "java.lang.String";

		locator.clear();
		assertThat( locator.size() ).isEqualTo( 0 );

		DynamicObject target = locator.load( new TemplateBoxContext(), targetClass );
		target.invokeConstructor( "Hola ClassLoader" );
		assertThat( target.getTargetInstance() ).isEqualTo( "Hola ClassLoader" );

		assertThat( target ).isNotNull();
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( locator.size() ).isEqualTo( 1 );
		assertThat( locator.hasClass( targetClass ) ).isTrue();
		assertThat( locator.classSet() ).containsAnyIn( new Object[] { targetClass } );
	}

	@DisplayName( "It can work with the resolver cache" )
	@Test
	public void testCanWorkWithTheResolverCache() throws ClassNotFoundException {
		ClassLocator	locator		= ClassLocator.getInstance( "" );
		String			targetClass	= "java.lang.String";

		assertThat( locator.getResolverCache().size() ).isEqualTo( 0 );
		assertThat( locator.size() ).isEqualTo( 0 );
		assertThat( locator.hasClass( targetClass ) ).isFalse();
		assertThat( locator.isEmpty() ).isTrue();
	}

}
