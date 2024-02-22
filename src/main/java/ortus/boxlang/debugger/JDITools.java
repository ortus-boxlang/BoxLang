package ortus.boxlang.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
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

import ortus.boxlang.debugger.types.Variable;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.types.BoxLangType;

public class JDITools {

	private static Map<Long, WrappedValue>	values;
	private static Map<Long, Long>			mirrorToId;
	private static long						variableReferenceCount	= 1;

	static {
		values		= new HashMap<Long, WrappedValue>();
		mirrorToId	= new HashMap<Long, Long>();
	}

	/**
	 * Produce a WrappedValue for a given thread and value.
	 * 
	 * @param thread A ThreadReference from a debug VM
	 * @param value  A Value from a debug VM
	 * 
	 * @return
	 */
	public static WrappedValue wrap( ThreadReference thread, Value value ) {
		long			id		= variableReferenceCount++;
		WrappedValue	wrapped	= new WrappedValue( id, thread, value );
		values.put( id, wrapped );

		wrapped.uniqueID().ifPresent( ( unique ) -> mirrorToId.put( unique, id ) );

		return wrapped;
	}

	public static Method findMethodByNameAndArgs( ClassType classType, String name, List<String> args ) {
		for ( Method method : classType.allMethods() ) {
			if ( method.name().compareToIgnoreCase( name ) != 0 ) {
				continue;
			}

			List<String> argumentNames = method.argumentTypeNames();

			if ( argumentNames.size() != args.size() ) {
				continue;
			}

			boolean matches = true;

			for ( int i = 0; i < argumentNames.size(); i++ ) {
				if ( argumentNames.get( i ).compareToIgnoreCase( args.get( i ) ) != 0 ) {
					matches = false;
					break;
				}
			}

			if ( matches ) {
				return method;
			}
		}

		return null;
	}

	/**
	 * This wraps up a Value from the com.sun.JDI package to provide a more convenient interface for accessing the state of objects in the VM.
	 * 
	 * Using methods that access the underlying VM object ( invoke, invokeByNameAndArgs, property, etc... ) also return a WrappedValue. The returned
	 * WrappedValue is created using the same ThreadReference as the parent WrappedValue and contains the value recovered from the debugee VM.
	 * 
	 * WrappedValue is also used for tracking variables that have been seen within a debug session so that they can be accesed by ID when referenced by a
	 * Debug Adapter Protocol request.
	 * 
	 * Do not use this directly but get an isntance of it using JDITools.wrap( threadReference, value );
	 */
	public static record WrappedValue( long id, ThreadReference thread, Value value ) {

		private Method findFirstMethodByName( ReferenceType type, String methodName ) {
			for ( Method method : type.allMethods() ) {
				if ( method.name().compareTo( methodName ) == 0 ) {
					return method;
				}
			}

			return null;
		}

		/**
		 * Check if the underlying Value is an instance of a specific class.
		 * 
		 * @param type The Class type to check.
		 * 
		 * @return
		 */
		public boolean isOfType( String type ) {
			return value.type().name().compareToIgnoreCase( type ) == 0;
		}

		/**
		 * Checks if the underlying value implements ortus.boxlang.runtime.types.IStruct
		 * 
		 * @return boolean
		 */
		public boolean isStruct() {
			if ( ! ( value.type() instanceof ClassType ) ) {
				return false;
			}

			return ( ( ClassType ) value.type() ).allInterfaces()
			    .stream().anyMatch( ( i ) -> i.name().compareToIgnoreCase( "ortus.boxlang.runtime.types.IStruct" ) == 0 );
		}

		/**
		 * Check to see if this class immediately extends a particular class.
		 * 
		 * @param type The class FQN to check
		 * 
		 * @return boolean
		 */
		public boolean hasSuperClass( String type ) {
			return value instanceof ObjectReference
			    && value.type() instanceof ClassType ctype
			    && ctype.superclass().name().compareToIgnoreCase( type ) == 0;
		}

		/**
		 * Get the value of a property on the value. This can access any property defined in this object's class or any of its parent classes. It can also
		 * access any property regardless of visibility (protected, private, public, etc...).
		 * 
		 * @param name The name of the property to retrieve
		 * 
		 * @return WrappedValue containing the property value
		 */
		public WrappedValue property( String name ) {
			return wrap( thread, findPropertyByName( ( ObjectReference ) value, name ) );
		}

