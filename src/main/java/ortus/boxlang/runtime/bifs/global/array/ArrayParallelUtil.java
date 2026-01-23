/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;

final class ArrayParallelUtil {

	private ArrayParallelUtil() {
	}

	static ParallelSettings resolveParallelSettings( ArgumentsScope arguments ) {
		Object maxThreads = arguments.get( Key.maxThreads );
		if ( maxThreads instanceof Boolean castBoolean ) {
			// If maxThreads is a boolean, we assign it to virtual
			arguments.put( Key.virtual, castBoolean );
			maxThreads = null;
		}

		CastAttempt<Integer>	maxThreadsAttempt	= IntegerCaster.attempt( maxThreads );
		boolean					virtual				= BooleanCaster.cast( arguments.getOrDefault( Key.virtual, false ) );
		return new ParallelSettings( maxThreadsAttempt.getOrDefault( 0 ), virtual );
	}

	static final class ParallelSettings {

		private final int		maxThreads;
		private final boolean	virtual;

		private ParallelSettings( int maxThreads, boolean virtual ) {
			this.maxThreads	= maxThreads;
			this.virtual	= virtual;
		}

		int maxThreads() {
			return maxThreads;
		}

		boolean virtual() {
			return virtual;
		}
	}
}
