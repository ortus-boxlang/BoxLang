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

package ortus.boxlang.runtime.context;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

import org.junit.jupiter.api.DisplayName;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName( "TemplateBoxContext Tests" )
public class TemplateBoxContextTest {

	@Test
	@DisplayName( "Test default constructor" )
	void testDefaultConstructor() {
		TemplateBoxContext context = new TemplateBoxContext();
		assertThat( context.getTemplate() ).isNull();
		assertThat( context.getParent() ).isNull();
		assertThat( context.hasTemplate() ).isFalse();
	}

	@Test
	@DisplayName( "Test constructor with template path" )
	void testConstructorWithTemplatePath() {
		TemplateBoxContext context = new TemplateBoxContext( new BaseTemplate() );
		assertThat( context.getTemplate() ).isNotNull();
		assertThat( context.hasTemplate() ).isTrue();
	}

	@Test
	@DisplayName( "Test setTemplate" )
	void testsetTemplate() {
		TemplateBoxContext	context		= new TemplateBoxContext();
		BaseTemplate		template	= new BaseTemplate();
		template.path = "my/path/to/template";
		context.setTemplate( template );
		assertThat( "my/path/to/template" ).isEqualTo( context.getTemplate().path );
	}

	@Test
	@DisplayName( "Test scopeFind with existing key" )
	void testScopeFindExistingKey() {
		TemplateBoxContext	context	= new TemplateBoxContext();
		Key					key		= Key.of( "testIt" );
		context.getScopeLocal( Key.of( "variables" ) ).put( key, "value" );
		assertThat( context.scopeFindLocal( key ) ).isEqualTo( "value" );
	}

	@Test
	@DisplayName( "Test scopeFind with missing key" )
	void testScopeFindMissingKey() {
		TemplateBoxContext context = new TemplateBoxContext();
		assertThrows( KeyNotFoundException.class, () -> context.scopeFindLocal( new Key( "nonExistentKey" ) ) );
	}
}
