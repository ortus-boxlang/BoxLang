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
package ortus.boxlang.runtime.runnables;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.meta.BoxMeta;

public interface IClassRunnable extends ITemplateRunnable, IStruct {

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the name
	 */
	public Key bxGetName();

	/**
	 * Get the variables scope
	 */
	public VariablesScope getVariablesScope();

	/**
	 * Get the this scope
	 */
	public ThisScope getThisScope();

	/**
	 * Get the static scope
	 */
	public StaticScope getStaticScope();

	/**
	 * Get annotations
	 */
	public IStruct getAnnotations();

	/**
	 * Get documentation
	 */
	public IStruct getDocumentation();

	/**
	 * Get the properties
	 */
	public Map<Key, Property> getProperties();

	/**
	 * Run the pseudo constructor
	 */
	public void pseudoConstructor( IBoxContext context );

	/**
	 * Run the internal pseudo constructor implementation
	 *
	 * @param context The context to run the pseudo constructor in
	 */
	public void _pseudoConstructor( IBoxContext context );

	/**
	 * Get the combined metadata for this class and all it's functions
	 * This follows the format of Lucee and Adobe's "combined" metadata
	 *
	 * @return The metadata as a struct
	 */
	public IStruct getMetaData();

	// Duplicate from IType
	public BoxMeta<?> getBoxMeta();

	/**
	 * Get the internal BoxMeta instance
	 *
	 * @return The BoxMeta instance
	 */
	public BoxMeta<?> _getbx();

	/**
	 * Set the internal BoxMeta instance
	 *
	 * @param bx The BoxMeta instance to set
	 */
	public void _setbx( BoxMeta<?> bx );

	/**
	 * Register an interface that this class implements
	 *
	 * @param _interface The interface to register
	 */
	public void registerInterface( BoxInterface _interface );

	/**
	 * Get the list of interfaces this class implements
	 *
	 * @return The list of interfaces
	 */
	public List<BoxInterface> getInterfaces();

	/**
	 * A helper to look at the "output" annotation, caching the result
	 *
	 * @return Whether the function can output
	 */
	public Boolean canOutput();

	/**
	 * Get the cached canOutput value
	 *
	 * @return Whether this class can output, or null if not yet determined
	 */
	public Boolean getCanOutput();

	/**
	 * Set the cached canOutput value
	 *
	 * @param canOutput Whether this class can output
	 */
	public void setCanOutput( Boolean canOutput );

	/**
	 * Check if implicit accessors can be invoked on this class
	 *
	 * @param context The current context
	 *
	 * @return Whether implicit accessors can be invoked
	 */
	public Boolean canInvokeImplicitAccessor( IBoxContext context );

	/**
	 * Get the cached canInvokeImplicitAccessor value
	 *
	 * @return Whether implicit accessors can be invoked, or null if not yet determined
	 */
	public Boolean getCanInvokeImplicitAccessor();

	/**
	 * Set the cached canInvokeImplicitAccessor value
	 *
	 * @param canInvokeImplicitAccessor Whether implicit accessors can be invoked
	 */
	public void setCanInvokeImplicitAccessor( Boolean canInvokeImplicitAccessor );

	/**
	 * Get the super class definition. Null if there is none
	 * 
	 * Deprecated in favor of getBoxSuperClassName
	 */
	@Deprecated
	default DynamicObject getSuperClass() {
		return getBoxSuperClass();
	}

	/**
	 * Get the super class definition. Null if there is none
	 */
	default DynamicObject getBoxSuperClass() {
		return getSuper() != null ? DynamicObject.of( getSuper() ) : null;
	}

	/**
	 * Get the super class name. Null if there is none or if it doesn't extend a Java class
	 */
	default String getBoxSuperClassName() {
		return null;
	}

	/**
	 * Get the interface names. Empty array if there are none
	 * 
	 * @return Array of interface names implemented by this class
	 */
	default String[] getBoxInterfaceNames() {
		return new String[ 0 ];
	}

