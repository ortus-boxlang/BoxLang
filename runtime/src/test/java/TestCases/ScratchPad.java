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

package TestCases;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.TemplateBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.loader.resolvers.BaseResolver;
import ortus.boxlang.runtime.loader.resolvers.JavaResolver;

import org.junit.jupiter.api.DisplayName;
import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.List;

public class ScratchPad {

    @DisplayName( "Test it" )
    @Test
    void testIt() {
        List<ImportDefinition> imports   = Arrays.asList(
            ImportDefinition.parse( "java:java.lang.String" ),
            ImportDefinition.parse( "java:java.lang.Integer" ),
            ImportDefinition.parse( "ortus.boxlang.runtime.loader.resolvers.BaseResolver" ),
            ImportDefinition.parse( "java:java.lang.List as jList" )
        );

        BaseResolver           jResolver = JavaResolver.getInstance();

        String                 fqn       = jResolver.expandFromImport( new TemplateBoxContext(), "String", imports );
        assertThat( fqn ).isEqualTo( "java.lang.String" );

        fqn = jResolver.expandFromImport( new TemplateBoxContext(), "Integer", imports );
        assertThat( fqn ).isEqualTo( "java.lang.Integer" );

        fqn = jResolver.expandFromImport( new TemplateBoxContext(), "BaseResolver", imports );
        assertThat( fqn ).isEqualTo( "ortus.boxlang.runtime.loader.resolvers.BaseResolver" );

        fqn = jResolver.expandFromImport( new TemplateBoxContext(), "jList", imports );
        assertThat( fqn ).isEqualTo( "java.lang.List" );

    }

}
