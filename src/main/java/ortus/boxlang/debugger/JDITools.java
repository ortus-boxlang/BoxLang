package ortus.boxlang.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

import ortus.boxlang.debugger.types.Variable;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;

public class JDITools {

	private static Map<Long, VariableReference>	values;
	private static long							variableReferenceCount	= 1;

	private static enum VariableType {
		ARRAY,
		STRUCT
	}

	private static record VariableReference( long id, VariableType type, Value value ) {

	}

	static {
		values = new HashMap<Long, VariableReference>();
	}

	public static boolean hasSeen( long variableReference ) {
		return values.containsKey( variableReference );
	}

	public static Value getSeenValue( long variableReference ) {
		// TODO check to make sure the value hasn't been garbage collected - if it has remove it from the map and return null
		return values.get( variableReference ).value;
	}

	public static List<Variable> gerVariablesFromStruct( ObjectReference struct ) {
		var	wrapped	= JDITools.getObjRefByName( struct, "wrapped" );
		var	table	= ( ArrayReference ) getFieldValueByName( wrapped, "table" );

		// TODO remove functions
		return table.getValues().stream()
		    .filter( item -> item != null )
		    .map( item -> {
			    ObjectReference obj = ( ObjectReference ) item;

			    Variable	var	= getVariableFromValue( getNameFromKeyValue( obj ), getFieldValueByName( obj, "val" ) );

			    return var;
		    } ).toList();
	}

	public static String getNameFromKeyValue( ObjectReference item ) {
		return getStringFromValue( getFieldValueByName( ( ObjectReference ) getFieldValueByName( item, "key" ), "originalValue" ) );
	}

	public static Variable getVariableFromValue( String name, Value val ) {
		Variable var = new Variable();
		var.value	= "";
		var.type	= "null";

		if ( val instanceof StringReference stringRef ) {
			var.value	= "\"" + stringRef.value() + "\"";
			var.type	= "String";
		} else if ( val instanceof IntegerValue integerVal ) {
			var.value	= Integer.toString( integerVal.intValue() );
			var.type	= "numeric";
		} else if ( val instanceof DoubleValue doubleVal ) {
			var.value	= StringCaster.cast( doubleVal.doubleValue() );
			var.type	= "numeric";
		} else if ( val.type().name().compareToIgnoreCase( "java.lang.Boolean" ) == 0 ) {
			var.value	= StringCaster.cast( ( ( BooleanValue ) getFieldValueByName( ( ObjectReference ) val, "value" ) ).booleanValue() );
			var.type	= "boolean";
		} else if ( val.type().name().compareToIgnoreCase( "java.lang.double" ) == 0 ) {
			var.value	= StringCaster.cast( ( ( DoubleValue ) getFieldValueByName( ( ObjectReference ) val, "value" ) ).doubleValue() );
			var.type	= "numeric";
		} else if ( val.type().name().compareToIgnoreCase( "ortus.boxlang.runtime.types.array" ) == 0 ) {
			var = getVariableFromArray( ( ObjectReference ) val );
		} else if ( val.type().name().compareToIgnoreCase( "ortus.boxlang.runtime.types.struct" ) == 0 ) {
			var = getVariableFromStruct( ( ObjectReference ) val );
		} else if ( val instanceof ObjectReference
		    && val.type() instanceof ClassType ctype
		    && ctype.superclass().name().compareToIgnoreCase( "ortus.boxlang.runtime.types.Closure" ) == 0 ) {
			var.type	= "closure";
			var.value	= "closure";
		} else if ( val instanceof ObjectReference
		    && val.type() instanceof ClassType ctype
		    && ctype.superclass().name().compareToIgnoreCase( "ortus.boxlang.runtime.types.Lambda" ) == 0 ) {
			var.type	= "lambda";
			var.value	= "lambda";
		} else if ( val instanceof ObjectReference
		    && val.type() instanceof ClassType ctype
		    && ctype.superclass().name().compareToIgnoreCase( "ortus.boxlang.runtime.types.UDF" ) == 0 ) {
			var.type	= "function";
			var.value	= "() => {}";
		} else if ( val != null ) {
			var.value = "Unimplemented type - " + val.getClass().getName() + " " + val.type().name();
		}

		var.name = name;

		return var;
	}

