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
package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/Consumer.html
 */
public class Consumer<T> extends BaseProxy implements java.util.function.Consumer<T> {

	public Consumer( Object target, IBoxContext context, String method ) {
		super( target, context, method );
		prepLogger( Consumer.class );
	}

	@Override
	public void accept( Object t ) {
		try {
			invoke( t );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking Consumer", e );
			throw new BoxRuntimeException( "Error invoking Consumer", e );
		}
	}

}
