package ortus.boxlang.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class JDITools {

	public static Method findFirstMethodByName( ReferenceType type, String methodName ) {
		for ( Method method : type.allMethods() ) {
			if ( method.name().compareTo( methodName ) == 0 ) {
				return method;
			}
		}

		return null;
	}

	public static Object findVariableyName( StackFrame stackFrame, String name ) {
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

	public static Object findPropertyByName( ObjectReference object, String name ) {
		for ( Field field : object.referenceType().allFields() ) {
			if ( field.name().compareTo( name ) == 0 ) {
				return object.getValue( field );
			}
		}

		return null;
	}

	public static Value invoke( ThreadReference thread, ObjectReference obj, String methodName, List<Value> args ) {
		try {
			return obj.invokeMethod( thread, findFirstMethodByName( obj.referenceType(), methodName ), args, 0 );
		} catch ( InvalidTypeException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( ClassNotLoadedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IncompatibleThreadStateException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( InvocationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static Map<String, Object> getValuesFromScope( ObjectReference scope ) {
		Map<String, Object>	values	= new HashMap<String, Object>();
		ObjectReference		wrapped	= ( ObjectReference ) findPropertyByName( scope, "wrapped" );

		return values;
	}

	public static Map<String, Object> convertObjectReferenceToMap( ObjectReference obj ) {
		Map<String, Object>	values	= new HashMap<String, Object>();
		Map<String, Object>	fields	= new HashMap<String, Object>();

		values.put( "_class", obj.referenceType().genericSignature() );
		values.put( "fields", fields );

		for ( Field field : obj.referenceType().allFields() ) {
			Map<String, Object> f = new HashMap<String, Object>();
			f.put( "name", field.name() );

			Value val = obj.getValue( field );

			if ( val instanceof IntegerValue iobj ) {
				f.put( "value", iobj.value() );
			} else if ( val instanceof BooleanValue boolobj ) {
				f.put( "value", boolobj.value() );
			} else if ( val instanceof StringReference strobj ) {
				f.put( "value", strobj.value() );
			} else if ( val instanceof ObjectReference subObj ) {
				f.put( "value", convertObjectReferenceToMap( subObj ) );
			}

			fields.put( field.name(), f );

		}

		return values;
	}

	public static Map<String, String> convertScopeToMap( VirtualMachine vm, ThreadReference thread, ObjectReference scope ) {
		Value				returnValue	= invoke( thread, scope, "getKeysAsStrings", new ArrayList<Value>() );
		List<String>		keys		= getStringsFromListReference( vm, thread, ( ObjectReference ) returnValue );
		Map<String, String>	values		= new HashMap<String, String>();

		for ( String key : keys ) {
			List<Value> args = new ArrayList<Value>();
			args.add( vm.mirrorOf( key ) );
			values.put( key, ( invoke( thread, scope, "get", args ) ).toString() );
		}

		return values;
	}

	public static List<String> getStringsFromListReference( VirtualMachine vm, ThreadReference thread, ObjectReference list ) {
		int				size	= ( ( IntegerValue ) invoke( thread, list, "size", new ArrayList<Value>() ) ).intValue();
		List<String>	keys	= new ArrayList<String>();

		List<Value>		args	= new ArrayList<Value>();
		args.add( vm.mirrorOf( 0 ) );
		for ( int i = 0; i < size; i++ ) {
			args.set( 0, vm.mirrorOf( i ) );
			keys.add( ( ( StringReference ) invoke( thread, list, "get", args ) ).value() );
		}

		return keys;
	}
}
