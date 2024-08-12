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
	public Key getName();

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

	public void _pseudoConstructor( IBoxContext context );

	/**
	 * Get the combined metadata for this class and all it's functions
	 * This follows the format of Lucee and Adobe's "combined" metadata
	 * TODO: Move this to compat module
	 *
	 * @return The metadata as a struct
	 */
	public IStruct getMetaData();

	// Duplicate from IType
	public BoxMeta getBoxMeta();

	public BoxMeta _getbx();

	public void _setbx( BoxMeta bx );

	public void registerInterface( BoxInterface _interface );

	public List<BoxInterface> getInterfaces();

	/**
	 * A helper to look at the "output" annotation, caching the result
	 * 
	 * @return Whether the function can output
	 */
	public Boolean canOutput();

	public Boolean getCanOutput();

	public void setCanOutput( Boolean canOutput );

	public Boolean canInvokeImplicitAccessor( IBoxContext context );

	public Boolean getCanInvokeImplicitAccessor();

	public void setCanInvokeImplicitAccessor( Boolean canInvokeImplicitAccessor );

	/**
	 * Get the super class. Null if there is none
	 */
	public IClassRunnable getSuper();

	public boolean isJavaExtends();

	public MethodHandle lookupPrivateMethod( Method method );

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

	public Map<Key, Property> getGetterLookup();

	public Map<Key, Property> getSetterLookup();

	public Map<Key, AbstractFunction> getAbstractMethods();

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
	default Map<Key, Object> getWrapped() {
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

	default IClassRunnable getAsClassRunnable( Key key ) {
		return getThisScope().getAsClassRunnable( key );
	}

}
