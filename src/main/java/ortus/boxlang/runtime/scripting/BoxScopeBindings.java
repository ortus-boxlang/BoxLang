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
package ortus.boxlang.runtime.scripting;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.Bindings;

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

public class BoxScopeBindings implements Bindings {

    IScope scope;

    public BoxScopeBindings( IScope scope ) {
        this.scope = scope;
    }

    @Override
    public int size() {
        return scope.size();
    }

    @Override
    public boolean isEmpty() {
        return scope.isEmpty();
    }

    @Override
    public boolean containsKey( Object key ) {
        return scope.containsKey( Key.of( key.toString() ) );
    }

    @Override
    public boolean containsValue( Object value ) {
        return scope.containsValue( value );
    }

    @Override
    public Object get( Object key ) {
        return scope.get( Key.of( key.toString() ) );
    }

    @Override
    public Object put( String key, Object value ) {
        Object oldValue = scope.get( Key.of( key ) );
        scope.put( Key.of( key ), value );
        return oldValue;
    }

    @Override
    public void putAll( Map<? extends String, ? extends Object> m ) {
        for ( Map.Entry<? extends String, ? extends Object> entry : m.entrySet() ) {
            scope.put( Key.of( entry.getKey() ), entry.getValue() );
        }
    }

    @Override
    public Object remove( Object key ) {
        Object oldValue = scope.get( Key.of( key.toString() ) );
        scope.remove( Key.of( key.toString() ) );
        return oldValue;
    }

    @Override
    public void clear() {
        scope.clear();
    }

    @Override
    public Set<String> keySet() {
        return scope.keySet().stream().map( Key::toString ).collect( Collectors.toSet() );
    }

    @Override
    public Collection<Object> values() {
        return scope.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return scope.entrySet().stream().map( entry -> Map.entry( entry.getKey().toString(), entry.getValue() ) ).collect( Collectors.toSet() );
    }
}