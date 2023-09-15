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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ImportDefinitionTest {

	@DisplayName( "It can use default constructor" )
	@Test
	public void testCanUseDefaultConstructor() {
		ImportDefinition ImportDefinition = new ImportDefinition( "java.lang.String", "java", "jString" );

		assertThat( ImportDefinition.className() ).isEqualTo( "java.lang.String" );
		assertThat( ImportDefinition.resolverPrefix() ).isEqualTo( "java" );
		assertThat( ImportDefinition.alias() ).isEqualTo( "jString" );
	}

	@DisplayName( "It can use default constructor with nulls" )
	@Test
	public void testCanUseDefaultConstructorWithNulls() {
		ImportDefinition ImportDefinition = new ImportDefinition( "java.lang.String", null, null );

		assertThat( ImportDefinition.className() ).isEqualTo( "java.lang.String" );
		assertThat( ImportDefinition.resolverPrefix() ).isEqualTo( null );
		assertThat( ImportDefinition.alias() ).isEqualTo( null );

		assertThrows( Throwable.class, () -> new ImportDefinition( null, null, null ) );

	}

	@DisplayName( "It can use static constructor parser" )
	@Test
	public void testCanUseStaticConstructorParser() {
		ImportDefinition importDef = ImportDefinition.parse( "java:java.lang.String AS jString" );
		assertThat( importDef.className() ).isEqualTo( "java.lang.String" );
		assertThat( importDef.resolverPrefix() ).isEqualTo( "java" );
		assertThat( importDef.alias() ).isEqualTo( "jString" );

		importDef = ImportDefinition.parse( "java:java.lang.String" );
		assertThat( importDef.className() ).isEqualTo( "java.lang.String" );
		assertThat( importDef.resolverPrefix() ).isEqualTo( "java" );
		assertThat( importDef.alias() ).isEqualTo( "String" );

		importDef = ImportDefinition.parse( "java.lang.String" );
		assertThat( importDef.className() ).isEqualTo( "java.lang.String" );
		assertThat( importDef.resolverPrefix() ).isEqualTo( null );
		assertThat( importDef.alias() ).isEqualTo( "String" );

		importDef = ImportDefinition.parse( "java.util.*" );
		assertThat( importDef.className() ).isEqualTo( "java.util.*" );
		assertThat( importDef.resolverPrefix() ).isEqualTo( null );
		assertThat( importDef.alias() ).isEqualTo( "*" );
	}

}
