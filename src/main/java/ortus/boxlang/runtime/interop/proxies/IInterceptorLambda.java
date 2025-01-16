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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.events.Interceptor;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@Interceptor( autoLoad = false )
public class IInterceptorLambda extends BaseProxy implements ortus.boxlang.runtime.events.IInterceptorLambda {

	public IInterceptorLambda() {
		super();
	}

	public IInterceptorLambda( Object target, IBoxContext context, String method ) {
		super( target, context, method );
		prepLogger( IInterceptorLambda.class );
	}

	@Override
	public Boolean intercept( IStruct data ) {
		try {
			return BooleanCaster.cast( invoke( data ) );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking IInterceptorLambda", e );
			throw new BoxRuntimeException( "Error invoking IInterceptorLambda", e );
		}
	}

}
