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
package ortus.boxlang.runtime.testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URL;

public class ExamplesTest {

	@DisplayName( "It can execute Phase1 example" )
	@Test
	public void testItCanExecutePhase1() throws Throwable {
		Phase1.main( new String[] {
		} );
	}

	@DisplayName( "It can execute Phase1 try/catch example" )
	@Test
	public void testItCanExecutePhase1TryCatch() throws Throwable {
		Phase1TryCatch.main( new String[] {
		} );
	}

	@DisplayName( "It can execute Phase1 switch example" )
	@Test
	public void testItCanExecutePhase1Switch() throws Throwable {
		Phase1Switch.main( new String[] {
		} );
	}

}
