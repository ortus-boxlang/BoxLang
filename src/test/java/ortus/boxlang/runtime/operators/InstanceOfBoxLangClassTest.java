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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;

/**
 * Tests for InstanceOf operator with BoxLang pre-compiled class names
 * that include prefixes like "boxgenerated.boxclass." and suffixes like "$cfc", "$bx", etc.
 */
public class InstanceOfBoxLangClassTest {

	BoxRuntime	instance		= BoxRuntime.getInstance( true );
	IBoxContext	runtimeContext	= instance.getRuntimeContext();
	IBoxContext	context;

	@BeforeEach
	void setUp() {
		context = new ScriptingRequestBoxContext( runtimeContext );
	}

	@DisplayName( "Can handle BoxLang compiled class names with boxgenerated.boxclass prefix and $cfc suffix" )
	@Test
	void testBoxLangCompiledClassNamesCfc() throws Exception {
		// Test the looseClassCheck method directly using reflection
		Method looseClassCheckMethod = InstanceOf.class.getDeclaredMethod( "looseClassCheck", String.class, String.class );
		looseClassCheckMethod.setAccessible( true );

		// Test case 1: boxgenerated.boxclass.coldbox.system.logging.Logevent$cfc should match coldbox.system.logging.LogEvent
		String actualClassName = "boxgenerated.boxclass.coldbox.system.logging.Logevent$cfc";
		String expectedClassName = "coldbox.system.logging.LogEvent";
		
		Boolean result = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, expectedClassName );
		assertThat( result ).isTrue();

		// Test case insensitive matching
		Boolean resultCaseInsensitive = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, expectedClassName.toLowerCase() );
		assertThat( resultCaseInsensitive ).isTrue();

		// Test with just the class name (no package)
		Boolean resultClassOnly = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, "LogEvent" );
		assertThat( resultClassOnly ).isTrue();
	}

	@DisplayName( "Can handle BoxLang compiled class names with boxgenerated.boxclass prefix and $bx suffix" )
	@Test
	void testBoxLangCompiledClassNamesBx() throws Exception {
		Method looseClassCheckMethod = InstanceOf.class.getDeclaredMethod( "looseClassCheck", String.class, String.class );
		looseClassCheckMethod.setAccessible( true );

		// Test case 2: boxgenerated.boxclass.coldbox.system.logging.Logevent$bx should match coldbox.system.logging.LogEvent
		String actualClassName = "boxgenerated.boxclass.coldbox.system.logging.Logevent$bx";
		String expectedClassName = "coldbox.system.logging.LogEvent";
		
		Boolean result = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, expectedClassName );
		assertThat( result ).isTrue();

		// Test case insensitive matching
		Boolean resultCaseInsensitive = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, expectedClassName.toLowerCase() );
		assertThat( resultCaseInsensitive ).isTrue();

		// Test with just the class name (no package)
		Boolean resultClassOnly = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, "LogEvent" );
		assertThat( resultClassOnly ).isTrue();
	}

	@DisplayName( "Can handle all BoxLang compiled suffixes" )
	@Test
	void testAllBoxLangSuffixes() throws Exception {
		Method looseClassCheckMethod = InstanceOf.class.getDeclaredMethod( "looseClassCheck", String.class, String.class );
		looseClassCheckMethod.setAccessible( true );

				String[] suffixes = { "$cfc", "$bx" };
		String baseClassName = "boxgenerated.boxclass.myapp.components.MyComponent";
		String expectedClassName = "myapp.components.MyComponent";

		for ( String suffix : suffixes ) {
			String actualClassName = baseClassName + suffix;
			Boolean result = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, expectedClassName );
			assertThat( result ).isTrue(); // Should match for suffix: " + suffix

			// Test case insensitive
			Boolean resultCaseInsensitive = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, expectedClassName.toLowerCase() );
			assertThat( resultCaseInsensitive ).isTrue(); // Should match case insensitive for suffix: " + suffix

			// Test with just class name
			Boolean resultClassOnly = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, "MyComponent" );
			assertThat( resultClassOnly ).isTrue(); // Should match class only for suffix: " + suffix
		}
	}

	@DisplayName( "Can handle BoxLang templates prefix" )
	@Test
	void testBoxLangTemplatesPrefix() throws Exception {
		Method looseClassCheckMethod = InstanceOf.class.getDeclaredMethod( "looseClassCheck", String.class, String.class );
		looseClassCheckMethod.setAccessible( true );

		// Test boxgenerated.templates prefix
		String actualClassName = "boxgenerated.templates.views.main.index$cfc";
		String expectedClassName = "views.main.index";
		
		Boolean result = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, expectedClassName );
		assertThat( result ).isTrue();

		// Test with just the filename
		Boolean resultFileOnly = ( Boolean ) looseClassCheckMethod.invoke( null, actualClassName, "index" );
		assertThat( resultFileOnly ).isTrue();
	}

	@DisplayName( "Verify constants are properly defined" )
	@Test
	void testBoxLangConstants() throws Exception {
		// Verify that the constants we defined are properly set
		Field suffixesField = InstanceOf.class.getDeclaredField( "BOXLANG_SUFFIXES" );
		suffixesField.setAccessible( true );
		String[] suffixes = ( String[] ) suffixesField.get( null );
		
				assertThat( suffixes ).asList().containsExactly( "$cfc", "$bx" );

		Field prefixField = InstanceOf.class.getDeclaredField( "BOXLANG_PREFIX" );
		prefixField.setAccessible( true );
		String prefix = ( String ) prefixField.get( null );
		
		assertThat( prefix ).isEqualTo( "boxgenerated." );

		Field prefixLengthField = InstanceOf.class.getDeclaredField( "BOXLANG_PREFIX_LENGTH" );
		prefixLengthField.setAccessible( true );
		int prefixLength = ( int ) prefixLengthField.get( null );
		
		assertThat( prefixLength ).isEqualTo( "boxgenerated.".length() );
	}

	@DisplayName( "Regression test: existing behavior still works" )
	@Test
	void testExistingBehaviorStillWorks() throws Exception {
		Method looseClassCheckMethod = InstanceOf.class.getDeclaredMethod( "looseClassCheck", String.class, String.class );
		looseClassCheckMethod.setAccessible( true );

		// Test existing Java class matching behavior
		Boolean result1 = ( Boolean ) looseClassCheckMethod.invoke( null, "java.lang.String", "String" );
		assertThat( result1 ).isTrue();

		Boolean result2 = ( Boolean ) looseClassCheckMethod.invoke( null, "java.lang.String", "string" );
		assertThat( result2 ).isTrue();

		Boolean result3 = ( Boolean ) looseClassCheckMethod.invoke( null, "java.lang.String", "java.lang.String" );
		assertThat( result3 ).isTrue();

		Boolean result4 = ( Boolean ) looseClassCheckMethod.invoke( null, "java.lang.String", "JAVA.LANG.STRING" );
		assertThat( result4 ).isTrue();

		// Test non-matching cases
		Boolean result5 = ( Boolean ) looseClassCheckMethod.invoke( null, "java.lang.String", "Integer" );
		assertThat( result5 ).isFalse();
	}
}