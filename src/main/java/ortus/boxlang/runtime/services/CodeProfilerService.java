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
package ortus.boxlang.runtime.services;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.coverage.CoverageDataBuilder;
import ortus.boxlang.runtime.services.coverage.CoverageJSONExporter;
import ortus.boxlang.runtime.services.coverage.CoverageSonarQubeExporter;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * The runtime {@link IService} responsible for collecting per-source-span
 * execution data produced by instrumented bytecode.
 * <p>
 * Design (see workbench/internal-docs/COVERAGE_DESIGN.md): the boxpiler bakes a
 * per-file BLUEPRINT of every executable SPAN into the generated class. When the
 * class is first loaded, it registers the blueprint
 * ({@link #registerBlueprint}), getting a compact {@code fileId}, and the service
 * pre-allocates a counter for every executable span. Instrumented bytecode then
 * calls {@link #mark(int,int)} with a file id + span id — a direct array-index
 * increment, no positions.
 * <p>
 * A span is the atomic executable unit. Non-executable spans (whitespace, `?`,
 * `:`, `;`, braces, `else`, comments) have no id and are never registered. Spans
 * support 3 states: COVERED (ran), MISSED (registered but never ran), and
 * NOT_EXECUTABLE (not in the blueprint).
 * <p>
 * Only {@link #mark} is on the hot path; everything else is lifecycle/querying.
 */
public class CodeProfilerService extends BaseService {

	/**
	 * The singleton instance. Used by the static {@link #mark} to delegate.
	 */
	private static CodeProfilerService						instance;

	/**
	 * Whether live capture is active. Off => {@link #mark} returns immediately.
	 */
	private volatile boolean								active					= false;

	/**
	 * Assigns compact file ids to blueprints.
	 */
	// REMOVED: the file id is the blueprint KEY (path or source hash) — no integer
	// sequence. (fileIdSeq removed)

	/**
	 * Registered blueprints by NORMALIZED file path / source hash (the blueprint
	 * KEY — the "id" bytecode embeds as a String static field).
	 */
	private final ConcurrentHashMap<String, FileBlueprint>	byPath					= new ConcurrentHashMap<>();

	/**
	 * Thread-local timestamp of the last span fired on this thread (probe-charging).
	 */
	private final ThreadLocal<Long>							lastNano				= new ThreadLocal<>();

	/**
	 * Thread-local FileBlueprint that opened the current interval.
	 */
	private final ThreadLocal<FileBlueprint>				lastFile				= new ThreadLocal<>();

	/**
	 * Thread-local span id that opened the current interval.
	 */
	private final ThreadLocal<Integer>						lastSpanId				= new ThreadLocal<>();

	/**
	 * The capture ACTIVATION GENERATION. Bumped every time capture is re-enabled
	 * ({@link #setActive(true)}). Each thread records the generation it last saw
	 * when it opened its probe interval; on the next {@link #mark}, a mismatch means
	 * the interval was opened in a PREVIOUS capture session (before a disable/
	 * enable cycle), so the huge wall-clock gap (the disabled period) must NOT be
	 * charged to the old span. Without this, re-enabling capture would corrupt
	 * every worker thread's timing data for the whole time capture was off.
	 */
	private volatile long									activationGeneration	= 0L;

	/**
	 * Thread-local activation generation of the interval currently open on this
	 * thread (0 = none/unknown). Compared against {@link #activationGeneration} in
	 * {@link #mark}/{@link #markEnd} to detect stale cross-disable intervals.
	 */
	private final ThreadLocal<Long>							lastGeneration			= new ThreadLocal<>();

	/**
	 * Per-thread stack of open call-site brackets, for INCLUSIVE call timing
	 * ("how long was this line on the stack"). A call site emits {@code markEnter}
	 * before the invocation and {@code markExit} after it; the publishable product
	 * charges the elapsed wall time to the call-site span's {@code inclusiveNanos}
	 * (which, unlike self time, includes everything the call itself ran). The stack
	 * makes nested calls in one expression (e.g. {@code foo( bar() )}) attribute to
	 * the correct call site.
	 */
	private final ThreadLocal<CallStack>					callStack				= ThreadLocal.withInitial( CallStack::new );

	/**
	 * Mutable per-thread call-bracket stack. Uses primitive arrays grown on demand
	 * (no per-call allocation). Each frame holds the call-site span, its start time,
	 * and the call-site blueprint, so {@link #markExit} can accumulate inclusive time
	 * onto the right span. The brackets are PURELY ADDITIVE — they never touch the
	 * flat probe-charging owner, so self-time accounting is byte-for-byte unchanged.
	 */
	private static final class CallStack {

		long[]		nums	= new long[ 32 ];	// interleaved [callSpanId, startNano] per frame
		Object[]	files	= new Object[ 16 ];	// call-site FileBlueprint
		int[][]		shells	= new int[ 16 ][];	// call-site SHELL members to charge on exit (see #markEnter)
		int			depth	= 0;

		void ensureCapacity( int d ) {
			if ( 2 * d + 1 >= nums.length ) {
				nums = java.util.Arrays.copyOf( nums, nums.length * 2 );
			}
			if ( d >= files.length ) {
				files = java.util.Arrays.copyOf( files, files.length * 2 );
			}
			if ( d >= shells.length ) {
				shells = java.util.Arrays.copyOf( shells, shells.length * 2 );
			}
		}
	}

	/**
	 * Construct the CodeProfilerService. Called by {@link BoxRuntime} during startup.
	 *
	 * @param runtime the BoxRuntime singleton
	 */
	public CodeProfilerService( BoxRuntime runtime ) {
		super( runtime, Key.codeProfilerService );
		instance = this;
	}

	// -------------------------------------------------------------------------
	// IService lifecycle
	// -------------------------------------------------------------------------

	@Override
	public void onConfigurationLoad() {
		// No config wiring yet.
	}

	@Override
	public void onStartup() {
		// No-op for now.
	}

	@Override
	public void onShutdown( Boolean force ) {
		instance = null;
	}

	// -------------------------------------------------------------------------
	// Hot-path (instrumentation-facing) API
	// -------------------------------------------------------------------------

	/**
	 * Record that one span executed, charging elapsed time since the previous
	 * span on this thread to the span that was current (probe-charging).
	 * <p>
	 * The ONLY method instrumented bytecode calls for a single span. Direct array
	 * increment, no positions, no map lookup, NO ALLOCATION (it does not build an
	 * {@code int[]} to hand to the varargs form — the single-span logic is inline).
	 * Returns immediately when inactive.
	 *
	 * @param id     the blueprint KEY — the normalized file path, or the source
	 *               hash when there is no file. This is the SAME key that
	 *               {@link #registerBlueprintForFile}/{@link #registerBlueprintForSource}
	 *               stored the blueprint under, and is embedded as a String static
	 *               field in the instrumented class.
	 * @param spanId the id of the executable span within that file's blueprint
	 *               (0-based; indexes the blueprint's executable span array)
	 */
	public static void mark( String id, int spanId ) {
		mark( id, spanId, null );
	}

	/**
	 * Record that one span executed, charging elapsed time since the previous
	 * span on this thread to the span that was current (probe-charging).
	 * <p>
	 * As {@link #mark(String, int)} but also receives the class's own
	 * {@link Blueprint}. If the blueprint is not currently registered for
	 * {@code id} — e.g. it was cleared by {@link #reset()} — it is RE-REGISTERED
	 * from this reference, so an already-loaded instrumented class (whose
	 * {@code <clinit>} cannot run again) keeps recording marks across a reset.
	 *
	 * @param id        the blueprint KEY (normalized file path or source hash)
	 * @param spanId    the id of the executable span within that blueprint
	 * @param blueprint the class's blueprint, or null when not supplied
	 */
	public static void mark( String id, int spanId, Blueprint blueprint ) {
		CodeProfilerService service = instance;
		if ( service == null || !service.active ) {
			return;
		}

		long	now				= System.nanoTime();
		long	curGeneration	= service.activationGeneration;
		Long	prevNano		= service.lastNano.get();
		Long	prevGeneration	= service.lastGeneration.get();

		// Charge the interval since the previous timestamp to the span that opened it.
		// SKIP the charge if the interval was opened in a PREVIOUS capture session
		// (capture was disabled + re-enabled in between): the gap then spans the
		// disabled period and would corrupt timing. This thread's stale clock is
		// invalidated by the generation mismatch on EVERY thread — not just the one
		// that called setActive.
		if ( prevNano != null && prevNano > 0 && now >= prevNano
		    && prevGeneration != null && prevGeneration == curGeneration ) {
			FileBlueprint	prevFile	= service.lastFile.get();
			Integer			prevSpan	= service.lastSpanId.get();
			if ( prevFile != null && prevSpan != null ) {
				SpanStats prev = prevFile.spanStats( prevSpan );
				if ( prev != null ) {
					prev.addNanos( now - prevNano );
				}
			}
		}

		service.lastNano.set( now );
		service.lastGeneration.set( curGeneration );

		// The CURRENT span owns the next interval. Locate the blueprint by its KEY;
		// if it is gone (cleared by reset()), re-register it from the passed
		// reference so marks from an already-loaded class are never dropped.
		FileBlueprint fb = service.fileBlueprintOrRegister( id, blueprint );
		if ( fb == null ) {
			return;
		}
		service.lastFile.set( fb );

		SpanStats stats = fb.spanStats( spanId );
		if ( stats != null ) {
			stats.incrementCount();
		}
		service.lastSpanId.set( spanId );
	}

	/**
	 * Record that SEVERAL spans executed atomically — used for an all-or-nothing
	 * unit (e.g. a function/closure/lambda declaration shell). The whole unit
	 * either runs or does not.
	 * <p>
	 * Implemented as a loop of individual {@link #mark(String, int)} calls — each
	 * span is charged/incremented independently, and the LAST span ends up owning
	 * the next probe-charging interval, so a following mark still charges the tail
	 * to the shell's end.
	 *
	 * @param id      the blueprint KEY (normalized file path or source hash)
	 * @param spanIds the ids of all spans covered by this atomic unit, in source
	 *                order; the last becomes the current span for the next interval
	 */
	public static void mark( String id, int... spanIds ) {
		mark( null, id, spanIds );
	}

	/**
	 * As {@link #mark(String, int...)} but also receives the class's own
	 * {@link Blueprint} so a cleared registration is RE-REGISTERED on demand.
	 *
	 * @param blueprint the class's blueprint, or null when not supplied
	 * @param id        the blueprint KEY (normalized file path or source hash)
	 * @param spanIds   the atomic unit's span ids, in source order
	 */
	public static void mark( Blueprint blueprint, String id, int... spanIds ) {
		CodeProfilerService service = instance;
		if ( service == null || !service.active || spanIds.length == 0 ) {
			return;
		}
		for ( int spanId : spanIds ) {
			mark( id, spanId, blueprint );
		}
	}

	/**
	 * Close the probe-charging interval for the CURRENT span (charge the elapsed time
	 * since its mark to it) WITHOUT opening a new interval or incrementing any count.
	 * Emitted at the end of a body so the last span's self-time is not lost.
	 *
	 * @param id the blueprint KEY (normalized file path or source hash) whose
	 *           current span interval to close
	 */
	public static void markEnd( String id ) {
		markEnd( id, null );
	}

	/**
	 * As {@link #markEnd(String)} but also receives the class's own
	 * {@link Blueprint} so a cleared registration is RE-REGISTERED on demand (see
	 * {@link #mark(String, int, Blueprint)}).
	 *
	 * @param id        the blueprint KEY (normalized file path or source hash)
	 * @param blueprint the class's blueprint, or null when not supplied
	 */
	public static void markEnd( String id, Blueprint blueprint ) {
		CodeProfilerService service = instance;
		if ( service == null || !service.active ) {
			return;
		}

		long	now				= System.nanoTime();
		long	curGeneration	= service.activationGeneration;
		Long	prevNano		= service.lastNano.get();
		Long	prevGeneration	= service.lastGeneration.get();
		// Same stale-interval guard as mark(): never charge a gap that spans a
		// disable/re-enable cycle on this thread.
		if ( prevNano != null && prevNano > 0 && now >= prevNano
		    && prevGeneration != null && prevGeneration == curGeneration ) {
			FileBlueprint	prevFile	= service.lastFile.get();
			Integer			prevSpan	= service.lastSpanId.get();
			if ( prevFile != null && prevSpan != null && prevFile == service.fileBlueprintOrRegister( id, blueprint ) ) {
				SpanStats prev = prevFile.spanStats( prevSpan );
				if ( prev != null ) {
					prev.addNanos( now - prevNano );
				}
			}
		}

		// Close the interval: no new span owns the next interval.
		service.lastNano.remove();
		service.lastFile.remove();
		service.lastSpanId.remove();
		service.lastGeneration.remove();
	}

	/**
	 * Open a call-site bracket for INCLUSIVE timing: remember the call-site span and
	 * the start time so the matching {@link #markExit} can charge the whole elapsed
	 * wall time of the invocation to that span. Emitted just before a BoxLang
	 * invocation (UDF/closure/lambda); the argument expressions were already
	 * evaluated, so the bracket measures the CALL itself (args + body), which is
	 * exactly "how long this call was on the stack".
	 * <p>
	 * PURELY ADDITIVE: the bracket does NOT touch the flat probe-charging owner, so
	 * existing self-time accounting is unchanged. The per-thread stack makes nested
	 * calls in one expression (e.g. {@code foo( bar() )}) attribute correctly.
	 *
	 * @param id        the blueprint KEY (normalized file path or source hash)
	 * @param spanId    the call-site span id within that blueprint
	 * @param blueprint the class's blueprint, or null when not supplied
	 */
	public static void markEnter( String id, int spanId, Blueprint blueprint ) {
		markEnter( id, spanId, blueprint, null );
	}

	/**
	 * Open a call-site bracket for INCLUSIVE timing TOGETHER WITH the call's atomic
	 * SHELL members — the fragments that always run when the call runs (the head
	 * {@code callee(} and the closing {@code )}). The shell is atomic: every member
	 * is on the stack for the WHOLE call, so on {@link #markExit} each shell member
	 * shares the call-site span's inclusive time rather than keeping its own tiny
	 * probe time. This is what makes the {@code )} span report the same on-stack time
	 * as the head.
	 *
	 * @param id         the blueprint KEY (normalized file path or source hash)
	 * @param spanId     the call-site span id within that blueprint
	 * @param blueprint  the class's blueprint, or null when not supplied
	 * @param shellSpans the call's atomic shell span ids (may be null/empty), or null
	 */
	public static void markEnter( String id, int spanId, Blueprint blueprint, int[] shellSpans ) {
		CodeProfilerService service = instance;
		if ( service == null || !service.active ) {
			return;
		}
		FileBlueprint fb = service.fileBlueprintOrRegister( id, blueprint );
		if ( fb == null ) {
			return;
		}
		CallStack cs = service.callStack.get();
		cs.ensureCapacity( cs.depth );
		cs.files[ cs.depth ]		= fb;
		cs.shells[ cs.depth ]		= shellSpans;
		cs.nums[ 2 * cs.depth ]		= spanId;
		cs.nums[ 2 * cs.depth + 1 ]	= System.nanoTime();
		cs.depth++;
	}

	/**
	 * Close the innermost call-site bracket opened by {@link #markEnter}: charge the
	 * elapsed wall time to the call-site span's {@code inclusiveNanos} (which includes
	 * the called subtree, unlike self time). Safe to call with no open bracket
	 * (returns immediately); repeated calls unwind one frame each, so a deep exception
	 * unwind can close several brackets.
	 *
	 * @param id        the blueprint KEY (normalized file path or source hash)
	 * @param blueprint the class's blueprint, or null when not supplied
	 */
	public static void markExit( String id, Blueprint blueprint ) {
		CodeProfilerService service = instance;
		if ( service == null || !service.active ) {
			return;
		}
		CallStack cs = service.callStack.get();
		if ( cs.depth == 0 ) {
			return;
		}
		int				top			= cs.depth - 1;
		FileBlueprint	fb			= ( FileBlueprint ) cs.files[ top ];
		int				callSpanId	= ( int ) cs.nums[ 2 * top ];
		long			startNano	= cs.nums[ 2 * top + 1 ];
		int[]			shellSpans	= cs.shells[ top ];

		long			elapsed		= System.nanoTime() - startNano;
		// Charge the call-site span AND every atomic shell member (the `callee(` head
		// and the `)` tail) the SAME inclusive time: the shell is on the stack for the
		// whole call, so the `)` reports the same on-stack time as the head.
		SpanStats		callStats	= fb == null ? null : fb.spanStats( callSpanId );
		if ( callStats != null ) {
			callStats.addInclusiveNanos( elapsed );
		}
		chargeShell( fb, shellSpans, callSpanId, elapsed );

		// LINE DOUBLE-COUNT GUARD: if this call bracket ran inside ANOTHER call
		// bracket on the same line (e.g. the argument call in
		// `processData( getData1() )`), its time is already inside the enclosing
		// bracket's inclusive time — record it so LINE aggregation subtracts it.
		markSameLineNested( cs, top, elapsed );

		cs.files[ top ]		= null;
		cs.shells[ top ]	= null;
		cs.depth--;
	}

	/**
	 * Charge the atomic SHELL members of a call (the head {@code callee(} and the
	 * closing {@code )}) the same inclusive time as the call-site span — the shell is
	 * on the stack for the whole call, so all its fragments report identical on-stack
	 * time.
	 * <p>
	 * Each non-call-site member also records the SAME amount as line-nested, because
	 * it is a duplicate of the call-site span's time on the line: LINE aggregation
	 * then sums the shell exactly once (via the head/call-site span) instead of
	 * counting the head and the {@code )} separately. Span-level reports still show
	 * the member's full on-stack time.
	 *
	 * @param fb         the call-site blueprint
	 * @param shellSpans the shell span ids (may be null/empty)
	 * @param callSpanId the call-site span id (skipped — it owns the line)
	 * @param elapsed    the inclusive nanoseconds to charge
	 */
	private static void chargeShell( FileBlueprint fb, int[] shellSpans, int callSpanId, long elapsed ) {
		if ( fb == null || shellSpans == null || shellSpans.length == 0 || elapsed <= 0 ) {
			return;
		}
		for ( int member : shellSpans ) {
			if ( member == callSpanId ) {
				continue;
			}
			SpanStats memberStats = fb.spanStats( member );
			if ( memberStats != null ) {
				memberStats.addInclusiveNanos( elapsed );
				memberStats.addNestedNanos( elapsed );
			}
		}
	}

	/**
	 * Record on the frame at {@code top} the elapsed time that was same-line-nested
	 * inside the enclosing frame (immediately below it on the stack) when both call
	 * spans sit on the SAME source line.
	 *
	 * @param cs      the thread's call stack
	 * @param top     the index of the frame being closed
	 * @param elapsed the bracket's elapsed nanoseconds
	 */
	private static void markSameLineNested( CallStack cs, int top, long elapsed ) {
		if ( top <= 0 ) {
			return;
		}
		FileBlueprint	childFb		= ( FileBlueprint ) cs.files[ top ];
		int				childSpan	= ( int ) cs.nums[ 2 * top ];
		FileBlueprint	parentFb	= ( FileBlueprint ) cs.files[ top - 1 ];
		int				parentSpan	= ( int ) cs.nums[ 2 * ( top - 1 ) ];
		if ( childFb == null || parentFb == null ) {
			return;
		}
		Span	child	= childFb.span( childSpan );
		Span	parent	= parentFb.span( parentSpan );
		if ( child != null && parent != null && child.startLine() == parent.startLine() ) {
			child.stats().addNestedNanos( elapsed );
		}
	}

	/**
	 * The current call-bracket depth on this thread. Emitted at a method's entry and
	 * saved to a local so the method's catch-all handler can unwind back to it if an
	 * exception escapes (see {@link #markUnwindTo}).
	 *
	 * @return the current call-stack depth (0 when no bracket is open)
	 */
	public static int markDepth() {
		CodeProfilerService service = instance;
		if ( service == null || !service.active ) {
			return 0;
		}
		return service.callStack.get().depth;
	}

	/**
	 * Unwind the call-bracket stack back to {@code depth}, charging the elapsed time
	 * of every abandoned bracket to its call-site span's inclusive time. Emitted in a
	 * method's catch-all handler just before rethrowing, so an exception (or abort)
	 * escaping the method closes the brackets its body opened. Without this, a
	 * thrown-through call would leave its bracket open until the next push.
	 *
	 * @param depth the depth to unwind to (captured via {@link #markDepth} at entry)
	 */
	public static void markUnwindTo( int depth ) {
		CodeProfilerService service = instance;
		if ( service == null || !service.active ) {
			return;
		}
		CallStack	cs	= service.callStack.get();
		long		end	= System.nanoTime();
		while ( cs.depth > depth ) {
			int				top			= cs.depth - 1;
			FileBlueprint	fb			= ( FileBlueprint ) cs.files[ top ];
			int				callSpanId	= ( int ) cs.nums[ 2 * top ];
			long			startNano	= cs.nums[ 2 * top + 1 ];
			int[]			shellSpans	= cs.shells[ top ];
			long			elapsed		= Math.max( 0L, end - startNano );
			// Charge the call-site span AND its atomic shell members (head `callee(` /
			// tail `)`) the same inclusive time on the exception path too.
			SpanStats		callStats	= fb == null ? null : fb.spanStats( callSpanId );
			if ( callStats != null ) {
				callStats.addInclusiveNanos( elapsed );
			}
			chargeShell( fb, shellSpans, callSpanId, elapsed );
			markSameLineNested( cs, top, elapsed );
			cs.files[ top ]		= null;
			cs.shells[ top ]	= null;
			cs.depth--;
		}
	}

	/**
	 * Reset the probe-charging clock for the current thread.
	 */
	public void resetThreadClock() {
		CodeProfilerService inst = instance;
		if ( inst != null ) {
			inst.lastNano.remove();
			inst.lastFile.remove();
			inst.lastSpanId.remove();
			inst.lastGeneration.remove();
			inst.callStack.get().depth = 0;
		}
	}

	// -------------------------------------------------------------------------
	// Lifecycle API (non-hot)
	// -------------------------------------------------------------------------

	/**
	 * Turn live capture on or off.
	 * <p>
	 * Re-ENABLING capture bumps the activation generation, which invalidates every
	 * worker thread's stale probe clock (see {@link #activationGeneration}). Any
	 * interval a thread had open BEFORE the disable can no longer charge its
	 * (huge, disabled-period) gap to the old span.
	 *
	 * @param active whether to capture
	 */
	public void setActive( boolean active ) {
		if ( instance != null ) {
			instance.active = active;
			if ( active ) {
				// Invalidate stale intervals opened in a previous capture session.
				instance.activationGeneration++;
			}
		}
	}

	/**
	 * Whether live capture is currently active.
	 *
	 * @return {@code true} if capturing
	 */
	public boolean isActive() {
		return instance != null && instance.active;
	}

	/**
	 * Clear all collected data AND registered blueprints, starting a fresh capture
	 * session.
	 * <p>
	 * Clearing blueprints is SAFE: instrumented classes carry their own
	 * {@link Blueprint} reference, so the next {@code mark} from an already-loaded
	 * class (whose {@code <clinit>} cannot run again) RE-REGISTERS its blueprint on
	 * demand (see {@link #mark(String, int, Blueprint)} / {@link #fileBlueprintOrRegister}).
	 * Compilation-time registration is unaffected, so blueprints for files that
	 * were compiled but never EXECUTED remain queryable (e.g. by test harnesses)
	 * until this is called.
	 */
	public void reset() {
		if ( instance != null ) {
			instance.byPath.clear();
			instance.lastNano.remove();
			instance.lastFile.remove();
			instance.lastSpanId.remove();
			instance.lastGeneration.remove();
			instance.callStack.get().depth = 0;
			// Bump the generation so any OTHER thread's stale interval (opened before
			// the reset) is invalidated and cannot charge across the reset boundary.
			instance.activationGeneration++;
		}
	}

	/**
	 * Register (or replace) the blueprint for a real FILE. The key is the source
	 * path, normalized internally. Pre-allocates a counter for every executable span.
	 *
	 * @param filePath  the source file path (normalized internally)
	 * @param blueprint the file's blueprint
	 *
	 * @return the blueprint KEY (the normalized file path) — the id bytecode embeds
	 *         as a String static field and passes to {@link #mark}; empty if no instance
	 */
	public String registerBlueprintForFile( String filePath, Blueprint blueprint ) {
		return registerBlueprint( normalize( filePath ), blueprint, Blueprint.Kind.FILE );
	}

	/**
	 * Register (or replace) the blueprint for ADHOC SOURCE. The key is the source
	 * hash, passed through untouched (not a filesystem path). Pre-allocates a
	 * counter for every executable span.
	 *
	 * @param sourceHash the hash of the adhoc source
	 * @param blueprint  the source's blueprint
	 *
	 * @return the blueprint KEY (the source hash) — the id bytecode embeds as a
	 *         String static field and passes to {@link #mark}; empty if no instance
	 */
	public String registerBlueprintForSource( String sourceHash, Blueprint blueprint ) {
		return registerBlueprint( sourceHash, blueprint, Blueprint.Kind.SOURCE );
	}

	/**
	 * Register a blueprint that was serialized INTO the class's {@code <clinit>}
	 * bytecode, so a class loaded from disk (not recompiled in this session) can
	 * self-register its blueprint on first load. Idempotent per file path: the
	 * existing registration is reused if already present; a NEWER blueprint (file's
	 * lastModified is greater) replaces the stale one.
	 * <p>
	 * The {@code id} IS the blueprint key (file path, or the source hash for adhoc
	 * source) — the SAME String baked into the class's {@code mark} static field,
	 * so marks always resolve to the FileBlueprint registered here.
	 *
	 * @param id           the blueprint KEY (file path, or source hash for adhoc)
	 * @param lastModified the source file's last-modified time (0 for adhoc source)
	 * @param kind         the blueprint kind (FILE or SOURCE)
	 * @param spanData     flat int[] describing every span: groups of 5 ints
	 *                     (startLine, startCol, endLine, endCol, executableFlag)
	 *
	 * @return a blueprint whose {@code spans()} are ALL spans of the file (in source
	 *         order, covering executable AND non-executable) — stored in the class's
	 *         static blueprint field so its marks can re-register it after a reset;
	 *         or null if no instance
	 */
	public static Blueprint registerBlueprintFromClinit( String id, long lastModified, Blueprint.Kind kind, int[] spanData ) {
		return registerBlueprintFromClinitInternal( id, lastModified, kind, spanData );
	}

	/**
	 * Register a blueprint that was serialized INTO the class's {@code <clinit>}
	 * as a packed, chunked String (see the {@code int[]} sibling overload for the
	 * semantics). The boxpiler emits the span data as {@code LDC} string constants
	 * rather than inline array-build bytecode so that even very large blueprints
	 * keep the clinit well under the JVM's 64KB per-method limit (inline
	 * {@code int[]} construction with thousands of {@code DUP/LDC/IASTORE} can
	 * overflow a single {@code _clinit_part} and splitter frames can't subdivide
	 * an array whose reference never leaves the stack).
	 * <p>
	 * The string is a single space-separated run of the flat ints (groups of 5:
	 * startLine, startCol, endLine, endCol, executableFlag), broken into multiple
	 * chunks so each chunk is a single {@code LDC} well under the JVM's 65535-byte
	 * {@code CONSTANT_Utf8} limit even for very large files.
	 *
	 * @param id             the blueprint KEY (file path, or source hash for adhoc)
	 * @param lastModified   the source file's last-modified time (0 for adhoc source)
	 * @param kind           the blueprint kind (FILE or SOURCE)
	 * @param spanDataChunks packed span data as space-separated ints, in order
	 *
	 * @return a blueprint whose {@code spans()} are ALL spans of the file (see the
	 *         {@code int[]} overload); or null if no instance
	 */
	public static Blueprint registerBlueprintFromClinit( String id, long lastModified, Blueprint.Kind kind, String... spanDataChunks ) {
		if ( instance == null ) {
			return null;
		}
		int[] spanData = parseSpanDataChunks( spanDataChunks );
		return registerBlueprintFromClinitInternal( id, lastModified, kind, spanData );
	}

	/**
	 * Parse the packed, space-separated span-int chunks (emitted by the boxpiler
	 * as {@code LDC} string constants) back into the flat {@code int[]}.
	 *
	 * @param spanDataChunks the chunked, space-separated ints
	 *
	 * @return the concatenated flat int array
	 */
	private static int[] parseSpanDataChunks( String... spanDataChunks ) {
		if ( spanDataChunks == null || spanDataChunks.length == 0 ) {
			return new int[ 0 ];
		}
		// Single pass split is more memory-friendly than concatenating then re-splitting.
		java.util.List<Integer> ints = new java.util.ArrayList<>();
		for ( String chunk : spanDataChunks ) {
			if ( chunk == null || chunk.isEmpty() ) {
				continue;
			}
			for ( String part : chunk.trim().split( "\\s+" ) ) {
				ints.add( Integer.parseInt( part ) );
			}
		}
		int[] result = new int[ ints.size() ];
		for ( int i = 0; i < result.length; i++ ) {
			result[ i ] = ints.get( i );
		}
		return result;
	}

	private static Blueprint registerBlueprintFromClinitInternal( String id, long lastModified, Blueprint.Kind kind, int[] spanData ) {
		CodeProfilerService service = instance;
		if ( service == null ) {
			return null;
		}
		List<Blueprint.SpanDef> spanDefs = new java.util.ArrayList<>();
		for ( int i = 0; i + 4 < spanData.length; i += 5 ) {
			spanDefs.add( new Blueprint.SpanDef(
			    spanData[ i ], spanData[ i + 1 ], spanData[ i + 2 ], spanData[ i + 3 ],
			    spanData[ i + 4 ] == 1 ) );
		}
		// Only FILE ids are filesystem paths that get normalized. SOURCE ids are
		// adhoc source hashes and must be keyed verbatim, or the self-registration
		// would mint a bogus <cwd>/<hash> FILE entry while marks keep the original.
		String			key			= kind == Blueprint.Kind.SOURCE ? id : normalize( id );
		Blueprint		blueprint	= new Blueprint( spanDefs, kind, lastModified );

		FileBlueprint	existing	= service.byPath.get( key );
		if ( existing != null ) {
			// A newer blueprint replaces the stale one under the SAME key.
			if ( blueprint.lastModified() > existing.blueprint().lastModified() ) {
				service.byPath.put( key, new FileBlueprint( key, blueprint ) );
			}
			// Return the passed blueprint so the class's static field references the
			// SAME object each mark passes back for re-registration.
			return blueprint;
		}
		service.byPath.put( key, new FileBlueprint( key, blueprint ) );
		return blueprint;
	}

	/**
	 * Core registration: build the FileBlueprint (keyed by the given opaque key
	 * string, which is already canonical) and pre-allocate per-span counters.
	 * <p>
	 * IMPORTANT: registration is IDEMPOTENT per key. A span is identified by its
	 * (blueprint key, span number) — the key is the ONLY identity of a blueprint.
	 * If a blueprint is already registered for this key, it is reused as-is. If
	 * the incoming blueprint is NEWER (its {@code lastModified} is greater — the
	 * source file changed and was recompiled), the stale span map no longer
	 * corresponds, so it is REPLACED (fresh zeroed counters).
	 *
	 * @return the blueprint KEY (the id bytecode uses in {@link #mark}).
	 */
	private static String registerBlueprint( String key, Blueprint blueprint, Blueprint.Kind kind ) {
		CodeProfilerService service = instance;
		if ( service == null ) {
			return "";
		}
		FileBlueprint registered = service.byPath.compute( key, ( k, existing ) -> {
			if ( existing == null || blueprint.lastModified() > existing.blueprint().lastModified() ) {
				return new FileBlueprint( k, blueprint );
			}
			return existing;
		} );
		return registered.filePath;
	}

	/**
	 * All registered blueprints (by their key) for generic reporting. Each returned
	 * blueprint is self-describing via its {@link Blueprint.Kind}.
	 *
	 * @return map of registered key -> blueprint
	 */
	public Map<String, Blueprint> trackedBlueprints() {
		if ( instance == null ) {
			return Map.of();
		}
		Map<String, Blueprint> result = new ConcurrentHashMap<>( instance.byPath.size() );
		for ( Map.Entry<String, FileBlueprint> entry : instance.byPath.entrySet() ) {
			result.put( entry.getKey(), entry.getValue().blueprint() );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Query API (cold path)
	// -------------------------------------------------------------------------

	/**
	 * The executable span containing the given point, or null if the point is in no
	 * executable span (NOT_EXECUTABLE).
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param line     the line to look up (1-based, like all line indexes in this class)
	 * @param col      the column to look up (0-based, matching {@link Blueprint.SpanDef#startCol()})
	 *
	 * @return the executable {@link Span} containing the point, or null if the point
	 *         falls in a gap/non-executable region or the file isn't registered
	 */
	public Span spanAt( String filePath, int line, int col ) {
		FileBlueprint fb = fileBlueprint( filePath );
		return fb == null ? null : fb.spanAt( line, col );
	}

	/**
	 * The executable span by its id.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param spanId   the span id (0-based; indexes the file's executable span array)
	 *
	 * @return the {@link Span} with that id, or null if the id is out of range or
	 *         the file isn't registered
	 */
	public Span span( String filePath, int spanId ) {
		FileBlueprint fb = fileBlueprint( filePath );
		return fb == null ? null : fb.span( spanId );
	}

	/**
	 * All executable spans touching the given line.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param line     the line to look up (1-based)
	 *
	 * @return the executable spans on that line, in source order; empty list if none
	 *         or the file isn't registered
	 */
	public List<Span> spansOnLine( String filePath, int line ) {
		FileBlueprint fb = fileBlueprint( filePath );
		return fb == null ? List.of() : fb.spansOnLine( line );
	}

	/**
	 * All executable spans for a file, in blueprint (source) order.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 *
	 * @return the file's executable spans; empty list if not registered
	 */
	public List<Span> fileSpans( String filePath ) {
		FileBlueprint fb = fileBlueprint( filePath );
		return fb == null ? List.of() : fb.fileSpans();
	}

	/**
	 * Print EVERY span's source characters to stdout for a blueprint, reading the
	 * source straight from disk: {@code System.out.println( CodeProfilerService.dumpSpans( filePath ) ) }.
	 *
	 * @param filePath the blueprint key (normalized file path or adhoc source hash)
	 *
	 * @return a multi-line string, one {@code spanN (line,col)-(line,col) count=.. [chars] } per span
	 */
	public String dumpSpans( String filePath ) {
		return dumpSpans( filePath, null );
	}

	/**
	 * Print every span's source characters to stdout, for eyeballing what each span
	 * maps to in a test: {@code System.out.println( CodeProfilerService.dumpSpans( key, source ) ) }.
	 * <p>
	 * If {@code source} is {@code null} and the registered key is a real {@link Blueprint.Kind#FILE}
	 * (a normalized path), the source is read straight from disk — no need to pass it.
	 *
	 * @param filePath the blueprint key (adhoc source hash or normalized file path)
	 * @param source   the source text to slice spans from, or {@code null} to read from disk
	 *
	 * @return a multi-line string, one {@code spanN (line,col)-(line,col) count=.. [chars] } per span
	 */
	public String dumpSpans( String filePath, String source ) {
		FileBlueprint fb = fileBlueprint( filePath );
		if ( fb == null ) {
			return "<no blueprint for " + filePath + ">";
		}
		String resolvedSource = source;
		if ( resolvedSource == null && fb.kind() == Blueprint.Kind.FILE ) {
			try {
				// Read from the CANONICAL stored path, not the caller's spelling —
				// a differently-cased query may match the blueprint but still not
				// exist on disk verbatim.
				resolvedSource = java.nio.file.Files.readString( Paths.get( fb.filePath ) );
			} catch ( java.io.IOException e ) {
				return "<cannot read " + fb.filePath + ": " + e.getMessage() + ">";
			}
		}
		StringBuilder	sb		= new StringBuilder();
		List<Span>		spans	= fb.fileSpans();
		int				i		= 0;
		for ( Span s : spans ) {
			String text = resolvedSource == null ? "<no source>"
			    : sliceSource( resolvedSource, s.startLine(), s.startCol(), s.endLine(), s.endCol() );
			sb.append( "span" ).append( i )
			    .append( " (" ).append( s.startLine() ).append( "," ).append( s.startCol() )
			    .append( ")-(" ).append( s.endLine() ).append( "," ).append( s.endCol() )
			    .append( ") count=" ).append( s.stats().count() )
			    .append( " [" ).append( text.replace( "\n", "\\n" ).replace( "\r", "\\r" ) ).append( "]" )
			    .append( '\n' );
			i++;
		}
		return sb.toString();
	}

	/**
	 * The RAW SOURCE CHARACTERS a single executable span covers, for eyeballing what
	 * a span maps to. Span coordinates are 1-based lines and 0-based columns, so this
	 * slices {@code source} (split on {@code \n}) between the span's start and end.
	 * <p>
	 * Add {@code System.out.println( CodeProfilerService.spanSourceText( filePath, source, span ) ) }
	 * to a test to confirm the exact text of a single span.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param source   the original source text (may have trailing newline; handled)
	 * @param span     the span whose characters to extract
	 *
	 * @return the characters the span covers, {@code <no source>} if the source is
	 *         absent, or {@code null} if the span is null
	 */
	public String spanSourceText( String filePath, String source, Span span ) {
		if ( span == null ) {
			return null;
		}
		if ( source == null ) {
			return "<no source>";
		}
		return sliceSource( source, span.startLine(), span.startCol(), span.endLine(), span.endCol() );
	}

	/**
	 * Split the source into lines and return the substring a (1-based line, 0-based
	 * column) span covers. Multi-line spans join the intervening lines with {@code \n}.
	 */
	private static String sliceSource( String source, int startLine, int startCol, int endLine, int endCol ) {
		String[] lines = source.split( "\n", -1 );
		// Drop a trailing empty element left by a terminal newline in the text block.
		if ( lines.length > 0 && lines[ lines.length - 1 ].isEmpty() ) {
			String[] trimmed = new String[ lines.length - 1 ];
			System.arraycopy( lines, 0, trimmed, 0, trimmed.length );
			lines = trimmed;
		}
		if ( startLine > lines.length || startLine < 1 ) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if ( startLine == endLine ) {
			String line = lines[ startLine - 1 ];
			// Guard against inverted spans (startCol > endCol) or out-of-range endCol
			// so the dump NEVER crashes — it must always reveal the raw spans.
			if ( startCol < line.length() ) {
				if ( endCol >= startCol ) {
					return line.substring( startCol, Math.min( endCol, line.length() ) );
				}
				// Inverted: return from startCol to end of line, flagged.
				return "<inverted " + line.substring( startCol ) + ">";
			}
			return "";
		}
		String first = lines[ startLine - 1 ];
		sb.append( startCol < first.length() ? first.substring( startCol ) : "" );
		for ( int ln = startLine; ln < endLine - 1 && ln < lines.length; ln++ ) {
			sb.append( '\n' ).append( lines[ ln ] );
		}
		if ( endLine <= lines.length && endCol < lines[ endLine - 1 ].length() ) {
			sb.append( '\n' ).append( lines[ endLine - 1 ], 0, endCol );
		}
		return sb.toString();
	}

	/**
	 * Aggregated data for a single line.
	 * <ul>
	 * <li>{@code covered} = any executable span touching the line has count &gt; 0</li>
	 * <li>{@code count} = execution count of the first/outermost span touching the
	 * line <b>that ran</b> (0 when none ran). This is always consistent with
	 * {@code covered}: the first span on the line can be a fragment that never gets
	 * its own mark (e.g. the {@code )>} gap before a block's opening brace), so
	 * blindly taking the first span's count would report 0 for a line that executed.
	 * Like before it is ONE span's count, never a sum across nested spans.</li>
	 * <li>{@code nanos} = SUM of the spans' ON-THE-STACK time over all spans touching
	 * the line (inclusive when a span made calls, self otherwise)</li>
	 * </ul>
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param line     the line to aggregate (1-based)
	 *
	 * @return per-line aggregate, or null if no executable span touches the line
	 *         or the file isn't registered
	 */
	public LineCoverage lineAt( String filePath, int line ) {
		FileBlueprint fb = fileBlueprint( filePath );
		if ( fb == null ) {
			return null;
		}
		List<Span> spans = fb.spansOnLine( line );
		if ( spans.isEmpty() ) {
			return null;
		}
		// The single line time: SUM of the spans' ON-THE-STACK time (inclusive for
		// call spans, self for leaf spans), EXCLUDING spans whose bracket ran nested
		// inside another call bracket on the same line — their time is already inside
		// the enclosing span's inclusive time, so counting them would double it.
		boolean	covered		= false;
		long	nanos		= 0L;
		long	lineCount	= -1L;
		for ( Span s : spans ) {
			SpanStats	stats	= s.stats();
			long		count	= stats.count();
			if ( count > 0 ) {
				covered = true;
				// The line's count is the first span that RAN, so `count` can never
				// contradict `covered`.
				if ( lineCount < 0 ) {
					lineCount = count;
				}
			}
			nanos += Math.max( 0L, stats.onStackNanos() - stats.lineNestedNanos() );
		}
		if ( lineCount < 0 ) {
			// Nothing ran: report the first/outermost span's count (0).
			lineCount = spans.get( 0 ).stats().count();
		}
		return new LineCoverage( line, covered, nanos, lineCount );
	}

	/**
	 * Per-line aggregates for a file, keyed by line.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 *
	 * @return map of 1-based line number to its {@link LineCoverage}; empty map if
	 *         the file isn't registered
	 */
	public Map<Integer, LineCoverage> fileLines( String filePath ) {
		FileBlueprint fb = fileBlueprint( filePath );
		if ( fb == null ) {
			return Map.of();
		}
		// TreeMap keeps the lines in ASCENDING numeric order so callers never have to
		// re-sort — a ConcurrentHashMap iterates in hash-bin order, which is NOT sorted.
		Map<Integer, LineCoverage> result = new TreeMap<>();
		// A multi-line span covers every line in [startLine, endLine] — register a
		// per-line aggregate for EACH of them (not just the start line), so the
		// line-level renderers color all the lines an executable span touches.
		for ( Span s : fb.spans() ) {
			for ( int line = s.startLine(); line <= s.endLine(); line++ ) {
				if ( !result.containsKey( line ) ) {
					result.put( line, lineAt( fb.filePath, line ) );
				}
			}
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Coverage data + export
	// -------------------------------------------------------------------------

	/**
	 * Build PER-LINE coverage for every registered blueprint of the requested
	 * kinds, as NATIVE BoxLang structures ({@link Struct}/{@link Array}) — directly
	 * consumable in BoxLang, and serializable via
	 * {@code CoverageJSONExporter.toJSON(...)}. Delegates to
	 * {@link CoverageDataBuilder#buildLineCoverage(Blueprint.Kind...)}.
	 * <p>
	 * Each file value:
	 * 
	 * <pre>
	 * {
	 *   "filePath"          : &lt;normalized path or source hash&gt;,
	 *   "kind"              : "FILE" | "SOURCE",
	 *   "numExecutableLines": &lt;lines with an executable span&gt;,
	 *   "numCoveredLines"   : &lt;lines with count &gt; 0&gt;,
	 *   "percCoverage"      : &lt;covered / executable, or 1 if none&gt;,
	 *   "lines"             : {
	 *       "&lt;lineNum&gt;"      : { "covered": bool, "count": n, "time": n }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return a {@link Struct} keyed by blueprint key
	 */
	public IStruct buildLineCoverage( Blueprint.Kind... kinds ) {
		return CoverageDataBuilder.buildLineCoverage( this, kinds );
	}

	/**
	 * Build EVERY SPAN (in blueprint order) for every registered blueprint of the
	 * requested kinds, as NATIVE BoxLang structures. Delegates to
	 * {@link CoverageDataBuilder#buildSpanCoverage(Blueprint.Kind...)}.
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return a {@link Struct} keyed by blueprint key
	 */
	public IStruct buildSpanCoverage( Blueprint.Kind... kinds ) {
		return CoverageDataBuilder.buildSpanCoverage( this, kinds );
	}

	/**
	 * Build the per-line coverage JSON string for every registered blueprint of
	 * the requested kinds (default {@link Blueprint.Kind#FILE}).
	 *
	 * @param kinds the blueprint kinds to include
	 *
	 * @return the JSON string
	 */
	public String buildLineCoverageJSON( Blueprint.Kind... kinds ) {
		return CoverageJSONExporter.toJSON( buildLineCoverage( kinds ) );
	}

	/**
	 * Build the per-span coverage JSON string for every registered blueprint of
	 * the requested kinds (default {@link Blueprint.Kind#FILE}).
	 *
	 * @param kinds the blueprint kinds to include
	 *
	 * @return the JSON string
	 */
	public String buildSpanCoverageJSON( Blueprint.Kind... kinds ) {
		return CoverageJSONExporter.toJSON( buildSpanCoverage( kinds ) );
	}

	/**
	 * Write a JSON report of PER-LINE coverage to {@code path}. The shape is
	 * inspired by TestBox's {@code coverageReport.json} but uses objects instead of
	 * a query, and adds timing. Delegates to {@link CoverageJSONExporter}.
	 *
	 * @param path   the absolute or relative output file path
	 * @param pretty whether to pretty-print the JSON
	 * @param kinds  the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the JSON string written (also available for embedding)
	 */
	public String writeLineCoverageJSON( String path, boolean pretty, Blueprint.Kind... kinds ) {
		return CoverageJSONExporter.write( path, buildLineCoverage( kinds ), pretty );
	}

	/**
	 * {@link #writeLineCoverageJSON(String, boolean, Blueprint.Kind...)} with
	 * pretty printing on.
	 *
	 * @param path  the output file path
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the JSON string written
	 */
	public String writeLineCoverageJSON( String path, Blueprint.Kind... kinds ) {
		return writeLineCoverageJSON( path, true, kinds );
	}

	/**
	 * Write a JSON report of EVERY SPAN in blueprint order to {@code path}.
	 * Delegates to {@link CoverageJSONExporter}.
	 *
	 * @param path   the absolute or relative output file path
	 * @param pretty whether to pretty-print the JSON
	 * @param kinds  the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the JSON string written
	 */
	public String writeSpanCoverageJSON( String path, boolean pretty, Blueprint.Kind... kinds ) {
		return CoverageJSONExporter.write( path, buildSpanCoverage( kinds ), pretty );
	}

	/**
	 * {@link #writeSpanCoverageJSON(String, boolean, Blueprint.Kind...)} with
	 * pretty printing on.
	 *
	 * @param path  the output file path
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the JSON string written
	 */
	public String writeSpanCoverageJSON( String path, Blueprint.Kind... kinds ) {
		return writeSpanCoverageJSON( path, true, kinds );
	}

	/**
	 * Write SonarQube Generic Coverage XML for per-line coverage to {@code path}.
	 * Delegates to {@link CoverageSonarQubeExporter}.
	 *
	 * @param path  the output file path
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the XML string written
	 */
	public String writeSonarQubeXML( String path, Blueprint.Kind... kinds ) {
		return CoverageSonarQubeExporter.write( buildLineCoverage( kinds ), path );
	}

	/**
	 * Generate SonarQube Generic Coverage XML for per-line coverage.
	 * Delegates to {@link CoverageSonarQubeExporter}.
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the XML string
	 */
	public String buildSonarQubeXML( Blueprint.Kind... kinds ) {
		return CoverageSonarQubeExporter.generateXML( buildLineCoverage( kinds ) );
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private static String normalize( String filePath ) {
		// Canonicalize the path so registration and lookup agree regardless of
		// slashes, redundant segments, or relative/absolute forms.
		Path path = Paths.get( filePath ).toAbsolutePath().normalize();
		return path.toString();
	}

	private static FileBlueprint fileBlueprint( String key ) {
		if ( instance == null ) {
			return null;
		}
		FileBlueprint fb = instance.byPath.get( key );
		if ( fb == null ) {
			// Not an exact key match; maybe a real path needing normalization.
			fb = instance.byPath.get( normalize( key ) );
		}
		if ( fb == null ) {
			// Still not found — the caller may have passed a differently-cased
			// spelling of a registered FILE path (e.g. C:\foo\bar.cfm vs
			// c:/foo/BAR.cfm), and the separators may differ too (Windows \ vs /).
			// Fall back to a case-insensitive, separator-insensitive match over
			// registered FILE blueprints. COLD path — a linear scan, acceptable
			// for lookups (not the mark hot path).
			String lowerKey = key.toLowerCase( Locale.ROOT ).replace( '\\', '/' );
			for ( var entry : instance.byPath.entrySet() ) {
				if ( entry.getValue().kind() == Blueprint.Kind.FILE
				    && entry.getValue().filePath.toLowerCase( Locale.ROOT ).replace( '\\', '/' ).equals( lowerKey ) ) {
					return entry.getValue();
				}
			}
		}
		return fb;
	}

	/**
	 * Locate the live {@link FileBlueprint} for {@code id}; if it is NOT currently
	 * registered and a {@code blueprint} reference is supplied, RE-REGISTER it from
	 * that reference. This is how a cleared blueprint (see {@link #reset()}) is
	 * rebuilt on demand by the marks of an already-loaded instrumented class whose
	 * {@code <clinit>} cannot run again.
	 *
	 * @param id        the blueprint KEY (normalized file path or source hash)
	 * @param blueprint the class's blueprint, or null when not supplied
	 *
	 * @return the live blueprint, or null if not found and none supplied
	 */
	private FileBlueprint fileBlueprintOrRegister( String id, Blueprint blueprint ) {
		FileBlueprint fb = fileBlueprint( id );
		if ( fb != null ) {
			return fb;
		}
		if ( blueprint == null ) {
			return null;
		}
		// Re-register from the passed reference so subsequent marks (and this one)
		// resolve. registerBlueprint is idempotent and keyed by id; FILE keys are
		// normalized internally, SOURCE keys are used verbatim.
		registerBlueprint( blueprint.kind() == Blueprint.Kind.SOURCE ? id : normalize( id ), blueprint, blueprint.kind() );
		return fileBlueprint( id );
	}

	/**
	 * Immutable descriptor + mutable stats of an executable span.
	 */
	public record Span(
	    int id,
	    int startLine,
	    int startCol,
	    int endLine,
	    int endCol,
	    SpanStats stats ) {
	}

	/**
	 * Mutable per-span runtime counters, mutated by {@link #mark} and read by tools.
	 */
	public static final class SpanStats {

		private final AtomicLong	count			= new AtomicLong();
		private final AtomicLong	totalNanos		= new AtomicLong();
		private final AtomicLong	inclusiveNanos	= new AtomicLong();

		/**
		 * Inclusive time of this span's invocations that ran NESTED inside another call
		 * bracket on the SAME line (see {@link #lineNestedNanos()}). Subtracted from the
		 * span's time during LINE aggregation so the same wall time is not counted twice.
		 */
		private final AtomicLong	nestedNanos		= new AtomicLong();

		public long count() {
			return count.get();
		}

		public long totalNanos() {
			return totalNanos.get();
		}

		/**
		 * INCLUSIVE wall time charged to this span by call-site brackets: the elapsed
		 * time of every invocation whose call site is this span, INCLUDING the work the
		 * called code ran. Only populated for call-site spans (see {@link #markEnter}/
		 * {@link #markExit}); self time ({@link #totalNanos()}) excludes called-subtree
		 * time, but this field includes it.
		 *
		 * @return accumulated inclusive nanoseconds
		 */
		public long inclusiveNanos() {
			return inclusiveNanos.get();
		}

		/**
		 * The "ON THE STACK" time of this span — the single time number surfaced by
		 * every report/export. It is the span's INCLUSIVE time when the span made
		 * BoxLang calls (so a call like {@code f1()} reports the whole time it was on
		 * the stack, e.g. 81ms because f1 called f2 called f3...), and its self time
		 * otherwise (identical for a leaf span that makes no calls).
		 * <p>
		 * Self-vs-call time is an INTERNAL distinction (both counters are retained on
		 * {@link SpanStats}); consumers should use THIS method.
		 *
		 * @return the inclusive nanoseconds if the span made calls, else self nanos
		 */
		public long onStackNanos() {
			return Math.max( totalNanos.get(), inclusiveNanos.get() );
		}

		/**
		 * The part of this span's inclusive time that was spent NESTED inside another
		 * call bracket on the SAME source line (e.g. the argument call in
		 * {@code processData( getData1() )}). That time is already counted inside the
		 * enclosing span's inclusive time, so LINE aggregation subtracts it from this
		 * span's contribution to avoid double-counting the same wall time.
		 *
		 * @return the same-line-nested nanoseconds
		 */
		public long lineNestedNanos() {
			return nestedNanos.get();
		}

		void addNestedNanos( long delta ) {
			nestedNanos.addAndGet( delta );
		}

		void incrementCount() {
			count.incrementAndGet();
		}

		void addNanos( long delta ) {
			totalNanos.addAndGet( delta );
		}

		void addInclusiveNanos( long delta ) {
			inclusiveNanos.addAndGet( delta );
		}

		@Override
		public String toString() {
			return "count=" + count.get() + ", nanos=" + totalNanos.get() + ", inclusive=" + inclusiveNanos.get();
		}
	}

	/**
	 * Immutable per-line aggregate.
	 *
	 * @param line    the 1-based line number
	 * @param covered true when any executable span touching the line ran
	 * @param nanos   the line's ON-THE-STACK time: sum of {@code onStackNanos} over all
	 *                spans on the line (inclusive for call spans, self for leaf spans)
	 * @param count   execution count of the first/outermost span on the line that ran
	 *                (0 when nothing on the line ran), matching {@code lineAt}
	 */
	public record LineCoverage(
	    int line,
	    boolean covered,
	    long nanos,
	    long count ) {
	}

	/**
	 * A registered file's blueprint + its pre-allocated span/counter storage.
	 */
	static final class FileBlueprint {

		final String							filePath;
		private final Blueprint					blueprint;
		private final Span[]					spans;	// executable spans by id
		private final Map<Integer, List<Span>>	byLine	= new ConcurrentHashMap<>();

		Blueprint.Kind kind() {
			return blueprint.kind();
		}

		FileBlueprint( String filePath, Blueprint blueprint ) {
			this.filePath	= filePath;
			this.blueprint	= blueprint;
			// Allocate counters for executable spans (span id = its position in the array).
			List<Blueprint.SpanDef>	defs			= blueprint.spans();
			int						executableCount	= 0;
			for ( Blueprint.SpanDef def : defs ) {
				if ( def.executable() ) {
					executableCount++;
				}
			}
			this.spans = new Span[ executableCount ];
			int idx = 0;
			for ( Blueprint.SpanDef b : defs ) {
				if ( !b.executable() ) {
					continue;
				}
				Span sp = new Span( idx, b.startLine(), b.startCol(), b.endLine(), b.endCol(), new SpanStats() );
				this.spans[ idx++ ] = sp;
				for ( int line = b.startLine(); line <= b.endLine(); line++ ) {
					this.byLine.computeIfAbsent( line, k -> new ArrayList<>() ).add( sp );
				}
			}
			for ( List<Span> lineSpans : this.byLine.values() ) {
				lineSpans.sort( ( a, c ) -> Integer.compare( a.startCol(), c.startCol() ) );
			}
		}

		Span span( int spanId ) {
			return spanId >= 0 && spanId < spans.length ? spans[ spanId ] : null;
		}

		SpanStats spanStats( int spanId ) {
			Span s = span( spanId );
			return s == null ? null : s.stats();
		}

		Span spanAt( int line, int col ) {
			List<Span> lineSpans = byLine.get( line );
			if ( lineSpans == null ) {
				return null;
			}
			// A column that begins a span owns the point at that boundary
			// (e.g. col 34 starts the "2" branch of "1 : 2" even though it is also
			// the inclusive end of the "1 : " span). A span only claims a boundary
			// column if its START LINE is the queried line.
			for ( Span s : lineSpans ) {
				if ( s.startLine() == line && col == s.startCol() ) {
					return s;
				}
			}
			// Otherwise the point belongs to the INNERMOST span that contains it.
			// A multi-line span's column bounds apply ONLY on its boundary lines:
			// the start-column constrains the start line, the end-column constrains
			// the end line, and every column on an intermediate line is inside the
			// span. (Previously both bounds were applied on every line the span
			// touched, so an intermediate line could never satisfy them and the span
			// was wrongly reported NOT_EXECUTABLE.)
			Span best = null;
			for ( Span s : lineSpans ) {
				if ( contains( s, line, col ) && ( best == null || startsAfter( s, best ) ) ) {
					best = s;
				}
			}
			return best;
		}

		/**
		 * True when the point (line, col) lies inside the span. The start-column bound
		 * applies only on the start line and the end-column bound only on the end line;
		 * intermediate lines are contained at every column.
		 */
		private static boolean contains( Span s, int line, int col ) {
			boolean	afterStart	= line > s.startLine() || ( line == s.startLine() && col >= s.startCol() );
			boolean	beforeEnd	= line < s.endLine() || ( line == s.endLine() && col <= s.endCol() );
			return afterStart && beforeEnd;
		}

		/**
		 * True when span {@code a} starts strictly after span {@code b} in source order.
		 * The later-starting span is the innermost (a nested body statement beats the
		 * multi-line shell that encloses it).
		 */
		private static boolean startsAfter( Span a, Span b ) {
			return a.startLine() > b.startLine() || ( a.startLine() == b.startLine() && a.startCol() > b.startCol() );
		}

		List<Span> spansOnLine( int line ) {
			return byLine.getOrDefault( line, List.of() );
		}

		List<Span> fileSpans() {
			return java.util.Arrays.asList( spans );
		}

		List<Span> spans() {
			return java.util.Arrays.asList( spans );
		}

		Blueprint blueprint() {
			return blueprint;
		}
	}
}
