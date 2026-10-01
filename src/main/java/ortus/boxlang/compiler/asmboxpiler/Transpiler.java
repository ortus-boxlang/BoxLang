package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxInterfaceTransformer;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public abstract class Transpiler implements ITranspiler {

	private final HashMap<String, String>					properties				= new HashMap<String, String>();
	private final HashMap<String, List<AbstractInsnNode>>	udfs					= new HashMap<String, List<AbstractInsnNode>>();
	private Map<String, BoxExpression>						keys					= new LinkedHashMap<String, BoxExpression>();
	private Map<String, ClassNode>							auxiliaries				= new LinkedHashMap<String, ClassNode>();
	/**
	 * Registry of named local classes defined inside a script or template.
	 * Maps the simple class name (alias) to the Java class name of the compiled auxiliary class.
	 * e.g. "Person" -&gt; "boxgenerated/scripts/MyScript$LocalClass$Person"
	 */
	private Map<String, String>								localClasses			= new LinkedHashMap<String, String>();
	private List<TryCatchBlockNode>							tryCatchBlockNodes		= new ArrayList<TryCatchBlockNode>();
	private int												lambdaCounter			= 0;
	private int												closureCounter			= 0;
	private int												componentCounter		= 0;
	private int												functionBodyCounter		= 0;
	private List<ImportDefinition>							imports					= new ArrayList<>();
	/**
	 * Stores local-class imports that carry a resolved {@code Class<?>} reference.
	 * Each entry is {@code {alias, internalClassName}}. At code-generation time these
	 * are emitted as {@link ImportDefinition#fromClassRef(String, String, Class)} calls.
	 */
	private List<String[]>									localClassRefImports	= new ArrayList<>();
	private List<MethodContextTracker>						methodContextTrackers	= new ArrayList<MethodContextTracker>();
	private List<BoxStaticInitializer>						staticInitializers		= new ArrayList<>();
	private ClassNode										owningClassNode			= null;
	/**
	 * Tracks all function names for which an invokeFunction_* static method has been generated,
	 * including static functions. This prevents duplicate method generation when the same function
	 * is encountered through multiple AST traversal paths (e.g., tag-based CFC with cfscript blocks).
	 */
	private Set<String>										compiledFunctionNames	= new HashSet<>();
	/**
	 * Storage for UDF implementations: maps function name (Key) to the instantiation bytecode
	 * that creates a new UDF instance with a method reference to the static invoker.
	 */
	private Map<Key, List<AbstractInsnNode>>				udfInstantiations		= new LinkedHashMap<>();

	/**
	 * Storage for Lambda implementations: list of instantiation bytecodes
	 * that create new Lambda instances with method references to the static invokers.
	 */
	private List<List<AbstractInsnNode>>					lambdaInstantiations	= new ArrayList<>();

	/**
	 * Storage for Closure implementations: list of instantiation bytecodes
	 * that create new ClosureDefinition instances with method references to the static invokers.
	 */
	private List<List<AbstractInsnNode>>					closureInstantiations	= new ArrayList<>();
	/**
	 * Manually debugging properties
	 */
	private Map<String, LabelNode>							breaks					= new LinkedHashMap<>();
	private Map<String, LabelNode>							continues				= new LinkedHashMap<>();

	/**
	 * Whether code-profiling instrumentation is enabled for this compilation.
	 * Set by the boxpiler from {@code Configuration.codeProfilerEnabled}. When false,
	 * no span registry is built and no {@code mark} instructions are emitted.
	 */
	private boolean											profilingEnabled		= false;

	/**
	 * The code profiler blueprint KEY for this compilation — the normalized file
	 * path (or source hash for adhoc source). This String IS the blueprint's id;
	 * it is stored as a static field on the instrumented class and passed as the
	 * first arg of every {@code mark(id, spanId)} instruction.
	 */
	private String											fileId					= null;

	/**
	 * Source-position -> span id map, built during Pass A (span detection).
	 * Keyed by packed start position (line/col). Pass B looks up a node's span id
	 * from its start position to emit the matching mark.
	 */
	private Map<Long, Integer>								spanIds					= new HashMap<>();

	/**
	 * Span ids already given a {@code mark} instruction in this compilation. Several
	 * AST nodes can share a span's start position (e.g. a statement and the call
	 * inside it), so we emit at most one mark per span to keep counts exact.
	 */
	private Set<Integer>									emittedMarks			= new HashSet<>();

	/**
	 * Set a property
	 *
	 * @param key   key of the Property
	 * @param value value of the Property
	 */
	public void setProperty( String key, String value ) {
		properties.put( key, value );
	}

	public void setOwningClass( ClassNode node ) {
		owningClassNode = node;
	}

	public ClassNode getOwningClass() {
		return owningClassNode;
	}

	/**
	 * Whether profiling instrumentation is enabled for this compilation.
	 *
	 * @return {@code true} if instrumentation is on
	 */
	public boolean isProfilingEnabled() {
		return profilingEnabled;
	}

	/**
	 * Set whether profiling instrumentation is enabled for this compilation.
	 *
	 * @param profilingEnabled enable/disable
	 */
	public void setProfilingEnabled( boolean profilingEnabled ) {
		this.profilingEnabled = profilingEnabled;
	}

	/**
	 * The code profiler blueprint KEY for this compilation's blueprint.
	 *
	 * @return the blueprint key (file path or source hash), or null if not registered
	 */
	public String getFileId() {
		return fileId;
	}

	/**
	 * Set the code profiler blueprint KEY for this compilation's blueprint.
	 *
	 * @param fileId the blueprint key (file path or source hash) returned by the
	 *               code profiler service registration
	 */
	public void setFileId( String fileId ) {
		this.fileId = fileId;
	}

	/**
	 * The name of the static field that holds this class's profiler blueprint key
	 * (id). Added to every instrumented class so the {@code mark} bytecode can
	 * load the id WITHOUT a per-file LDC — the field is the single, canonical store.
	 */
	public static final String PROFILER_ID_FIELD = "codeProfilerId";

	/**
	 * Whether this compilation currently has a registered blueprint key (profiling on).
	 *
	 * @return true if profiling is enabled AND a blueprint key is set
	 */
	public boolean hasProfilerId() {
		return profilingEnabled && fileId != null;
	}

	/**
	 * Emit a single-span {@code mark}: {@code CodeProfilerService.mark(id, spanId)}
	 * where {@code id} is this class's {@link #PROFILER_ID_FIELD} static field.
	 *
	 * @param spanId the span id within this blueprint
	 *
	 * @return the instructions, or an empty list when profiling is off
	 */
	public List<AbstractInsnNode> emitMark( int spanId ) {
		if ( !hasProfilerId() ) {
			return List.of();
		}
		return AsmHelper.invokeStaticMark( getProperty( "classTypeInternal" ), PROFILER_ID_FIELD, spanId );
	}

	/**
	 * Emit the markEnd close-interval instruction:
	 * {@code CodeProfilerService.markEnd(id)}.
	 *
	 * @return the instructions, or an empty list when profiling is off
	 */
	public List<AbstractInsnNode> emitMarkEnd() {
		if ( !hasProfilerId() ) {
			return List.of();
		}
		return AsmHelper.invokeStaticMarkEnd( getProperty( "classTypeInternal" ), PROFILER_ID_FIELD );
	}

	/**
	 * Emit an atomic shell mark covering several spans:
	 * {@code CodeProfilerService.mark(id, int... spanIds)}.
	 *
	 * @param spanIds the shell spans to batch, in source order
	 *
	 * @return the instructions, or an empty list when profiling is off
	 */
	public List<AbstractInsnNode> emitMarkVarargs( int[] spanIds ) {
		if ( !hasProfilerId() ) {
			return List.of();
		}
		return AsmHelper.invokeStaticMarkVarargs( getProperty( "classTypeInternal" ), PROFILER_ID_FIELD, spanIds );
	}

	/**
	 * The {@link Blueprint.SpanDef} list discovered in Pass A (span detection).
	 * Embedded into the generated class's {@code <clinit>} so the class can
	 * self-register its blueprint on first LOAD (not just at compile time),
	 * surviving a runtime restart where in-memory blueprints are cleared.
	 */
	private List<Blueprint.SpanDef> spanDefs = List.of();

	/**
	 * Store the Pass A span definitions for embedding into {@code <clinit>}.
	 *
	 * @param spanDefs the ordered list of executable span definitions
	 */
	public void setSpanDefs( List<Blueprint.SpanDef> spanDefs ) {
		this.spanDefs = spanDefs != null ? spanDefs : List.of();
	}

	/**
	 * The Pass A span definitions to embed into the compiled class's {@code <clinit>}.
	 *
	 * @return the ordered executable span definitions
	 */
	public List<Blueprint.SpanDef> getSpanDefs() {
		return this.spanDefs;
	}

	/**
	 * The total source line count for this compilation (from the Blueprint),
	 * used when embedding the blueprint into {@code <clinit>}.
	 */
	private int totalLines = 1;

	/**
	 * Store the total source line count for embedding into {@code <clinit>}.
	 *
	 * @param totalLines the source line count
	 */
	public void setTotalLines( int totalLines ) {
		this.totalLines = totalLines;
	}

	/**
	 * The total source line count to embed with the blueprint.
	 *
	 * @return the source line count
	 */
	public int getTotalLines() {
		return this.totalLines;
	}

	/**
	 * The source file's last-modified time for this compilation, used to detect a
	 * newer blueprint (recompiled file) and replace stale span data on load.
	 */
	private long lastModified = 0L;

	/**
	 * Store the source file's last-modified time for embedding into {@code <clinit>}.
	 *
	 * @param lastModified the file mtime (0 for adhoc/source-hash blueprints)
	 */
	public void setLastModified( long lastModified ) {
		this.lastModified = lastModified;
	}

	/**
	 * The source file's last-modified time to embed with the blueprint.
	 *
	 * @return the file mtime
	 */
	public long getLastModified() {
		return this.lastModified;
	}

	/**
	 * Register a source span (by its packed start position) for this compilation.
	 * Assigns the next sequential span id.
	 *
	 * @param packedStart packed (line, col) start position
	 *
	 * @return the assigned span id
	 */
	public int registerSpan( long packedStart ) {
		int spanId = spanIds.size();
		spanIds.put( packedStart, spanId );
		return spanId;
	}

	/**
	 * Look up the span id for a node's source start position.
	 *
	 * @param packedStart packed (line, col) start position
	 *
	 * @return the span id, or -1 if the position is not a span start
	 */
	public int getSpanId( long packedStart ) {
		return spanIds.getOrDefault( packedStart, -1 );
	}

	/**
	 * Claim a span for mark emission. Returns {@code true} the first time a span's
	 * mark is emitted for this compilation, {@code false} for every later request.
	 * Several AST nodes can share a span's start position (e.g. a statement and the
	 * call inside it), so only the first gets a mark instruction — keeping counts exact.
	 *
	 * @param spanId the span id
	 *
	 * @return true if this compilation has not yet emitted a mark for the span
	 */
	public boolean claimSpanMark( int spanId ) {
		return emittedMarks.add( spanId );
	}

	/**
	 * Span ids grouped into an atomic shell unit. The identified group — the head +
	 * interstitial + tail span fragments of a declaration shell split at lazy
	 * defaults — runs all-or-nothing at declaration, so the transformer emits a
	 * single varargs {@code mark(fileId, ...group)} instead of N marks. Keyed by the
	 * first (head) span id.
	 */
	private Map<Integer, int[]> spanGroups = new java.util.HashMap<>();

	/**
	 * Register an atomic shell span group. The group's first span id becomes the key
	 * so the transform hook for the statement (whose start lands on the head shell
	 * span) can look it up and batch the whole group.
	 *
	 * @param spanIds the shell span ids, in source order (head first)
	 */
	public void registerSpanGroup( int[] spanIds ) {
		if ( spanIds != null && spanIds.length > 0 ) {
			this.spanGroups.put( spanIds[ 0 ], spanIds );
		}
	}

	/**
	 * Look up and consume an atomic shell span group whose first span is the given id.
	 * Returns the full group (or null if the id is not a group head).
	 *
	 * @param spanId the first span id of the group
	 *
	 * @return the full group, or null if not a group head
	 */
	public int[] takeSpanGroup( int spanId ) {
		return this.spanGroups.remove( spanId );
	}

	/**
	 * Look up an atomic span group WITHOUT consuming it. Used when the same group
	 * must be emitted in MULTIPLE bytecode copies (e.g. a finally block compiled
	 * both inline and in the exception handler — only one runs at runtime, so both
	 * copies need the mark).
	 *
	 * @param spanId the first span id of the group
	 *
	 * @return the full group, or null if not a group head
	 */
	public int[] peekSpanGroup( int spanId ) {
		return this.spanGroups.get( spanId );
	}

	/**
	 * The property-DEFAULT span ids of the current class, collected in Pass A.
	 * Property defaults are HOISTED — {@code BoxClassSupport.defaultProperties()}
	 * applies them in the pseudo-constructor — so the class transformer emits ONE
	 * varargs {@code mark(fileId, ...propertyDefaults)} at that call site.
	 */
	private int[] propertyDefaultSpans;

	/**
	 * Register the class's property-default span ids (atomic hoisted group).
	 *
	 * @param spanIds the property default span ids, in source order
	 */
	public void registerPropertyDefaultSpans( int[] spanIds ) {
		this.propertyDefaultSpans = spanIds;
	}

	/**
	 * The registered property-default span ids (or null if none).
	 *
	 * @return the property default span ids
	 */
	public int[] getPropertyDefaultSpans() {
		return this.propertyDefaultSpans;
	}

	/**
	 * Snapshot the set of span marks claimed so far in this compilation. Used by
	 * transformers that duplicate the same AST (e.g. a {@code finally} body, which
	 * is compiled both inline and in the exception handler) so the duplicated copy
	 * can reclaim the marks and emit them on its own execution path.
	 *
	 * @return a defensive copy of the claimed-mark ids
	 */
	public Set<Integer> snapshotEmittedMarks() {
		return new HashSet<>( this.emittedMarks );
	}

	/**
	 * Restore the claimed-mark set to a prior snapshot, releasing any marks claimed
	 * since. Call with the snapshot taken before transforming a duplicated AST body
	 * so the duplicate copy's marks can be re-claimed and emitted.
	 *
	 * @param snapshot the set previously returned by {@link #snapshotEmittedMarks()}
	 */
	public void restoreEmittedMarks( Set<Integer> snapshot ) {
		this.emittedMarks.clear();
		this.emittedMarks.addAll( snapshot );
	}

	/**
	 * Adopt the profiling context of another transpiler (the OUTER script/class).
	 * A local class compiles as a separate auxiliary JVM class, but from the
	 * USER's perspective its code is part of the container script — so its marks
	 * must land on the SAME fileId and span-id mapping as the outer source, not a
	 * synthetic per-class blueprint.
	 * <p>
	 * After this call, the child's {@code mark} emission targets the outer file's
	 * counters: {@link #getFileId()} returns the outer fileId, and
	 * {@link #getSpanId(long)} resolves positions through the OUTER span registry
	 * (which already includes the local class's spans from the parent's Pass A).
	 *
	 * @param outer the transpiler whose profiling context to share
	 */
	public void adoptProfilingContext( Transpiler outer ) {
		this.fileId					= outer.fileId;
		this.spanIds				= outer.spanIds;
		this.profilingEnabled		= outer.profilingEnabled;
		// Marks claimed by the outer compilation already cover the class's spans
		// (the parent's Pass A walked the whole tree). Share the dedup set so the
		// child doesn't emit duplicate marks for the same span positions.
		this.emittedMarks			= outer.emittedMarks;
		// Property defaults are hoisted into the class's pseudo-constructor; the
		// child transforms that method, so it needs the span group the parent's
		// Pass A collected.
		this.propertyDefaultSpans	= outer.propertyDefaultSpans;
		// Span GROUPS (atomic shell/brace units) are also registered by the
		// parent's Pass A (e.g. the class shell + closing "}" group keyed by the
		// first annotation). The child's clinit emits those marks, so share the
		// group map too.
		this.spanGroups				= outer.spanGroups;
	}

	public boolean canReturn() {
		String returnType = getProperty( "returnType" );
		if ( returnType != null && !returnType.equals( "void" ) ) {
			return true;
		}
		return functionBodyCounter > 0;
	}

	public void incrementfunctionBodyCounter() {
		functionBodyCounter++;
	}

	public void decrementfunctionBodyCounter() {
		functionBodyCounter--;
	}

	public boolean isInsideComponent() {
		return componentCounter > 0;
	}

	public int getComponentCounter() {
		return componentCounter;
	}

	public void setComponentCounter( int counter ) {
		componentCounter = counter;
	}

	public void incrementComponentCounter() {
		componentCounter++;
	}

	public void decrementComponentCounter() {
		componentCounter--;
	}

	public ClassNode transpile( BoxInterface boxClass ) throws BoxRuntimeException {
		return BoxInterfaceTransformer.transpile( this, boxClass );
	}

	/**
	 * Get a Propoerty
	 *
	 * @param key key of the Property
	 *
	 * @return the value of the property or null if not defined
	 */
	public String getProperty( String key ) {
		return ( String ) properties.get( key );
	}

	public static Transpiler getTranspiler() {
		return new AsmTranspiler();
	}

	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) {
		return transform( node, context, ReturnValueContext.EMPTY );
	}

	public void addUDFRegistration( String name, List<AbstractInsnNode> nodes ) {
		this.udfs.put( name, nodes );
	}

	public boolean hasCompiledFunction( String name ) {
		return this.compiledFunctionNames.contains( name.toLowerCase() );
	}

	public void markFunctionCompiled( String name ) {
		this.compiledFunctionNames.add( name.toLowerCase() );
	}

	public List<AbstractInsnNode> getUDFRegistrations() {
		Optional<MethodContextTracker> tracker = getCurrentMethodContextTracker();
		return this.udfs.values().stream().flatMap( l -> {
			List<AbstractInsnNode> result = new ArrayList<>();
			if ( tracker.isPresent() ) {
				result.addAll( tracker.get().loadCurrentContext() );
			}
			result.addAll( l );
			return result.stream();
		} ).collect( Collectors.toList() );
	}

	public abstract List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnValueContext );

	public int registerKey( BoxExpression key ) {
		String name;
		if ( key instanceof BoxStringLiteral str ) {
			name = str.getValue();
		} else if ( key instanceof BoxIntegerLiteral intr ) {
			name = intr.getValue();
		} else {
			throw new IllegalStateException( "Key must be a string or integer literal" );
		}
		// check if exists
		if ( keys.containsKey( name ) ) {
			return new ArrayList<>( keys.keySet() ).indexOf( name );
		}
		keys.put( name, key );
		return keys.size() - 1;
	}

	public Map<String, BoxExpression> getKeys() {
		return keys;
	}

	// TODO I don't think this actually needs to be optional I think I only ran into issues because I hadn't updated all the method visitor
	// areas to create a MethodContextTracker - this should be revisited
	public Optional<MethodContextTracker> getCurrentMethodContextTracker() {
		return methodContextTrackers.size() > 0 ? Optional.of( methodContextTrackers.getLast() ) : Optional.empty();
	}

	public void addMethodContextTracker( MethodContextTracker methodContextTracker ) {
		methodContextTrackers.add( methodContextTracker );
	}

	public void popMethodContextTracker() {
		methodContextTrackers.removeLast();
	}

	public List<TryCatchBlockNode> getTryCatchStack() {
		return tryCatchBlockNodes;
	}

	public void addTryCatchBlock( TryCatchBlockNode tryCatchBlockNode ) {
		tryCatchBlockNodes.add( tryCatchBlockNode );
	}

	public void clearTryCatchStack() {
		tryCatchBlockNodes = new ArrayList<TryCatchBlockNode>();
	}

	public void addBoxStaticInitializer( BoxStaticInitializer staticInitializer ) {
		this.staticInitializers.add( staticInitializer );
	}

	public List<BoxStaticInitializer> getBoxStaticInitializers() {
		return this.staticInitializers;
	}

	public Map<String, ClassNode> getAuxiliary() {
		return auxiliaries;
	}

	/**
	 * Register a named local class for the current compilation unit.
	 * Called during script/template transpilation, before the body is compiled.
	 *
	 * @param alias         the simple name as written in source (e.g. {@code "Person"})
	 * @param javaClassName the Java class name of the generated class
	 *                      (e.g. {@code "boxgenerated/scripts/MyScript$LocalClass$Person"})
	 */
	public void registerLocalClass( String alias, String javaClassName ) {
		this.localClasses.put( alias, javaClassName );
	}

	/**
	 * Returns the Java class name of a local class if the given alias refers to one, or
	 * {@code null} if it is not a local class.
	 *
	 * @param alias the simple name as written in source (e.g. {@code "Person"})
	 *
	 * @return the Java class name, or {@code null}
	 */
	public String getLocalClassName( String alias ) {
		return this.localClasses.get( alias );
	}

	/**
	 * Returns the map of all registered local classes.
	 *
	 * @return map of alias to Java internal class name
	 */
	public Map<String, String> getLocalClasses() {
		return this.localClasses;
	}

	public void setAuxiliary( String name, ClassNode classNode ) {
		auxiliaries.put( name, classNode );
		// if ( auxiliaries.putIfAbsent( name, classNode ) != null ) {
		// throw new IllegalArgumentException( "Auxiliary already registered: " + name );
		// }
	}

	public int incrementAndGetLambdaCounter() {
		return ++lambdaCounter;
	}

	public int incrementAndGetClosureCounter() {
		return ++closureCounter;
	}

	/**
	 * Get the map of UDF instantiation bytecodes keyed by function name.
	 */
	public Map<Key, List<AbstractInsnNode>> getUDFInstantiations() {
		return udfInstantiations;
	}

	/**
	 * Get the list of Lambda instantiation bytecodes.
	 */
	public List<List<AbstractInsnNode>> getLambdaInstantiations() {
		return lambdaInstantiations;
	}

	/**
	 * Get the list of Closure instantiation bytecodes.
	 */
	public List<List<AbstractInsnNode>> getClosureInstantiations() {
		return closureInstantiations;
	}

	public abstract List<List<AbstractInsnNode>> transformProperties( Type declaringType, List<BoxProperty> properties, String sourceType );

	/**
	 * Create a key and register it in the list compiled array of pre-calculated keys.
	 *
	 * @param expr the string name of the key
	 *
	 * @return a list of instructions that will create the key
	 */
	public List<AbstractInsnNode> createKey( String expr ) {
		return createKey( new BoxStringLiteral( expr, null, expr ) );
	}

	/**
	 * Create a key and register it in the list compiled array of pre-calculated keys.
	 *
	 * @param expr the Box expression name of the key
	 *
	 * @return a list of instructions that will create the key
	 */
	public List<AbstractInsnNode> createKey( BoxExpression expr ) {
		// If this key is a literal, we can optimize it
		if ( expr instanceof BoxStringLiteral || expr instanceof BoxIntegerLiteral ) {
			int pos = registerKey( expr );
			// Instead of Key.of(), we'll reference a static array of pre-created keys on the class
			return List.of( new FieldInsnNode(
			    Opcodes.GETSTATIC,
			    getProperty( "packageName" ).replace( '.', '/' )
			        + "/"
			        + getProperty( "classname" ),
			    "keys",
			    Type.getDescriptor( Key[].class ) ), new LdcInsnNode( pos ), new InsnNode( Opcodes.AALOAD ) );
		} else {
			// TODO: likely needs to retain return type info on transformed expression or extract from "expr"
			// Dynamic values will be created at runtime
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( transform( expr, TransformerContext.NONE, ReturnValueContext.VALUE ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Key.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( Object.class ) ),
			    false ) );
			return nodes;
		}
	}

	/**
	 * Create a key ad-hoc, without registering it in the list of pre-calculated keys.
	 *
	 * @param name the name of the key
	 *
	 * @return a list of instructions that will create the key
	 */
	public List<AbstractInsnNode> createKeyAdHoc( String name ) {
		return createKeyAdHoc( new BoxStringLiteral( name, null, name ) );
	}

	/**
	 * Create a key ad-hoc, without registering it in the list of pre-calculated keys.
	 *
	 * @param expr the Box expression name of the key
	 *
	 * @return a list of instructions that will create the key
	 */
	public List<AbstractInsnNode> createKeyAdHoc( BoxExpression expr ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.addAll( transform(
		    expr,
		    TransformerContext.NONE,
		    ReturnValueContext.VALUE
		) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( Key.class ),
		    "of",
		    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( Object.class ) ),
		    false ) );
		return nodes;
	}

	public List<AbstractInsnNode> transformDocumentation( List<BoxDocumentationAnnotation> documentation ) {
		List<List<AbstractInsnNode>> members = new ArrayList<>();
		documentation.forEach( doc -> {
			List<AbstractInsnNode> annotationKey = createKey( doc.getKey().getValue() );
			members.add( annotationKey );
			List<AbstractInsnNode> value = transform( doc.getValue(), TransformerContext.NONE, ReturnValueContext.VALUE );
			members.add( value );
		} );
		if ( members.isEmpty() ) {
			return List.of( new FieldInsnNode( Opcodes.GETSTATIC,
			    Type.getInternalName( Struct.class ),
			    "EMPTY",
			    Type.getDescriptor( IStruct.class ) ) );
		} else {
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), members ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Struct.class ),
			    "linkedOfNonConcurrent",
			    Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
			    false ) );
			return nodes;
		}
	}

	public List<AbstractInsnNode> transformAnnotations( List<BoxAnnotation> annotations, Boolean defaultTrue, boolean onlyLiteralValues ) {
		List<List<AbstractInsnNode>> members = new ArrayList<>();

		annotations.forEach( annotation -> {
			List<AbstractInsnNode> annotationKey = createKey( annotation.getKey().getValue() );
			members.add( annotationKey );
			BoxExpression			thisValue	= annotation.getValue();
			List<AbstractInsnNode>	value;
			if ( thisValue != null ) {
				// Literal values are transformed directly
				if ( thisValue.isLiteral() ) {
					value = transform( thisValue, TransformerContext.NONE, ReturnValueContext.VALUE );
				}
				// gonna try commenting this out
				else if ( onlyLiteralValues ) {
					// Runtime expressions we just put this place holder text in for
					value = List.of( new LdcInsnNode( "<Runtime Expression>" ) );
				} else if ( thisValue instanceof BoxStringInterpolation bsi && bsi.getValues().size() == 1 ) {
					// A quoted attribute value with a single interpolation element isn't forced to a string.
					// Ex: <bx:myComponent foo="#complexValue#">
					// It's represented as a BoxStringInterpolation, but we DON'T want to use the actual string transformer
					// as it will force the output to be a string!!
					value = transform( bsi.getValues().get( 0 ), TransformerContext.NONE, ReturnValueContext.VALUE );
				} else {
					value = transform( thisValue, TransformerContext.NONE, ReturnValueContext.VALUE );
				}
			} else if ( defaultTrue ) {
				// Annotations in tags with no value default to true string (CF compat)
				value = List.of( new FieldInsnNode( Opcodes.GETSTATIC,
				    Type.getInternalName( Boolean.class ),
				    "TRUE",
				    Type.getDescriptor( Boolean.class ) ) );
			} else {
				// Annotations in script with no value default to empty string (CF compat)
				value = List.of( new LdcInsnNode( "" ) );
			}
			members.add( value );
		} );

		if ( annotations.isEmpty() ) {
			return List.of(
			    new TypeInsnNode( Opcodes.NEW, Type.getInternalName( Struct.class ) ),
			    new InsnNode( Opcodes.DUP ),
			    new MethodInsnNode( Opcodes.INVOKESPECIAL,
			        Type.getInternalName( Struct.class ),
			        "<init>",
			        Type.getMethodDescriptor( Type.VOID_TYPE ),
			        false )
			);
		} else {
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), members ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Struct.class ),
			    "linkedOfNonConcurrent",
			    Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
			    false ) );
			return nodes;
		}
	}

	public List<AbstractInsnNode> transformAnnotations( List<BoxAnnotation> annotations ) {
		return transformAnnotations( annotations, false, true );
	}

	// public LabelNode getCurrentBreak( String label ) {
	// return breaks.get( label == null ? "" : label );
	// }

	// public void setCurrentBreak( String label, LabelNode labelNode ) {
	// this.breaks.put( label == null ? "" : label, labelNode );
	// }

	// public void removeCurrentBreak( String label ) {
	// this.breaks.remove( label == null ? "" : label );
	// }

	// public LabelNode getCurrentContinue( String label ) {
	// return continues.get( label == null ? "" : label );
	// }

	// public void setCurrentContinue( String label, LabelNode labelNode ) {
	// this.continues.put( label == null ? "" : label, labelNode );
	// }

	// public void removeCurrentContinue( String label ) {
	// this.continues.remove( label == null ? "" : label );
	// }

	public void addImport( BoxExpression expression, BoxIdentifier alias ) {
		imports.add( ImportDefinition.parse( alias == null
		    ? expression.toString()
		    : ( expression + " as " + alias.getName() ) ) );
	}

	public List<List<AbstractInsnNode>> getImports() {
		return imports.stream().map( anImport -> {
			String importStr = anImport.resolverPrefix() != null
			    ? anImport.resolverPrefix() + ":" + anImport.className()
			    : anImport.className();
			if ( anImport.isModuleImport() ) {
				importStr += "@" + anImport.moduleName();
			}
			importStr += " as " + anImport.alias();
			return List.<AbstractInsnNode>of( new LdcInsnNode( importStr ) );
		} ).toList();
	}

	public boolean matchesImport( String token ) {
		/*
		 * Not supporting
		 * - java:System
		 * - java:java.lang.System
		 * - java.lang.System
		 *
		 * right now, just
		 *
		 * - System
		 *
		 * as all the other options require grammar changes or are more complicated to recognize
		 */
		return imports.stream().anyMatch( i -> token.equalsIgnoreCase( i.alias() ) || token.equalsIgnoreCase( i.className() ) )
		    || localClassRefImports.stream().anyMatch( entry -> token.equalsIgnoreCase( entry[ 0 ] ) );
	}

	/**
	 * Register a local-class import that should be emitted at runtime with a resolved {@code Class<?>}
	 * reference via {@link ImportDefinition#fromClassRef(String, String, Class)}.
	 *
	 * @param alias             the simple name of the local class (e.g. "Config")
	 * @param internalClassName the JVM internal name of the compiled class
	 */
	public void addLocalClassRefImport( String alias, String internalClassName ) {
		this.localClassRefImports.add( new String[] { alias, internalClassName } );
	}

	/**
	 * Returns bytecode instruction lists that each create an {@link ImportDefinition} via
	 * {@link ImportDefinition#fromClassRef(String, String, Class)} for every registered local-class import.
	 * Each inner list leaves one {@code ImportDefinition} on the stack.
	 *
	 * @return a list of instruction lists, one per local-class import
	 */
	public List<List<AbstractInsnNode>> getLocalClassRefImportNodes() {
		return this.localClassRefImports.stream().map( entry -> {
			String	alias				= entry[ 0 ];
			String	internalClassName	= entry[ 1 ];
			return List.<AbstractInsnNode>of(
			    new LdcInsnNode( ClassLocator.BX_PREFIX ),
			    new LdcInsnNode( alias ),
			    new LdcInsnNode( Type.getObjectType( internalClassName ) ),
			    new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( ImportDefinition.class ),
			        "fromClassRef",
			        Type.getMethodDescriptor( Type.getType( ImportDefinition.class ),
			            Type.getType( String.class ), Type.getType( String.class ), Type.getType( Class.class ) ),
			        false )
			);
		} ).toList();
	}
}
