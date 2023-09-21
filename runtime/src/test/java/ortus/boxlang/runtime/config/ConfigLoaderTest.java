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

package ortus.boxlang.runtime.config;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

import org.junit.jupiter.api.DisplayName;
import static com.google.common.truth.Truth.assertThat;

public class ConfigLoaderTest {

	@DisplayName( "It can load the default config file" )
	@Test
	void testItCanLoadTheDefaultConfig() {
		Configuration config = ConfigLoader.getInstance().load();

		assertThat( config.compiler ).isNotNull();
		assertThat( config.runtime ).isNotNull();

		System.out.println( "classGenerationDirectory " + config.compiler.classGenerationDirectory );
		System.out.println( "mappings: " + config.runtime.mappings );
		System.out.println( "modulesDirectory " + config.runtime.modulesDirectory );
		System.out.println( "caches: " + config.runtime.caches.get( Key.of( "importcache" ) ) );
	}

}
