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
package ortus.boxlang.runtime.util;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;

public class StructUtil {

	/**
	 * Method to invoke a function for every item in a struct
	 *
	 * @param struct          The struct to iterate
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 * @param ordered         Boolean as to whether to maintain order in parallel execution
	 *
	 * @return
	 */
	public static void each(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads,
	    Boolean ordered ) {

		Stream<Map.Entry<Key, Object>>		entryStream	= struct.entrySet().stream();

		Consumer<Map.Entry<Key, Object>>	exec		= item -> callbackContext.invokeFunction(
		    callback,
		    new Object[] { item.getKey().getName(), item.getValue(), struct }
		);

		if ( !parallel ) {
			entryStream.forEach( exec );
		} else if ( ordered ) {
			AsyncService.buildExecutor(
			    "ArrayFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> entryStream.parallel().forEachOrdered( exec ) );
		} else {
			AsyncService.buildExecutor(
			    "ArrayFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> entryStream.parallel().forEach( exec ) );
		}

	}

}
