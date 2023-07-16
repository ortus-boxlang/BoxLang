package ortus.boxlang.runtime;

/**
 * Represents the top level runtime container for box lang.  Config, global scopes, mappings, threadpools, etc all go here.
 * All threads, requests, invocations, etc share this.
 */
public class BoxRuntime {
    private static BoxRuntime boxRuntimeInstance;


    // Prevent outside instantiation to follow singleton pattern
    protected BoxRuntime() {

    }

    public synchronized static BoxRuntime startup() {
        System.out.println( "Starting up Box Runtime" );
        boxRuntimeInstance = new BoxRuntime();
        return getInstance();
    }

    public static void shutdown() {
        System.out.println( "Shutting down Box Runtime" );
        boxRuntimeInstance = null;
    }

    public static BoxRuntime getInstance() {
        return boxRuntimeInstance;
    }

}
