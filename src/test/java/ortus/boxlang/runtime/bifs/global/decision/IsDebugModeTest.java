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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;

public class IsDebugModeTest {

	static BoxRuntime instance;

	@AfterEach
	public void setupEach() {
		instance.shutdown();
	}

	@DisplayName( "It detects debug mode" )
	@Test
	public void testItDetectsDebugMode() {
		instance = BoxRuntime.getInstance( true );
		assertThat( ( Boolean ) instance.executeStatement( "isDebugMode()" ) ).isTrue();
	}

	@DisplayName( "It detects non-debug mode" )
	@Test
	public void testItDetectsNonDebugMode() {
		instance = BoxRuntime.getInstance( false );
		assertThat( ( Boolean ) instance.executeStatement( "isDebugMode()" ) ).isFalse();
	}
}