		/**
		 * Attempts to get the uniqueID of the contained value provided by the debugee VM. Only complex types are assigned uniqueIDs by the debuggee VM.
		 * 
		 * @return
		 */
		public OptionalLong uniqueID() {
			if ( value instanceof ObjectReference objRef ) {
				return OptionalLong.of( objRef.uniqueID() );
			} else if ( value instanceof ArrayReference arrRef ) {
				return OptionalLong.of( arrRef.uniqueID() );
			}

			return OptionalLong.empty();
		}

		/**
		 * Convenience method for casting to a BooleanValue.
		 * 
		 * @return
		 */
		public BooleanValue asBooleanValue() {
			return ( BooleanValue ) value;
		}

		/**
		 * Convenience method for casting to a DoubleValue.
		 * 
		 * @return
		 */
		public DoubleValue asDoubleValue() {
			return ( DoubleValue ) value;
		}

		/**
		 * Convenience method for casting to an ArrayReference.
		 * 
		 * @return
		 */
		public ArrayReference asArrayReference() {
			return ( ArrayReference ) value;
		}

		/**
		 * Convenience method for casting to a StringReference.
		 * 
		 * @return
		 */
		public StringReference asStringReference() {
			return ( StringReference ) value;
		}

		/**
		 * Convenience method for casting to an ObjectReference.
		 * 
		 * @return
		 */
		public ObjectReference asObjectReference() {
			return ( ObjectReference ) value;
		}

		/**
		 * Invoke a method on the contained value in the debugee VM. This is a shortcut for invoking a method with no args.
		 * 
		 * See
		 * https://docs.oracle.com/en/java/javase/17/docs/api/jdk.jdi/com/sun/jdi/ObjectReference.html#invokeMethod(com.sun.jdi.ThreadReference,com.sun.jdi.Method,java.util.List,int)
		 * for more information about invoking methods within a debugee VM.
		 * 
		 * @param methodName The method to invoke
		 * 
		 * @return
		 */
		public WrappedValue invoke( String methodName ) {
			return invoke( methodName, new ArrayList<Value>() );
		}

		/**
		 * Access the VM being debugged.
		 * 
		 * @return
		 */
		public VirtualMachine vm() {
			return thread.virtualMachine();
		}

