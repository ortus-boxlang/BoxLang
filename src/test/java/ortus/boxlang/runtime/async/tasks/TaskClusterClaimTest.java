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
 * Tests for claim arbitration: two simulated nodes, one shared cache, one occurrence.
 * <p>
 * Each "node" is a separate {@link ClusterService} with its own node id but the same cluster name, so
 * they see each other and compete for the same task key - which is exactly the situation two real
 * servers are in.
 */
class TaskClusterClaimTest {

	static BoxRuntime		runtime;
	static ICacheProvider	cache;

	@BeforeAll
	static void setUp() {
		runtime	= BoxRuntime.getInstance( true );
		cache	= runtime.getCacheService().getDefaultCache();
	}

	/**
	 * Build a node: same cluster name, its own node id, fast claim timings.
	 */
	private ClusterService node( String clusterName, String nodeId ) {
		ClusterConfig config = new ClusterConfig();
		config.enabled			= true;
		config.name				= clusterName;
		config.nodeId			= nodeId;
		// Short heartbeat: the claim heartbeat follows it, so these tests do not wait 5s per tick
		config.heartbeatSeconds	= 3;
		return new ClusterService( runtime, config, cache );
	}

	/**
	 * A task that participates in clustering.
	 */
	private ScheduledTask task() {
		return new ScheduledTask( "claim-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).onOneServer();
	}

	private String occurrenceOf( ScheduledTask task ) {
		return occurrenceOf( task, Instant.now() );
	}

	private String occurrenceOf( ScheduledTask task, Instant due ) {
		return TaskClusterClaim.occurrenceId( task, due );
	}

	@DisplayName( "each occurrence has its own key, so an older occurrence cannot displace a newer one" )
	@Test
	void testOccurrencesHaveIndependentKeys() {
		// The bug this design removes: with one slot per task, a node evaluating a bucket that was already
		// behind could overwrite a newer occurrence's live claim, deleting that newer occurrence's guard so the
		// next node to evaluate it ran it a second time. Per-occurrence keys make that impossible - there is
		// nothing to overwrite.
		String					clusterName	= "claim-displace-" + UUID.randomUUID();
		ClusterService			one			= node( clusterName, "nodeOne" );
		ClusterService			two			= node( clusterName, "nodeTwo" );
		// overlap=run so the two occurrences may legitimately be in flight together; the point of this test is
		// that their keys are independent, not the overlap policy.
		ScheduledTask			task		= task().setOverlap( "run" );

		Instant					now			= Instant.now();
		TaskClusterClaim.Claim	newer		= TaskClusterClaim.attempt( task, now, one );
		assertThat( newer ).isNotNull();

		// A different (older) occurrence belongs to a different key, and is free to be claimed separately
		Instant					behind	= now.minusSeconds( 10 );
		TaskClusterClaim.Claim	older	= TaskClusterClaim.attempt( task, behind, two );
		assertThat( older ).isNotNull();
		assertThat( older.cacheKey() ).isNotEqualTo( newer.cacheKey() );

		// Both records survive independently - claiming the older one did not touch the newer one
		IStruct newerRecord = ( IStruct ) cache.get( newer.cacheKey() ).orElse( null );
		assertThat( newerRecord ).isNotNull();
		assertThat( newerRecord.getAsString( Key.nodeId ) ).isEqualTo( "nodeOne" );
		assertThat( newerRecord.getAsString( Key.status ) ).isEqualTo( "running" );
		assertThat( newerRecord.getAsString( Key.occurrenceId ) ).isEqualTo( TaskClusterClaim.occurrenceId( task, now ) );

		// And neither occurrence may be claimed again by anyone
		assertThat( TaskClusterClaim.attempt( task, now, two ) ).isNull();
		assertThat( TaskClusterClaim.attempt( task, behind, one ) ).isNull();

		newer.finish( "success", null );
		older.finish( "success", null );
	}

	@DisplayName( "an unverified candidate claim blocks another node just like a running one" )
	@Test
	void testCandidateClaimBlocksOtherNodes() {
		// "claimed" is written before verification and promoted to "running" after it. It is still a claim
		// in progress, so another node must not treat it as an empty slot.
		String			clusterName	= "claim-candidate-" + UUID.randomUUID();
		ClusterService	one			= node( clusterName, "nodeOne" );
		ClusterService	two			= node( clusterName, "nodeTwo" );
		ScheduledTask	task		= task();
		Instant			now			= Instant.now();
		String			occurrence	= TaskClusterClaim.occurrenceId( task, now );
		String			key			= TaskClusterClaim.buildTaskKey( one, task, occurrence );

		cache.set( key, Struct.ofNonConcurrent(
		    Key.claimId, UUID.randomUUID().toString(),
		    Key.occurrenceId, occurrence,
		    Key.nodeId, "nodeOne",
		    Key.status, "claimed",
		    Key.claimedAt, Instant.now(),
		    Key.heartbeatAt, Instant.now()
		), 120 );

		assertThat( TaskClusterClaim.attempt( task, now, two ) ).isNull();
	}

	@DisplayName( "a clustered cron task is measured by its own shortest interval, not its poll cadence" )
	@Test
	void testCronTaskIsNotPeriodBound() {
		// A cron task polls every second (or minute), which is its poll cadence, not its occurrence cadence. Reading
		// that as the period rejected every clustered cron task. The interval is derived from the expression instead,
		// so an every-second cron is still refused and a sane one is accepted.
		ClusterService	cluster		= node( "overlap-cron-" + UUID.randomUUID(), "nodeOne" );

		// Every second: cannot service a claim, so it is effectively a 1s task
		ScheduledTask	everySecond	= new ScheduledTask( "cron-fast-" + UUID.randomUUID() ).cron( "* * * * * *" ).setCluster( true );
		assertThat( TaskClusterClaim.periodSeconds( everySecond ) ).isEqualTo( 1L );
		assertThat( TaskClusterClaim.meetsMinimumPeriod( everySecond, cluster.getConfig() ) ).isFalse();

		// Every five seconds: below the eager floor of 4s? No - 5 >= 4, so it is accepted
		ScheduledTask everyFive = new ScheduledTask( "cron-5-" + UUID.randomUUID() ).cron( "*/5 * * * * *" ).setCluster( true );
		assertThat( TaskClusterClaim.periodSeconds( everyFive ) ).isEqualTo( 5L );
		assertThat( TaskClusterClaim.attempt( everyFive, Instant.now(), cluster ) ).isNotNull();
	}

	@DisplayName( "one node claims an occurrence and the other is refused" )
	@Test
	void testOnlyOneNodeClaims() {
		String					clusterName	= "claim-race-" + UUID.randomUUID();
		ClusterService			one			= node( clusterName, "nodeOne" );
		ClusterService			two			= node( clusterName, "nodeTwo" );
		ScheduledTask			task		= task();
		Instant					due			= Instant.now();

		TaskClusterClaim.Claim	first		= TaskClusterClaim.attempt( task, due, one );
		assertThat( first ).isNotNull();
		assertThat( first.lostClaim() ).isFalse();

		TaskClusterClaim.Claim second = TaskClusterClaim.attempt( task, due, two );
		assertThat( second ).isNull();
	}

	@DisplayName( "the winner keeps the claim, and the loser keeps losing" )
	@Test
	void testWinnerKeepsWinning() {
		String					clusterName	= "claim-sticky-" + UUID.randomUUID();
		ClusterService			one			= node( clusterName, "nodeOne" );
		ClusterService			two			= node( clusterName, "nodeTwo" );
		ScheduledTask			task		= task();
		Instant					due			= Instant.now();

		TaskClusterClaim.Claim	winner		= TaskClusterClaim.attempt( task, due, one );
		assertThat( winner ).isNotNull();

		for ( int i = 0; i < 3; i++ ) {
			assertThat( TaskClusterClaim.attempt( task, due, one ) ).isNull();
			assertThat( TaskClusterClaim.attempt( task, due, two ) ).isNull();
		}
	}

	@DisplayName( "a finished occurrence is never claimed again" )
	@Test
	void testCompletedOccurrenceIsNotReclaimed() {
		String					clusterName	= "claim-done-" + UUID.randomUUID();
		ClusterService			one			= node( clusterName, "nodeOne" );
		ClusterService			two			= node( clusterName, "nodeTwo" );
		ScheduledTask			task		= task();
		Instant					due			= Instant.now();

		TaskClusterClaim.Claim	claim		= TaskClusterClaim.attempt( task, due, one );
		assertThat( claim ).isNotNull();
		claim.finish( "success", null );

		// Neither node may run this occurrence again
		assertThat( TaskClusterClaim.attempt( task, due, two ) ).isNull();
		assertThat( TaskClusterClaim.attempt( task, due, one ) ).isNull();
	}

	@DisplayName( "the stored claim records which node owns which occurrence" )
	@Test
	void testClaimRecordContents() {
		String					clusterName	= "claim-record-" + UUID.randomUUID();
		ClusterService			one			= node( clusterName, "nodeOne" );
		ScheduledTask			task		= task();
		Instant					due			= Instant.now();

		TaskClusterClaim.Claim	claim		= TaskClusterClaim.attempt( task, due, one );
		assertThat( claim ).isNotNull();

		IStruct record = ( IStruct ) cache.get( claim.cacheKey() ).orElse( null );
		assertThat( record ).isNotNull();
		assertThat( record.getAsString( Key.nodeId ) ).isEqualTo( "nodeOne" );
		assertThat( record.getAsString( Key.status ) ).isEqualTo( "running" );
		assertThat( record.getAsString( Key.occurrenceId ) ).isEqualTo( occurrenceOf( task, due ) );
		assertThat( record.get( Key.claimId ) ).isNotNull();
	}

	@DisplayName( "a claim lost mid-run is detected and reported" )
	@Test
	void testLostClaimIsDetected() throws Exception {
		String					clusterName	= "claim-lost-" + UUID.randomUUID();
		ClusterService			one			= node( clusterName, "nodeOne" );
		ClusterService			two			= node( clusterName, "nodeTwo" );
		ScheduledTask			task		= task();

		TaskClusterClaim.Claim	claim		= TaskClusterClaim.attempt( task, Instant.now(), one );
		assertThat( claim ).isNotNull();
		claim.startHeartbeat();

		// Simulate the residual race: another node's claim lands after the winner verified
		String	key		= claim.cacheKey();
		IStruct	stolen	= ortus.boxlang.runtime.types.Struct.ofNonConcurrent(
		    Key.claimId, UUID.randomUUID().toString(),
		    Key.occurrenceId, occurrenceOf( task ),
		    Key.nodeId, "nodeTwo",
		    Key.status, "running",
		    Key.claimedAt, Instant.now(),
		    Key.heartbeatAt, Instant.now()
		);
		cache.set( key, stolen, 60 );

		// The heartbeat notices on its next tick
		long deadline = System.currentTimeMillis() + 5_000;
		while ( !claim.lostClaim() && System.currentTimeMillis() < deadline ) {
			Thread.sleep( 100 );
		}

		assertThat( claim.lostClaim() ).isTrue();

		// And finishing must not overwrite the new owner's record
		claim.finish( "success", null );
		IStruct after = ( IStruct ) cache.get( key ).orElse( null );
		assertThat( after.getAsString( Key.nodeId ) ).isEqualTo( "nodeTwo" );
	}

	@DisplayName( "finishing a claim records the outcome, and the record stays fenced" )
	@Test
	void testFinishRecordsOutcome() {
		String					clusterName	= "claim-finish-" + UUID.randomUUID();
		ClusterService			one			= node( clusterName, "nodeOne" );
		ScheduledTask			task		= task();

		TaskClusterClaim.Claim	claim		= TaskClusterClaim.attempt( task, Instant.now(), one );
		assertThat( claim ).isNotNull();

		claim.finish( "failed", "boom" );

		IStruct record = ( IStruct ) cache.get( claim.cacheKey() ).orElse( null );
		assertThat( record.getAsString( Key.status ) ).isEqualTo( "failed" );
		assertThat( record.getAsString( Key.nodeId ) ).isEqualTo( "nodeOne" );
		assertThat( record.get( Key.finishedAt ) ).isNotNull();
	}

	@DisplayName( "an unclustered task never touches the cache" )
	@Test
	void testUnclusteredTaskDoesNotClaim() {
		ClusterService	one			= node( "claim-unclustered-" + UUID.randomUUID(), "nodeOne" );
		ScheduledTask	unclustered	= new ScheduledTask( "plain-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS ).setCluster( false );

		assertThat( TaskClusterClaim.attempt( unclustered, Instant.now(), one ) ).isNull();
		assertThat( cache.get( TaskClusterClaim.buildTaskKey( one, unclustered ) ).isPresent() ).isFalse();
	}

	@DisplayName( "a task that inherits and a disabled deployment does not cluster" )
	@Test
	void testInheritanceFromDisabledDeployment() {
		ClusterConfig config = new ClusterConfig();
		config.enabled	= false;
		config.nodeId	= "n1";
		config.name		= "inherit-" + UUID.randomUUID();
		ClusterService	cluster		= new ClusterService( runtime, config, cache );

		ScheduledTask	inheriting	= new ScheduledTask( "inherit-" + UUID.randomUUID() ).every( 10, TimeUnit.SECONDS );
		assertThat( TaskClusterClaim.isClustered( inheriting, cluster ) ).isFalse();

		inheriting.setCluster( true );
		assertThat( TaskClusterClaim.isClustered( inheriting, cluster ) ).isTrue();
	}
}
