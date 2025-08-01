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
package ortus.boxlang.runtime.context;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.FunctionCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;
import ortus.boxlang.runtime.util.Attachable;
import ortus.boxlang.runtime.util.DataNavigator;
import ortus.boxlang.runtime.util.DataNavigator.Navigator;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.IBoxAttachable;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This context represents the context of ANYTHING that can execute in BoxLang
 */
public class BaseBoxContext implements IBoxContext {

	/**
	 * TODO: This can be removed later, it was put here to catch some endless recursion bugs
	 */
	private static final ThreadLocal<Integer>	flushBufferDepth		= ThreadLocal.withInitial( () -> 0 );

	/**
	 * A flag to control whether null is considered undefined or not. Used by the compat module
	 */
	public static boolean						nullIsUndefined			= false;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Any context can have a parent it can delegate to
	 */
	protected IBoxContext						parent;

	/**
	 * A way to discover the current executing template. We're storing the path
	 * directly instead of the
	 * ITemplateRunnable instance to avoid memory leaks by keepin Box Classes in
	 * memory since all
	 * we really need is static data from them
	 */
	protected ArrayDeque<ResolvedFilePath>		templates				= new ArrayDeque<>();

	/**
	 * A way to discover the imports tied to the original source of the current
	 * template.
	 * This should always match the top current template stack
	 */
	protected List<ImportDefinition>			currentImports			= null;

	/**
	 * A way to discover the current executing componenet
	 */
	protected ArrayDeque<IStruct>				components				= new ArrayDeque<>();

	/**
	 * This is a denormalized cache of how many "output" components are on the component stack. We use this information very often when flushing output
	 */
	private final AtomicInteger					outputComponentCount	= new AtomicInteger( 0 );

	/**
	 * A way to track query loops
	 */
	protected LinkedHashMap<Query, Integer>		queryLoops				= new LinkedHashMap<>();

	/**
	 * A buffer to write output to
	 */
	protected ArrayDeque<StringBuffer>			buffers					= new ArrayDeque<>();

	/**
	 * The function service we can use to retrieve BIFS and member methods
	 */
	private final FunctionService				functionService;

	/**
	 * The component service
	 */
	private final ComponentService				componentService;

	/**
	 * Attachable delegate
	 */
	private final IBoxAttachable				attachable				= new Attachable();

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent
	 * context
	 *
	 * @param parent The parent context
	 */
	public BaseBoxContext( IBoxContext parent ) {
		this.parent				= parent;
		this.functionService	= BoxRuntime.getInstance().getFunctionService();
		this.componentService	= BoxRuntime.getInstance().getComponentService();
		buffers.push( new StringBuffer() );
	}

