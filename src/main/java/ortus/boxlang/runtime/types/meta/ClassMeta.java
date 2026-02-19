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
package ortus.boxlang.runtime.types.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableArray;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This class represents generic BoxLang metadata for a an object which has no object-specifc properties
 */
public class ClassMeta extends BoxMeta<IClassRunnable> {

	/**
	 * The target object this metadata is for
	 */
	private IClassRunnable		target;

	/**
	 * The Java class of the target
	 */
	public Class<?>				$class;

	/**
	 * The assembled metadata
	 */
	public IStruct				meta;

	/**
	 * Constants
	 */
	private static final String	CLASS_TYPE	= "Class";

	/**
	 * Constructor
	 *
	 * @param target The target object this metadata is for
	 */
	public ClassMeta( IClassRunnable target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();

		this.meta	= generateMeta(
		    target.getClass(),
		    target.bxGetName(),
		    target.getSourceType(),
		    target.getRunnablePath(),
		    target.getBoxSuperClass(),
		    target.getInterfaces(),
		    target.getAbstractMethods(),
		    target.getUDFs(),
		    target.getAnnotations(),
		    target.getDocumentation(),
		    target.getProperties(),
		    target.getStaticScope()
		);
	}

	/**
	 * Generate metadata for a class
	 *
	 * @param targetClass     The class reference (for future use)
	 * @param name            The class name key
	 * @param sourceType      The source type of the class
	 * @param runnablePath    The path to the class file
	 * @param superClass      The super class, if any
	 * @param interfaces      The list of interfaces implemented
	 * @param abstractMethods The abstract methods defined
	 * @param udfs            The UDFs defined in the class
	 * @param annotations     The class annotations
	 * @param documentation   The class documentation
	 * @param properties      The class properties
	 * @param staticScope     The static scope
	 *
	 * @return The metadata as a struct
	 */
	public static IStruct generateMeta(
	    Class<? extends IClassRunnable> targetClass,
	    Key name,
	    BoxSourceType sourceType,
	    ResolvedFilePath runnablePath,
	    DynamicObject superClass,
	    List<BoxInterface> interfaces,
	    Map<Key, AbstractFunction> abstractMethods,
	    Map<Key, ? extends Function> udfs,
	    IStruct annotations,
	    IStruct documentation,
	    Map<Key, ortus.boxlang.runtime.types.Property> properties,
	    StaticScope staticScope ) {

		// Assemble the metadata
		var mdFunctions = new ArrayList<Object>();

		// Micro-optimize list allocation
		mdFunctions.ensureCapacity( udfs.size() );
		// Iterate and add UDF metadata
		for ( var fun : udfs.values() ) {
			mdFunctions.add( ( ( FunctionMeta ) fun.getBoxMeta() ).meta );
		}

		// Add all static methods as well, if any
		if ( staticScope != null ) {
			// iterate over the entrySet, each value that's a Function and is declared in this class, add it
			for ( var entry : staticScope.entrySet() ) {
				if ( entry.getValue() instanceof Function castedFunction ) {
					mdFunctions.add( ( ( FunctionMeta ) castedFunction.getBoxMeta() ).meta );
				}
			}
		}

		// Add all abstract methods as well, if any
		for ( var entry : abstractMethods.keySet() ) {
			AbstractFunction value = abstractMethods.get( entry );
			mdFunctions.add( ( ( FunctionMeta ) value.getBoxMeta() ).meta );
		}

		// Process Properties
		var mdProperties = new ArrayList<Object>();

		// Micro-optimize list allocation
		mdProperties.ensureCapacity( properties.size() );
		// Iterate and add
		for ( var entry : properties.entrySet() ) {
			if ( entry.getValue().declaringClass() == targetClass ) {
				mdProperties.add( Struct.ofNonConcurrent(
				    Key._NAME, entry.getKey().getName(),
				    Key.nameAsKey, entry.getKey(),
				    Key.type, entry.getValue().type(),
				    Key.defaultValue, entry.getValue().getDefaultValueForMeta(),
				    Key.annotations, new Struct( entry.getValue().annotations() ),
				    Key.documentation, new Struct( entry.getValue().documentation() )
				) );
			}
		}

		// Build the meta struct
		var		fullName	= name.getName();
		IStruct	meta		= Struct.ofNonConcurrent(
		    Key._NAME, fullName,
		    Key.nameAsKey, name,
		    Key.simpleName, fullName.substring( fullName.lastIndexOf( '.' ) + 1 ),
		    Key.output, BoxClassSupport.canOutput( annotations, name.getName() ),
		    Key.documentation, new Struct( documentation ),
		    Key.annotations, new Struct( annotations ),
		    Key._EXTENDS, superClass != null ? superClass.invokeStatic( BoxRuntime.getInstance().getRuntimeContext(), "getMetaStatic" ) : Struct.EMPTY,
		    Key.functions, UnmodifiableArray.fromList( mdFunctions ),
		    // Key._HASHCODE, targetClass.hashCode(),
		    Key.properties, UnmodifiableArray.fromList( mdProperties ),
		    Key.type, CLASS_TYPE,
		    Key.fullname, fullName,
		    Key.path, runnablePath.absolutePath().toString()
		);

		// Add interfaces if any
		meta.put(
		    Key._IMPLEMENTS,
		    interfaces.stream()
		        .collect(
		            Struct::new,
		            ( struct, iface ) -> struct.put( iface.getName(), iface.getBoxMeta().getMeta() ),
		            Struct::putAll
		        )
		);
		return meta;
	}

	/**
	 * So we can get a pretty print of the metadata
	 */
	public String toString() {
		return Struct.of(
		    "meta", this.meta.asString(),
		    "$class", this.$class.getName()
		).asString();
	}

	/**
	 * Get target object this metadata is for
	 */
	public IClassRunnable getTarget() {
		return target;
	}

	/**
	 * Direct invoke a Java method on the target bypassing the referencable methods
	 */
	public Object invokeTargetMethod( IBoxContext context, String methodName, Object[] args ) {
		return DynamicObject.of( target ).invoke( context, methodName, args );
	}

	/**
	 * Direct invoke a static Java method on the target's class bypassing the referencable methods
	 */
	public Object invokeTargetMethodStatic( IBoxContext context, String methodName, Object[] args ) {
		return DynamicObject.of( target ).invokeStatic( context, methodName, args );
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return meta;
	}

	/**
	 * Get the variables scope directly
	 *
	 * @return The variables scope
	 */
	public IScope getVariablesScope() {
		return target.getVariablesScope();
	}

	/**
	 * Get the this scope directly
	 *
	 * @return The this scope
	 */
	public IScope getThisScope() {
		return target.getThisScope();
	}

	/**
	 * Get the static scope directly
	 *
	 * @return The static scope
	 */
	public IScope getStaticScope() {
		return target.getStaticScope();
	}

}