	/**
	 * Get the super class instance. Null if there is none
	 */
	public IClassRunnable getSuper();

	/**
	 * Whether this class extends a Java class
	 *
	 * @return True if this class extends a Java class
	 */
	public boolean isJavaExtends();

	/**
	 * Is there a final annotation. Cached at compiled time and faster than the annotation map lookup.
	 * 
	 * @return Whether this class is final
	 */
	default boolean isFinalClass() {
		return false;
	}

	/**
	 * Is there an abstract annotation. Cached at compiled time and faster than the annotation map lookup.
	 * 
	 * @return Whether this class is abstract
	 */
	default boolean isAbstractClass() {
		return false;
	}

	/**
	 * What is the init method
	 * 
	 * @return The init method key
	 */
	default Key getInitMethod() {
		return Key.init;
	}

	/**
	 * Look up a private method handle for the given method
	 *
	 * @param method The method to look up
	 *
	 * @return The method handle for the private method
	 */
	public MethodHandle lookupPrivateMethod( Method method );

	/**
	 * Look up a private field handle for the given field
	 *
	 * @param field The field to look up
	 *
	 * @return The method handle for the private field
	 */
	public MethodHandle lookupPrivateField( Field field );

	/**
	 * Set the super class.
	 */
	public void setSuper( IClassRunnable _super );

	/**
	 * Set the super class.
	 */
	public void _setSuper( IClassRunnable _super );

	/**
	 * Get the child class. Null if there is none
	 */
	public IClassRunnable getChild();

	/**
	 * Set the child class.
	 */
	public void setChild( IClassRunnable child );

	/**
	 * Get the bottom class in the inheritance chain
	 */
	public IClassRunnable getBottomClass();

	/**
	 * Get the getter lookup map for properties with implicit getters
	 *
	 * @return Map of property keys to their Property definitions
	 */
	public Map<Key, Property> getGetterLookup();

	/**
	 * Get the setter lookup map for properties with implicit setters
	 *
	 * @return Map of property keys to their Property definitions
	 */
	public Map<Key, Property> getSetterLookup();

	/**
	 * Get the abstract methods declared directly in this class
	 *
	 * @return Map of method keys to their AbstractFunction definitions
	 */
	public Map<Key, AbstractFunction> getAbstractMethods();

	/**
	 * Get the set of method names known at compile time
	 *
	 * @return Set of compile time method name keys
	 */
	public Set<Key> getCompileTimeMethodNames();

	/**
	 * Get compile time methods (legacy - for backwards compatibility)
	 *
	 * @return Map of compile time method classes
	 */
	default Map<Key, Class<? extends UDF>> getCompileTimeMethods() {
		return Map.of();
	}

	/**
	 * Get UDFs defined in this class
	 *
	 * @return Map of UDF instances
	 */
	default Map<Key, UDF> getUDFs() {
		return Map.of();
	}

	/**
	 * Get all abstract methods including those inherited from superclasses and interfaces
	 *
	 * @return Map of method keys to their AbstractFunction definitions
	 */
	public Map<Key, AbstractFunction> getAllAbstractMethods();

	/*
	 * These methods allow IClassRunnable to extend IStruct without putting the actual implementations in the BoxClassTransformer
	 * This is what allows a Box Class to be used as a struct
	 */

	@Override
	default Object get( Object key ) {
		return getThisScope().get( key );
	}

	@Override
	default Object put( String key, Object value ) {
		return getThisScope().put( key, value );
	}

	@Override
	default Set<Key> keySet() {
		return getThisScope().keySet();
	}

	@Override
	default Set<Entry<Key, Object>> entrySet() {
		return getThisScope().entrySet();
	}

	@Override
	default boolean containsKey( Object key ) {
		return getThisScope().containsKey( key );
	}

	@Override
	default boolean containsValue( Object value ) {
		return getThisScope().containsValue( value );
	}

	@Override
	default int size() {
		return getThisScope().size();
	}

	@Override
	default boolean isEmpty() {
		return getThisScope().isEmpty();
	}