	/**
	 * Creates a new execution context with no execution template
	 */
	public BaseBoxContext() {
		this( null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the context logger
	 */
	public BoxLangLogger getLogger() {
		return BoxRuntime.getInstance().getLoggingService().RUNTIME_LOGGER;
	}

	/**
	 * Push a template to the stack
	 *
	 * @param template The template that this execution context is bound to
	 *
	 * @return IBoxContext
	 */
	public IBoxContext pushTemplate( IBoxRunnable template ) {
		pushTemplate( template.getRunnablePath() );
		this.currentImports = template.getImports();
		return this;
	}

	/**
	 * Push a template to the stack
	 *
	 * @param template The template that this execution context is bound to
	 *
	 * @return IBoxContext
	 */
	public IBoxContext pushTemplate( ResolvedFilePath template ) {
		this.templates.push( template );
		return this;
	}

	/**
	 * Pop a template from the stack
	 *
	 * @return The template that this execution context is bound to
	 */
	public ResolvedFilePath popTemplate() {
		return this.templates.pop();
	}

	/**
	 * Get templates
	 *
	 * @return The templates
	 */
	public ResolvedFilePath[] getTemplates() {
		return this.templates.toArray( new ResolvedFilePath[ 0 ] );
	}

	/**
	 * Push a Component to the stack
	 *
	 * @param executionState The state for this component execution
	 *
	 * @return This context
	 */
	public IBoxContext pushComponent( IStruct executionState ) {
		this.components.push( executionState );
		if ( executionState.getAsKey( Key._NAME ).equals( Key.output ) ) {
			// If this is a component, we need to increment the output component count
			outputComponentCount.incrementAndGet();
		}
		return this;
	}

	/**
	 * Pop a template from the stack
	 *
	 * @return This context
	 */
	public IBoxContext popComponent() {
		// decrement the output component count if the component being popped is an output component
		IStruct popped = this.components.pop();
		if ( popped.getAsKey( Key._NAME ).equals( Key.output ) ) {
			outputComponentCount.decrementAndGet();
		}
		return this;
	}

	/**
	 * Is there at least one output component on the stack
	 *
	 * @return True if there is at least one output component, else false
	 */
	public boolean isInOutputComponent() {
		return outputComponentCount.get() > 0;
	}

	/**
	 * Gets the execution state for the closest component.
	 *
	 * @return The execution state for the closest component, null if none was found
	 */
	public IStruct findClosestComponent( Key name, int offset ) {
		return findClosestComponent( name, offset, null );
	}

	/**
	 * Gets the execution state for the closest component.
	 *
	 * @return The execution state for the closest component, null if none was found
	 */
	public IStruct findClosestComponent( Key name ) {
		return findClosestComponent( name, 0, null );
	}

	/**
	 * Gets the execution state for the closest component with a predicate to
	 * filter.
	 *
	 * @return The execution state for the closest component, null if none was found
	 */
	public IStruct findClosestComponent( Key name, int offset, Predicate<IStruct> predicate ) {
		IStruct[] componentArray = getComponents();
		for ( int i = offset; i < componentArray.length; i++ ) {
			IStruct component = componentArray[ i ];

			if ( component.get( Key._NAME ).equals( name ) && ( predicate == null || predicate.test( component ) ) ) {
				return component;
			}
		}
		return null;
	}

	/**
	 * Gets the execution state for the closest component with a predicate to
	 * filter.
	 *
	 * @return The execution state for the closest component, null if none was found
	 */
	public IStruct findClosestComponent( Key name, Predicate<IStruct> predicate ) {
		return findClosestComponent( name, 0, predicate );
	}

	/**
	 * Get the stack of components as an array
	 *
	 * @return This context
	 */
	public IStruct[] getComponents() {
		// get parent components and append our own
		if ( hasParent() ) {
			IStruct[]	parentComponents	= getParent().getComponents();
			IStruct[]	myComponents		= this.components.toArray( new IStruct[ 0 ] );
			IStruct[]	allComponents		= new IStruct[ parentComponents.length + myComponents.length ];
			System.arraycopy( myComponents, 0, allComponents, 0, myComponents.length );
			System.arraycopy( parentComponents, 0, allComponents, myComponents.length, parentComponents.length );
			return allComponents;
		}
		return this.components.toArray( new IStruct[ 0 ] );
	}

	/**
	 * Has the execution context been bound to a template?
	 *
	 * @return True if bound, else false
	 */
	public boolean hasTemplates() {
		return !this.templates.isEmpty();
	}

	/**
	 * Finds the closest template
	 *
	 * @return The template instance if found, null if this code is not called from
	 *         a template
	 */
	public ResolvedFilePath findClosestTemplate() {
		// If this context has templates, grab the first
		if ( hasTemplates() ) {
			return this.templates.peek();
		}

		// Otherwise, if we have a parent, ask them
		if ( hasParent() ) {
			return getParent().findClosestTemplate();
		}

		// There are none to be found!
		return null;
	}

	/**
	 * Finds the base (first) template in this request
	 *
	 * @return The template instance if found, null if this code is not called from
	 *         a template
	 */
	public ResolvedFilePath findBaseTemplate() {
		ResolvedFilePath result = null;
		// If we have a parent, ask them
		if ( hasParent() ) {
			result = getParent().findBaseTemplate();
			if ( result != null ) {
				return result;
			}
		}
		// Otherwise, If this context has templates, grab the last
		if ( hasTemplates() ) {
			return this.templates.peekLast();
		}

		// There are none to be found!
		return null;
	}

	/**
	 * rethrows the closest exception
	 */
	public void rethrow() {

		// If we have a parent, ask them
		if ( hasParent() ) {
			getParent().rethrow();
		}

		throw new BoxRuntimeException( "No exception to rethrow.  You can only rethrow inside of a catch block." );
	}

	/**
	 * Returns the parent box context. Null if none.
	 *
	 * @return The parent box context. Null if none.
	 */
	public IBoxContext getParent() {
		return this.parent;
	}

	/**
	 * Sets the parent box context.
	 *
	 * @param parentContext The parent context to set
	 *
	 * @return This context
	 */
	public IBoxContext setParent( IBoxContext parentContext ) {
		this.parent = parentContext;
		return this;
	}

	/**
	 * Inject a parent context, moving the current parent to the grandparent
	 * Any existing parent in the passed context will be overwritten with the
	 * current parent
	 *
	 * @param parentContext The parent context to inject
	 *
	 * @return This context
	 */
	public IBoxContext injectParentContext( IBoxContext parentContext ) {
		parentContext.setParent( getParent() );
		setParent( parentContext );
		return this;
	}

	/**
	 * Inject a top parent context above the request-type context, moving the
	 * request context's current parent to its grandparent
	 *
	 * @param parentContext The parent context to inject
	 *
	 * @return This context
	 */
	public IBoxContext injectTopParentContext( IBoxContext parentContext ) {
		var requestContext = getParentOfType( RequestBoxContext.class );
		// If there is no request-type context (unlikely), just fall back to injecting
		// our own parent
		if ( requestContext == null ) {
			return injectParentContext( parentContext );
		}
		requestContext.injectParentContext( parentContext );
		return this;
	}

	/**
	 * Remove ancestor contexts of this type
	 *
	 * @param type The type of context to remove
	 *
	 * @return This context
	 */
	public IBoxContext removeParentContext( Class<? extends IBoxContext> type ) {
		if ( hasParent() ) {
			if ( type.isInstance( getParent() ) ) {
				setParent( getParent().getParent() );
			} else {
				getParent().removeParentContext( type );
			}
		}
		return this;
	}

	/**
	 * Verifies if a parent context is attached to this context
	 *
	 * @return True if a parent context is attached to this context, else false
	 */
	public Boolean hasParent() {
		return this.parent != null;
	}

	/**
	 * Invoke a function call such as foo() using positional args. Will check for a
	 * registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Object[] positionalArguments ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, positionalArguments, false, name );
		}

		Function function = findFunction( name );
		if ( function == null ) {
			throw new BoxRuntimeException( "Function '" + name.getName() + "' not found" );
		}
		return invokeFunction( function, name, positionalArguments );
	}

	/**
	 * Invoke a function call such as foo() using named args. Will check for a
	 * registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Map<Key, Object> namedArguments ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, namedArguments, false, name );
		}

		Function function = findFunction( name );
		if ( function == null ) {
			throw new BoxRuntimeException( "Function '" + name.getName() + "' not found" );
		}
		return invokeFunction( function, name, namedArguments );
	}

	/**
	 * Invoke a function call such as foo() using no args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, false );
		}

		Function function = findFunction( name );
		if ( function == null ) {
			throw new BoxRuntimeException( "Function '" + name.getName() + "' not found" );
		}
		return invokeFunction( function, name, new Object[] {} );
	}

	/**
	 * Invoke a component call
	 *
	 * @param name          The name of the component to invoke
	 * @param attributes    The attributes to pass to the component
	 * @param componentBody The body of the component
	 *
	 */
	public Component.BodyResult invokeComponent( Key name, IStruct attributes, Component.ComponentBody componentBody ) {
		getRuntime().getConfiguration().security.isComponentAllowed( name.getName() );
		ComponentDescriptor comp = componentService.getComponent( name );
		if ( comp != null ) {
			return comp.invoke( this, attributes, componentBody );
		} else {
			throw new BoxRuntimeException( "Component [" + name.getName() + "] could not be found." );
		}
	}

	/**
	 * Invoke a function expression such as (()=>{})() using positional args.
	 * This method will validate the incoming object is a function type.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Object function, Object[] positionalArguments ) {
		Function func = FunctionCaster.cast( function );
		return invokeFunction(
		    func,
		    func.getName(),
		    positionalArguments );
	}

	/**
	 * Find out if the given function name is a BIF in the Function Service
	 *
	 * @param name The name of the function to find
	 *
	 * @return The BIFDescriptor if found, else null
	 */
	protected BIFDescriptor findBIF( Key name ) {
		getRuntime().getConfiguration().security.isBIFAllowed( name.getName() );
		return this.functionService.getGlobalFunction( name );
	}

	/**
	 * Invoke a function expression such as (()=>{})() using named args.
	 * This method will validate the incoming object is a function type.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Object function, Map<Key, Object> namedArguments ) {
		Function func = FunctionCaster.cast( function );
		return invokeFunction(
		    func,
		    func.getName(),
		    namedArguments );
	}

	/**
	 * Invoke a function expression such as (()=>{})() using no args.
	 * This method will validate the incoming object is a function type.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Object function ) {
		Function func = FunctionCaster.cast( function );
		return func.invoke(
		    Function.generateFunctionContext(
		        func,
		        getFunctionParentContext(),
		        func.getName(),
		        new Object[] {},
		        getFunctionClass(),
		        getFunctionStaticClass(),
		        getFunctionInterface() ) );
	}

	/**
	 * Invoke a function expression such as (()=>{})() using named args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Function function, Key calledName, Object[] positionalArguments ) {
		return function.invoke(
		    Function.generateFunctionContext(
		        function,
		        getFunctionParentContext(),
		        calledName,
		        positionalArguments,
		        getFunctionClass(),
		        getFunctionStaticClass(),
		        getFunctionInterface() ) );
	}

	/**
	 * Invoke a function expression such as (()=>{})() using named args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Function function, Key calledName, Map<Key, Object> namedArguments ) {
		return function.invoke(
		    Function.generateFunctionContext(
		        function,
		        getFunctionParentContext(),
		        calledName,
		        namedArguments,
		        getFunctionClass(),
		        getFunctionStaticClass(),
		        getFunctionInterface() ) );
	}

	/**
	 * Find a function in the corrent context. Will search known scopes for a UDF.
	 *
	 * @param name The name of the function to find
	 *
	 * @return The function instance
	 */
	protected Function findFunction( Key name ) {
		ScopeSearchResult result = null;
		try {
			result = scopeFindNearby( name, null, false );
		} catch ( KeyNotFoundException e ) {
			return null;
		}
		if ( result == null ) {
			return null;
		}
		CastAttempt<Function> funcAttempt = FunctionCaster.attempt( result.value() );
		if ( funcAttempt.wasSuccessful() ) {
			return funcAttempt.get();
		} else {
			throw new BoxRuntimeException(
			    "Variable '" + name + "' of type  '" + result.value().getClass().getName() + "'  is not a function." );
		}
	}

	/**
	 * Invoke a template in the current context
	 *
	 * @param templatePath A relateive template path
	 */
	@Override
	public void includeTemplate( String templatePath, boolean externalOnly ) {
		Set<String>	VALID_TEMPLATE_EXTENSIONS	= BoxRuntime.getInstance().getConfiguration().getValidTemplateExtensions();

		String		ext							= "";
		// If there is double //, remove the first char
		if ( templatePath.startsWith( "//" ) ) {
			templatePath = templatePath.substring( 1 );
		}
		Path pfileName = Paths.get( templatePath ).getFileName();

		if ( pfileName == null ) {
			throw new BoxRuntimeException( "Template path [" + templatePath + "] does not have a filename" );
		}

		String fileName = pfileName.toString().toLowerCase();
		if ( fileName.contains( "." ) ) {
			ext = fileName.substring( fileName.lastIndexOf( "." ) + 1 );
		}

		// This extension check is duplicated in the runnableLoader right now since some code paths hit the runnableLoader directly
		if ( ext.equals( "*" ) || VALID_TEMPLATE_EXTENSIONS.contains( ext ) ) {
			// Load template class, compiling if neccessary
			BoxTemplate template = RunnableLoader.getInstance().loadTemplateRelative( this, templatePath, externalOnly );

			template.invoke( this );
		} else {
			// If this extension is not one we compile, then just read the contents and flush it to the buffer
			writeToBuffer(
			    invokeFunction( Key.fileread, new Object[] { FileSystemUtil.expandPath( this, templatePath, externalOnly ).absolutePath().toString() } ) );
		}
	}

	/**
	 * This is mostly for the debugger. It returns all visible scopes from this
	 * context.
	 *
	 * @return A struct containing all contextual and lexically visible scopes
	 *
	 */
	public IStruct getVisibleScopes() {
		IStruct scopes = Struct.linkedOf(
		    Key.contextual,
		    Struct.linkedOf(),
		    Key.lexical,
		    Struct.linkedOf() );
		return getVisibleScopes( scopes, true, false );
	}

	/**
	 * This is mostly for the debugger. It returns all visible scopes from this
	 * context.
	 *
	 * @return A struct containing all contextual and lexically visible scopes
	 *
	 */
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, nearby, shallow );
		}
		return scopes;
	}

	/**
	 * Check if a key is visible in the current context as a scope name.
	 * This allows us to "reserve" known scope names to ensure arguments.foo
	 * will always look in the proper arguments scope and never in
	 * local.arguments.foo for example
	 *
	 * @param key The key to check for visibility
	 *
	 * @return True if the key is visible in the current context, else false
	 */
	public boolean isKeyVisibleScope( Key key ) {
		return isKeyVisibleScope( key, true, false );
	}

	/**
	 * Check if a key is visible in the current context as a scope name.
	 * This allows us to "reserve" known scope names to ensure arguments.foo
	 * will always look in the proper arguments scope and never in
	 * local.arguments.foo for example
	 *
	 * @param key     The key to check for visibility
	 * @param nearby  true, check only scopes that are nearby to the current execution context
	 * @param shallow true, do not delegate to parent or default scope if not found
	 *
	 * @return True if the key is visible in the current context, else false
	 */
	public boolean isKeyVisibleScope( Key key, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			return getParent().isKeyVisibleScope( key, nearby, shallow );
		}
		return false;
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are nearby to an execution context
	 *
	 * @param name The name of the scope to get
	 *
	 * @return The requested scope
	 *
	 * @throws ScopeNotFoundException If the scope was not found in any context
	 */
	public IScope getScope( Key name ) throws ScopeNotFoundException {
		throw new BoxRuntimeException( "Unimplemented method 'getScope'" );
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all known scopes
	 *
	 * @param name    The name of the scope to get
	 * @param shallow true, do not delegate to parent or default scope if not found
	 *
	 * @return The requested scope
	 *
	 * @throws ScopeNotFoundException If the scope was not found in any context
	 */
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {
		throw new BoxRuntimeException( "Unimplemented method 'getScopeNearby'" );
	}

	/**
	 * Try to get the requested key from an unknown scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * Unlike scopeFindNearby(), this version only searches trancedent scopes like
	 * cgi or server which are never encapsulated like variables is inside a class.
	 *
	 * If defaultScope is null and the key can't be found, a KeyNotFoundException
	 * will be thrown
	 * If defaultScope is not null, it will return a record with the default scope
	 * and null value if the key is not found
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {
		throw new BoxRuntimeException( "Unimplemented method 'scopeFind'" );
	}

	/**
	 * Try to get the requested key from an unknown scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * A nearby lookup is used for the closest context to the executing code
	 *
	 * If defaultScope is null and the key can't be found, a KeyNotFoundException
	 * will be thrown
	 * If defaultScope is not null, it will return a record with the default scope
	 * and null value if the key is not found
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {
		throw new BoxRuntimeException( "Unimplemented method 'scopeFindNearby'" );
	}

	/**
	 * Decide if a value found in a scope is defined or not
	 *
	 * @param value The value to check, possibly null, possibly an instance of NullValue
	 *
	 * @return True if the value is defined, else false
	 */
	public boolean isDefined( Object value, boolean forAssign ) {
		// If the value is null, it's not defined because the struct litearlly has no key for this
		if ( value == null ) {
			return false;
		}
		// Default BoxLang behavior is null is defined, but if compat has toggled the nullIsUndefined setting, then we need to check for our placeHolder NullValue value
		// Unless we're check for the purpose of assignment, then we need to treat NullValue as undefined
		if ( !forAssign && nullIsUndefined && value instanceof NullValue ) {
			return false;
		}
		// Otherwise, it's defined
		return true;
	}

	/**
	 * Search any query loops for a column name matching the uncscoped variable
	 *
	 * @param key The key to search for
	 *
	 * @return A ScopeSearchResult if found, else null
	 */
	protected ScopeSearchResult queryFindNearby( Key key ) {
		if ( queryLoops.size() > 0 ) {
			var queries = queryLoops.keySet().toArray( new Query[ 0 ] );
			for ( int i = queries.length - 1; i >= 0; i-- ) {
				Query query = queries[ i ];
				if ( key.equals( Key.recordCount ) ) {
					return new ScopeSearchResult( null, query.size(), key );
				}
				if ( key.equals( Key.currentRow ) ) {
					return new ScopeSearchResult( null, queryLoops.get( query ) + 1, key );
				}
				if ( key.equals( Key.columnList ) ) {
					return new ScopeSearchResult( null, query.getColumnList(), key );
				}
				if ( key.equals( Key.columnArray ) || key.equals( Key.columnNames ) ) {
					return new ScopeSearchResult( null, query.getColumnArray(), key );
				}
				if ( query.hasColumn( key ) ) {
					// TODO: create query scope wrapper for edge cases
					return new ScopeSearchResult( null, query.getCell( key, queryLoops.get( query ) ), key );
				}
			}
		}
		return null;
	}

	/**
	 * Register a UDF with the local context. Will override any existing methods
	 *
	 * @param udf The UDF to register
	 */
	public void registerUDF( UDF udf ) {
		registerUDF( udf, true );
	}

	/**
	 * Register a UDF with the local context choosing to override.
	 *
	 * @param udf      The UDF to register
	 * @param override true, override any existing UDF with the same name
	 */
	public void registerUDF( UDF udf, boolean override ) {
		throw new BoxRuntimeException( "This context [" + getClass().getSimpleName() + "] cannot register a function" );
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	public IScope getDefaultAssignmentScope() {
		throw new BoxRuntimeException( "Unimplemented method 'getDefaultAssignmentScope'" );
	}

	/**
	 * Finds the closest function call name
	 *
	 * @return The called name of the function if found, null if this code is not
	 *         called from a function
	 */
	public Key findClosestFunctionName() {
		if ( hasParent() ) {
			return getParent().findClosestFunctionName();
		}
		return null;
	}

	/**
	 * Get parent context for a function execution happening in this context
	 *
	 * @return The context to use
	 */
	public IBoxContext getFunctionParentContext() {
		return this;
	}

	/**
	 * Get the class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	public IClassRunnable getFunctionClass() {
		return null;
	}

	/**
	 * Get the class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	public DynamicObject getFunctionStaticClass() {
		return null;
	}

	/**
	 * Get the interface, if any, for a function invocation
	 *
	 * @return The interface to use, or null if none
	 */
	public BoxInterface getFunctionInterface() {
		return null;
	}

	/**
	 * Try to get the requested key from an unkonwn scope but overriding the parent
	 * to check if not found
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean forAssign ) {
		return scopeFindNearby( key, defaultScope, false, forAssign );
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all known scopes
	 *
	 * @param name The name of the scope to get
	 *
	 * @return The requested scope
	 *
	 * @throws ScopeNotFoundException If the scope was not found in any context
	 */
	public IScope getScopeNearby( Key name ) throws ScopeNotFoundException {
		return getScopeNearby( name, false );
	}

	/**
	 * Retrieve all known imports for the current template
	 *
	 * @return List of import definitions
	 */
	public List<ImportDefinition> getCurrentImports() {
		return currentImports;
	}

	/**
	 * If input is a QueryColumn, unwrap it to the underlying value
	 * If input is not a QueryColumn, return it as-is
	 *
	 * @param value The value to unwrap
	 *
	 * @return The unwrapped value
	 */
	public Object unwrapQueryColumn( Object value ) {
		if ( value instanceof QueryColumn col ) {
			return col.getCell( getQueryRow( col.getQuery() ) );
		}
		return value;
	}

	/**
	 * Get the current query row
	 *
	 * @param query The query to get the row from
	 *
	 * @return The current row
	 */
	public int getQueryRow( Query query ) {
		return getQueryRow( query, 0 );
	}

	/**
	 * Get the current query row, providing a default value if the query is not registered
	 *
	 * @param query      The query to get the row from
	 * @param defaultRow The default value to return if the query is not registered
	 *
	 * @return The current row
	 */
	public int getQueryRow( Query query, int defaultRow ) {
		// If we're not looping over this query, then we're on the first row
		if ( !queryLoops.containsKey( query ) ) {
			return defaultRow;
		}
		return queryLoops.get( query );
	}

	/**
	 * Register a query loop
	 *
	 * @param query The query to register
	 */
	public void registerQueryLoop( Query query, int row ) {
		queryLoops.put( query, row );
	}

	/**
	 * Unregister a query loop
	 *
	 * @param query The query to unregister
	 */
	public void unregisterQueryLoop( Query query ) {
		queryLoops.remove( query );
	}

	/**
	 * Increment the query loop
	 *
	 * @param query The query to increment
	 */
	public void incrementQueryLoop( Query query ) {
		queryLoops.put( query, queryLoops.get( query ) + 1 );
	}

	/**
	 * Write output to this buffer. Any input object will be converted to a string
	 * If force is true, write even if the setting component has been used with
	 * enableOutputOnly=true
	 *
	 * @param o     The object to write
	 * @param force true, write even if output is disabled
	 *
	 * @return This context
	 */
	public IBoxContext writeToBuffer( Object o, boolean force ) {
		if ( o == null ) {
			return this;
		}
		// IStruct outputState = null;
		if ( !force ) {
			Boolean explicitOutput = ( Boolean ) getConfigItem( Key.enforceExplicitOutput, false );
			if ( explicitOutput && !isInOutputComponent() ) {
				return this;
				// If we are requiring to be in an output component, let's look fo r it
				// outputState = findClosestComponent( Key.output );
				// if ( outputState == null ) {
				// return this;
				// }
			}
		}

		String content = StringCaster.cast( o );
		// If the closest output didn't have an encode for, let's look a little harder
		// to see if we can find one.
		// if ( outputState == null || outputState.getAsString( Key.encodefor ) == null ) {
		// outputState = findClosestComponent( Key.output, state -> state.get( Key.encodefor ) != null );
		// }
		// if ( outputState != null ) {
		// String encodeFor = outputState.getAsString( Key.encodefor );
		// TODO: encode the content
		// Waiting on ESAPI implementation
		// }

		getBuffer().append( content );
		return this;
	}

	/**
	 * Write output to this buffer. Any input object will be converted to a string
	 *
	 * @param o The object to write
	 *
	 * @return This context
	 */
	public IBoxContext writeToBuffer( Object o ) {
		return writeToBuffer( o, false );
	}

	/**
	 * Can the current context output to the response stream?
	 * Contexts tied to a specific object like a function or class may override this
	 * to return false based on their own logic.
	 */
	public Boolean canOutput() {
		return true;
	}

	/**
	 * Flush the buffer to the output stream and then clears the local buffers
	 *
	 * @param force true, flush even if output is disabled
	 *
	 * @return This context
	 */
	public IBoxContext flushBuffer( boolean force ) {
		if ( !canOutput() && !force ) {
			return this;
		}
		flushBufferDepth.set( flushBufferDepth.get() + 1 );

		// Check the depth
		if ( flushBufferDepth.get() > 50 ) {
			throw new RuntimeException( "Nested flushBuffer() calls exceeded 50" );
		}

		try {
			// If there are extra buffers registered, we ignore flush requests since someone
			// out there is wanting to capture our buffer instead.
			if ( hasParent() && buffers.size() == 1 ) {
				StringBuffer thisBuffer = getBuffer();
				synchronized ( thisBuffer ) {
					getParent().writeToBuffer( thisBuffer.toString(), true );
					thisBuffer.setLength( 0 );
				}
				if ( force ) {
					getParent().flushBuffer( true );
				}
			} else if ( force && hasParent() ) {
				for ( StringBuffer buf : buffers ) {
					synchronized ( buf ) {
						getParent().writeToBuffer( buf.toString(), true );
						buf.setLength( 0 );
					}
				}
				getParent().flushBuffer( true );
			}
			return this;
		} finally {
			// Decrement the depth
			flushBufferDepth.set( flushBufferDepth.get() - 1 );
		}
	}

	/**
	 * Clear the buffer
	 *
	 * @return This context
	 */
	public IBoxContext clearBuffer() {
		getBuffer().setLength( 0 );
		if ( hasParent() ) {
			getParent().clearBuffer();
		}
		return this;
	}

	/**
	 * Get the buffer
	 *
	 * @return The buffer
	 */
	public StringBuffer getBuffer() {
		return this.buffers.peek();
	}

	/**
	 * Push a buffer onto the stack. This is mostly so components can capture any
	 * output generated in their body
	 *
	 * @param buffer The buffer to push
	 *
	 * @return This context
	 */
	public IBoxContext pushBuffer( StringBuffer buffer ) {
		this.buffers.push( buffer );
		return this;
	}

	/**
	 * Pop a buffer from the stack
	 *
	 * @return This context
	 */
	public IBoxContext popBuffer() {
		this.buffers.pop();
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public Navigator navigateConfig( String... path ) {
		return DataNavigator.of( getConfig() ).from( path );
	}

	/**
	 * Get the contexual config struct. Each context has a chance to add in config
	 * of their
	 * own to the struct, or override existing config with a new struct of their own
	 * design.
	 * It depends on whether the context wants its changes to exist for the rest of
	 * the entire
	 * request or only for code that executes in the current context and below.
	 *
	 * @return A struct of configuration
	 */
	public IStruct getConfig() {
		if ( hasParent() ) {
			return getParent().getConfig();
		}
		return new Struct();
	}

	/**
	 * Contexts can optionallky cache their config. If so, they must override this method
	 * to clear the cache when requested, and propagate the request to their parent context
	 */
	public void clearConfigCache() {
		if ( hasParent() ) {
			getParent().clearConfigCache();
		}
	}

	/**
	 * Convenience method to retrieve a single config item
	 *
	 * @param itemKey the object key to retrieve
	 *
	 * @return The object value of the key or null if not found
	 */
	public Object getConfigItem( Key itemKey ) {
		return getConfig().get( itemKey );
	}

	/**
	 * Convenience method to retrieve a config item(s). You can pass in multiple
	 * keys separated by commas. It will traverse the keys in order and return the last
	 * key requested..
	 *
	 * <pre>
	 * // Example:
	 * // config = { a: { b: { c: 1 } } }
	 * * // getConfigItems( a, b, c ) will return 1
	 * </pre>
	 *
	 * @param itemKey the object key(s)
	 *
	 * @return The object value of the key or null if not found
	 */
	public Object getConfigItems( Key... itemKey ) {
		Object	config		= getConfig();
		Object	lastResult	= null;

		for ( Key key : itemKey ) {

			if ( config instanceof IStruct castedConfig && castedConfig.containsKey( key ) ) {
				lastResult	= castedConfig.get( key );
				config		= lastResult;
			} else {
				break;
			}
		}

		return lastResult;
	}

	/**
	 * Convenience method to retrieve a config item with with an optional default
	 *
	 * @param itemKey      the object key
	 * @param defaultValue a default value to return
	 *
	 * @return The object value of the key or the default value if not found
	 */
	public Object getConfigItem( Key itemKey, Object defaultValue ) {
		return getConfig().getOrDefault( itemKey, defaultValue );
	}

	/**
	 * Get the BoxLang runtime
	 * '
	 *
	 * @return The runtime
	 */
	public BoxRuntime getRuntime() {
		return BoxRuntime.getInstance();
	}

	/**
	 * Get a struct of module settings
	 *
	 * @param name The name of the module
	 *
	 * @throws BoxRuntimeException If the module was not found
	 *
	 * @return The module settings struct
	 */
	public IStruct getModuleSettings( Key name ) {
		return getRuntime().getModuleService().getModuleSettings( name );
	}

	/**
	 * Get a module record
	 *
	 * @param name The name of the module
	 *
	 * @throws BoxRuntimeException If the module was not found
	 *
	 * @return The module record
	 */
	public ModuleRecord getModuleRecord( Key name ) {
		return getRuntime().getModuleService().getModuleRecord( name );
	}

	/**
	 * Serach for an ancestor context of the given type
	 *
	 * @param <T> The type of context to search for
	 *
	 * @return The matching parent context, or null if one is not found of this
	 *         type.
	 */
	@Override
	@SuppressWarnings( "unchecked" )
	public <T> T getParentOfType( Class<T> type ) {
		if ( type.isAssignableFrom( this.getClass() ) ) {
			return ( T ) this;
		}
		if ( hasParent() ) {
			return getParent().getParentOfType( type );
		}
		return null;
	}

	/**
	 * Serach for an ancestor context of RequestBoxContext
	 * This is a convenience method for getParentOfType( RequestBoxContext.class )
	 * since it is so common
	 *
	 * @return The matching parent RequestBoxContext, or null if one is not found of this
	 *         type.
	 */
	public RequestBoxContext getRequestContext() {
		return getParentOfType( RequestBoxContext.class );
	}

	/**
	 * Serach for an ancestor context of ApplicationBoxContext
	 * This is a convenience method for getParentOfType( ApplicationBoxContext.class )
	 * since it is so common
	 *
	 * @return The matching parent ApplicationBoxContext, or null if one is not found of this
	 *         type.
	 */
	public ApplicationBoxContext getApplicationContext() {
		return getParentOfType( ApplicationBoxContext.class );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Attachable Delegation
	 * --------------------------------------------------------------------------
	 */

	@Override
	public <T> T putAttachment( Key key, T value ) {
		return this.attachable.putAttachment( key, value );
	}

	@Override
	public <T> T getAttachment( Key key ) {
		return this.attachable.getAttachment( key );
	}

	@Override
	public boolean hasAttachment( Key key ) {
		return this.attachable.hasAttachment( key );
	}

	@Override
	public <T> T removeAttachment( Key key ) {
		return this.attachable.removeAttachment( key );
	}

	@Override
	public Key[] getAttachmentKeys() {
		return this.attachable.getAttachmentKeys();
	}

	@Override
	public <T> T computeAttachmentIfAbsent( Key key, java.util.function.Function<? super Key, ? extends T> mappingFunction ) {
		return this.attachable.computeAttachmentIfAbsent( key, mappingFunction );
	}

}
