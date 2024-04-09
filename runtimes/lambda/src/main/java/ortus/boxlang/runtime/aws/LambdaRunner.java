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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.jr.ob.JSON.Feature;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;

public class LambdaRunner implements RequestHandler<Map<String, String>, String> {

	public static final String	LAMBDA_CLASS	= "Lambda.bx";

	private Path				lambdaPath;

	/**
	 * Constructor
	 */
	public LambdaRunner() {
		// By convention we look for a `Lambda.bx` file in the current directory
		this.lambdaPath = Path.of( System.getProperty( "user.dir" ), LAMBDA_CLASS ).toAbsolutePath();
	}

	/**
	 * Constructor: Useful for tests
	 *
	 * @param lambdaPath The absolute path to the Lambda.bx file
	 */
	public LambdaRunner( Path lambdaPath ) {
		this.lambdaPath = lambdaPath;
	}

	/**
	 * Handle the incoming request from the AWS Lambda
	 *
	 * @param event   The incoming event as a Struct
	 * @param context The AWS Lambda context
	 *
	 * @return The response as a JSON string
	 *
	 */
	@Override
	public String handleRequest( Map<String, String> event, Context context ) {
		LambdaLogger logger = context.getLogger();
		logger.log( "Incoming Struct " + event.toString() );

		// Startup the runtime
		BoxRuntime	runtime		= BoxRuntime.getInstance();
		IBoxContext	boxContext	= runtime.getRuntimeContext();

		// Prep the response
		IStruct		response	= Struct.of(
		    "statusCode", 200,
		    "headers", new Struct(),
		    "body", "" );

		// Prep the incoming event as a struct
		IStruct		eventStruct	= Struct.fromMap( event );

		try {
			// Verify the Lambda.bx file
			if ( !lambdaPath.toFile().exists() ) {
				throw new BoxRuntimeException( "Lambda.bx file not found in [" + lambdaPath + "]" );
			}

			// Compile + Get the Lambda Class
			IClassRunnable lambda = ( IClassRunnable ) DynamicObject.of(
			    RunnableLoader.getInstance().loadClass( lambdaPath, this.getClass().getPackageName(), boxContext )
			)
			    .invokeConstructor( boxContext )
			    .getTargetInstance();

			// Verify the run method
			if ( !lambda.getThisScope().containsKey( Key.run ) ) {
				throw new BoxRuntimeException( "Lambda.bx file does not contain a `run` method" );
			}

			// Invoke the run method
			lambda.dereferenceAndInvoke(
			    boxContext,
			    Key.run,
			    new Object[] { eventStruct, context, response },
			    false
			);

			// Response back to JSON
			return JSONUtil.getJSONBuilder()
			    .with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( response );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error converting response to JSON", e );
		} finally {
			runtime.shutdown();
		}
	}
}
