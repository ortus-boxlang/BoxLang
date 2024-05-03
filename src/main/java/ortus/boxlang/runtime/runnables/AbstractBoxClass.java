package ortus.boxlang.runtime.runnables;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBoxClass implements IClassRunnable, IReferenceable, IType {

	public void pseudoConstructor( IBoxContext context ) {
		context.pushTemplate( this );
		try {
			// loop over properties and create variables.
			for ( var property : getProperties().values() ) {
				if ( getVariablesScope().get( property.name() ) == null ) {
					getVariablesScope().assign( context, property.name(), property.defaultValue() );
				}
			}
			// TODO: pre/post interceptor announcements here
			_pseudoConstructor( context );
		} finally {
			context.popTemplate();
		}
	}

	protected abstract void _pseudoConstructor( IBoxContext context );

	protected Boolean doCanOutput() {
		BoxSourceType sourceType = getSourceType();
		return BooleanCaster.cast(
		    getAnnotations().getOrDefault(
		        Key.output,
		        sourceType == BoxSourceType.CFSCRIPT || sourceType == BoxSourceType.CFTEMPLATE
		    )
		);
	}

	public String asString() {
		return "Class: " + getName().getName();
	}

	protected void doSetSuper( IClassRunnable _super ) {
		_super.setChild( this );
		// This runs before the psedu constructor and init, so the base class will override anything it declares
		// System.out.println( "Setting super class: " + _super.getName().getName() + " into " + this.getName().getName() );
		// System.out.println( "Setting super class variables: " + _super.getVariablesScope().asString() );
		getVariablesScope().addAll( _super.getVariablesScope().getWrapped() );
		getThisScope().addAll( _super.getThisScope().getWrapped() );

		// merge properties that don't already exist
		for ( var entry : _super.getProperties().entrySet() ) {
			if ( !getProperties().containsKey( entry.getKey() ) ) {
				getProperties().put( entry.getKey(), entry.getValue() );
			}
		}
		// merge getterLookup and setterLookup
		getGetterLookup().putAll( _super.getGetterLookup() );
		getSetterLookup().putAll( _super.getSetterLookup() );

		// merge annotations
		for ( var entry : _super.getAnnotations().entrySet() ) {
			Key key = entry.getKey();
			if ( !getAnnotations().containsKey( key ) && !key.equals( Key._EXTENDS ) && !key.equals( Key._IMPLEMEMTS ) ) {
				getAnnotations().put( key, entry.getValue() );
			}
		}
	}

	public IClassRunnable getBottomClass() {
		if ( getChild() != null ) {
			return getChild().getBottomClass();
		}
		return this;
	}

	public Object assign( IBoxContext context, Key key, Object value ) {
		getThisScope().assign( context, key, value );
		return value;
	}

	public Object dereference( IBoxContext context, Key key, Boolean safe ) {

		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		// TODO: implicit getters
		return getThisScope().dereference( context, key, safe );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		// TODO: component member methods?

		BaseScope scope = getThisScope();
		// we are a super class, so we reached here via super.method()
		if( getChild() != null ) {
			scope = getVariablesScope();
		}

		// Look for function in this
		Object value = scope.get( name );
		if ( value instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the CFC it's executing in
			    // because we've pushed the CFC onto the template stack in the function context.
			    context,
			    name,
			    positionalArguments,
			    this
			);

			functionContext.setThisClass( this );
			return function.invoke( functionContext );
		}

		if ( value != null ) {
			throw new BoxRuntimeException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
		}

		// Check for generated accessors
		Object hasAccessors = getAnnotations().get( Key.accessors );
		if ( hasAccessors != null && BooleanCaster.cast( hasAccessors ) ) {
			Property getterProperty = getGetterLookup().get( name );
			if ( getterProperty != null ) {
				return getBottomClass().getVariablesScope().dereference( context, getGetterLookup().get( name ).name(), safe );
			}
			Property setterProperty = getSetterLookup().get( name );
			// System.out.println( "setterProperty lookup: " + setterProperty );
			if ( setterProperty != null ) {
				Key thisName = setterProperty.name();
				if ( positionalArguments.length == 0 ) {
					throw new BoxRuntimeException( "Missing argument for setter '" + name.getName() + "'" );
				}
				getBottomClass().getVariablesScope().assign( context, thisName, positionalArguments[ 0 ] );
				return this;
			}
		}

		if ( getThisScope().get( Key.onMissingMethod ) != null ) {
			return dereferenceAndInvoke( context, Key.onMissingMethod, new Object[] { name.getName(), positionalArguments }, safe );
		}

		if ( !safe ) {
			throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
		}
		return null;
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		BaseScope scope = getThisScope();
		// we are a super class, so we reached here via super.method()
		if( getChild() != null ) {
			scope = getVariablesScope();
		}

		Object value = scope.get( name );
		if ( value instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the CFC it's executing in
			    // because we've pushed the CFC onto the template stack in the function context.
			    context,
			    name,
			    namedArguments,
			    this
			);

			functionContext.setThisClass( this );
			return function.invoke( functionContext );
		}

		if ( getSuper() != null && getSuper().getThisScope().get( name ) != null ) {
			return getSuper().dereferenceAndInvoke( context, name, namedArguments, safe );
		}

		if ( value != null ) {
			throw new BoxRuntimeException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
		}

		// Check for generated accessors
		Object hasAccessors = getAnnotations().get( Key.accessors );
		if ( hasAccessors != null && BooleanCaster.cast( hasAccessors ) ) {
			Property getterProperty = getGetterLookup().get( name );
			if ( getterProperty != null ) {
				return getBottomClass().getVariablesScope().dereference( context, getterProperty.name(), safe );
			}
			Property setterProperty = getSetterLookup().get( name );
			if ( setterProperty != null ) {
				Key thisName = setterProperty.name();
				if ( !namedArguments.containsKey( thisName ) ) {
					throw new BoxRuntimeException( "Missing argument for setter '" + name.getName() + "'" );
				}
				getBottomClass().getVariablesScope().assign( context, thisName, namedArguments.get( thisName ) );
				return this;
			}
		}

		if ( getThisScope().get( Key.onMissingMethod ) != null ) {
			Map<Key, Object> args = new HashMap<>();
			args.put( Key.missingMethodName, name.getName() );
			args.put( Key.missingMethodArguments, namedArguments );
			return dereferenceAndInvoke( context, Key.onMissingMethod, args, safe );
		}

		if ( !safe ) {
			throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
		}
		return null;
	}

	public IStruct getMetaData() {
		IStruct meta = new Struct( IStruct.TYPES.SORTED );
		meta.putIfAbsent( "hint", "" );
		meta.putIfAbsent( "output", canOutput() );

		// Assemble the metadata
		var functions = new ArrayList<>();
		// loop over target's variables scope and add metadata for each function
		for ( var entry : getThisScope().keySet() ) {
			var value = getThisScope().get( entry );
			if ( value instanceof Function fun ) {
				functions.add( fun.getMetaData() );
			}
		}
		meta.put( "name", getName().getName() );
		meta.put( "accessors", false );
		meta.put( "functions", Array.fromList( functions ) );
		// meta.put( "hashCode", hashCode() );
		var properties = new Array();
		// loop over properties list and add struct for each property
		for ( var entry : getProperties().entrySet() ) {
			var	property		= entry.getValue();
			var	propertyStruct	= new Struct( IStruct.TYPES.LINKED );
			propertyStruct.put( "name", property.name().getName() );
			propertyStruct.put( "type", property.type() );
			propertyStruct.put( "default", property.defaultValue() );
			if ( property.documentation() != null ) {
				propertyStruct.putAll( property.documentation() );
			}
			if ( property.annotations() != null ) {
				propertyStruct.putAll( property.annotations() );
			}
			properties.add( propertyStruct );
		}
		meta.put( "properties", properties );
		meta.put( "type", "Component" );
		meta.put( "name", getName().getName() );
		meta.put( "fullname", getName().getName() );
		meta.put( "path", getRunnablePath().toString() );
		meta.put( "persisent", false );

		if ( getDocumentation() != null ) {
			meta.putAll( getDocumentation() );
		}
		if ( getAnnotations() != null ) {
			meta.putAll( getAnnotations() );
		}
		if ( getSuper() != null ) {
			meta.put( "extends", getSuper().getMetaData() );
		}
		return meta;
	}
}
