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
package ortus.boxlang.cluster;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.cluster.ClusterSoakTimeline.Execution;
import ortus.boxlang.cluster.ClusterSoakTimeline.LifecycleEvent;
import ortus.boxlang.cluster.ClusterSoakTimeline.NodeSpan;
import ortus.boxlang.cluster.ClusterSoakTimeline.PeerObservation;
import ortus.boxlang.cluster.ClusterSoakTimeline.Run;

/**
 * The soak timeline is what a human looks at to decide whether an exactly-once run actually behaved, so
 * the rendering itself needs coverage: no leftover placeholders, every node and occurrence present, and
 * duplicates called out rather than hidden.
 */
public class ClusterSoakTimelineTest {

	private static final long T0 = 1_700_000_000_000L;

	private Run run( List<Execution> executions, boolean passed ) {
		return new Run(
		    "eager",
		    3_000,
		    T0,
		    T0 + 120_000,
		    List.of(
		        new NodeSpan( "node1", T0, T0 + 40_000, "retired" ),
		        new NodeSpan( "node2", T0, T0 + 60_000, "killed" ),
		        new NodeSpan( "node2", T0 + 66_000, T0 + 120_000, "stopped" ),
		        new NodeSpan( "node3", T0 + 30_000, T0 + 120_000, "stopped" )
		    ),
		    executions,
		    List.of(
		        new LifecycleEvent( T0, "node1", "started", "initial deployment" ),
		        new LifecycleEvent( T0 + 40_000, "node1", "retired", "retired for good (scale down)" ),
		        new LifecycleEvent( T0 + 60_000, "node2", "killed", "hard kill (rolling restart)" ),
		        new LifecycleEvent( T0 + 30_000, "node3", "started", "fresh node (scale up)" )
		    ),
		    List.of( new PeerObservation( "node3", T0 + 115_000, new LinkedHashSet<>( Set.of( "node2" ) ) ) ),
		    passed,
		    List.of() );
	}

	@DisplayName( "It renders a self-contained document with a row per node and no leftovers" )
	@Test
	void testRendersEveryNodeAndOccurrence() {
		String html = ClusterSoakTimeline.render( run(
		    List.of(
		        new Execution( "1000", "node1", T0 + 1_000, T0 + 1_400, "success", true ),
		        new Execution( "1001", "node2", T0 + 4_000, T0 + 4_500, "success", true ),
		        new Execution( "1002", "node3", T0 + 61_000, T0 + 61_300, "unknown", true )
		    ),
		    true ) );

		assertThat( html ).contains( "<!DOCTYPE html>" );
		assertThat( html ).contains( "node1" );
		assertThat( html ).contains( "node2" );
		assertThat( html ).contains( "node3" );
		assertThat( html ).contains( "<code>1002</code>" );
		assertThat( html ).doesNotContain( "/*__" );
		// No external dependencies: it has to open straight off disk. The only absolute URL is the SVG
		// namespace, which is an identifier and is never fetched.
		assertThat( html ).doesNotContain( "<script" );
		assertThat( html ).doesNotContain( "href=\"http" );
		assertThat( html ).doesNotContain( "src=\"http" );
	}

	@DisplayName( "It reports a clean run as passing with no duplicates" )
	@Test
	void testCleanRunPasses() {
		String html = ClusterSoakTimeline.render( run(
		    List.of(
		        new Execution( "1000", "node1", T0 + 1_000, T0 + 1_400, "success", true ),
		        new Execution( "1001", "node2", T0 + 4_000, T0 + 4_500, "success", true )
		    ),
		    true ) );

		assertThat( html ).contains( "passed - 2 clustered runs across 2 executions" );
		assertThat( html ).contains( "no duplicates" );
		assertThat( html ).contains( "<div class=\"v\">PASS</div>" );
		assertThat( html ).contains( "<div class=\"v\">0</div>" );
	}

