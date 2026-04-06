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
package ortus.boxlang.runtime.config.segments;

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Configuration segment for the {@code watcher} top-level key in {@code boxlang.json}.
 * <p>
 * All properties serve as defaults inherited by every named watcher definition.
 * Per-definition overrides take precedence.
 * </p>
 */
public class WatcherConfig implements IConfigSegment {

	/**
	 * Whether watchers recurse into subdirectories by default.
	 */
	public Boolean	recursive		= true;

	/**
	 * Debounce window in milliseconds. 0 = disabled.
	 * Events are suppressed until no new events arrive within this window.
	 */
	public Long		debounce		= 0L;

	/**
	 * Throttle window in milliseconds. 0 = disabled.
	 * Fires at most once per window; additional events in the window are dropped.
	 */
	public Long		throttle		= 0L;

	/**
	 * Whether to skip intermediate filesystem events caused by atomic writes
	 * (e.g. editor swap files, write-to-tmp-then-rename patterns).
	 */
	public Boolean	atomicWrites	= true;

	/**
	 * Startup delay in milliseconds before the watch loop begins processing events.
	 */
	public Long		delay			= 0L;

	/**
	 * Number of consecutive errors before a watcher auto-shuts down. 0 = never.
	 */
	public Integer	errorThreshold	= 10;

	/**
	 * Named watcher definitions loaded at runtime startup.
	 * Each entry maps a watcher name to a configuration struct.
	 */
	public IStruct	definitions		= new Struct();

	/**
	 * Default constructor.
	 */
	public WatcherConfig() {
	}

	/**
	 * Process the configuration struct, overriding defaults with any values present.
	 *
	 * @param config the configuration struct
	 *
	 * @return this config segment
	 */
	@Override
	public IConfigSegment process( IStruct config ) {
		if ( config.containsKey( Key.recursive ) ) {
			this.recursive = BooleanCaster.cast( config.get( Key.recursive ) );
		}
		if ( config.containsKey( Key.debounce ) ) {
			this.debounce = LongCaster.cast( config.get( Key.debounce ) );
		}
		if ( config.containsKey( Key.throttle ) ) {
			this.throttle = LongCaster.cast( config.get( Key.throttle ) );
		}
		if ( config.containsKey( Key.atomicWrites ) ) {
			this.atomicWrites = BooleanCaster.cast( config.get( Key.atomicWrites ) );
		}
		if ( config.containsKey( Key.delay ) ) {
			this.delay = LongCaster.cast( config.get( Key.delay ) );
		}
		if ( config.containsKey( Key.errorThreshold ) ) {
			this.errorThreshold = IntegerCaster.cast( config.get( Key.errorThreshold ) );
		}
		if ( config.containsKey( Key.definitions ) ) {
			Object raw = config.get( Key.definitions );
			if ( raw instanceof IStruct castedStruct ) {
				this.definitions = castedStruct;
			} else {
				IStruct casted = StructCaster.cast( raw );
				if ( casted != null ) {
					this.definitions = casted;
				}
			}
		}
		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		return Struct.ofNonConcurrent(
		    Key.recursive, this.recursive,
		    Key.debounce, this.debounce,
		    Key.throttle, this.throttle,
		    Key.atomicWrites, this.atomicWrites,
		    Key.delay, this.delay,
		    Key.errorThreshold, this.errorThreshold,
		    Key.definitions, this.definitions
		);
	}

}
