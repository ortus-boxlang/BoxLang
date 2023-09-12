package ourtus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;

public class BoxBuiltinRegistry {

	private static BoxBuiltinRegistry		instance;

	private final HashMap<String, String>	registry;

	private BoxBuiltinRegistry() {
		this.registry = new HashMap<>() {

			{
				put( "init", "invokeConstructor( new Object[] { ${args} }  )" );
				put( "createobject", "JavaLoader.load( context, ${arg1} )" );
			}
		};
	}

	public static synchronized BoxBuiltinRegistry getInstance() {
		if ( instance == null ) {
			instance = new BoxBuiltinRegistry();
		}
		return instance;
	}

	public HashMap<String, String> getRegistry() {
		return registry;
	}
}