	@DisplayName( "It calls out an occurrence that ran twice instead of hiding it" )
	@Test
	void testDuplicateIsCalledOut() {
		String html = ClusterSoakTimeline.render( run(
		    List.of(
		        new Execution( "1000", "node1", T0 + 1_000, T0 + 1_400, "success", true ),
		        new Execution( "1000", "node2", T0 + 1_100, T0 + 1_500, "success", true )
		    ),
		    false ) );

		assertThat( html ).contains( "executed more than once" );
		assertThat( html ).contains( "1000 on [node1, node2]" );
		assertThat( html ).contains( "<div class=\"v\">FAIL</div>" );
		// The duplicated row is flagged in the table as well as the banner
		assertThat( html ).contains( "<tr style=\"background:#fdf2f2\">" );
	}

	@DisplayName( "A shutdown request is marked on the node's row and the drain window is measured" )
	@Test
	void testShutdownRequestMarksTheDrainWindow() {
		// node1 is up 40s, asked to stop at 27.6s, so it drains for 12.4s during which no new runs start.
		Run		run		= new Run(
		    "balanced",
		    3_000,
		    T0,
		    T0 + 40_000,
		    List.of( new NodeSpan( "node1", T0, T0 + 40_000, "retired" ) ),
		    List.of( new Execution( "1000", "node1", T0 + 1_000, T0 + 1_500, "success", true ) ),
		    List.of( new LifecycleEvent( T0 + 27_600, "node1", "stop-requested", "graceful shutdown requested" ) ),
		    List.of(),
		    true, List.of() );

		String	html	= ClusterSoakTimeline.render( run );

		// The marker and the shaded drain band are both present, and the size is readable without hovering
		assertThat( html ).contains( "stroke-dasharray=\"4 2\"" );
		assertThat( html ).contains( "draining 12.4s" );
		assertThat( html ).contains( "shutdown requested at 0:27.6, process exited at 0:40.0" );
		assertThat( html ).contains( "no new runs accepted" );

		// Nothing was executing, so the band must say so rather than implying the node was idle
		assertThat( html ).contains( "draining 12.4s (claim)" );
		assertThat( html ).contains( "finishing a claim attempt already in progress" );
	}

	@DisplayName( "The drain band names the task it is waiting for when one is actually running" )
	@Test
	void testDrainNamesTheRunningTask() {
		// A 30s job is in flight when the stop is requested at 5s, so the drain is waiting for that job.
		Run		run		= new Run(
		    "balanced",
		    3_000,
		    T0,
		    T0 + 40_000,
		    List.of( new NodeSpan( "node1", T0, T0 + 36_000, "retired" ) ),
		    List.of( new Execution( "soak-task-4 @2026-11-13T17:32:16", "node1", T0 + 2_000, T0 + 32_000, "success", true ) ),
		    List.of( new LifecycleEvent( T0 + 5_000, "node1", "stop-requested", "graceful shutdown requested" ) ),
		    List.of(),
		    true, List.of() );

		String	html	= ClusterSoakTimeline.render( run );

		assertThat( html ).contains( "waiting for soak-task-4 to finish" );
		assertThat( html ).contains( "draining 31.0s (soak-task-4)" );
		assertThat( html ).doesNotContain( "finishing a claim attempt already in progress" );
	}

	@DisplayName( "A node that was killed outright has no shutdown request and no drain band" )
	@Test
	void testKilledNodeHasNoDrainWindow() {
		Run		run		= new Run(
		    "balanced",
		    3_000,
		    T0,
		    T0 + 40_000,
		    List.of( new NodeSpan( "node1", T0, T0 + 40_000, "crashed" ) ),
		    List.of(),
		    List.of(),
		    List.of(),
		    true, List.of() );

		String	html	= ClusterSoakTimeline.render( run );

		// No marker line, no per-row drain tooltip, no drain card. The legend still describes the feature,
		// so the assertions are on the row output rather than the word.
		assertThat( html ).doesNotContain( "stroke-dasharray=\"4 2\"" );
		assertThat( html ).doesNotContain( "shutdown requested at " );
	}

