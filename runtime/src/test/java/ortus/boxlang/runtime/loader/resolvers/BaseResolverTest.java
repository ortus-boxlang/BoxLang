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

package ortus.boxlang.runtime.loader.resolvers;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;

public class BaseResolverTest {

	@DisplayName( "It can create a base resolver" )
	@Test
	void testItCanCreateIt() {
		BaseResolver target = new BaseResolver( "test", "TEST" );
		assertThat( target ).isInstanceOf( BaseResolver.class );
		assertThat( target.getName() ).isEqualTo( "test" );
		assertThat( target.getPrefix() ).isEqualTo( "test" );
		assertThat( target.getImportCacheSize() ).isEqualTo( 0 );
	}

	@DisplayName( "It can expand basic imports" )
	@Test
	void testItCanResolveBasicImports() {
		List<ImportDefinition>	imports		= Arrays.asList(
		    ImportDefinition.parse( "java:java.lang.String" ),
		    ImportDefinition.parse( "java:java.lang.Integer" ),
		    ImportDefinition.parse( "java.lang.Integer" ),
		    ImportDefinition.parse( "ortus.boxlang.runtime.loader.resolvers.BaseResolver" ),
		    // The Java resolver will ignore this mapping
		    ImportDefinition.parse( "bx:models.test.HelloWorld" ),
		    ImportDefinition.parse( "java:java.lang.List as jList" )
		);
		BaseResolver			jResolver	= JavaResolver.getInstance();
		jResolver.clearImportCache();

		String fqn = jResolver.expandFromImport( new ScriptingBoxContext(), "String", imports );
		assertThat( fqn ).isEqualTo( "java.lang.String" );
		assertThat( jResolver.getImportCacheSize() ).isEqualTo( 1 );

		fqn = jResolver.expandFromImport( new ScriptingBoxContext(), "Integer", imports );
		assertThat( fqn ).isEqualTo( "java.lang.Integer" );
		assertThat( jResolver.getImportCacheSize() ).isEqualTo( 2 );

		// The Java resolver will ignore this mapping
		fqn = jResolver.expandFromImport( new ScriptingBoxContext(), "HelloWorld", imports );
		assertThat( fqn ).isEqualTo( "HelloWorld" );
		assertThat( jResolver.getImportCacheSize() ).isEqualTo( 2 );

		fqn = jResolver.expandFromImport( new ScriptingBoxContext(), "BaseResolver", imports );
		assertThat( fqn ).isEqualTo( "ortus.boxlang.runtime.loader.resolvers.BaseResolver" );
		assertThat( jResolver.getImportCacheSize() ).isEqualTo( 3 );

		fqn = jResolver.expandFromImport( new ScriptingBoxContext(), "jList", imports );
		assertThat( fqn ).isEqualTo( "java.lang.List" );
		assertThat( jResolver.getImportCacheSize() ).isEqualTo( 4 );
	}

}
