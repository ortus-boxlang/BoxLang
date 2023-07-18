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
package ortus.boxlang.runtime.scopes;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Base scope implementation. Extends HashMap for now. May want to switch to composition over inheritance, but this
 * is simpler for now and using the Key class provides our case insensitivity automatically.
 */
public class BaseScope extends ConcurrentHashMap<Key, Object> implements IScope {

	private int lookupOrder;

	public BaseScope( int lookupOrder ) {
		super();
		this.lookupOrder = lookupOrder;
	}

	public int getLookupOrder() {
		return this.lookupOrder;
	}

	// private Map<Key,Object> data = new HashMap<Key,Object>();
	// public Object getValue( Key name ) { }

	// public IScope setValue( Key name, Object value ) { }

}
