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
package ortus.boxlang.runtime.config.segments;

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.config.util.PropertyHelper;

/**
 * The cluster service configuration: how this node identifies itself, which cluster it belongs to, and which
 * shared cache it coordinates through.
 * <p>
 * Everything here is an operator decision and is therefore in {@code boxlang.json}. Nothing else is: how
 * occurrences are shared out is a scheduler policy, and the claim budget, delays, load window and snapshot
 * timeouts are constants in the classes that use them, because they are how the algorithm works rather than
 * settings.
 */
public class ClusterConfig implements IConfigSegment {

	/**
	 * Whether clustering is on for this node.
	 */
	public boolean	enabled				= false;

	/**
	 * This node's identity. Null means not set: the cluster service then derives "{hostname}-{pid}".
	 */
	public String	nodeId				= null;

	/**
	 * The cluster name. Nodes with the same name see each other; different names never do.
	 */
	public String	name				= "default";

	/**
	 * The cache used for coordination. It must be backed by a store every node can see.
	 */
	public String	cacheName			= "bxcluster";

	/**
	 * How often this node rewrites its record, attributes included.
	 */
	public long		heartbeatSeconds	= 5;

	/**
	 * @inheritDoc
	 */
	@Override
	public IConfigSegment process( IStruct config ) {
		this.enabled	= BooleanCaster.cast( config.getOrDefault( Key.enabled, this.enabled ) );
		this.cacheName	= PropertyHelper.processString( config, Key.cacheName, this.cacheName );
		this.name		= PropertyHelper.processString( config, Key._name, this.name );
		// null means not set, and is a legitimate value here - do not string-resolve it
		Object rawNodeId = config.get( Key.nodeId );
		this.nodeId				= rawNodeId == null ? null : PropertyHelper.processString( config, Key.nodeId, this.nodeId );
		this.heartbeatSeconds	= IntegerCaster.cast( config.getOrDefault( Key.heartbeatSeconds, this.heartbeatSeconds ) );

		// Null means not set. A blank value is not an identity, so it collapses to the same thing.
		if ( this.nodeId != null && this.nodeId.isBlank() ) {
			this.nodeId = null;
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		return Struct.ofNonConcurrent(
		    Key.enabled, this.enabled,
		    Key.nodeId, this.nodeId,
		    Key._name, this.name,
		    Key.cacheName, this.cacheName,
		    Key.heartbeatSeconds, this.heartbeatSeconds
		);
	}

	/**
	 * The default identity: hostname plus pid, which is unique per process.
	 *
	 * @return The default node id
	 */
	public static String defaultNodeId() {
		return ortus.boxlang.runtime.net.NetworkUtil.getLocalHostname() + "-" + ProcessHandle.current().pid();
	}

}
