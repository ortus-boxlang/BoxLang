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

public class JDITools {

	private static Map<Long, WrappedValue>	values;
	private static Map<Long, Long>			mirrorToId;
	private static long						variableReferenceCount	= 1;

	static {
		values		= new HashMap<Long, WrappedValue>();
		mirrorToId	= new HashMap<Long, Long>();
	}

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

	public static record WrappedValue( long id, ThreadReference thread, Value value ) {

		private Method findFirstMethodByName( ReferenceType type, String methodName ) {
			for ( Method method : type.allMethods() ) {
				if ( method.name().compareTo( methodName ) == 0 ) {
					return method;
				}
			}

			return null;
		}

		public boolean isOfType( String type ) {

			return value.type().name().compareToIgnoreCase( type ) == 0;
		}

		public boolean isStruct() {
			if ( ! ( value.type() instanceof ClassType ) ) {
				return false;
			}

			return ( ( ClassType ) value.type() ).allInterfaces()
			    .stream().anyMatch( ( i ) -> i.name().compareToIgnoreCase( "ortus.boxlang.runtime.types.IStruct" ) == 0 );
		}

		public boolean hasSuperClass( String type ) {
			return value instanceof ObjectReference
			    && value.type() instanceof ClassType ctype
			    && ctype.superclass().name().compareToIgnoreCase( type ) == 0;
		}

		public WrappedValue property( String name ) {
			return wrap( thread, findPropertyByName( ( ObjectReference ) value, name ) );
		}

		public OptionalLong uniqueID() {
			if ( value instanceof ObjectReference objRef ) {
				return OptionalLong.of( objRef.uniqueID() );
			} else if ( value instanceof ArrayReference arrRef ) {
				return OptionalLong.of( arrRef.uniqueID() );
			}

			return OptionalLong.empty();
		}

		public BooleanValue asBooleanValue() {
			return ( BooleanValue ) value;
		}

		public DoubleValue asDoubleValue() {
			return ( DoubleValue ) value;
		}

		public ArrayReference asArrayReference() {
			return ( ArrayReference ) value;
		}

		public StringReference asStringReference() {
			return ( StringReference ) value;
		}

		public ObjectReference asObjectReference() {
			return ( ObjectReference ) value;
		}

		public WrappedValue invoke( String methodName ) {
			return invoke( methodName, new ArrayList<Value>() );
		}

		public VirtualMachine vm() {
			return thread.virtualMachine();
		}

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

	public static enum BoxLangType {
		ARRAY,
		STRUCT,
		UDF,
		CLOSURE,
		LAMBDA,
		UNKNOWN
	};

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

		return BoxLangType.UNKNOWN;
	}

	public static boolean doesExtend( ReferenceType type, String superType ) {
		return type instanceof ClassType ctype
		    && ctype.superclass().name().compareToIgnoreCase( superType ) == 0;
	}

	public static boolean hasSeen( long variableReference ) {
		return values.containsKey( variableReference );
	}

	public static Value getSeenValue( long variableReference ) {
		// TODO check to make sure the value hasn't been garbage collected - if it has remove it from the map and return null
		return values.get( variableReference ).value;
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
