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
package ortus.boxlang.runtime.util.conversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ortus.boxlang.compiler.javaboxpiler.transformer.BoxClassTransformer;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

/**
 * This class is in charge of marshalling objects to binary formats and
 * vice-versa
 */
public class ObjectMarshaller {

	/**
	 * Serialize the incoming object into a binary format
	 *
	 * @param context The context in which the object is being serialized
	 * @param target  The object to serialize
	 *
	 * @throws BoxValidationException If the object is not serializable
	 *
	 * @return The binary representation of the object
	 */
	public static byte[] serialize( IBoxContext context, Object target ) {
		// Throw exception if object is null
		if ( target == null ) {
			throw new BoxValidationException( "Object is null and cannot be serialized" );
		}

		// Throw exception if object is not serializable
		if ( ! ( target instanceof java.io.Serializable ) ) {
			throw new BoxValidationException( "Object is not serializable" );
		}

		// Announce the event
		context.getRuntime()
		    .getInterceptorService()
		    .announce( BoxEvent.BEFORE_OBJECT_MARSHALL_SERIALIZE, Struct.of( "object", target ) );

		// Serialize the object
		try ( ByteArrayOutputStream bos = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream( bos ) ) {
			oos.writeObject( target );
			oos.flush();
			// Get the serialized object
			byte[] result = bos.toByteArray();
			// Announce the event
			context.getRuntime()
			    .getInterceptorService()
			    .announce( BoxEvent.AFTER_OBJECT_MARSHALL_SERIALIZE, Struct.of( "binary", result ) );
			return result;
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to serialize object", e );
		}
	}

	/**
	 * Deserialize the incoming binary data into an object
	 *
	 * @param context The context in which the object is being deserialized
	 * @param data    The binary data to deserialize
	 *
	 * @throws BoxValidationException If the binary data is null
	 * @throws BoxIOException         If the binary data cannot be deserialized
	 *
	 * @return The object representation of the binary data
	 */
	public static Object deserialize( IBoxContext context, byte[] data ) {
		if ( data == null ) {
			throw new BoxValidationException( "Byte array is null and cannot be deserialized" );
		}

		// Announce the event
		context.getRuntime()
		    .getInterceptorService()
		    .announce( BoxEvent.BEFORE_OBJECT_MARSHALL_DESERIALIZE, Struct.of( "binary", data ) );

		try ( ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( data );
		    ObjectInputStream objectInputStream = new ObjectInputStream( byteArrayInputStream ) ) {

			// Deserialize the object
			Object result = objectInputStream.readObject();

			// If this is BoxLang class, then load back it's state
			// According to BoxLang creation rules, the object should be a BoxClassState
			if ( result instanceof BoxClassState classState ) {
				IClassRunnable boxClass = ( IClassRunnable ) ClassLocator
				    .getInstance()
				    .load(
				        context,
				        classState.classPath.getName(),
				        ClassLocator.BX_PREFIX,
				        true,
				        context.getCurrentImports()
				    )
				    .invokeConstructor( context, Key.noInit )
				    .unWrapBoxLangClass();

				// Restore the state
				boxClass.getVariablesScope().putAll( classState.variablesScope );
				boxClass.getThisScope().putAll( classState.thisScope );
				// Restored
				result = boxClass;
			}

			// Announce the event
			context.getRuntime()
			    .getInterceptorService()
			    .announce( BoxEvent.AFTER_OBJECT_MARSHALL_DESERIALIZE, Struct.of( "object", result ) );
			return result;
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to deserialize object", e );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Failed to load the object", e );
		}
	}

	/**
	 * Serialize a BoxLang class into a binary format. This is used to serialize the state of
	 * a BoxLang class so it can be reconstructed later. The {@link BoxClassTransformer}
	 * uses it to do custom serialization of classes.
	 *
	 * @param target The class to serialize
	 */
	public static Object serializeClass( IClassRunnable target ) {
		// Create the state of the class
		return new BoxClassState(
		    target.getName(),
		    target.getVariablesScope(),
		    target.getThisScope()
		);
	}

}