		/**
		 * Invoke a method on the contained Value within the debugee VM. Uses the provided argTypes to find the correct method signature to call.
		 * 
		 * See
		 * https://docs.oracle.com/en/java/javase/17/docs/api/jdk.jdi/com/sun/jdi/ObjectReference.html#invokeMethod(com.sun.jdi.ThreadReference,com.sun.jdi.Method,java.util.List,int)
		 * for more information about invoking methods within a debugee VM.
		 * 
		 * @param methodName The name of the method to invoke
		 * @param argTypes   The argument types to match the method signature
		 * @param args       The arguments to pass into the method invocation
		 * 
		 * @return A WrappedValue containing the return value of the method invocation.
		 */
		public WrappedValue invokeByNameAndArgs( String methodName, List<String> argTypes, List<Value> args ) {
			try {
				Value val = ( ( ObjectReference ) this.value )
				    .invokeMethod(
				        thread,
				        JDITools.findMethodByNameAndArgs( ( ClassType ) ( ( ObjectReference ) value ).referenceType(), methodName, argTypes ),
				        args,
				        ObjectReference.INVOKE_SINGLE_THREADED
				    );

				return wrap( thread, val );
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

		public WrappedValue invoke( String methodName, List<Value> args ) {
			try {
				Value val = ( ( ObjectReference ) this.value )
				    .invokeMethod(
				        thread,
				        findFirstMethodByName( ( ( ObjectReference ) value ).referenceType(), methodName ),
				        args,
				        ObjectReference.INVOKE_SINGLE_THREADED
				    );

				return wrap( thread, val );
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

	public static BoxLangType determineBoxLangType( ReferenceType type ) {
		if ( doesExtend( type, "ortus.boxlang.runtime.types.struct" ) ) {
			return BoxLangType.STRUCT;
		} else if ( doesExtend( type, "ortus.boxlang.runtime.types.array" ) ) {
			return BoxLangType.ARRAY;
		} else if ( doesExtend( type, "ortus.boxlang.runtime.types.udf" ) ) {
			return BoxLangType.UDF;
		} else if ( doesExtend( type, "ortus.boxlang.runtime.types.closure" ) ) {
			return BoxLangType.CLOSURE;
		} else if ( doesExtend( type, "ortus.boxlang.runtime.types.lambda" ) ) {
			return BoxLangType.LAMBDA;
		}

		return null;
	}

	public static boolean doesExtend( ReferenceType type, String superType ) {
		return type instanceof ClassType ctype
		    && ctype.superclass().name().compareToIgnoreCase( superType ) == 0;
	}

	public static boolean hasSeen( long variableReference ) {
		return values.containsKey( variableReference );
	}

	public static List<Variable> getVariablesFromSeen( long variableReference ) {
		WrappedValue wrapped = values.get( variableReference );

		if ( wrapped.isOfType( "ortus.boxlang.runtime.types.array" ) ) {
			return gerVariablesFromArray( wrapped );
		} else if ( wrapped.isStruct() ) {
			return gerVariablesFromStruct( wrapped );
		}

		return new ArrayList<Variable>();
	}

	public static WrappedValue findVariableyName( StackFrame stackFrame, String name ) {
		Map<LocalVariable, Value> visibleVariables;
		try {
			visibleVariables = stackFrame.getValues( stackFrame.visibleVariables() );

			for ( Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet() ) {
				if ( entry.getKey().name().compareTo( name ) == 0 ) {
					return wrap( stackFrame.thread(), entry.getValue() );
				}
			}

		} catch ( AbsentInformationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	public static List<Variable> gerVariablesFromStruct( WrappedValue struct ) {
		List<Value> entries = struct.invoke( "entrySet" ).invoke( "toArray" ).asArrayReference().getValues();

		return entries.stream()
		    .filter( entry -> entry != null )
		    .map( entry -> wrap( struct.thread, entry ) )
		    .map( wrappedEntry -> {

			    String	keyName	= wrappedEntry.invoke( "getKey" ).invoke( "getOriginalValue" ).asStringReference().value();

			    Variable var	= getVariable( keyName, wrappedEntry.invoke( "getValue" ) );

			    return var;
		    } ).toList();
	}

	private static Variable getVariable( String name, WrappedValue wrapped ) {
		Value		val	= wrapped.value;
		Variable	var	= new Variable();
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
		} else if ( wrapped.isOfType( "java.lang.Boolean" ) ) {
			var.value	= StringCaster.cast( wrapped.property( "value" ).asBooleanValue().booleanValue() );
			var.type	= "boolean";
		} else if ( wrapped.isOfType( "java.lang.double" ) ) {
			var.value	= StringCaster.cast( wrapped.property( "value" ).asDoubleValue().doubleValue() );
			var.type	= "numeric";
		} else if ( wrapped.isOfType( "ortus.boxlang.runtime.types.array" ) ) {
			var.type				= "array";
			var.value				= "[]";
			var.variablesReference	= ( int ) wrapped.id;
		} else if ( wrapped.isStruct() ) {
			var.type				= "Struct";
			var.value				= "{}";
			var.variablesReference	= ( int ) wrapped.id;
		} else if ( wrapped.hasSuperClass( "ortus.boxlang.runtime.types.Closure" ) ) {
			var.type	= "closure";
			var.value	= "closure";
		} else if ( wrapped.hasSuperClass( "ortus.boxlang.runtime.types.Lambda" ) ) {
			var.type	= "lambda";
			var.value	= "lambda";
		} else if ( wrapped.hasSuperClass( "ortus.boxlang.runtime.types.UDF" ) ) {
			var.type	= "function";
			var.value	= "() => {}";
		} else if ( val != null ) {
			var.value = "Unimplemented type - " + val.getClass().getName() + " " + val.type().name();
		}

		var.name = name;

		return var;
	}

	private static List<Variable> gerVariablesFromArray( WrappedValue wrapped ) {
		ArrayReference	table	= wrapped.invoke( "toArray" ).asArrayReference();
		List<Variable>	vars	= new ArrayList<Variable>();

		for ( int i = 0; i < table.length(); i++ ) {
			vars.add( getVariable( Integer.toString( i ), wrap( wrapped.thread, table.getValue( i ) ) ) );
		}

		return vars;
	}

	private static Value findPropertyByName( ObjectReference object, String name ) {
		for ( Field field : object.referenceType().allFields() ) {
			if ( field.name().compareTo( name ) == 0 ) {
				return object.getValue( field );
			}
		}

		return null;
	}
}
