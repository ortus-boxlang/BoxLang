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
package ortus.boxlang.runtime.async.tasks;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.config.segments.ClusterConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ClusterService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Overlap policy and minimum-period enforcement.
 * <p>
 * Overlap is what stops a slowly-running occurrence from starting a second copy of itself, and the
 * minimum period is what stops a clustered task being configured too fast for the claim protocol to
 * service. Both are policy, so both are tested here rather than in the cluster service.
 */
class TaskOverlapAndFloorTest {

	static BoxRuntime		runtime;
	static ICacheProvider	cache;

	@BeforeAll
	static void setUp() {
		runtime	= BoxRuntime.getInstance( true );
		cache	= runtime.getCacheService().getDefaultCache();
	}

	private ClusterService node( String clusterName, String nodeId, long settleMillis ) {
		ClusterConfig config = new ClusterConfig();
		config.enabled	= true;
		config.name		= clusterName;
		config.nodeId	= nodeId;
		return new ClusterService( runtime, config, cache );
	}

	@DisplayName( "overlap=skip refuses a new occurrence while the previous one is live" )
	@Test
	void testSkipOverlap() {
		ClusterService			cluster		= node( "overlap-skip-" + UUID.randomUUID(), "nodeOne", 1 );
		ScheduledTask			task		= new ScheduledTask( "overlap-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( true )
		    .setOverlap( "skip" );

		Instant					firstDue	= Instant.now();
		Instant					nextDue		= firstDue.plusSeconds( 10 );

		// Claim the first occurrence and leave it running
		TaskClusterClaim.Claim	first		= TaskClusterClaim.attempt( task, firstDue, cluster );
		assertThat( first ).isNotNull();

		// The next occurrence must be dropped, not queued and not run
		assertThat( TaskClusterClaim.attempt( task, nextDue, cluster ) ).isNull();
	}

	@DisplayName( "overlap=run allows the next occurrence to proceed" )
	@Test
	void testRunOverlap() {
		ClusterService			cluster		= node( "overlap-run-" + UUID.randomUUID(), "nodeOne", 1 );
		ScheduledTask			task		= new ScheduledTask( "overlap-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( true )
		    .setOverlap( "run" );

		Instant					firstDue	= Instant.now();
		Instant					nextDue		= firstDue.plusSeconds( 10 );

		TaskClusterClaim.Claim	first		= TaskClusterClaim.attempt( task, firstDue, cluster );
		assertThat( first ).isNotNull();

		// With "run" a long job may overlap its own next occurrence
		TaskClusterClaim.Claim second = TaskClusterClaim.attempt( task, nextDue, cluster );
		assertThat( second ).isNotNull();
	}

	@DisplayName( "the overlap policy is read from the task, then the cluster default" )
	@Test
	void testEffectiveOverlapResolution() {
		ClusterService	cluster	= node( "overlap-default-" + UUID.randomUUID(), "nodeOne", 1 );
		ScheduledTask	task	= new ScheduledTask( "overlap-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS );

		// The scheduler default applies when the task says nothing
		ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.taskOverlapPolicy = "run";
		assertThat( TaskClusterClaim.effectiveOverlap( task, cluster ) ).isEqualTo( "run" );
		ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.taskOverlapPolicy = "skip";

		// The task's own setting wins
		task.setOverlap( "skip" );
		assertThat( TaskClusterClaim.effectiveOverlap( task, cluster ) ).isEqualTo( "skip" );
	}

	@DisplayName( "a previous occurrence that is still running is left alone, not reclaimed" )
	@Test
	void testLiveOccurrenceIsNotReclaimed() {
		ClusterService	cluster		= node( "overlap-live-" + UUID.randomUUID(), "nodeOne", 1 );
		ClusterService	other		= node( cluster.getClusterName(), "nodeTwo", 1 );
		ScheduledTask	task		= new ScheduledTask( "overlap-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( true );

		Instant			firstDue	= Instant.now();
		assertThat( TaskClusterClaim.attempt( task, firstDue, cluster ) ).isNotNull();

		// A different node must not be able to take over a live occurrence
		assertThat( TaskClusterClaim.attempt( task, firstDue, other ) ).isNull();
	}

	@DisplayName( "the minimum period floor scales with settle rounds and the claim delay" )
	@Test
	void testFloorMath() {
		ClusterConfig config = new ClusterConfig();

		// 2 x 1000 settle + 1000 eager delay + 1000 margin = 4s
		assertThat( TaskClusterClaim.minimumPeriodSeconds() ).isEqualTo( 4 );

		// balanced adds the 5000 pushback: 2 x 1000 + 5000 + 1000 = 8s
		ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode = "balanced";
		try {
			assertThat( TaskClusterClaim.minimumPeriodSeconds() ).isEqualTo( 8 );
		} finally {
			ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode = "eager";
		}
	}

	@DisplayName( "a cron task has no period floor, because it is polled rather than period bound" )
	@Test
	void testCronHasNoFloor() {
		ClusterConfig	config	= new ClusterConfig();
		ScheduledTask	cron	= new ScheduledTask( "cron-" + UUID.randomUUID() ).cron( "*/1 * * * *" );

		// A cron task is polled, not period bound. Its getPeriodMillis() is the poll cadence (a second for a
		// second-level cron), which is not the occurrence cadence - reporting that as the period made every
		// clustered cron task look like a 1s task and rejected it against the minimum period floor.
		assertThat( TaskClusterClaim.periodSeconds( cron ) ).isEqualTo( 0L );
	}

	@DisplayName( "a task with no fixed period reports a zero period" )
	@Test
	void testOneShotHasNoPeriod() {
		ScheduledTask oneShot = new ScheduledTask( "once-" + UUID.randomUUID() );
		assertThat( TaskClusterClaim.periodSeconds( oneShot ) ).isEqualTo( 0L );
	}

	@DisplayName( "the record keeps the occurrence id of the run that finished" )
	@Test
	void testFinishKeepsOccurrence() {
		ClusterService			cluster	= node( "overlap-finish-" + UUID.randomUUID(), "nodeOne", 1 );
		ScheduledTask			task	= new ScheduledTask( "overlap-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( true );
		Instant					due		= Instant.now();

		TaskClusterClaim.Claim	claim	= TaskClusterClaim.attempt( task, due, cluster );
		assertThat( claim ).isNotNull();
		claim.finish( "success", null );

		IStruct record = ( IStruct ) cache.get( TaskClusterClaim.buildTaskKey( cluster, task ) ).orElse( null );
		assertThat( record.get( Key.occurrenceId ) ).isEqualTo( TaskClusterClaim.occurrenceId( task, due ) );
		assertThat( record.get( Key.status ) ).isEqualTo( "success" );
	}

	@DisplayName( "a stale running occurrence is reported abandoned and never re-run" )
	@Test
	void testStaleRunningIsNotRerun() {
		ClusterService	cluster	= node( "overlap-stale-" + UUID.randomUUID(), "nodeOne", 1 );
		ScheduledTask	task	= new ScheduledTask( "overlap-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( true );
		Instant			due		= Instant.now();
		String			key		= TaskClusterClaim.buildTaskKey( cluster, task );

		// A node claimed it and then stopped heartbeating
		cache.set( key, Struct.ofNonConcurrent(
		    Key.claimId, UUID.randomUUID().toString(),
		    Key.occurrenceId, TaskClusterClaim.occurrenceId( task, due ),
		    Key.nodeId, "deadNode",
		    Key.status, "running",
		    Key.claimedAt, Instant.now().minusSeconds( 120 ),
		    Key.heartbeatAt, Instant.now().minusSeconds( 120 )
		), 600 );

		// Stale, but still never re-run: that would be a second execution of the same occurrence
		assertThat( TaskClusterClaim.attempt( task, due, cluster ) ).isNull();

		// And the record still names the dead node, untouched
		IStruct record = ( IStruct ) cache.get( key ).orElse( null );
		assertThat( record.getAsString( Key.nodeId ) ).isEqualTo( "deadNode" );
		assertThat( record.getAsString( Key.status ) ).isEqualTo( "running" );
	}

	@DisplayName( "balanced mode ranks nodes: the least busy waits 0, the most busy the full pushback" )
	@Test
	void testBalancedRanking() {
		String			clusterName	= "rank-" + UUID.randomUUID();
		ClusterService	me			= node( clusterName, "me", 1 );
		ClusterConfig	config		= me.getConfig();

		ScheduledTask	task		= new ScheduledTask( "rank-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( true );

		// Publish two peers into the shared cluster: one idle, one very busy
		writePeerRecord( clusterName, "idlePeer", 0 );
		writePeerRecord( clusterName, "busyPeer", 500 );
		writeMemberList( clusterName, "me", "idlePeer", "busyPeer" );
		me.invalidateSnapshot();

		// Sanity: both peers are visible to this node
		assertThat( me.getPeers().keySet().stream().map( Key::getName ).toList() ).containsAtLeast( "idlePeer", "busyPeer" );

		// This node reports 100 task-seconds, so it sits between the two peers: 1 of 2 ranks
		me.setAttribute( "scheduler.taskSeconds", 100 );

		ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode = "balanced";
		try {
			// rank 1 of 2 -> half the pushback, plus tie jitter
			long delay = TaskClusterClaim.computeDelayMillis( task, me, config );
			assertThat( delay ).isAtLeast( TaskClusterClaim.MAX_PUSHBACK_MILLIS / 2 );
			assertThat( delay ).isAtMost( ( TaskClusterClaim.MAX_PUSHBACK_MILLIS / 2 ) + TaskClusterClaim.TIE_JITTER_MILLIS );
		} finally {
			ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode = "eager";
		}
	}

	@DisplayName( "the least busy node in the cluster waits zero pushback" )
	@Test
	void testLeastBusyWaitsZero() {
		String			clusterName	= "rank-low-" + UUID.randomUUID();
		ClusterService	me			= node( clusterName, "me", 1 );
		ClusterConfig	config		= me.getConfig();

		ScheduledTask	task		= new ScheduledTask( "rank-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( true );

		writePeerRecord( clusterName, "busyPeer", 900 );
		writeMemberList( clusterName, "me", "busyPeer" );
		me.invalidateSnapshot();
		me.setAttribute( "scheduler.taskSeconds", 0 );

		ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode = "balanced";
		try {
			// Least busy waits no pushback at all - only the tie jitter, so equally loaded nodes break step
			assertThat( TaskClusterClaim.computeDelayMillis( task, me, config ) ).isAtMost( TaskClusterClaim.TIE_JITTER_MILLIS );
		} finally {
			ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode = "eager";
		}
	}

	/**
	 * Write a peer node record into the shared cache so it shows up in a snapshot refresh.
	 */
	private void writePeerRecord( String clusterName, String nodeId, long taskSeconds ) {
		IStruct	attributes	= Struct.ofNonConcurrent(
		    Key.scheduler, Struct.ofNonConcurrent( Key.taskSeconds, taskSeconds )
		);
		IStruct	record		= Struct.ofNonConcurrent(
		    Key.nodeId, nodeId,
		    Key.hostname, "peer-host",
		    Key.ip, "127.0.0.1",
		    Key.startedAt, Instant.now(),
		    Key.heartbeatAt, Instant.now(),
		    Key.attributesUpdatedAt, Instant.now(),
		    Key.attributes, attributes
		);
		cache.set( "bxcluster:" + clusterName + ":node:" + nodeId, record, 600 );
	}

	/**
	 * Write the shared member list once, naming every node in the cluster.
	 */
	private void writeMemberList( String clusterName, String... nodeIds ) {
		cache.set( "bxcluster:" + clusterName + ":nodes", String.join( System.lineSeparator(), nodeIds ), 600 );
	}

	@DisplayName( "an abandoned occurrence is not restarted by anyone" )
	@Test
	void testAbandonedOccurrenceIsNotRerun() {
		ClusterService	cluster	= node( "overlap-abandoned-" + UUID.randomUUID(), "nodeOne", 1 );
		ScheduledTask	task	= new ScheduledTask( "overlap-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( true );
		Instant			due		= Instant.now();
		String			key		= TaskClusterClaim.buildTaskKey( cluster, task );

		// Simulate a node that claimed and died: the record says abandoned
		cache.set( key, Struct.ofNonConcurrent(
		    Key.claimId, UUID.randomUUID().toString(),
		    Key.occurrenceId, TaskClusterClaim.occurrenceId( task, due ),
		    Key.nodeId, "deadNode",
		    Key.status, "abandoned",
		    Key.claimedAt, Instant.now().minusSeconds( 60 ),
		    Key.heartbeatAt, Instant.now().minusSeconds( 60 )
		), 600 );

		// Nobody may re-run it
		assertThat( TaskClusterClaim.attempt( task, due, cluster ) ).isNull();
	}
}
