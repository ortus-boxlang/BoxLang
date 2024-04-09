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
package ortus.boxlang.runtime.aws;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class LambdaRunnerTest {

	private static final Logger logger = LoggerFactory.getLogger( LambdaRunnerTest.class );

	@Test
	@DisplayName( "LambdaRunner throws BoxRuntimeException when Lambda.bx not found" )
	public void testLambdaNotFound() throws IOException {
		// Set a non-existent path
		Path			invalidPath	= Path.of( "invalid_path", "Lambda.bx" );
		LambdaRunner	runner		= new LambdaRunner( invalidPath );
		// Create a AWS Lambda Context
		Context			context		= new TestContext();
		var				event		= new HashMap<String, String>();

		// Expect BoxRuntimeException
		assertThrows( BoxRuntimeException.class, () -> runner.handleRequest( event, context ) );
	}

	@DisplayName( "Test a valid Lambda.bx" )
	@Test
	public void testValidLambda() throws IOException {
		// Set a valid path
		Path			validPath	= Path.of( "src", "test", "resources", "Lambda.bx" );
		LambdaRunner	runner		= new LambdaRunner( validPath );
		// Create a AWS Lambda Context
		Context			context		= new TestContext();
		var				event		= new HashMap<String, String>();
		// Add some mock data to the event
		event.put( "name", "Ortus Solutions" );
		event.put( "when", Instant.now().toString() );

		var results = runner.handleRequest( event, context );
		System.out.println( "====> RESULTS " + results );
	}

}
