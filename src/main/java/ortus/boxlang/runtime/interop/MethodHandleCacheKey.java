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
package ortus.boxlang.runtime.interop;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable cache key for the method handle cache in {@link DynamicInteropService}.
 *
 * Using a structured key class instead of a concatenated String avoids polluting the JVM-wide
 * string intern pool with dynamic runtime keys, and includes classloader identity so that
 * classes with the same name loaded by different classloaders (e.g. across BoxLang modules)
 * never collide in the cache.
 *
 * The {@code hashCode} is pre-computed at construction time so that every map lookup is O(1)
 * with no per-call hash computation cost.
 */
public final class MethodHandleCacheKey {

	/**
	 * The {@link System#identityHashCode} of the target class's classloader,
	 * or {@code 0} for the bootstrap classloader (null classloader).
	 */
	private final int		classLoaderIdentity;

	/**
	 * The fully-qualified binary name of the target class ({@link Class#getName()}).
	 */
	private final String	targetClassName;

	/**
	 * The name of the method being cached.
	 */
	private final String	methodName;

	/**
	 * {@link java.util.Arrays#hashCode} over the method's parameter type array.
	 */
	private final int		argumentTypesHash;

	/**
	 * Human-readable name of the classloader for diagnostic and observability purposes.
	 * Derived from {@link ClassLoader#getName()}; normalized to {@code "bootstrap"} when the
	 * classloader is null (bootstrap) and to {@code "unnamed"} when the name itself is null.
	 * This field is intentionally excluded from {@link #equals} and {@link #hashCode} — cache
	 * correctness is guaranteed by {@link #classLoaderIdentity} alone.
	 */
	private final String	classLoaderName;

	/**
	 * Pre-computed hash code for O(1) lookups in hash-based collections.
	 * Computed once in the constructor since this class is immutable.
	 */
	private final int		hashCode;

	/**
	 * The instant at which this cache key was created (i.e. when the method handle was first resolved
	 * and stored in the cache). Captured via {@link Instant#now()} in the constructor.
	 * Intentionally excluded from {@link #equals} and {@link #hashCode} so that cache lookups remain
	 * purely identity-based. Available for future LRU / TTL eviction strategies.
	 */
	private final Instant	createdAt;

	/**
	 * Constructs a new immutable method handle cache key.
	 *
	 * @param classLoaderIdentity {@link System#identityHashCode} of the class's classloader, {@code 0} for bootstrap
	 * @param classLoaderName     human-readable classloader name for diagnostics (e.g. module name); use {@code "bootstrap"} for null classloaders
	 * @param targetClassName     fully-qualified binary class name
	 * @param methodName          method name
	 * @param argumentTypesHash   {@code Arrays.hashCode} of the parameter type array
	 */
	public MethodHandleCacheKey( int classLoaderIdentity, String classLoaderName, String targetClassName, String methodName, int argumentTypesHash ) {
		this.classLoaderIdentity	= classLoaderIdentity;
		this.classLoaderName		= classLoaderName;
		this.targetClassName		= targetClassName;
		this.methodName				= methodName;
		this.argumentTypesHash		= argumentTypesHash;
		this.hashCode				= computeHashCode();
		this.createdAt				= Instant.now();
	}

	/**
	 * Returns the instant at which this cache key was created.
	 * Useful for LRU eviction, TTL expiry, and cache diagnostics.
	 * Not used in {@link #equals} or {@link #hashCode}.
	 */
	public Instant createdAt() {
		return this.createdAt;
	}

	/**
	 * Returns the classloader identity component.
	 */
	public int classLoaderIdentity() {
		return this.classLoaderIdentity;
	}

	/**
	 * Returns the human-readable classloader name, for use in diagnostics and cache key inspection.
	 * Not used in {@link #equals} or {@link #hashCode}.
	 */
	public String classLoaderName() {
		return this.classLoaderName;
	}

	/**
	 * Returns the fully-qualified target class name component.
	 */
	public String targetClassName() {
		return this.targetClassName;
	}

	/**
	 * Returns the method name component.
	 */
	public String methodName() {
		return this.methodName;
	}

	/**
	 * Returns the argument types hash component.
	 */
	public int argumentTypesHash() {
		return this.argumentTypesHash;
	}

	/**
	 * Computes the combined hash code across all four fields using the standard 31-multiplier pattern.
	 *
	 * @return computed hash code
	 */
	private int computeHashCode() {
		int result = 31;
		result	= 31 * result + this.classLoaderIdentity;
		result	= 31 * result + ( this.targetClassName == null ? 0 : this.targetClassName.hashCode() );
		result	= 31 * result + ( this.methodName == null ? 0 : this.methodName.hashCode() );
		result	= 31 * result + this.argumentTypesHash;
		return result;
	}

	/**
	 * Returns the pre-computed hash code. O(1) — no computation occurs here.
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Compares all four fields for equality, providing correct classloader-isolated cache lookups.
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( ! ( obj instanceof MethodHandleCacheKey other ) ) {
			return false;
		}
		return this.classLoaderIdentity == other.classLoaderIdentity
		    && this.argumentTypesHash == other.argumentTypesHash
		    && Objects.equals( this.methodName, other.methodName )
		    && Objects.equals( this.targetClassName, other.targetClassName );
	}

	/**
	 * Returns a human-readable representation useful for diagnostics and cache key inspection.
	 * Format: {@code classLoaderIdentity(classLoaderName)|targetClassName|methodName|argumentTypesHash|createdAt}
	 */
	@Override
	public String toString() {
		return this.classLoaderIdentity + "(" + this.classLoaderName + ")|" + this.targetClassName + "|" + this.methodName + "|" + this.argumentTypesHash + "|"
		    + this.createdAt;
	}

}
