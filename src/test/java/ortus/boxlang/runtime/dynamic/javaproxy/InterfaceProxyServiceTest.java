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
package ortus.boxlang.runtime.dynamic.javaproxy;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.Array;

public class InterfaceProxyServiceTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "It can generate a proxy" )
	@Test
	void testItCanGenerateAProxy() {
		IClassRunnable	boxClassRunnable	= ( IClassRunnable ) ClassLocator.getInstance()
		    .load( context, "bx:" + "src.test.java.ortus.boxlang.runtime.dynamic.javaproxy.BoxClassRunnable", context.getCurrentImports() )
		    .invokeConstructor( context )
		    .unWrapBoxLangClass();

		IProxyRunnable	result				= InterfaceProxyService.createProxy( context, boxClassRunnable, Array.of( "java.lang.Runnable" ) );
		assertThat( result ).isInstanceOf( java.lang.Runnable.class );
		java.lang.Runnable jRunable = ( java.lang.Runnable ) result;
		jRunable.run();
	}

}
