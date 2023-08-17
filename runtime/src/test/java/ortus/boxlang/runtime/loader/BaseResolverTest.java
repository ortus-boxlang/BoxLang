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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static com.google.common.truth.Truth.assertThat;

public class BaseResolverTest {

	@DisplayName( "It can create a base resolver" )
	@Test
	void testItCanCreateIt() {
		BaseResolver target = new BaseResolver( "test", "TEST" );
		assertThat( target ).isInstanceOf( BaseResolver.class );
		assertThat( target.getName() ).isEqualTo( "test" );
		assertThat( target.getPrefix() ).isEqualTo( "test" );
	}

}
