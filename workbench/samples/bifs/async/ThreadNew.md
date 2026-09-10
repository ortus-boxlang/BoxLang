### Create a new thread

```java
t = threadNew( () => {
    sleep( 100 );
    println( "thread running" );
} );
threadJoin( t );
writeOutput( "done" );

```

Result: done

### Named thread

```java
t = threadNew( "myThread", () => {
    return "hello";
} );
writeOutput( t.getName() );

```

Result: myThread