	public static List<Variable> getVariablesFromSeen( long variableReference ) {
		VariableReference ref = values.get( variableReference );

		switch ( ref.type ) {
			case STRUCT :
				return gerVariablesFromStruct( ( ObjectReference ) ref.value );
			case ARRAY :
				return gerVariablesFromArray( ( ObjectReference ) ref.value );
		}

		return new ArrayList<Variable>();
	}

	private static List<Variable> gerVariablesFromArray( ObjectReference value ) {
		var				wrapped	= JDITools.getObjRefByName( value, "wrapped" );
		var				list	= ( ObjectReference ) getFieldValueByName( wrapped, "list" );
		var				table	= ( ArrayReference ) getFieldValueByName( list, "elementData" );
		List<Variable>	vars	= new ArrayList<Variable>();

		for ( int i = 0; i < table.length(); i++ ) {
			vars.add( getVariableFromValue( Integer.toString( i ), table.getValue( i ) ) );
		}

		return vars;
	}

	private static Variable getVariableFromArray( ObjectReference val ) {
		Variable	var	= new Variable();
		long		id	= variableReferenceCount++;

		var.type				= "array";
		var.value				= "[]";
		var.variablesReference	= ( int ) id;
		values.put( id, new VariableReference( id, VariableType.ARRAY, val ) );

		return var;
	}

	private static Variable getVariableFromStruct( ObjectReference val ) {
		Variable	var	= new Variable();
		long		id	= variableReferenceCount++;

		var.type				= "Struct";
		var.value				= "{}";
		var.variablesReference	= ( int ) id;
		values.put( id, new VariableReference( id, VariableType.STRUCT, val ) );

		return var;
	}

	public static ObjectReference getObjRefByName( ObjectReference object, String name ) {
		for ( Field field : object.referenceType().allFields() ) {
			if ( field.name().compareTo( name ) == 0 ) {
				return ( ObjectReference ) object.getValue( field );
			}
		}

		return null;
	}

	public static Value getFieldValueByName( ObjectReference object, String name ) {
		for ( Field field : object.referenceType().allFields() ) {
			if ( field.name().compareTo( name ) == 0 ) {
				return object.getValue( field );
			}
		}

		return null;
	}

	public static String getStringFromValue( Value value ) {
		if ( value == null ) {
			return null;
		} else if ( value instanceof IntegerValue iv ) {
			return Integer.toString( iv.intValue() );
		} else if ( value instanceof StringReference sv ) {
			return sv.value();
		} else if ( value instanceof ObjectReference objref ) {
			return objref.type().name();
		}

		return null;
	}

	public static Method findFirstMethodByName( ReferenceType type, String methodName ) {
		for ( Method method : type.allMethods() ) {
			if ( method.name().compareTo( methodName ) == 0 ) {
				return method;
			}
		}

		return null;
	}

	public static Value findVariableyName( StackFrame stackFrame, String name ) {
		Map<LocalVariable, Value> visibleVariables;
		try {
			visibleVariables = stackFrame.getValues( stackFrame.visibleVariables() );

			for ( Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet() ) {
				if ( entry.getKey().name().compareTo( name ) == 0 ) {
					return entry.getValue();
				}
			}

		} catch ( AbsentInformationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	public static Value findPropertyByName( ObjectReference object, String name ) {
		for ( Field field : object.referenceType().allFields() ) {
			if ( field.name().compareTo( name ) == 0 ) {
				return object.getValue( field );
			}
		}

		return null;
	}
}
