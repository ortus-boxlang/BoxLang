package ortus.boxlang.debugger;

import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

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
}
