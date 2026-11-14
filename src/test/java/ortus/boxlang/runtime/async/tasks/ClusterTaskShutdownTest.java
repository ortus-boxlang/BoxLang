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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.config.segments.ClusterConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ClusterService;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Shutdown behaviour, both for queued tasks and for a clustered run that is cut short.
 * <p>
 * Two contracts matter here:
 * <ol>
 * <li>A graceful shutdown <b>waits</b> for tasks already running, up to its timeout, before anything
 * is interrupted - so a short task finishes normally.</li>
 * <li>If the run is interrupted anyway (forced shutdown or timeout expiry), the cluster claim is left
 * in a <b>terminal</b> state. It must never look completed, and it must never be re-run.</li>
 * </ol>
 */
class ClusterTaskShutdownTest {

	static BoxRuntime		runtime;
	static ICacheProvider	cache;

	@BeforeAll
	static void setUp() {
		runtime	= BoxRuntime.getInstance( true );
		cache	= runtime.getCacheService().getDefaultCache();
	}

	/**
	 * Wait for a boolean flag to be set, up to a wall-clock bound.
	 */
	private boolean awaitFlag( AtomicBoolean flag, long timeoutMillis ) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while ( !flag.get() && System.currentTimeMillis() < deadline ) {
			Thread.sleep( 10 );
		}
		return flag.get();
	}

	private ClusterService node( String clusterName ) {
		ClusterConfig config = new ClusterConfig();
		config.enabled	= true;
		config.name		= clusterName;
		config.nodeId	= "shutdown-node";
		return new ClusterService( runtime, config, cache );
	}

	@DisplayName( "a shared-executor scheduler interrupts its own overrunning task at the timeout" )
	@Test
	void testSharedExecutorShutdownInterruptsOverrunningTask() throws Exception {
		BaseScheduler	scheduler	= new BaseScheduler( "shared-interrupt-" + UUID.randomUUID() )
		    .useGlobalExecutor( "scheduled-tasks" );
		CountDownLatch	started		= new CountDownLatch( 1 );
		CountDownLatch	finish		= new CountDownLatch( 1 );
		AtomicBoolean	interrupted	= new AtomicBoolean( false );

		scheduler.task( "overrunning-task" )
		    .call( () -> {
			    started.countDown();
			    try {
				    // Deliberately longer than the shutdown timeout
				    finish.await( 20, TimeUnit.SECONDS );
			    } catch ( InterruptedException e ) {
				    interrupted.set( true );
				    Thread.currentThread().interrupt();
			    }
		    } )
		    .delay( 1, TimeUnit.HOURS ).every( 1, TimeUnit.HOURS );

		scheduler.startup();
		try {
			Thread overrun = new Thread( () -> scheduler.getTaskRecord( "overrunning-task" ).task.run( true ), "manual-overrun" );
			overrun.setDaemon( true );
			overrun.start();
			assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();

			long start = System.currentTimeMillis();
			// Graceful, with a timeout far shorter than the task: wait, then interrupt
			scheduler.shutdown( false, 1 );
			long elapsedMs = System.currentTimeMillis() - start;

			// The run was actually interrupted, not merely abandoned. Forced shutdown does not wait for
			// the interrupted run to unwind, so give the handler a moment.
			assertThat( awaitFlag( interrupted, 5_000 ) ).isTrue();
			// And shutdown did not hang waiting for a task that would never finish
			assertThat( elapsedMs ).isLessThan( 15_000L );
		} finally {
			finish.countDown();
			if ( scheduler.hasStarted() ) {
				scheduler.shutdown( true, 5 );
			}
		}
	}

	@DisplayName( "a forced scheduler shutdown interrupts an in-flight run immediately" )
	@Test
	void testForcedSchedulerShutdownInterrupts() throws Exception {
		BaseScheduler	scheduler	= new BaseScheduler( "forced-interrupt-" + UUID.randomUUID() )
		    .useGlobalExecutor( "scheduled-tasks" );
		CountDownLatch	started		= new CountDownLatch( 1 );
		CountDownLatch	finish		= new CountDownLatch( 1 );
		AtomicBoolean	interrupted	= new AtomicBoolean( false );

		scheduler.task( "forced-task" )
		    .call( () -> {
			    started.countDown();
			    try {
				    finish.await( 20, TimeUnit.SECONDS );
			    } catch ( InterruptedException e ) {
				    interrupted.set( true );
				    Thread.currentThread().interrupt();
			    }
		    } )
		    .delay( 1, TimeUnit.HOURS ).every( 1, TimeUnit.HOURS );

		scheduler.startup();
		try {
			Thread forcedRun = new Thread( () -> scheduler.getTaskRecord( "forced-task" ).task.run( true ), "manual-forced" );
			forcedRun.setDaemon( true );
			forcedRun.start();
			assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();
			assertThat( scheduler.getTaskRecord( "forced-task" ).task.getRunningCount() ).isAtLeast( 1 );

			long start = System.currentTimeMillis();
			scheduler.shutdown( true, 5 );
			long elapsedMs = System.currentTimeMillis() - start;

			assertThat( awaitFlag( interrupted, 5_000 ) ).isTrue();
			// Forced means forced: it did not sit through a graceful wait
			assertThat( elapsedMs ).isLessThan( 5_000L );
		} finally {
			finish.countDown();
			if ( scheduler.hasStarted() ) {
				scheduler.shutdown( true, 5 );
			}
		}
	}

	@DisplayName( "no new execution may be scheduled once shutdown has begun" )
	@Test
	void testNoNewSubmissionsAfterShutdownBegins() throws Exception {
		BaseScheduler	scheduler	= new BaseScheduler( "gate-" + UUID.randomUUID() )
		    .useGlobalExecutor( "scheduled-tasks" );
		AtomicBoolean	ran			= new AtomicBoolean( false );
		CountDownLatch	started		= new CountDownLatch( 1 );
		CountDownLatch	finish		= new CountDownLatch( 1 );

		scheduler.task( "gate-task" )
		    .call( () -> {
			    ran.set( true );
			    started.countDown();
			    try {
				    finish.await( 10, TimeUnit.SECONDS );
			    } catch ( InterruptedException e ) {
				    Thread.currentThread().interrupt();
			    }
		    } )
		    .delay( 1, TimeUnit.HOURS ).every( 1, TimeUnit.HOURS );

		scheduler.startup();
		try {
			Thread gateRun = new Thread( () -> scheduler.getTaskRecord( "gate-task" ).task.run( true ), "gate-run" );
			gateRun.setDaemon( true );
			gateRun.start();
			assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();
			assertThat( ran.get() ).isTrue();

			// Begin shutdown in another thread, so we can probe while it is in progress
			Thread shutdown = new Thread( () -> scheduler.shutdown( false, 2 ), "gate-shutdown" );
			shutdown.start();

			// Wait for the gate to close
			long deadline = System.currentTimeMillis() + 5_000;
			while ( !scheduler.isShuttingDown() && System.currentTimeMillis() < deadline ) {
				Thread.sleep( 10 );
			}
			assertThat( scheduler.isShuttingDown() ).isTrue();

			// The task itself refuses to schedule anything new
			assertThat( scheduler.getTaskRecord( "gate-task" ).task.isAcceptingSubmissions() ).isFalse();
			assertThat( scheduler.getTaskRecord( "gate-task" ).task.start() ).isNull();

			// And the scheduler refuses to schedule it for us: nothing new was submitted, and whatever
			// future was there before is cancelled
			var gateRecord = scheduler.startupTask( "gate-task" );
			assertThat( gateRecord.future == null || gateRecord.future.isCancelled() ).isTrue();

			// Registering a brand new task is refused too
			boolean threw = false;
			try {
				scheduler.task( "late-task" ).call( () -> {
				} );
			} catch ( Exception e ) {
				threw = true;
			}
			assertThat( threw ).isTrue();

			shutdown.join( 15_000 );
		} finally {
			finish.countDown();
			if ( scheduler.hasStarted() ) {
				scheduler.shutdown( true, 5 );
			}
		}
	}

	@DisplayName( "a task refuses to start a run once shutdown has begun, even if the executor fires it" )
	@Test
	void testRunIsRefusedAfterShutdownBegins() {
		BaseScheduler	scheduler	= new BaseScheduler( "run-gate-" + UUID.randomUUID() )
		    .useGlobalExecutor( "scheduled-tasks" );
		int[]			runs		= { 0 };

		scheduler.task( "gated" )
		    .call( () -> {
			    runs[ 0 ]++;
		    } )
		    .delay( 1, TimeUnit.HOURS ).every( 1, TimeUnit.HOURS );

		scheduler.startup();
		try {
			ScheduledTask task = scheduler.getTaskRecord( "gated" ).task;

			// Shutdown has been signalled: the gate is closed. A fixed-rate tick fired by the executor between
			// the signal and the pool being shut down must not start a run.
			task.stopAcceptingSubmissions();
			task.run( false );
			assertThat( runs[ 0 ] ).isEqualTo( 0 );

			// An explicit manual run is still honoured
			task.run( true );
			assertThat( runs[ 0 ] ).isEqualTo( 1 );

			// A restart reopens the gate, otherwise every restart would be a scheduler that never runs again
			task.resumeAcceptingSubmissions();
			task.run( false );
			assertThat( runs[ 0 ] ).isEqualTo( 2 );
		} finally {
			if ( scheduler.hasStarted() ) {
				scheduler.shutdown( true, 5 );
			}
		}
	}

	@DisplayName( "shut-down cluster loops cannot be restarted" )
	@Test
	void testClusterLoopsCannotRestartAfterShutdown() {
		String			clusterName	= "gate-cluster-" + UUID.randomUUID();
		ClusterService	cluster		= node( clusterName );
		cluster.getConfig().heartbeatSeconds = 3_600;

		cluster.startLoops();
		assertThat( cluster.isRunning() ).isTrue();

		cluster.onShutdown( false );
		assertThat( cluster.isRunning() ).isFalse();

		// A late start must not resurrect the loops
		cluster.startLoops();
		assertThat( cluster.isRunning() ).isFalse();
	}

	@DisplayName( "a zero grace skips the wait entirely and kills the run" )
	@Test
	void testZeroGraceKillsImmediately() throws Exception {
		// Both settings at zero: no graceful wait, no interrupt grace - straight to shutdownNow
		long	originalGrace		= runtime.getConfiguration().scheduler.shutdownGraceSeconds;
		long	originalInterrupt	= runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds;
		runtime.getConfiguration().scheduler.shutdownGraceSeconds			= 0;
		runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds	= 0;
		try {
			BaseScheduler	scheduler	= new BaseScheduler( "zero-grace-" + UUID.randomUUID() )
			    .useGlobalExecutor( "scheduled-tasks" );
			CountDownLatch	started		= new CountDownLatch( 1 );
			CountDownLatch	finish		= new CountDownLatch( 1 );
			AtomicBoolean	interrupted	= new AtomicBoolean( false );

			scheduler.task( "stuck-task" )
			    .call( () -> {
				    started.countDown();
				    try {
					    finish.await( 30, TimeUnit.SECONDS );
				    } catch ( InterruptedException e ) {
					    interrupted.set( true );
					    Thread.currentThread().interrupt();
				    }
			    } )
			    .delay( 1, TimeUnit.HOURS )
			    .every( 1, TimeUnit.HOURS );

			scheduler.startup();
			try {
				Thread runner = new Thread( () -> scheduler.getTaskRecord( "stuck-task" ).task.run( true ), "stuck-run" );
				runner.setDaemon( true );
				runner.start();
				assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();

				long start = System.currentTimeMillis();
				scheduler.shutdown( false );
				long elapsedMs = System.currentTimeMillis() - start;

				// No 30s graceful wait happened
				assertThat( elapsedMs ).isLessThan( 5_000L );
				assertThat( awaitFlag( interrupted, 5_000 ) ).isTrue();
			} finally {
				finish.countDown();
				if ( scheduler.hasStarted() ) {
					scheduler.shutdown( true, 5 );
				}
			}
		} finally {
			runtime.getConfiguration().scheduler.shutdownGraceSeconds			= originalGrace;
			runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds	= originalInterrupt;
		}
	}

	@DisplayName( "a zero interrupt grace interrupts and then goes straight to the forced shutdown" )
	@Test
	void testZeroInterruptGraceSkipsTheSecondWait() throws Exception {
		// A non-zero graceful wait, but no second wait after the interrupt: the run is interrupted
		// immediately and shutdown does not sit through the interrupt grace
		long	originalGrace		= runtime.getConfiguration().scheduler.shutdownGraceSeconds;
		long	originalInterrupt	= runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds;
		runtime.getConfiguration().scheduler.shutdownGraceSeconds			= 30;
		runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds	= 0;
		try {
			BaseScheduler	scheduler	= new BaseScheduler( "no-interrupt-grace-" + UUID.randomUUID() )
			    .useGlobalExecutor( "scheduled-tasks" );
			CountDownLatch	started		= new CountDownLatch( 1 );
			CountDownLatch	finish		= new CountDownLatch( 1 );
			AtomicBoolean	interrupted	= new AtomicBoolean( false );

			scheduler.task( "overrun-task" )
			    .call( () -> {
				    started.countDown();
				    try {
					    finish.await( 30, TimeUnit.SECONDS );
				    } catch ( InterruptedException e ) {
					    interrupted.set( true );
					    Thread.currentThread().interrupt();
				    }
			    } )
			    .delay( 1, TimeUnit.HOURS ).every( 1, TimeUnit.HOURS );

			scheduler.startup();
			try {
				Thread overrun = new Thread( () -> scheduler.getTaskRecord( "overrun-task" ).task.run( true ), "overrun-run" );
				overrun.setDaemon( true );
				overrun.start();
				assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();

				long start = System.currentTimeMillis();
				scheduler.shutdown( false, 1 );
				long elapsedMs = System.currentTimeMillis() - start;

				// Waited its 1s timeout, interrupted, then did not wait any longer
				assertThat( elapsedMs ).isAtLeast( 700L );
				assertThat( elapsedMs ).isLessThan( 5_000L );
				assertThat( awaitFlag( interrupted, 5_000 ) ).isTrue();
			} finally {
				finish.countDown();
				if ( scheduler.hasStarted() ) {
					scheduler.shutdown( true, 5 );
				}
			}
		} finally {
			runtime.getConfiguration().scheduler.shutdownGraceSeconds			= originalGrace;
			runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds	= originalInterrupt;
		}
	}

	@DisplayName( "the configured grace is the value shutdown actually uses" )
	@Test
	void testConfiguredGraceIsUsed() {
		// Whatever the shipped default is, the getter must reflect the configuration
		long original = runtime.getConfiguration().scheduler.shutdownGraceSeconds;
		assertThat( runtime.getSchedulerService().getShutdownGraceSeconds() ).isEqualTo( original );

		runtime.getConfiguration().scheduler.shutdownGraceSeconds = 7;
		try {
			assertThat( runtime.getSchedulerService().getShutdownGraceSeconds() ).isEqualTo( 7L );
		} finally {
			runtime.getConfiguration().scheduler.shutdownGraceSeconds = original;
		}
	}

	@DisplayName( "the configured interrupt grace is the value shutdown actually uses" )
	@Test
	void testConfiguredInterruptGraceIsUsed() {
		long original = runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds;
		assertThat( runtime.getSchedulerService().getShutdownInterruptGraceSeconds() ).isEqualTo( original );

		runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds = 3;
		try {
			assertThat( runtime.getSchedulerService().getShutdownInterruptGraceSeconds() ).isEqualTo( 3L );
		} finally {
			runtime.getConfiguration().scheduler.shutdownInterruptGraceSeconds = original;
		}
	}

	@DisplayName( "a graceful scheduler shutdown waits for an in-flight task before interrupting it" )
	@Test
	void testGracefulShutdownWaitsForRunningTask() throws Exception {
		CountDownLatch	started		= new CountDownLatch( 1 );
		CountDownLatch	completed	= new CountDownLatch( 1 );
		long			taskMillis	= 600;

		// Shared executor: this is the path where the scheduler itself has to wait for in-flight runs,
		// because it is not allowed to shut the pool down
		BaseScheduler	scheduler	= new BaseScheduler( "graceful-shutdown-" + UUID.randomUUID() )
		    .useGlobalExecutor( "scheduled-tasks" );
		scheduler.task( "slow-task" )
		    .call( () -> {
			    started.countDown();
			    try {
				    Thread.sleep( taskMillis );
				    completed.countDown();
			    } catch ( InterruptedException e ) {
				    Thread.currentThread().interrupt();
			    }
		    } )
		    .delay( 1, TimeUnit.HOURS ).every( 1, TimeUnit.HOURS );

		scheduler.startup();
		try {
			// Kick the task off by hand, so the test does not wait for its schedule
			assertThat( started.getCount() ).isEqualTo( 1L );
			Thread manualRun = new Thread( () -> scheduler.getTaskRecord( "slow-task" ).task.run( true ), "manual-run" );
			manualRun.setDaemon( true );
			manualRun.start();
			assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();

			long start = System.currentTimeMillis();
			// Graceful, with a timeout comfortably longer than the task
			scheduler.shutdown( false, 10 );
			long elapsedMs = System.currentTimeMillis() - start;

			// The task was allowed to finish, and shutdown blocked until it did
			assertThat( completed.await( 1, TimeUnit.SECONDS ) ).isTrue();
			assertThat( elapsedMs ).isAtLeast( taskMillis / 2 );
		} finally {
			if ( scheduler.hasStarted() ) {
				scheduler.shutdown( true, 5 );
			}
		}
	}

	@DisplayName( "a forced shutdown interrupts the run, and the claim is left terminal" )
	@Test
	void testForcedShutdownLeavesTerminalClaim() throws Exception {
		String					clusterName	= "shutdown-forced-" + UUID.randomUUID();
		ClusterService			cluster		= node( clusterName );
		ScheduledTask			task		= new ScheduledTask( "forced-" + UUID.randomUUID() )
		    .every( 1, TimeUnit.HOURS )
		    .setCluster( true );

		Instant					due			= Instant.now();
		TaskClusterClaim.Claim	claim		= TaskClusterClaim.attempt( task, due, cluster );
		assertThat( claim ).isNotNull();

		CountDownLatch	started		= new CountDownLatch( 1 );
		AtomicBoolean	interrupted	= new AtomicBoolean( false );

		Thread			run			= new Thread( () -> {
										try {
											claim.startHeartbeat();
											started.countDown();
											Thread.sleep( 30_000 );
										} catch ( InterruptedException e ) {
											// This is what a forced shutdown does to a running task
											interrupted.set( true );
											Thread.currentThread().interrupt();
										} finally {
											claim.finish( interrupted.get() ? "abandoned" : "failed", null );
										}
									}, "forced-run" );
		run.setDaemon( true );
		run.start();

		assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();

		// Force it
		run.interrupt();
		run.join( 5_000 );

		assertThat( interrupted.get() ).isTrue();

		IStruct record = ( IStruct ) cache.get( TaskClusterClaim.buildTaskKey( cluster, task ) ).orElse( null );
		assertThat( record ).isNotNull();
		assertThat( record.getAsString( Key.status ) ).isEqualTo( "abandoned" );

		// A cut-short run must never be re-run: that would be a second execution of the same occurrence
		assertThat( TaskClusterClaim.attempt( task, due, cluster ) ).isNull();
	}

	@DisplayName( "an interrupted run is never recorded as success" )
	@Test
	void testInterruptedRunIsNotSuccess() throws Exception {
		String					clusterName	= "shutdown-not-success-" + UUID.randomUUID();
		ClusterService			cluster		= node( clusterName );
		ScheduledTask			task		= new ScheduledTask( "interrupted-" + UUID.randomUUID() )
		    .every( 1, TimeUnit.HOURS )
		    .setCluster( true );
		String					key			= TaskClusterClaim.buildTaskKey( cluster, task );

		TaskClusterClaim.Claim	claim		= TaskClusterClaim.attempt( task, Instant.now(), cluster );
		assertThat( claim ).isNotNull();

		// What runInternal() now does on an interrupt
		claim.finish( "abandoned", null );

		IStruct record = ( IStruct ) cache.get( key ).orElse( null );
		assertThat( record.getAsString( Key.status ) ).isNotEqualTo( "success" );
		assertThat( record.getAsString( Key.status ) ).isEqualTo( "abandoned" );
	}

	@DisplayName( "a shared-executor scheduler gives up waiting at its timeout without hanging" )
	@Test
	void testSharedExecutorShutdownRespectsTimeout() throws Exception {
		// A shared executor is never shut down by the scheduler, so the only guarantee is that the
		// graceful wait is bounded
		BaseScheduler	scheduler	= new BaseScheduler( "shared-shutdown-" + UUID.randomUUID() )
		    .useGlobalExecutor( "scheduled-tasks" );
		CountDownLatch	started		= new CountDownLatch( 1 );
		CountDownLatch	finish		= new CountDownLatch( 1 );

		scheduler.task( "long-task" )
		    .call( () -> {
			    started.countDown();
			    try {
				    // Deliberately longer than the shutdown timeout
				    finish.await( 10, TimeUnit.SECONDS );
			    } catch ( InterruptedException e ) {
				    Thread.currentThread().interrupt();
			    }
		    } )
		    .delay( 1, TimeUnit.HOURS ).every( 1, TimeUnit.HOURS );

		scheduler.startup();
		try {
			Thread longRun = new Thread( () -> scheduler.getTaskRecord( "long-task" ).task.run( true ), "manual-long-run" );
			longRun.setDaemon( true );
			longRun.start();
			assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();

			long start = System.currentTimeMillis();
			scheduler.shutdown( false, 1 );
			long elapsedMs = System.currentTimeMillis() - start;

			// Bounded: it waited roughly the timeout, then moved on rather than blocking forever
			assertThat( elapsedMs ).isAtLeast( 700L );
			assertThat( elapsedMs ).isLessThan( 15_000L );
		} finally {
			finish.countDown();
			if ( scheduler.hasStarted() ) {
				scheduler.shutdown( true, 5 );
			}
		}
	}
}