	@Override
	default void clear() {
		getThisScope().clear();
	}

	@Override
	default boolean containsKey( Key key ) {
		return getThisScope().containsKey( key );
	}

	@Override
	default boolean containsKey( String key ) {
		return getThisScope().containsKey( key );
	}

	@Override
	default Object get( String key ) {
		return getThisScope().get( key );
	}

	@Override
	default Object getOrDefault( Key key, Object defaultValue ) {
		return getThisScope().getOrDefault( key, defaultValue );
	}

	@Override
	default Object getOrDefault( String key, Object defaultValue ) {
		return getThisScope().getOrDefault( key, defaultValue );
	}

	@Override
	default Object getRaw( Key key ) {
		return getThisScope().getRaw( key );
	}

	@Override
	default Object put( Key key, Object value ) {
		return getThisScope().put( key, value );
	}

	@Override
	default Object putIfAbsent( Key key, Object value ) {
		return getThisScope().putIfAbsent( key, value );
	}

	@Override
	default Object putIfAbsent( String key, Object value ) {
		return getThisScope().putIfAbsent( key, value );
	}

	@Override
	default Object remove( String key ) {
		return getThisScope().remove( key );
	}

	@Override
	default Object remove( Key key ) {
		return getThisScope().remove( key );
	}

	@Override
	default void addAll( Map<? extends Object, ? extends Object> map ) {
		getThisScope().addAll( map );
	}

	@Override
	default List<Key> getKeys() {
		return getThisScope().getKeys();
	}

	@Override
	default List<String> getKeysAsStrings() {
		return getThisScope().getKeysAsStrings();
	}

	@Override
	default Map<? extends Object, Object> getWrapped() {
		return getThisScope().getWrapped();
	}

	@Override
	default Key getAsKey( Key key ) {
		return getThisScope().getAsKey( key );
	}

	@Override
	default Array getAsArray( Key key ) {
		return getThisScope().getAsArray( key );
	}

	@Override
	default IStruct getAsStruct( Key key ) {
		return getThisScope().getAsStruct( key );
	}

	@Override
	default DateTime getAsDateTime( Key key ) {
		return getThisScope().getAsDateTime( key );
	}

	@Override
	default String getAsString( Key key ) {
		return getThisScope().getAsString( key );
	}

	@Override
	default Double getAsDouble( Key key ) {
		return getThisScope().getAsDouble( key );
	}

	@Override
	default Long getAsLong( Key key ) {
		return getThisScope().getAsLong( key );
	}

	@Override
	default Integer getAsInteger( Key key ) {
		return getThisScope().getAsInteger( key );
	}

	@Override
	default Boolean getAsBoolean( Key key ) {
		return getThisScope().getAsBoolean( key );
	}

	@Override
	default Function getAsFunction( Key key ) {
		return getThisScope().getAsFunction( key );
	}

	@Override
	default Query getAsQuery( Key key ) {
		return getThisScope().getAsQuery( key );
	}

	@Override
	default XML getAsXML( Key key ) {
		return getThisScope().getAsXML( key );
	}

	@Override
	default TYPES getType() {
		return getThisScope().getType();
	}

	@Override
	default Boolean isCaseSensitive() {
		return getThisScope().isCaseSensitive();
	}

	@Override
	default Boolean isSoftReferenced() {
		return getThisScope().isSoftReferenced();
	}

	@Override
	default Object remove( Object key ) {
		return getThisScope().remove( key );
	}

	@Override
	default void putAll( Map<? extends Key, ? extends Object> m ) {
		getThisScope().putAll( m );
	}

	@Override
	default Collection<Object> values() {
		return getThisScope().values();
	}

	/**
	 * Get a value from the this scope cast as an IClassRunnable
	 *
	 * @param key The key to look up
	 *
	 * @return The value cast as an IClassRunnable
	 */
	default IClassRunnable getAsClassRunnable( Key key ) {
		return getThisScope().getAsClassRunnable( key );
	}

}
