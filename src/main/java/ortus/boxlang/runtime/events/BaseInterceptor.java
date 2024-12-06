/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.events;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * An abstract base class for interceptors with many helper methods useful during interceptions
 */
public abstract class BaseInterceptor implements IInterceptor {

	/**
	 * The properties to configure the interceptor with (if any)
	 */
	protected IStruct properties = new Struct();

	/**
	 * This method is called by the BoxLang runtime to configure the interceptor
	 * with a Struct of properties
	 *
	 * @param properties The properties to configure the interceptor with (if any)
	 */
	public void configure( IStruct properties ) {
		this.properties = properties;
	}

	/**
	 * This method is called by the BoxLang runtime to configure the interceptor
	 */
	public void configure() {
		configure( new Struct() );
	}

	/**
	 * Get the properties
	 *
	 * @return The properties
	 */
	public IStruct getProperties() {
		return this.properties;
	}

	/**
	 * Get a property
	 *
	 * @param name The property name
	 *
	 * @return The property value or null if not found
	 */
	public Object getProperty( Key name ) {
		return this.properties.get( name );
	}

	/**
	 * Get a property or a default value
	 *
	 * @param name         The property name
	 * @param defaultValue The default value
	 *
	 * @return The property value or the default value if not found
	 */
	public Object getProperty( Key name, Object defaultValue ) {
		return this.properties.getOrDefault( name, defaultValue );
	}

	/**
	 * Verify if a property exists
	 *
	 * @param name The property name
	 *
	 * @return True if the property exists, false otherwise
	 */
	public boolean hasProperty( Key name ) {
		return this.properties.containsKey( name );
	}

	/**
	 * Set a property
	 *
	 * @param name  The property name
	 * @param value The property value
	 */
	public IInterceptor setProperty( Key name, Object value ) {
		this.properties.put( name, value );
		return this;
	}

	/**
	 * Get the runtime
	 *
	 * @return The runtime
	 */
	public BoxRuntime getRuntime() {
		return BoxRuntime.getInstance();
	}

	/**
	 * Unregister the interceptor from all states
	 */
	public void unregister() {
		getRuntime().getInterceptorService().unregister( DynamicObject.of( this ) );
	}

}
