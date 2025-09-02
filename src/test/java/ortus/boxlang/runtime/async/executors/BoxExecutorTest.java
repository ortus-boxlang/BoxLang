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
package ortus.boxlang.runtime.async.executors;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.services.AsyncService.ExecutorType;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class BoxExecutorTest {

	private BoxExecutor	fixedExecutor;
	private BoxExecutor	singleExecutor;
	private BoxExecutor	cachedExecutor;

	@BeforeEach
	void setUp() {
		// Create different types of executors for testing
		fixedExecutor	= new BoxExecutor(
		    Executors.newFixedThreadPool( 4 ),
		    "test-fixed",
		    ExecutorType.FIXED,
		    4
		);

		singleExecutor	= new BoxExecutor(
		    Executors.newSingleThreadExecutor(),
		    "test-single",
		    ExecutorType.SINGLE,
		    1
		);

		cachedExecutor	= new BoxExecutor(
		    Executors.newCachedThreadPool(),
		    "test-cached",
		    ExecutorType.CACHED,
		    Integer.MAX_VALUE
		);
	}

	@AfterEach
	void tearDown() {
		// Clean up executors
		if ( fixedExecutor != null ) {
			fixedExecutor.shutdownQuiet();
		}
		if ( singleExecutor != null ) {
			singleExecutor.shutdownQuiet();
		}
		if ( cachedExecutor != null ) {
			cachedExecutor.shutdownQuiet();
		}
	}

	@Test
	@DisplayName( "Test BoxExecutor record-like accessors" )
	void testRecordAccessors() {
		assertThat( fixedExecutor.name() ).isEqualTo( "test-fixed" );
		assertThat( fixedExecutor.type() ).isEqualTo( ExecutorType.FIXED );
		assertThat( fixedExecutor.maxThreads() ).isEqualTo( 4 );
		assertThat( fixedExecutor.executor() ).isNotNull();
		assertThat( fixedExecutor.executor() ).isInstanceOf( ThreadPoolExecutor.class );
	}

	@Test
	@DisplayName( "Test executor state methods" )
	void testExecutorState() {
		// Initially should not be shutdown or terminated
		assertFalse( fixedExecutor.isShutdown() );
		assertFalse( fixedExecutor.isTerminated() );
		assertFalse( fixedExecutor.isTerminating() );
		assertTrue( fixedExecutor.isHealthy() );

		// After shutdown, state should change
		fixedExecutor.shutdown();
		assertTrue( fixedExecutor.isShutdown() );
	}

	@Test
	@DisplayName( "Test basic stats structure" )
	void testBasicStats() {
		IStruct stats = fixedExecutor.getStats();

		// Check basic stats are present
		assertThat( stats.get( "name" ) ).isEqualTo( "test-fixed" );
		assertThat( stats.get( "type" ) ).isEqualTo( "java.util.concurrent.ThreadPoolExecutor" );
		assertThat( stats.get( "created" ) ).isNotNull();
		assertThat( stats.get( "lastActivity" ) ).isNotNull();
		assertThat( stats.get( "uptimeSeconds" ) ).isInstanceOf( Long.class );
		assertThat( stats.get( "taskSubmissionCount" ) ).isEqualTo( 0L );
		assertThat( stats.get( "averageTasksPerSecond" ) ).isEqualTo( 0.0 );

		// Check health status is included
		assertThat( stats.get( "healthStatus" ) ).isNotNull();
		assertThat( stats.get( "healthReport" ) ).isNotNull();
		assertThat( stats.get( "isShutdown" ) ).isEqualTo( false );
		assertThat( stats.get( "isTerminated" ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "Test ThreadPoolExecutor specific stats" )
	void testThreadPoolStats() {
		IStruct stats = fixedExecutor.getStats();

		// ThreadPoolExecutor specific stats
		assertThat( stats.get( "corePoolSize" ) ).isEqualTo( 4 );
		assertThat( stats.get( "maximumPoolSize" ) ).isEqualTo( 4 );
		assertThat( stats.get( "activeCount" ) ).isNotNull();
		assertThat( stats.get( "completedTaskCount" ) ).isNotNull();
		assertThat( stats.get( "taskCount" ) ).isNotNull();

		// Pool utilization should be calculated
		assertThat( stats.get( "poolUtilization" ) ).isInstanceOf( Double.class );
		assertThat( stats.get( "threadsUtilization" ) ).isInstanceOf( Double.class );

		// Queue stats should be present
		assertThat( stats.get( "queueSize" ) ).isNotNull();
		assertThat( stats.get( "queueCapacity" ) ).isNotNull();
		assertThat( stats.get( "queueType" ) ).isNotNull();
		assertThat( stats.get( "queueUtilization" ) ).isInstanceOf( Double.class );
	}

	@Test
	@DisplayName( "Test single thread executor stats" )
	void testSingleThreadStats() {
		IStruct stats = singleExecutor.getStats();

		// Single thread executor should have specific values
		assertThat( stats.get( "corePoolSize" ) ).isEqualTo( 1 );
		assertThat( stats.get( "maximumPoolSize" ) ).isEqualTo( 1 );
		assertThat( stats.get( "poolSize" ) ).isEqualTo( 1 );
		assertThat( stats.get( "largestPoolSize" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test task submission tracking" )
	void testTaskSubmissionTracking() throws InterruptedException {
		// Submit a simple task
		fixedExecutor.submit( () -> {
			try {
				Thread.sleep( 100 );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
		} );

		// Task submission count should be incremented
		IStruct stats = fixedExecutor.getStats();
		assertThat( stats.get( "taskSubmissionCount" ) ).isEqualTo( 1L );

		// Submit another task
		fixedExecutor.submit( () -> "test result" );

		stats = fixedExecutor.getStats();
		assertThat( stats.get( "taskSubmissionCount" ) ).isEqualTo( 2L );

		// Average tasks per second should be calculated (might be 0 if executed too quickly)
		Double avgTasksPerSecond = ( Double ) stats.get( "averageTasksPerSecond" );
		assertThat( avgTasksPerSecond ).isAtLeast( 0.0 );
	}

	@Test
	@DisplayName( "Test health status calculation" )
	void testHealthStatus() {
		IStruct	stats			= fixedExecutor.getStats();
		String	healthStatus	= ( String ) stats.get( "healthStatus" );

		// Should be healthy initially
		assertThat( healthStatus ).isEqualTo( "healthy" );
		assertTrue( fixedExecutor.isHealthy() );

		// After shutdown should change status
		fixedExecutor.shutdown();
		stats			= fixedExecutor.getStats();
		healthStatus	= ( String ) stats.get( "healthStatus" );
		// The status can vary depending on timing, but should not be healthy
		assertThat( healthStatus ).isNotEqualTo( "healthy" );
		assertFalse( fixedExecutor.isHealthy() );
	}

	@Test
	@DisplayName( "Test health report structure" )
	void testHealthReport() {
		IStruct	stats			= fixedExecutor.getStats();
		IStruct	healthReport	= ( IStruct ) stats.get( "healthReport" );

		assertNotNull( healthReport );
		assertThat( healthReport.get( "status" ) ).isNotNull();
		assertThat( healthReport.get( "summary" ) ).isNotNull();
		assertThat( healthReport.get( "issues" ) ).isInstanceOf( Array.class );
		assertThat( healthReport.get( "recommendations" ) ).isInstanceOf( Array.class );
		assertThat( healthReport.get( "alerts" ) ).isInstanceOf( Array.class );
		assertThat( healthReport.get( "insights" ) ).isInstanceOf( Array.class );
		assertThat( healthReport.get( "lastChecked" ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test queue methods" )
	void testQueueMethods() {
		// Should return a queue (even if empty for non-queued executors)
		assertNotNull( fixedExecutor.getQueue() );
		assertNotNull( singleExecutor.getQueue() );
		assertNotNull( cachedExecutor.getQueue() );

		// Queue should be empty initially
		assertTrue( fixedExecutor.getQueue().isEmpty() );
	}

	@Test
	@DisplayName( "Test pool size methods" )
	void testPoolSizeMethods() {
		// Fixed executor should have predictable pool sizes
		assertThat( fixedExecutor.getCorePoolSize() ).isEqualTo( 4 );
		assertThat( fixedExecutor.getMaximumPoolSize() ).isEqualTo( 4 );

		// Pool size should be >= 0
		assertThat( fixedExecutor.getPoolSize() ).isAtLeast( 0 );
		assertThat( fixedExecutor.getLargestPoolSize() ).isAtLeast( 0 );
		assertThat( fixedExecutor.getActiveCount() ).isAtLeast( 0 );
	}

	@Test
	@DisplayName( "Test task count methods" )
	void testTaskCountMethods() {
		// Initially should have no completed tasks
		assertThat( fixedExecutor.getCompletedTaskCount() ).isEqualTo( 0L );
		assertThat( fixedExecutor.getTaskCount() ).isAtLeast( 0L );

		// After submitting tasks, task count should increase
		fixedExecutor.submit( () -> "test" );

		// Task count should be at least 1
		assertThat( fixedExecutor.getTaskCount() ).isAtLeast( 1L );
	}

	@Test
	@DisplayName( "Test shutdown methods" )
	void testShutdownMethods() throws InterruptedException {
		assertFalse( fixedExecutor.isShutdown() );

		// Test graceful shutdown
		BoxExecutor result = fixedExecutor.shutdown();
		assertThat( result ).isSameInstanceAs( fixedExecutor ); // Should return self for chaining
		assertTrue( fixedExecutor.isShutdown() );

		// Test await termination
		boolean terminated = fixedExecutor.awaitTermination( 1L, TimeUnit.SECONDS );
		assertTrue( terminated );
		assertTrue( fixedExecutor.isTerminated() );
	}

	@Test
	@DisplayName( "Test shutdownNow method" )
	void testShutdownNow() {
		// Add some tasks that won't complete immediately
		fixedExecutor.submit( () -> {
			try {
				Thread.sleep( 5000 ); // Long running task
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
		} );

		// Force shutdown
		var pendingTasks = fixedExecutor.shutdownNow();
		assertNotNull( pendingTasks );
		assertTrue( fixedExecutor.isShutdown() );
	}

	@Test
	@DisplayName( "Test shutdownAndAwaitTermination" )
	void testShutdownAndAwaitTermination() {
		// This should complete without throwing exceptions
		fixedExecutor.shutdownAndAwaitTermination( 1L, TimeUnit.SECONDS );
		assertTrue( fixedExecutor.isShutdown() );
	}

	@Test
	@DisplayName( "Test task creation methods" )
	void testTaskCreation() {
		// Test creating named task
		var namedTask = fixedExecutor.newTask( "test-task" );
		assertNotNull( namedTask );
		assertThat( namedTask.getName() ).isEqualTo( "test-task" );

		// Test creating auto-named task
		var autoNamedTask = fixedExecutor.newTask();
		assertNotNull( autoNamedTask );
		assertThat( autoNamedTask.getName() ).startsWith( "Task-" );
		assertThat( autoNamedTask.getName() ).isNotEqualTo( namedTask.getName() );
	}

	@Test
	@DisplayName( "Test submitAndGet methods" )
	void testSubmitAndGet() {
		// Create a fresh executor for this test to avoid RejectedExecutionException
		BoxExecutor testExecutor = new BoxExecutor(
		    Executors.newFixedThreadPool( 2 ),
		    "test-submitAndGet",
		    ExecutorType.FIXED,
		    2
		);

		try {
			// Test with Callable
			Object result = testExecutor.submitAndGet( () -> "test result" );
			assertThat( result ).isEqualTo( "test result" );

			// Test with Runnable (creates a new executor each time)
			BoxExecutor	testExecutor2	= new BoxExecutor(
			    Executors.newSingleThreadExecutor(),
			    "test-submitAndGet-2",
			    ExecutorType.SINGLE,
			    1
			);

			Object		runnableResult	= testExecutor2.submitAndGet( () -> System.out.println( "test" ) );
			// Runnable submit typically returns null
		} finally {
			// Note: submitAndGet calls shutdownQuiet() internally, so no manual cleanup needed
		}
	}

	@Test
	@DisplayName( "Test logger access" )
	void testLoggerAccess() {
		assertNotNull( fixedExecutor.getLogger() );
		assertThat( fixedExecutor.getLogger().getName() ).contains( "ASYNC" );
	}

	@Test
	@DisplayName( "Test scheduled executor access" )
	void testScheduledExecutorAccess() {
		// Create a scheduled executor
		BoxExecutor scheduledExecutor = new BoxExecutor(
		    new BoxScheduledExecutor( 2 ),
		    "test-scheduled",
		    ExecutorType.SCHEDULED,
		    2
		);

		try {
			BoxScheduledExecutor scheduled = scheduledExecutor.scheduledExecutor();
			assertNotNull( scheduled );
		} finally {
			scheduledExecutor.shutdownQuiet();
		}
	}

	@Test
	@DisplayName( "Test features map" )
	void testFeatures() {
		IStruct	stats		= fixedExecutor.getStats();
		Object	features	= stats.get( "features" );
		assertNotNull( features );

		// Should contain feature information for ThreadPoolExecutor
		if ( features instanceof IStruct ) {
			IStruct featureStruct = ( IStruct ) features;
			assertThat( featureStruct.get( "pool" ) ).isEqualTo( true );
			assertThat( featureStruct.get( "taskMethods" ) ).isEqualTo( true );
		}
	}

	@Test
	@DisplayName( "Test thresholds in stats" )
	void testThresholds() {
		IStruct	stats		= fixedExecutor.getStats();
		Object	thresholds	= stats.get( "thresholds" );
		assertNotNull( thresholds );

		// Should contain health monitoring thresholds
		if ( thresholds instanceof IStruct ) {
			IStruct thresholdStruct = ( IStruct ) thresholds;
			assertNotNull( thresholdStruct.get( "poolUtilizationDegraded" ) );
			assertNotNull( thresholdStruct.get( "poolUtilizationCritical" ) );
		}
	}
}
