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

import static org.junit.Assert.assertThrows;

import java.util.Optional;

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.TemplateBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;

import static com.google.common.truth.Truth.assertThat;

public class ClassLocatorTest {

	@DisplayName( "It can register the core resolvers" )
	@Test
	public void testCanRegisterCoreResolvers() {
		ClassLocator locator = ClassLocator.getInstance();
		System.out.println( "prefixes " + locator.getResolvedPrefixes() );
		assertThat( locator.hasResolver( "bx" ) ).isTrue();
		assertThat( locator.hasResolver( "java" ) ).isTrue();
		assertThat( locator.getResolvedPrefixes() ).containsAtLeast( "bx", "java" );
		assertThat( locator.getResolver( "bx" ) ).isInstanceOf( BoxResolver.class );
	}

	@DisplayName( "It can throw an exception if you try to remove a core resolver" )
	@Test
	public void testItCannotRemoveACoreResolver() {
		ClassLocator locator = ClassLocator.getInstance();
		assertThrows( IllegalStateException.class, () -> {
			locator.removeResolver( "java" );
		} );
		assertThrows( IllegalStateException.class, () -> {
			locator.removeResolver( "bx" );
		} );
	}

	@DisplayName( "It can load native Java classes and add to the resolver cache" )
	@Test
	public void testCanLoadJavaClassesWithCaching() throws Throwable {
		ClassLocator	locator		= ClassLocator.getInstance();
		String			targetClass	= "java.lang.String";

		locator.clear();
		assertThat( locator.size() ).isEqualTo( 0 );

		DynamicObject target = locator.load( new TemplateBoxContext(), targetClass, "java" );
		target.invokeConstructor( "Hola ClassLoader" );
		assertThat( target.getTargetInstance() ).isEqualTo( "Hola ClassLoader" );

		assertThat( target ).isNotNull();
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( locator.size() ).isEqualTo( 1 );
		assertThat( locator.hasClass( "java:" + targetClass ) ).isTrue();
		assertThat( locator.classSet() ).containsAnyIn( new Object[] { "java:" + targetClass } );
	}

	@DisplayName( "It can safe load non-existen classes without throwing an exception" )
	@Test
	public void testCanSafeLoad() {
		ClassLocator			locator		= ClassLocator.getInstance();
		String					targetClass	= "java.lang.Bogus";

		Optional<DynamicObject>	target		= locator.safeLoad( new TemplateBoxContext(), targetClass, "java" );
		assertThat( target.isPresent() ).isFalse();

		target = locator.safeLoad( new TemplateBoxContext(), targetClass );
		assertThat( target.isPresent() ).isFalse();
	}

	@DisplayName( "Resolver cache methods work" )
	@Test
	public void testResolverCacheMethods() throws ClassNotFoundException {
		ClassLocator	locator		= ClassLocator.getInstance();
		String			targetClass	= "java.lang.String";

		assertThat( locator.isEmpty() ).isTrue();
		assertThat( locator.size() ).isEqualTo( 0 );
		assertThat( locator.getResolverCache().size() ).isEqualTo( 0 );
		assertThat( locator.hasClass( targetClass ) ).isFalse();
		assertThat( locator.clear( "bogus" ) ).isFalse();

		locator.getResolverCache().put(
		        targetClass,
		        new ClassLocation(
		                "String",
		                targetClass,
		                "java.lang",
		                ClassLocator.TYPE_JAVA,
		                String.class,
		                null
		        )
		);
		assertThat( locator.hasClass( targetClass ) ).isTrue();
		assertThat( locator.size() ).isEqualTo( 1 );
		assertThat( locator.isEmpty() ).isFalse();
		assertThat( locator.getClass( targetClass ).isPresent() ).isTrue();
		assertThat( locator.classSet() ).contains( targetClass );
		assertThat( locator.clear( targetClass ) ).isTrue();
	}

}