	@DisplayName( "A non-clustered task running on every node is shown, and is not a duplicate" )
	@Test
	void testUnclusteredFanOutIsNotADuplicate() {
		String html = ClusterSoakTimeline.render( run(
		    List.of(
		        new Execution( "soak-plain-1 @1000", "node1", T0 + 1_000, T0 + 1_500, "success", false ),
		        new Execution( "soak-plain-1 @1000", "node2", T0 + 1_050, T0 + 1_550, "success", false ),
		        new Execution( "soak-plain-1 @1000", "node3", T0 + 1_100, T0 + 1_600, "success", false )
		    ),
		    true ) );

		// It has to be visible: that is the whole reason it is on the timeline. One row per fire, with the
		// per-node details as comma-delimited lists.
		assertThat( html ).contains( "soak-plain-1 @1000" );
		assertThat( html ).contains( "<td>node1, node2, node3</td>" );
		assertThat( html ).contains( "<td>success, success, success</td>" );
		assertThat( html ).contains( "<div class=\"v\">3</div>" );
		assertThat( html ).contains( "3 unclustered runs" );

		// ... and the fan-out must never be treated as two nodes running one run
		assertThat( html ).doesNotContain( "executed more than once" );
		assertThat( html ).doesNotContain( "<tr style=\"background:#fdf2f2\">" );
		assertThat( html ).contains( "<div class=\"v\">PASS</div>" );
		assertThat( html ).contains( "<div class=\"v\">0</div>" );
	}

	@DisplayName( "A clustered duplicate is still caught when unclustered runs are present" )
	@Test
	void testClusteredDuplicateStillCaughtAlongsideUnclustered() {
		String html = ClusterSoakTimeline.render( run(
		    List.of(
		        new Execution( "soak-plain-1 @1000", "node1", T0 + 1_000, T0 + 1_500, "success", false ),
		        new Execution( "soak-plain-1 @1000", "node2", T0 + 1_050, T0 + 1_550, "success", false ),
		        new Execution( "soak-task-1 @2000", "node1", T0 + 2_000, T0 + 2_400, "success", true ),
		        new Execution( "soak-task-1 @2000", "node2", T0 + 2_100, T0 + 2_500, "success", true )
		    ),
		    false ) );

		assertThat( html ).contains( "executed more than once" );
		assertThat( html ).contains( "soak-task-1 @2000 on [node1, node2]" );
		// The unclustered fan-out is not what failed the run
		assertThat( html ).doesNotContain( "soak-plain-1 @1000 on [" );
		assertThat( html ).contains( "<div class=\"v\">FAIL</div>" );
	}

	@DisplayName( "Clustered and unclustered runs interleave chronologically in one list" )
	@Test
	void testRunsAreInterleavedChronologically() {
		// Fire times: task-1 @16, plain @20, task-1 @24. Sorting by the whole string would put soak-plain-1
		// first simply because "plain" < "task" - the ordering has to come from the fire time.
		String	html			= ClusterSoakTimeline.render( run(
		    List.of(
		        new Execution( "soak-plain-1 @2026-11-13T17:32:20", "node1", T0 + 1_000, T0 + 1_500, "success", false ),
		        new Execution( "soak-plain-1 @2026-11-13T17:32:20", "node2", T0 + 1_050, T0 + 1_550, "success", false ),
		        new Execution( "soak-task-1 @2026-11-13T17:32:16", "node1", T0 + 2_000, T0 + 2_400, "success", true ),
		        new Execution( "soak-task-1 @2026-11-13T17:32:24", "node2", T0 + 6_000, T0 + 6_400, "success", true )
		    ),
		    true ) );

		// Search from the table header: the same ids also appear in the SVG tooltips above it
		int		table			= html.indexOf( "<th>Run</th>" );
		int		firstClustered	= html.indexOf( "soak-task-1 @2026-11-13T17:32:16", table );
		int		unclustered		= html.indexOf( "soak-plain-1 @2026-11-13T17:32:20", table );
		int		lastClustered	= html.indexOf( "soak-task-1 @2026-11-13T17:32:24", table );

		assertThat( table ).isGreaterThan( -1 );
		assertThat( firstClustered ).isGreaterThan( table );
		assertThat( firstClustered ).isLessThan( unclustered );
		assertThat( unclustered ).isLessThan( lastClustered );
	}

