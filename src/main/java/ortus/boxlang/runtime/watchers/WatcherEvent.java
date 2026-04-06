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
package ortus.boxlang.runtime.watchers;

import java.nio.file.Path;
import java.time.Instant;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Immutable value object representing a single filesystem event delivered by a {@link WatcherInstance}.
 * <p>
 * Converted to an {@link IStruct} via {@link #toStruct()} for consumption by BoxLang listeners.
 * </p>
 */
public class WatcherEvent {

	/**
	 * The kind of filesystem event.
	 */
	public enum Kind {
		/** A new file or directory was created. */
		CREATED,
		/** An existing file or directory was modified. */
		MODIFIED,
		/** A file or directory was deleted. */
		DELETED,
		/** The watch service overflowed; some events may have been lost. */
		OVERFLOW
	}

	private final Kind		kind;

	/** Absolute path of the affected file or directory. Null for OVERFLOW events. */
	private final Path		path;

	/** Path relative to the watched root. Null for OVERFLOW events. */
	private final Path		relativePath;

	/** The root path that was registered with the watch service. Null for OVERFLOW events. */
	private final Path		watchRoot;

	private final Instant	timestamp;

	/**
	 * Construct a normal (non-OVERFLOW) event.
	 *
	 * @param kind         the event kind
	 * @param path         absolute path of the affected file
	 * @param relativePath path relative to the watch root
	 * @param watchRoot    the registered watch root
	 * @param timestamp    when the event was detected
	 */
	public WatcherEvent( Kind kind, Path path, Path relativePath, Path watchRoot, Instant timestamp ) {
		this.kind			= kind;
		this.path			= path;
		this.relativePath	= relativePath;
		this.watchRoot		= watchRoot;
		this.timestamp		= timestamp;
	}

	/**
	 * Convenience constructor for OVERFLOW events (no path context).
	 *
	 * @param timestamp when the overflow was detected
	 */
	public WatcherEvent( Instant timestamp ) {
		this( Kind.OVERFLOW, null, null, null, timestamp );
	}

	public Kind getKind() {
		return kind;
	}

	public Path getPath() {
		return path;
	}

	public Path getRelativePath() {
		return relativePath;
	}

	public Path getWatchRoot() {
		return watchRoot;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Convert this event to a BoxLang {@link IStruct} for use in listener callbacks.
	 *
	 * @return an IStruct representation of this event
	 */
	public IStruct toStruct() {
		return Struct.ofNonConcurrent(
		    Key.kind, kind.name().toLowerCase(),
		    Key.path, path != null ? path.toString() : "",
		    Key.relativePath, relativePath != null ? relativePath.toString() : "",
		    Key.watchRoot, watchRoot != null ? watchRoot.toString() : "",
		    Key.timestamp, timestamp.toString()
		);
	}

	@Override
	public String toString() {
		return "WatcherEvent{kind=" + kind + ", path=" + path + ", timestamp=" + timestamp + "}";
	}

}
