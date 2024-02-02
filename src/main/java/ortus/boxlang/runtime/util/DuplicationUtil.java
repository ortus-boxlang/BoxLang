package ortus.boxlang.runtime.util;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.SerializationUtils;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class DuplicationUtil {

	public static Object duplicate( Object target, Boolean deep ) {
		if ( ClassUtils.isPrimitiveOrWrapper( target.getClass() ) ) {
			return target;
		} else if ( target instanceof Struct ) {
			return duplicateStruct( StructCaster.cast( target ), deep );
		} else if ( target instanceof Array ) {
			return duplicateArray( ArrayCaster.cast( target ), deep );
		} else if ( target instanceof DateTime ) {
			return DateTimeCaster.cast( target ).clone();
		} else if ( target instanceof Function ) {
			// functions should never be duplicated
			return target;
		} else if ( target instanceof Serializable ) {
			// Once we get here duplication is deep but very slow, but many java classes like ArrayList and all HashMaps implement this class
			// If a new type is created, add a custom routine above for duplication
			return SerializationUtils.clone( ( Serializable ) target );
		} else {
			throw new BoxRuntimeException(
			    String.format(
			        "Duplication was requested on the class [%s] but we don't know how to proceed",
			        target.getClass().getSimpleName()
			    )
			);
		}
	}

	public static Struct duplicateStruct( IStruct target, Boolean deep ) {
		var entries = target.entrySet().stream();

		if ( target.getType().equals( Struct.TYPES.LINKED ) ) {
			return new Struct(
			    target.getType(),
			    entries.collect(
			        Collectors.toMap(
			            entry -> entry.getKey(),
			            entry -> {
				            Object val = entry.getValue();
				            return deep && val instanceof IStruct ? duplicateStruct( StructCaster.cast( val ), deep ) : val;
			            },
			            ( v1, v2 ) -> {
				            throw new BoxRuntimeException( "An exception occurred while duplicating the linked HashMap" );
			            },
			            LinkedHashMap<Key, Object>::new
			        )
			    )
			);
		} else if ( target.getType().equals( Struct.TYPES.SORTED ) ) {
			return new Struct(
			    target.getType(),
			    entries.collect(
			        Collectors.toMap(
			            entry -> entry.getKey(),
			            entry -> {
				            Object val = entry.getValue();
				            return deep && val instanceof IStruct ? duplicateStruct( StructCaster.cast( val ), deep ) : val;
			            },
			            ( v1, v2 ) -> {
				            throw new BoxRuntimeException( "An exception occurred while duplicating the linked HashMap" );
			            },
			            ConcurrentSkipListMap<Key, Object>::new
			        )
			    )
			);
		} else {
			return new Struct(
			    target.getType(),
			    entries.collect( Collectors.toConcurrentMap( entry -> entry.getKey(), entry -> {
				    Object val = entry.getValue();
				    return deep && val instanceof IStruct ? duplicateStruct( StructCaster.cast( val ), deep ) : val;
			    } ) )
			);
		}
	}

	public static Array duplicateArray( Array target, Boolean deep ) {
		return new Array(
		    target.intStream()
		        .mapToObj( idx -> deep ? ( Object ) duplicate( target.get( idx ), deep ) : ( Object ) target.get( idx ) )
		        .toArray()
		);
	}

}
