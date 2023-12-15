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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class ExamplesTest {

	@DisplayName( "It can execute Phase1 example" )
	@Test
	public void testItCanExecutePhase1() {
		Phase1.main( new String[] {
		} );
	}

	@DisplayName( "It can execute Phase1 try/catch example" )
	@Test
	public void testItCanExecutePhase1TryCatch() {
		Phase1TryCatch.main( new String[] {
		} );
	}

	@DisplayName( "It can execute Phase1 switch example" )
	@Test
	public void testItCanExecutePhase1Switch() {
		Phase1Switch.main( new String[] {
		} );
	}

	@DisplayName( "It can execute Phase2 UDF example" )
	@Test
	public void testItCanExecutePhase2UDF() {
		Phase2UDF.main( new String[] {
		} );
	}

	@DisplayName( "It can execute Phase2 Closure example" )
	@Test
	public void testItCanExecutePhase2Closure() {
		Phase2Closure.main( new String[] {
		} );
	}

	@DisplayName( "It can execute Phase2 Lambda example" )
	@Test
	public void testItCanExecutePhase2Lambda() {
		Phase2Lambda.main( new String[] {
		} );
	}

}
