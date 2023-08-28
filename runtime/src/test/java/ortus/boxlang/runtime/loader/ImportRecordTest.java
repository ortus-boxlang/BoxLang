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

import java.util.List;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.TemplateBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.resolvers.BoxResolver;
import ortus.boxlang.runtime.scopes.Key;

import static com.google.common.truth.Truth.assertThat;

public class ImportRecordTest {

	@DisplayName( "It can use default constructor" )
	@Test
	public void testCanUseDefaultConstructor() {
		ImportRecord importRecord = new ImportRecord( "java.lang.String", "java", "jString" );

		assertThat( importRecord.className() ).isEqualTo( "java.lang.String" );
		assertThat( importRecord.resolverPrefix() ).isEqualTo( "java" );
		assertThat( importRecord.alias() ).isEqualTo( "jString" );
	}

	@DisplayName( "It can use default constructor with nulls" )
	@Test
	public void testCanUseDefaultConstructorWithNulls() {
		ImportRecord importRecord = new ImportRecord( "java.lang.String", null, null );

		assertThat( importRecord.className() ).isEqualTo( "java.lang.String" );
		assertThat( importRecord.resolverPrefix() ).isEqualTo( null );
		assertThat( importRecord.alias() ).isEqualTo( null );

		assertThrows( Throwable.class, () -> new ImportRecord( null, null, null ) );

	}

	@DisplayName( "It can use static constructor" )
	@Test
	public void testCanUseStaticConstructor() {
		ImportRecord importRecord = ImportRecord.parse( "java:java.lang.String AS jString" );
		assertThat( importRecord.className() ).isEqualTo( "java.lang.String" );
		assertThat( importRecord.resolverPrefix() ).isEqualTo( "java" );
		assertThat( importRecord.alias() ).isEqualTo( "jString" );

		importRecord = ImportRecord.parse( "java:java.lang.String" );
		assertThat( importRecord.className() ).isEqualTo( "java.lang.String" );
		assertThat( importRecord.resolverPrefix() ).isEqualTo( "java" );
		assertThat( importRecord.alias() ).isEqualTo( "String" );

		importRecord = ImportRecord.parse( "java.lang.String" );
		assertThat( importRecord.className() ).isEqualTo( "java.lang.String" );
		assertThat( importRecord.resolverPrefix() ).isEqualTo( null );
		assertThat( importRecord.alias() ).isEqualTo( "String" );
	}

}
