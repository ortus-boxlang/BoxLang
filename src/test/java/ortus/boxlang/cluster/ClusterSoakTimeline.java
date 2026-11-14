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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Renders one soak run as a self-contained HTML timeline: a stacked view of which nodes were online
 * when, and which occurrence each node executed, so an exactly-once claim can be eyeballed as well as
 * asserted.
 * <p>
 * No dependencies and no network: the file opens straight from disk. Everything is inline SVG, so a
 * hover gives the raw numbers through the element's own title.
 */
public final class ClusterSoakTimeline {

	private static final int	LABEL_W	= 185;
	private static final int	PLOT_W	= 1180;
	private static final int	STAT_W	= 140;
	private static final int	ROW_H	= 32;
	private static final int	PAD		= 16;
	private static final int	AXIS_H	= 44;
	private static final int	SIZE_H	= 78;

	/**
	 * One stretch of time a node was online. Rows are drawn from these, so a node that was started
	 * three times has three spans.
	 *
	 * @param nodeId    The node
	 * @param from      When it came online (epoch millis)
	 * @param to        When it went away (epoch millis)
	 * @param endReason How it ended: stopped, killed, retired, crashed
	 */
	public record NodeSpan( String nodeId, long from, long to, String endReason ) {
	}

	/**
	 * One executed task run.
	 *
	 * @param occurrence The run's identity: the task plus its occurrence id. A non-clustered task has no
	 *                   occurrence id, so the soak synthesises one from the run's start instant.
	 * @param nodeId     The node that ran it
	 * @param start      When it started
	 * @param end        When it finished
	 * @param outcome    success, failed, or unknown
	 * @param clustered  Whether this run had to be claimed. A non-clustered task runs on <b>every</b> node
	 *                   by design, so the same occurrence on several nodes is correct behaviour and must
	 *                   never be reported as a duplicate.
	 */
	public record Execution( String occurrence, String nodeId, long start, long end, String outcome, boolean clustered ) {
	}

	/**
	 * Something that happened to a node, marked on its row.
	 *
	 * @param at     When it happened
	 * @param nodeId The node
	 * @param type   started, killed, retired, stopped, scaled
	 * @param detail Free text for the tooltip
	 */
	public record LifecycleEvent( long at, String nodeId, String type, String detail ) {
	}

	/**
	 * One node's view of the cluster at a moment in time, used to show that survivors drop a node that
	 * was turned off for good.
	 *
	 * @param observer The node that looked
	 * @param at       When it looked
	 * @param peers    Who it saw
	 */
	public record PeerObservation( String observer, long at, Set<String> peers ) {
	}

	/**
	 * Everything one run produced.
	 *
	 * @param mode         eager or balanced
	 * @param periodMillis Occurrence cadence
	 * @param startedAt    Run start, epoch millis
	 * @param endedAt      Run end, epoch millis
	 * @param spans        Node online spans
	 * @param executions   Executed occurrences
	 * @param events       Lifecycle markers
	 * @param peerViews    Peer observations
	 * @param passed       Whether the run's assertions passed
	 */
	public record Run(
	    String mode,
	    long periodMillis,
	    long startedAt,
	    long endedAt,
	    List<NodeSpan> spans,
	    List<Execution> executions,
	    List<LifecycleEvent> events,
	    List<PeerObservation> peerViews,
	    boolean passed,
	    List<String> failures ) {
	}

