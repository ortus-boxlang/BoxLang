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
package ortus.boxlang.compiler.prettyprint.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration options for method/dot chain formatting.
 */
public class ChainConfig {

	/**
	 * Number of chained method calls that triggers line breaking. If the chain
	 * has at least this many segments, each call is placed on its own line.
	 *
	 * <pre>
	 * // breakCount: 3 (default)
	 * // 3+ calls break:
	 * foo
	 *     .bar()
	 *     .baz()
	 *     .qux();
	 *
	 * // 2 calls stay flat:
	 * foo.bar().baz();
	 * </pre>
	 */
	@JsonProperty( "break_count" )
	private int		breakCount					= 3;

	/**
	 * Total character length of a method chain that triggers line breaking.
	 * If the flat-printed chain exceeds this length, it switches to multiline.
	 */
	@JsonProperty( "break_length" )
	private int		breakLength					= 60;

	/**
	 * Number of leading dot accesses in a method chain's receiver path to keep
	 * before breaking the remaining chain. A value of {@code 0} allows the entire
	 * receiver path to break, while {@code 1} keeps {@code variables.productions}
	 * together.
	 */
	@JsonProperty( "keep_receiver_count" )
	private int		keepReceiverCount			= 0;

	/**
	 * Prefer breaking a multi-method chain before breaking one of its argument
	 * lists solely because of the configured argument length threshold.
	 */
	@JsonProperty( "prefer_break_before_arguments" )
	private boolean	preferBreakBeforeArguments	= false;

	/** Default constructor. */
	public ChainConfig() {
	}

	/**
	 * Get the chain segment count threshold for line breaking.
	 *
	 * @return the break count threshold
	 */
	public int getBreakCount() {
		return breakCount;
	}

	/**
	 * Set the chain segment count threshold for line breaking.
	 *
	 * @param breakCount the break count threshold
	 *
	 * @return this config for chaining
	 */
	public ChainConfig setBreakCount( int breakCount ) {
		this.breakCount = breakCount;
		return this;
	}

	/**
	 * Get the total length threshold for line breaking.
	 *
	 * @return the break length threshold
	 */
	public int getBreakLength() {
		return breakLength;
	}

	/**
	 * Set the total length threshold for line breaking.
	 *
	 * @param breakLength the break length threshold
	 *
	 * @return this config for chaining
	 */
	public ChainConfig setBreakLength( int breakLength ) {
		this.breakLength = breakLength;
		return this;
	}

	/**
	 * Get the number of leading receiver-path dot accesses kept before a chain
	 * break.
	 *
	 * @return the number of receiver accesses to keep
	 */
	public int getKeepReceiverCount() {
		return keepReceiverCount;
	}

	/**
	 * Set the number of leading receiver-path dot accesses kept before a chain
	 * break.
	 *
	 * @param keepReceiverCount the number of receiver accesses to keep
	 *
	 * @return this config for chaining
	 */
	public ChainConfig setKeepReceiverCount( int keepReceiverCount ) {
		this.keepReceiverCount = Math.max( 0, keepReceiverCount );
		return this;
	}

	/**
	 * Get whether method chains should break before length-driven argument lists.
	 *
	 * @return true when chain breaks take precedence
	 */
	public boolean getPreferBreakBeforeArguments() {
		return preferBreakBeforeArguments;
	}

	/**
	 * Set whether method chains should break before length-driven argument lists.
	 *
	 * @param preferBreakBeforeArguments true to prefer breaking the chain
	 *
	 * @return this config for chaining
	 */
	public ChainConfig setPreferBreakBeforeArguments( boolean preferBreakBeforeArguments ) {
		this.preferBreakBeforeArguments = preferBreakBeforeArguments;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "break_count", breakCount );
		map.put( "break_length", breakLength );
		map.put( "keep_receiver_count", keepReceiverCount );
		map.put( "prefer_break_before_arguments", preferBreakBeforeArguments );
		return map;
	}

	/**
	 * Create a deep copy of this configuration.
	 *
	 * @return a new ChainConfig with the same settings
	 */
	public ChainConfig clone() {
		ChainConfig clone = new ChainConfig();
		clone.breakCount					= this.breakCount;
		clone.breakLength					= this.breakLength;
		clone.keepReceiverCount				= this.keepReceiverCount;
		clone.preferBreakBeforeArguments	= this.preferBreakBeforeArguments;
		return clone;
	}
}