	@DisplayName( "It distinguishes a run whose node died from one that succeeded" )
	@Test
	void testUnknownOutcomeUsesItsOwnColour() {
		String html = ClusterSoakTimeline.render( run(
		    List.of( new Execution( "1002", "node2", T0 + 59_000, T0 + 60_000, "unknown", true ) ),
		    true ) );

		assertThat( html ).contains( "started but never finished" );
		assertThat( html ).contains( "#f9a825" );
		assertThat( html ).contains( "(unknown)" );
	}

	@DisplayName( "It renders node lifespans, lifecycle markers and the peak node count" )
	@Test
	void testTimelineShowsLifespansAndChurn() {
		String html = ClusterSoakTimeline.render( run( List.of(), true ) );

		assertThat( html ).contains( "node2 online 0:00.0 to 1:00.0 (1m 0s), killed without a graceful shutdown" );
		assertThat( html ).contains( "node2 online 1:06.0 to 2:00.0 (54s), node stopped" );
		assertThat( html ).contains( "node1 online 0:00.0 to 0:40.0 (40s), node taken out of service" );
		assertThat( html ).contains( "killed without a graceful shutdown" );
		assertThat( html ).contains( "node3 online 0:30.0 to 2:00.0 (1m 30s), node stopped" );
		// node1 + node2 + node3 overlap between 0:30 and 0:40
		assertThat( html ).contains( "nodes up (max 3)" );
	}

	@DisplayName( "It escapes node names and occurrence ids instead of injecting them" )
	@Test
	void testEscapesInput() {
		Run		run		= new Run(
		    "<script>alert(1)</script>",
		    3_000,
		    T0,
		    T0 + 10_000,
		    List.of( new NodeSpan( "<img src=x onerror=alert(1)>", T0, T0 + 5_000, "stopped" ) ),
		    List.of( new Execution( "<b>bad</b>", "<img src=x onerror=alert(1)>", T0 + 1_000, T0 + 1_500, "success", true ) ),
		    List.of(),
		    List.of(),
		    true, List.of() );

		String	html	= ClusterSoakTimeline.render( run );

		assertThat( html ).doesNotContain( "<script>alert(1)</script>" );
		assertThat( html ).doesNotContain( "<img src=x onerror=alert(1)>" );
		assertThat( html ).contains( "&lt;script&gt;alert(1)&lt;/script&gt;" );
	}

	@DisplayName( "It reports load per node in task-seconds, normalised by uptime" )
	@Test
	void testLoadColumnsAreNormalisedByUptime() {
		// node1 is online 40s and runs one 10s job -> 10 task-seconds, 0.250 per second online.
		// node3 is online 90s and runs one 9s job -> 9 task-seconds, 0.100 per second online.
		// Raw counts would make node3 look busier; uptime-normalised load says otherwise.
		String html = ClusterSoakTimeline.render( run(
		    List.of(
		        new Execution( "1000", "node1", T0 + 1_000, T0 + 11_000, "success", true ),
		        new Execution( "1001", "node3", T0 + 31_000, T0 + 40_000, "success", true )
		    ),
		    true ) );

		assertThat( html ).contains( "<th>Task-seconds</th>" );
		assertThat( html ).contains( "<th>Load / sec up</th>" );
		assertThat( html ).contains( "<td>10.0</td>" );
		assertThat( html ).contains( "<td>0.250</td>" );
		assertThat( html ).contains( "<td>9.0</td>" );
		assertThat( html ).contains( "<td>0.100</td>" );
	}

	@DisplayName( "It survives a run with nothing in it" )
	@Test
	void testEmptyRunRenders() {
		Run		run		= new Run( "eager", 3_000, T0, T0, List.of(), List.of(), List.of(), List.of(), false, List.of() );

		String	html	= ClusterSoakTimeline.render( run );

		assertThat( html ).contains( "FAIL" );
		assertThat( html ).doesNotContain( "/*__" );
	}

}
