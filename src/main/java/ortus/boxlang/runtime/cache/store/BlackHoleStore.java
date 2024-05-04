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
package ortus.boxlang.runtime.cache.store;

import java.util.stream.Stream;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A black hole store that does nothing but simulate a cache store.
 */
public class BlackHoleStore extends AbstractStore {

	@Override
	public IObjectStore init( ICacheProvider provider, IStruct config ) {
		this.provider	= provider;
		this.config		= config;
		return this;
	}

	@Override
	public void shutdown() {
		// do nothing
	}

	@Override
	public int flush() {
		// do nothing
		return 0;
	}

	@Override
	public void evict() {
		// do nothing
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public void clearAll() {
		// do nothing
	}

	@Override
	public boolean clearAll( ICacheKeyFilter filter ) {
		return false;
	}

	@Override
	public boolean clear( Key key ) {
		// do nothing
		return true;
	}

	@Override
	public IStruct clear( Key... keys ) {
		var results = new Struct();
		for ( Key key : keys ) {
			results.put( key, true );
		}
		return results;
	}

	@Override
	public Key[] getKeys() {
		return new Key[ 0 ];
	}

	@Override
	public Key[] getKeys( ICacheKeyFilter filter ) {
		return new Key[ 0 ];
	}

	@Override
	public Stream<Key> getKeysStream() {
		return Stream.empty();
	}

	@Override
	public Stream<Key> getKeysStream( ICacheKeyFilter filter ) {
		return Stream.empty();
	}

	@Override
	public boolean lookup( Key key ) {
		return false;
	}

	@Override
	public IStruct lookup( Key... keys ) {
		var results = new Struct();
		for ( Key key : keys ) {
			results.put( key, false );
		}
		return results;
	}

	@Override
	public IStruct lookup( ICacheKeyFilter filter ) {
		return new Struct();
	}

	@Override
	public ICacheEntry get( Key key ) {
		return null;
	}

	@Override
	public IStruct get( Key... keys ) {
		return new Struct();
	}

	@Override
	public IStruct get( ICacheKeyFilter filter ) {
		return new Struct();
	}

	@Override
	public ICacheEntry getQuiet( Key key ) {
		return null;
	}

	@Override
	public IStruct getQuiet( Key... keys ) {
		return new Struct();
	}

	@Override
	public IStruct getQuiet( ICacheKeyFilter filter ) {
		return new Struct();
	}

	@Override
	public void set( Key key, ICacheEntry entry ) {
		// do nothing
	}

	@Override
	public void set( IStruct entries ) {
		// do nothing
	}

}
