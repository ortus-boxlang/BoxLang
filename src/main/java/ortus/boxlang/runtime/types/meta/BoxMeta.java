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
package ortus.boxlang.runtime.types.meta;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * This is a base class for all meta types
 */
public abstract class BoxMeta {

    public static final Key key = Key.of( "bx$" );

    public abstract Object getTarget();

    public void registerChangeListener( IChangeListener listener ) {
        ensureTargetListenable().registerChangeListener( listener );
    }

    public void registerChangeListener( Key key, IChangeListener listener ) {
        ensureTargetListenable().registerChangeListener( key, listener );

    }

    public void removeChangeListener( Key key ) {
        ensureTargetListenable().removeChangeListener( key );

    }

    private IListenable ensureTargetListenable() {
        if ( getTarget() instanceof IListenable listenable ) {
            return listenable;
        } else {
            throw new ApplicationException( "Target [" + getTarget().getClass().getName() + "] does not support change listeners." );
        }
    }

}