	/**
	 * Render the run as a complete HTML document.
	 *
	 * @param run The run to render
	 *
	 * @return The HTML
	 */
	public static String render( Run run ) {
		long	t0	= Math.min( run.startedAt(), run.spans().stream().mapToLong( NodeSpan::from ).min().orElse( run.startedAt() ) );
		long	t1	= Math.max( run.endedAt(), run.spans().stream().mapToLong( NodeSpan::to ).max().orElse( run.endedAt() ) );
		if ( t1 - t0 < 1_000 ) {
			t1 = t0 + 1_000;
		}

		List<String>				nodeIds	= orderedNodes( run );
		Map<String, List<NodeSpan>>	byNode	= spansByNode( run, nodeIds );
		int							top		= AXIS_H;
		int							sizeTop	= top;
		int							rowsTop	= sizeTop + SIZE_H + 8;

		// A node can be running several schedules at once, so each node's row is as tall as the number of
		// lanes its concurrent runs need. Stacking them on one line made overlapping runs unreadable.
		Map<String, LaneLayout>		lanes	= new LinkedHashMap<>();
		int							height	= rowsTop + PAD + 10;
		for ( String nodeId : nodeIds ) {
			LaneLayout layout = layoutLanes( run.executions().stream().filter( e -> e.nodeId().equals( nodeId ) ).toList() );
			lanes.put( nodeId, layout );
			height += layout.rowHeight();
		}

		StringBuilder svg = new StringBuilder();
		svg.append( "<svg id=\"timeline\" width=\"" ).append( LABEL_W + PLOT_W + STAT_W + PAD )
		    .append( "\" height=\"" ).append( height )
		    .append( "\" viewBox=\"0 0 " ).append( LABEL_W + PLOT_W + STAT_W + PAD ).append( " " ).append( height )
		    .append( "\" xmlns=\"http://www.w3.org/2000/svg\" font-family=\"Segoe UI, Helvetica, Arial, sans-serif\">" );

		svg.append( axis( t0, t1, top ) );
		svg.append( sizeStrip( run, t0, t1, sizeTop ) );

		int rowTop = rowsTop;
		for ( String nodeId : nodeIds ) {
			LaneLayout layout = lanes.get( nodeId );
			svg.append( nodeRow( run, nodeId, byNode.getOrDefault( nodeId, List.of() ), rowTop, layout, t0, t1 ) );
			rowTop += layout.rowHeight();
		}

		svg.append( "</svg>" );

		return document( run, nodeIds, byNode, svg.toString(), t0 );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Layout
	 * --------------------------------------------------------------------------
	 */

	private static int x( long at, long t0, long t1 ) {
		double ratio = ( double ) ( at - t0 ) / ( double ) ( t1 - t0 );
		return LABEL_W + ( int ) Math.round( ratio * PLOT_W );
	}

	private static String axis( long t0, long t1, int top ) {
		StringBuilder	out		= new StringBuilder();
		long			step	= tickStep( t1 - t0 );

		out.append( "<rect x=\"0\" y=\"0\" width=\"100%\" height=\"" ).append( top ).append( "\" fill=\"#fafafa\"/>" );

		for ( long at = ( ( t0 / step ) + 1 ) * step; at < t1; at += step ) {
			int px = x( at, t0, t1 );
			out.append( "<line x1=\"" ).append( px ).append( "\" y1=\"" ).append( top ).append( "\" x2=\"" ).append( px )
			    .append( "\" y2=\"100%\" stroke=\"#e0e0e0\" stroke-width=\"1\"/>" );
			out.append( "<text x=\"" ).append( px ).append( "\" y=\"" ).append( top - 12 )
			    .append( "\" font-size=\"11\" fill=\"#666\" text-anchor=\"middle\">" ).append( esc( clock( at, t0 ) ) ).append( "</text>" );
		}

		out.append( "<text x=\"" ).append( PAD ).append( "\" y=\"" ).append( top - 12 )
		    .append( "\" font-size=\"11\" fill=\"#333\" font-weight=\"600\">elapsed</text>" );
		return out.toString();
	}

	private static String sizeStrip( Run run, long t0, long t1, int top ) {
		StringBuilder		out			= new StringBuilder();
		List<long[]>		steps		= new ArrayList<>();
		Map<Long, Integer>	countAt		= new LinkedHashMap<>();

		// Build a step function of how many nodes were online at any moment
		TreeSet<Long>		instants	= new TreeSet<>();
		instants.add( t0 );
		for ( NodeSpan span : run.spans() ) {
			instants.add( Math.max( t0, span.from() ) );
			instants.add( Math.min( t1, span.to() ) );
		}
		instants.add( t1 );

		int			peak	= 0;
		List<Long>	points	= new ArrayList<>( instants );
		for ( Long at : points ) {
			int live = 0;
			for ( NodeSpan span : run.spans() ) {
				if ( span.from() <= at && span.to() > at ) {
					live++;
				}
			}
			countAt.put( at, live );
			peak = Math.max( peak, live );
		}
		peak = Math.max( 1, peak );

		int				baseline	= top + SIZE_H - 20;
		int				usable		= SIZE_H - 30;

		// A line graph of how many nodes were up. The filled area it used to be just restated the node bars
		// below without the runs, which told you nothing.
		StringBuilder	line		= new StringBuilder();
		for ( int i = 0; i < points.size(); i++ ) {
			long	at	= points.get( i );
			int		px	= x( at, t0, t1 );
			int		py	= baseline - ( int ) Math.round( ( ( double ) countAt.get( at ) / peak ) * usable );

			if ( i == 0 ) {
				line.append( "M " ).append( px ).append( " " ).append( py );
			} else {
				int previousY = baseline - ( int ) Math.round( ( ( double ) countAt.get( points.get( i - 1 ) ) / peak ) * usable );
				line.append( " L " ).append( px ).append( " " ).append( previousY ).append( " L " ).append( px ).append( " " ).append( py );
			}
		}
		out.append( "<path d=\"" ).append( line ).append( "\" fill=\"none\" stroke=\"#1e88e5\" stroke-width=\"2\"/>" );

		for ( Long at : points ) {
			int	px	= x( at, t0, t1 );
			int	py	= baseline - ( int ) Math.round( ( ( double ) countAt.get( at ) / peak ) * usable );
			out.append( "<circle cx=\"" ).append( px ).append( "\" cy=\"" ).append( py ).append( "\" r=\"2.5\" fill=\"#1e88e5\">" )
			    .append( "<title>" ).append( esc( clock( at, t0 ) + " - " + countAt.get( at ) + " node(s) up" ) ).append( "</title></circle>" );
		}

		out.append( "<text x=\"" ).append( PAD ).append( "\" y=\"" ).append( top + 16 )
		    .append( "\" font-size=\"11\" fill=\"#1e88e5\" font-weight=\"600\">nodes up (max " ).append( peak ).append( ")</text>" );

		for ( int level = 1; level <= peak; level++ ) {
			int y = baseline - ( int ) Math.round( ( ( double ) level / peak ) * usable );
			out.append( "<line x1=\"" ).append( LABEL_W ).append( "\" y1=\"" ).append( y ).append( "\" x2=\"" )
			    .append( LABEL_W + PLOT_W ).append( "\" y2=\"" ).append( y )
			    .append( "\" stroke=\"#e3f2fd\" stroke-width=\"1\"/>" );
			out.append( "<text x=\"" ).append( LABEL_W + PLOT_W + 6 ).append( "\" y=\"" ).append( y + 4 )
			    .append( "\" font-size=\"10\" fill=\"#90a4ae\">" ).append( level ).append( "</text>" );
		}

		return out.toString();
	}

	/**
	 * Plain-language description of how a lifespan ended.
	 *
	 * @param endReason stopped, killed, retired or crashed
	 *
	 * @return The description
	 */
	private static String endReasonText( String endReason ) {
		return switch ( endReason ) {
			case "killed" -> "killed without a graceful shutdown";
			case "crashed" -> "the process died on its own (crash)";
			case "retired" -> "node taken out of service";
			case "stopped" -> "node stopped";
			default -> "ended: " + endReason;
		};
	}

	/**
	 * One node's row layout: how many concurrent lanes its runs need, and which lane each run sits in.
	 *
	 * @param laneOf    execution start millis to lane index
	 * @param laneCount number of lanes
	 * @param rowHeight the row height that fits them
	 */
	private record LaneLayout( Map<Long, Integer> laneOf, int laneCount, int rowHeight ) {
	}

	private static final int LANE_H = 15;

	/**
	 * Assign each run of a node to the first lane that is free when it starts. Overlapping runs then sit on
	 * separate lines instead of drawing over each other.
	 */
	private static LaneLayout layoutLanes( List<Execution> executions ) {
		List<Execution> sorted = new ArrayList<>( executions );
		sorted.sort( Comparator.comparingLong( Execution::start ) );

		List<Long>			laneEnds	= new ArrayList<>();
		Map<Long, Integer>	laneOf		= new LinkedHashMap<>();

		for ( Execution execution : sorted ) {
			int lane = -1;
			for ( int i = 0; i < laneEnds.size(); i++ ) {
				if ( laneEnds.get( i ) <= execution.start() ) {
					lane = i;
					break;
				}
			}
			if ( lane < 0 ) {
				lane = laneEnds.size();
				laneEnds.add( execution.end() );
			} else {
				laneEnds.set( lane, execution.end() );
			}
			laneOf.put( execution.start(), lane );
		}

		int lanes = Math.max( 1, laneEnds.size() );
		return new LaneLayout( laneOf, lanes, Math.max( ROW_H, ( lanes * LANE_H ) + 12 ) );
	}

	private static String nodeRow( Run run, String nodeId, List<NodeSpan> spans, int top, LaneLayout layout, long t0, long t1 ) {
		StringBuilder	out				= new StringBuilder();
		int				rowHeight		= layout == null ? ROW_H : layout.rowHeight();
		int				midY			= top + ( rowHeight / 2 ) + 4;
		long			onlineMillis	= spans.stream().mapToLong( span -> span.to() - span.from() ).sum();
		List<Execution>	executions		= run.executions().stream().filter( e -> e.nodeId().equals( nodeId ) ).toList();

		// A row separator rather than a filled band: the fill would hide the vertical grid, and the grid
		// is what makes it possible to read a bar's time off the axis above.
		out.append( "<line x1=\"0\" y1=\"" ).append( top ).append( "\" x2=\"" ).append( LABEL_W + PLOT_W + STAT_W )
		    .append( "\" y2=\"" ).append( top ).append( "\" stroke=\"#f0f0f0\" stroke-width=\"1\"/>" );

		out.append( "<text x=\"" ).append( PAD ).append( "\" y=\"" ).append( top + 20 )
		    .append( "\" font-size=\"12\" fill=\"#222\" font-weight=\"600\">" ).append( esc( nodeId ) ).append( "</text>" );

		for ( NodeSpan span : spans ) {
			int		from	= x( Math.max( span.from(), t0 ), t0, t1 );
			int		to		= Math.max( x( Math.min( span.to(), t1 ), t0, t1 ), from + 2 );
			String	fill	= switch ( span.endReason() ) {
								case "crashed" -> "#e1bee7";
								case "killed" -> "#ffcdd2";
								default -> "#c8e6c9";
							};
			String	stroke	= switch ( span.endReason() ) {
								case "crashed" -> "#6a1b9a";
								case "killed" -> "#c62828";
								default -> "#2e7d32";
							};

			out.append( "<rect x=\"" ).append( from ).append( "\" y=\"" ).append( top + 5 ).append( "\" width=\"" )
			    .append( to - from ).append( "\" height=\"" ).append( rowHeight - 10 ).append( "\" rx=\"3\" fill=\"" ).append( fill )
			    .append( "\" stroke=\"" ).append( stroke ).append( "\" stroke-width=\"1\">" )
			    .append( "<title>" ).append( esc( nodeId + " online " + clock( span.from(), t0 ) + " to " + clock( span.to(), t0 )
			        + " (" + seconds( span.to() - span.from() ) + "), " + endReasonText( span.endReason() ) ) )
			    .append( "</title></rect>" );

			if ( !"stopped".equals( span.endReason() ) ) {
				int endX = x( Math.min( span.to(), t1 ), t0, t1 );
				out.append( "<line x1=\"" ).append( endX ).append( "\" y1=\"" ).append( top + 2 ).append( "\" x2=\"" ).append( endX )
				    .append( "\" y2=\"" ).append( top + rowHeight - 2 ).append( "\" stroke=\"" ).append( stroke )
				    .append( "\" stroke-width=\"2\"/>" );
			}

			// When the graceful shutdown was requested. From that instant the scheduler accepts no new runs,
			// so the remainder of the bar is drain time - without this the tail looks like a scheduler that
			// silently stopped working, which is exactly how it was read.
			long requestedAt = -1;
			for ( LifecycleEvent event : run.events() ) {
				if ( "stop-requested".equals( event.type() ) && event.nodeId().equals( nodeId )
				    && event.at() >= span.from() && event.at() <= span.to() ) {
					requestedAt = Math.max( requestedAt, event.at() );
				}
			}
			if ( requestedAt > 0 ) {
				int				fromX		= x( Math.max( requestedAt, t0 ), t0, t1 );
				int				toX			= x( Math.min( span.to(), t1 ), t0, t1 );
				int				drainMs		= ( int ) ( span.to() - requestedAt );

				// Say what the drain is actually waiting for. A node with nothing executing is not sitting idle:
				// it is finishing a claim attempt it had already started, and that has to complete or the
				// occurrence could be left un-run by every node.
				List<String>	waiting		= runsInFlightAt( run, nodeId, requestedAt );
				String			waitingText	= waiting.isEmpty()
				    ? "no task body was running - finishing a claim attempt already in progress"
				    : "waiting for " + String.join( ", ", waiting ) + " to finish";

				out.append( "<rect x=\"" ).append( fromX ).append( "\" y=\"" ).append( top + 5 ).append( "\" width=\"" )
				    .append( Math.max( 2, toX - fromX ) ).append( "\" height=\"" ).append( rowHeight - 10 )
				    .append( "\" rx=\"3\" fill=\"#ffe0b2\" fill-opacity=\"0.9\" stroke=\"#ef6c00\" stroke-width=\"1\"" )
				    .append( " stroke-dasharray=\"3 2\">" )
				    .append( "<title>" ).append( esc( "shutdown requested at " + clock( requestedAt, t0 ) + ", process exited at "
				        + clock( span.to(), t0 ) + " - " + String.format( "%.1f", drainMs / 1000.0 )
				        + "s draining, no new runs accepted; " + waitingText ) )
				    .append( "</title></rect>" );
				out.append( "<line x1=\"" ).append( fromX ).append( "\" y1=\"" ).append( top + 2 ).append( "\" x2=\"" ).append( fromX )
				    .append( "\" y2=\"" ).append( top + rowHeight - 2 ).append( "\" stroke=\"#e65100\" stroke-width=\"2\" stroke-dasharray=\"4 2\"/>" );
				out.append( "<text x=\"" ).append( fromX + 3 ).append( "\" y=\"" ).append( top + rowHeight - 3 )
				    .append( "\" font-size=\"9\" fill=\"#bf360c\" font-weight=\"600\">draining "
				        + String.format( "%.1f", drainMs / 1000.0 ) + "s"
				        + ( waiting.isEmpty() ? " (claim)" : " (" + esc( String.join( ", ", waiting ) ) + ")" ) )
				    .append( "</text>" );
			}
		}

		for ( Execution execution : executions ) {
			int		from	= x( Math.max( execution.start(), t0 ), t0, t1 );
			int		to		= Math.max( x( Math.min( execution.end(), t1 ), t0, t1 ), from + 4 );
			String	fill	= switch ( execution.outcome() ) {
								case "failed" -> "#b71c1c";
								case "unknown", "running" -> "#f9a825";
								default -> "#1b5e20";
							};

			// Each concurrent run gets its own lane, so overlapping schedules on one node are readable instead
			// of drawing over each other.
			int		lane	= layout == null ? 0 : layout.laneOf().getOrDefault( execution.start(), 0 );
			out.append( "<rect x=\"" ).append( from ).append( "\" y=\"" ).append( top + 7 + ( lane * LANE_H ) ).append( "\" width=\"" )
			    .append( to - from ).append( "\" height=\"" ).append( LANE_H - 4 ).append( "\" rx=\"2\" fill=\"" ).append( fill )
			    .append( "\" stroke=\"#ffffff\" stroke-width=\"0.75\">" )
			    .append( "<title>" ).append( esc( execution.occurrence() + " ran on " + nodeId + " at " + clock( execution.start(), t0 )
			        + " for " + ( execution.end() - execution.start() ) + "ms (" + execution.outcome() + ")" ) )
			    .append( "</title></rect>" );
		}

		out.append( "<text x=\"" ).append( LABEL_W + PLOT_W + 6 ).append( "\" y=\"" ).append( midY )
		    .append( "\" font-size=\"10\" fill=\"#555\">" )
		    .append( executions.size() ).append( " runs &#183; " ).append( spans.size() ).append( " start" ).append( spans.size() == 1 ? "" : "s" )
		    .append( " &#183; " ).append( seconds( onlineMillis ) )
		    .append( "</text>" );

		return out.toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Document
	 * --------------------------------------------------------------------------
	 */

	private static String document( Run run, List<String> nodeIds, Map<String, List<NodeSpan>> byNode, String svg, long t0 ) {
		Map<String, Set<String>> byOccurrence = new LinkedHashMap<>();
		for ( Execution execution : run.executions() ) {
			byOccurrence.computeIfAbsent( execution.occurrence(), key -> new TreeSet<>() ).add( execution.nodeId() );
		}
		// A duplicate is the same *clustered* run executed by more than one node. A non-clustered task is
		// supposed to run on every node, so its fan-out is expected and is shown rather than flagged.
		List<String>	duplicates	= byOccurrence.entrySet().stream()
		    .filter( entry -> entry.getValue().size() > 1 && isClustered( run, entry.getKey() ) )
		    .map( entry -> entry.getKey() + " on " + entry.getValue() )
		    .toList();

		long			duration	= run.endedAt() - run.startedAt();
		int				starts		= run.spans().size();
		int				retires		= ( int ) run.events().stream().filter( e -> "retired".equals( e.type() ) ).count();
		int				kills		= ( int ) run.events().stream().filter( e -> "killed".equals( e.type() ) ).count();
		int				crashes		= ( int ) run.events().stream().filter( e -> "crashed".equals( e.type() ) ).count();
		int				fresh		= ( int ) run.events().stream().filter( e -> "started".equals( e.type() ) && e.detail().contains( "fresh" ) ).count();
		int				peak		= peakNodes( run );

		StringBuilder	html		= new StringBuilder();
		html.append(
		    """
		    <!DOCTYPE html>
		    <html lang="en">
		    <head>
		    <meta charset="utf-8">
		    <title>Cluster soak timeline</title>
		    <style>
		      :root { --ink:#1c1c1c; --muted:#6b7280; --line:#e5e7eb; }
		      * { box-sizing: border-box; }
		      body { margin:0; padding:24px 32px 48px; font-family:"Segoe UI",Helvetica,Arial,sans-serif; color:var(--ink); background:#fff; }
		      h1 { font-size:20px; margin:0 0 4px; }
		      h2 { font-size:15px; margin:28px 0 10px; text-transform:uppercase; letter-spacing:.06em; color:var(--muted); }
		      .sub { color:var(--muted); font-size:13px; margin-bottom:18px; }
		      .cards { display:flex; flex-wrap:wrap; gap:10px; margin-bottom:8px; }
		      .card { border:1px solid var(--line); border-radius:8px; padding:10px 14px; min-width:118px; }
		      .card .k { font-size:11px; text-transform:uppercase; letter-spacing:.06em; color:var(--muted); }
		      .card .v { font-size:19px; font-weight:600; margin-top:2px; }
		      .card.pass { border-color:#a5d6a7; background:#f1f8f2; }
		      .card.fail { border-color:#ef9a9a; background:#fdf2f2; }
		      .banner { border-radius:8px; padding:10px 14px; font-size:13px; margin:14px 0 4px; }
		      .banner.ok { background:#f1f8f2; border:1px solid #a5d6a7; }
		      .banner.bad { background:#fdf2f2; border:1px solid #ef9a9a; }
		      .legend { display:flex; flex-wrap:wrap; gap:16px; font-size:12px; color:#374151; margin-top:10px; }
		      .legend span { display:flex; align-items:center; gap:6px; }
		      .swatch { width:14px; height:12px; border-radius:2px; display:inline-block; }
		      svg { border:1px solid var(--line); border-radius:8px; background:#fff; }
		      table { border-collapse:collapse; font-size:12px; width:100%; margin-top:8px; }
		      th, td { border-bottom:1px solid var(--line); padding:5px 8px; text-align:left; }
		      th { background:#f9fafb; font-weight:600; }
		      code { background:#f3f4f6; padding:1px 5px; border-radius:4px; font-size:12px; }
		      details { margin-top:8px; }
		      summary { cursor:pointer; font-size:13px; color:#1d4ed8; }
		    </style>
		    </head>
		    <body>
		    <h1>Cluster soak timeline</h1>
		    <div class="sub">mode <code>/*__MODE__*/</code> &#183; /*__SCHEDULES__*/ overlapping schedules &#183; run <code>/*__DURATION__*/</code></div>
		    <div class="cards">
		      /*__CARDS__*/
		    </div>
		    <div class="banner /*__BANNER_CLASS__*/">/*__BANNER__*/</div>
		    <h2>Stacked timeline</h2>
		    /*__SVG__*/
		    <div class="legend">
		      <strong style="font-size:12px">node:</strong>
		      <span><i class="swatch" style="background:#c8e6c9;border:1px solid #2e7d32"></i>up</span>
		      <span><i class="swatch" style="background:#e1bee7;border:1px solid #6a1b9a"></i>died on its own (crash)</span>
		    </div>
		    <div class="legend">
		      <strong style="font-size:12px">task run:</strong>
		      <span><i class="swatch" style="background:#1b5e20"></i>ran successfully</span>
		      <span><i class="swatch" style="background:#b71c1c"></i>failed</span>
		      <span><i class="swatch" style="background:#f9a825"></i>started but never finished</span>
		      <span><i class="swatch" style="background:#ffe0b2;border:1px dashed #ef6c00"></i>shutdown requested &#8594; draining (no new runs accepted)</span>
		    </div>
		    <div class="legend">
		      <strong style="font-size:12px">other:</strong>
		      <span><i class="swatch" style="background:#bbdefb;border:1px solid #1e88e5"></i>nodes up (top strip)</span>
		      <span>hover any bar or marker for its exact times</span>
		    </div>
		    <h2>Nodes</h2>
		    <div class="sub">Load is measured in task-seconds - one task running for one second is 1.0 - which is
		    the same unit the cluster uses to rank nodes in balanced mode. Dividing it by a node's uptime gives
		    the average number of tasks it had running at once, so nodes that came and went can be compared
		    fairly.</div>
		    /*__NODES__*/
		    <h2>Task runs</h2>
		    <details/*__OPEN__*/>
		      <summary>/*__EXEC_SUMMARY__*/</summary>
		      /*__EXEC_TABLE__*/
		    </details>
		    </body>
		    </html>
		    """ );

		html = new StringBuilder( html.toString()
		    .replace( "/*__MODE__*/", esc( run.mode() ) )
		    .replace( "/*__SCHEDULES__*/", String.valueOf( distinctSchedules( run ) ) )
		    .replace( "/*__DURATION__*/", duration( duration ) )
		    .replace( "/*__CARDS__*/", cards( run, nodeIds, byOccurrence, duration, crashes, peak ) )
		    .replace( "/*__BANNER_CLASS__*/", duplicates.isEmpty() && run.passed() ? "ok" : "bad" )
		    .replace( "/*__BANNER__*/", banner( run, duplicates, byOccurrence ) )
		    .replace( "/*__SVG__*/", svg )
		    .replace( "/*__NODES__*/", nodeTable( nodeIds, byNode, run, t0 ) )
		    .replace( "/*__OPEN__*/", duplicates.isEmpty() ? "" : " open" )
		    .replace( "/*__EXEC_SUMMARY__*/", byOccurrence.size() + " task runs, " + run.executions().size() + " executions" )
		    .replace( "/*__EXEC_TABLE__*/", executionTable( byOccurrence, run ) ) );

		return html.toString();
	}

	private static int distinctSchedules( Run run ) {
		return ( int ) run.executions().stream()
		    .map( execution -> execution.occurrence().contains( " @" )
		        ? execution.occurrence().substring( 0, execution.occurrence().indexOf( " @" ) )
		        : execution.occurrence() )
		    .distinct()
		    .count();
	}

	private static String cards( Run run, List<String> nodeIds, Map<String, Set<String>> byOccurrence, long duration, int crashes, int peak ) {
		long			clusteredRuns	= byOccurrence.keySet().stream().filter( key -> isClustered( run, key ) ).count();
		long			unclusteredRuns	= run.executions().stream().filter( execution -> !execution.clustered() ).count();

		StringBuilder	out				= new StringBuilder();
		out.append( card( "result", run.passed() ? "PASS" : "FAIL", run.passed() ? "pass" : "fail" ) );
		out.append( card( "clustered runs", String.valueOf( clusteredRuns ), null ) );
		if ( unclusteredRuns > 0 ) {
			out.append( card( "unclustered runs", String.valueOf( unclusteredRuns ), null ) );
		}
		out.append( card( "duplicates", String.valueOf( duplicateCount( run, byOccurrence ) ),
		    duplicateCount( run, byOccurrence ) > 0 ? "fail" : null ) );
		out.append( card( "nodes seen", String.valueOf( nodeIds.size() ), null ) );
		out.append( card( "peak online", String.valueOf( peak ), null ) );
		out.append( card( "crashes", String.valueOf( crashes ), crashes > 0 ? "fail" : null ) );
		out.append( card( "duration", duration( duration ), null ) );

		return out.toString();
	}

	private static String card( String key, String value, String cssClass ) {
		return "<div class=\"card" + ( cssClass != null ? " " + cssClass : "" ) + "\"><div class=\"k\">" + esc( key )
		    + "</div><div class=\"v\">" + esc( value ) + "</div></div>";
	}

	private static String banner( Run run, List<String> duplicates, Map<String, Set<String>> byOccurrence ) {
		List<String> reasons = new java.util.ArrayList<>();

		if ( !duplicates.isEmpty() ) {
			reasons.add( duplicates.size() + " task run(s) executed more than once: " + String.join( ", ", duplicates ) );
		}

		long unfinished = run.executions().stream()
		    .filter( execution -> !"success".equals( execution.outcome() ) && !"failed".equals( execution.outcome() ) )
		    .count();
		if ( unfinished > 0 ) {
			reasons.add( unfinished + " task run(s) started but never finished" );
		}

		long failed = run.executions().stream().filter( execution -> "failed".equals( execution.outcome() ) ).count();
		if ( failed > 0 ) {
			reasons.add( failed + " task run(s) failed" );
		}

		long crashes = run.events().stream().filter( event -> "crashed".equals( event.type() ) ).count();
		if ( crashes > 0 ) {
			reasons.add( crashes + " node(s) died on their own" );
		}

		// PASS or FAIL always has to say why. A green tick above the word FAIL is worse than no report.
		if ( !run.passed() ) {
			reasons.addAll( run.failures() );
			if ( reasons.isEmpty() ) {
				reasons.add( "a check failed that is not visible in this report - the soak's console output names it" );
			}
			return "&#10007; FAILED: " + String.join( "; ", reasons );
		}

		return "&#10003; passed - " + byOccurrence.keySet().stream().filter( key -> isClustered( run, key ) ).count()
		    + " clustered runs across " + run.executions().size() + " executions, no duplicates, every run finished"
		    + ( run.executions().stream().anyMatch( execution -> !execution.clustered() )
		        ? ", plus " + run.executions().stream().filter( execution -> !execution.clustered() ).count()
		            + " unclustered runs (expected on every node)"
		        : "" );
	}

	private static String nodeTable( List<String> nodeIds, Map<String, List<NodeSpan>> byNode, Run run, long t0 ) {
		StringBuilder out = new StringBuilder(
		    "<table><tr><th>Node</th><th>Up</th><th>Runs</th><th>Task-seconds</th><th>Load / sec up</th></tr>" );
		for ( String nodeId : nodeIds ) {
			List<NodeSpan>	spans		= byNode.getOrDefault( nodeId, List.of() );
			long			online		= spans.stream().mapToLong( span -> span.to() - span.from() ).sum();
			List<Execution>	executions	= run.executions().stream().filter( e -> e.nodeId().equals( nodeId ) ).toList();
			// Task-seconds: the same load unit the cluster ranks nodes by. Dividing by uptime is what makes
			// a node that was only up briefly comparable with one that ran the whole time.
			double			taskSeconds	= executions.stream().mapToDouble( e -> ( e.end() - e.start() ) / 1000.0 ).sum();
			double			loadPerSec	= online > 0 ? taskSeconds / ( online / 1000.0 ) : 0;

			out.append( "<tr><td><strong>" ).append( esc( nodeId ) ).append( "</strong></td><td>" ).append( seconds( online ) )
			    .append( "</td><td>" ).append( executions.size() )
			    .append( "</td><td>" ).append( String.format( "%.1f", taskSeconds ) )
			    .append( "</td><td>" ).append( String.format( "%.3f", loadPerSec ) )
			    .append( "</td></tr>" );
		}
		return out.append( "</table>" ).toString();
	}

	private static String executionTable( Map<String, Set<String>> byOccurrence, Run run ) {
		Map<String, List<Execution>> grouped = new LinkedHashMap<>();
		for ( Execution execution : run.executions() ) {
			grouped.computeIfAbsent( execution.occurrence(), key -> new ArrayList<>() ).add( execution );
		}

		// One list, in the order things actually happened. The occurrence id is "task @ fire time", so
		// sorting by the whole string would group by task name and destroy the chronology - sort by the fire
		// time first, and use the task name only to break ties.
		List<String>	order	= grouped.keySet().stream()
		    .sorted( Comparator.comparing( ClusterSoakTimeline::fireTime ).thenComparing( Comparator.naturalOrder() ) )
		    .toList();

		StringBuilder	out		= new StringBuilder(
		    "<table><tr><th>Run</th><th>Node</th><th>Duration</th><th>Result</th></tr>" );

		for ( String occurrence : order ) {
			List<Execution> executions = grouped.get( occurrence );

			if ( isClustered( run, occurrence ) ) {
				Execution	execution	= executions.get( 0 );
				Set<String>	runners		= byOccurrence.get( occurrence );
				String		flag		= runners.size() > 1 ? " style=\"background:#fdf2f2\"" : "";

				out.append( "<tr" ).append( flag ).append( "><td><code>" ).append( esc( occurrence ) ).append( "</code></td><td>" )
				    .append( esc( execution.nodeId() ) ).append( "</td><td>" )
				    .append( ( execution.end() - execution.start() ) + "ms" ).append( "</td><td>" )
				    .append( esc( execution.outcome() ) ).append( "</td></tr>" );
			} else {
				// A non-clustered task runs on every node, so its fan-out collapses into one row here, with
				// the per-node details as comma-delimited lists in the same column order. It sorts into the
				// same list as everything else rather than being set aside.
				List<Execution> fanned = new ArrayList<>( executions );
				fanned.sort( Comparator.comparing( Execution::nodeId ) );

				out.append( "<tr><td><code>" ).append( esc( occurrence ) ).append( "</code></td><td>" )
				    .append( esc( String.join( ", ", fanned.stream().map( Execution::nodeId ).toList() ) ) ).append( "</td><td>" )
				    .append( esc( String.join( ", ", fanned.stream().map( e -> ( e.end() - e.start() ) + "ms" ).toList() ) ) )
				    .append( "</td><td>" )
				    .append( esc( String.join( ", ", fanned.stream().map( Execution::outcome ).toList() ) ) )
				    .append( "</td></tr>" );
			}
		}

		return out.append( "</table>" ).toString();
	}

	/**
	 * The task runs executing on a node at a given instant, named by task. Used to explain what a shutdown drain
	 * was actually waiting for.
	 *
	 * @param run    The run
	 * @param nodeId The node
	 * @param at     The instant
	 *
	 * @return The task names of the runs in flight, or an empty list
	 */
	private static List<String> runsInFlightAt( Run run, String nodeId, long at ) {
		return run.executions().stream()
		    .filter( execution -> execution.nodeId().equals( nodeId ) )
		    .filter( execution -> execution.start() <= at && execution.end() > at )
		    .map( execution -> execution.occurrence().contains( " @" )
		        ? execution.occurrence().substring( 0, execution.occurrence().indexOf( " @" ) )
		        : execution.occurrence() )
		    .distinct()
		    .sorted()
		    .toList();
	}

	/**
	 * The fire time inside an occurrence id, used for chronological ordering.
	 *
	 * @param occurrence The occurrence id, {@code task @ yyyy-MM-dd'T'HH:mm:ss}
	 *
	 * @return The fire time as it appears in the id
	 */
	private static String fireTime( String occurrence ) {
		int at = occurrence.indexOf( '@' );
		// Tolerant of whether the id was written "task @ time" or "task @time"
		return at < 0 ? occurrence : occurrence.substring( at + 1 ).trim();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	private static List<String> orderedNodes( Run run ) {
		Map<String, Long> firstSeen = new LinkedHashMap<>();
		run.spans().stream().sorted( Comparator.comparingLong( NodeSpan::from ) )
		    .forEach( span -> firstSeen.putIfAbsent( span.nodeId(), span.from() ) );
		run.executions().stream().sorted( Comparator.comparingLong( Execution::start ) )
		    .forEach( execution -> firstSeen.putIfAbsent( execution.nodeId(), execution.start() ) );
		return new ArrayList<>( firstSeen.keySet() );
	}

	private static Map<String, List<NodeSpan>> spansByNode( Run run, List<String> nodeIds ) {
		Map<String, List<NodeSpan>> byNode = new LinkedHashMap<>();
		nodeIds.forEach( nodeId -> byNode.put( nodeId, new ArrayList<>() ) );
		for ( NodeSpan span : run.spans().stream().sorted( Comparator.comparingLong( NodeSpan::from ) ).toList() ) {
			byNode.computeIfAbsent( span.nodeId(), key -> new ArrayList<>() ).add( span );
		}
		return byNode;
	}

	private static int peakNodes( Run run ) {
		Set<Long> instants = new LinkedHashSet<>();
		run.spans().forEach( span -> {
			instants.add( span.from() );
			instants.add( span.to() );
		} );
		int peak = 0;
		for ( Long at : instants ) {
			int live = 0;
			for ( NodeSpan span : run.spans() ) {
				if ( span.from() <= at && span.to() > at ) {
					live++;
				}
			}
			peak = Math.max( peak, live );
		}
		return peak;
	}

	private static int duplicateCount( Run run, Map<String, Set<String>> byOccurrence ) {
		return ( int ) byOccurrence.entrySet().stream()
		    .filter( entry -> entry.getValue().size() > 1 && isClustered( run, entry.getKey() ) )
		    .count();
	}

	/**
	 * Whether a run had to be claimed. Derived from the executions themselves, so the renderer never has to
	 * be told which task names are clustered.
	 *
	 * @param run        The run
	 * @param occurrence The occurrence key
	 *
	 * @return true when every execution of this occurrence was a clustered one
	 */
	private static boolean isClustered( Run run, String occurrence ) {
		return run.executions().stream()
		    .filter( execution -> execution.occurrence().equals( occurrence ) )
		    .allMatch( Execution::clustered );
	}

	private static long tickStep( long spanMillis ) {
		long[] candidates = { 5_000, 10_000, 15_000, 30_000, 60_000, 120_000, 300_000, 600_000 };
		for ( long candidate : candidates ) {
			if ( spanMillis / candidate <= 12 ) {
				return candidate;
			}
		}
		return candidates[ candidates.length - 1 ];
	}

	private static String clock( long at, long t0 ) {
		long	elapsed	= Math.max( 0, at - t0 );
		long	minutes	= elapsed / 60_000;
		long	seconds	= ( elapsed % 60_000 ) / 1_000;
		long	millis	= elapsed % 1_000;
		return String.format( "%d:%02d.%01d", minutes, seconds, millis / 100 );
	}

	private static String seconds( long millis ) {
		long totalSeconds = Math.round( millis / 1000.0 );
		if ( totalSeconds < 60 ) {
			return totalSeconds + "s";
		}
		return ( totalSeconds / 60 ) + "m " + ( totalSeconds % 60 ) + "s";
	}

	private static String duration( long millis ) {
		return seconds( millis );
	}

	private static String esc( String value ) {
		if ( value == null ) {
			return "";
		}
		return value.replace( "&", "&amp;" ).replace( "<", "&lt;" ).replace( ">", "&gt;" ).replace( "\"", "&quot;" );
	}

}
