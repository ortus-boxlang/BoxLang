### Script Syntax

Outputs the current value of the internal millisecond timer

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSE8tCclMznbOL80r0dBU0LTmAgC%2FpQq4" target="_blank">Run Example</a>

```java
writeOutput( getTickCount() );

```

Result: 

### A simple timer

Outputs the millisecond difference between a starting point and end point

<a href="https://try.boxlang.io/?code=eJwrLkksKlGwVUhPLQnJTM52zi%2FNK9HQtOYqzklNLdBQMDQwMFAAcsuLMktS%2FUtLCkpLNNDUKugqFIMNASoDAPpmGRg%3D" target="_blank">Run Example</a>

```java
start = getTickCount();
sleep( 1000 );
writeOutput( getTickCount() - start );

```

Result: 1000 (note: may be off by a few ms depending on the environment)

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMhJTErNsVVKTy0JyUzOds4vzStR0MjNzMnJ1E9JTUsszSnRVNJRKEssskVWoqGpoGnNVY5hTF5iXj425QoQGQUc2opTk%2FPzUrBrhMpBtAIAPes99g%3D%3D" target="_blank">Run Example</a>

```java
writeDump( label="getTickCount (milli/default)", var=getTickCount() );
writeDump( label="nano", var=getTickCount( "nano" ) );
writeDump( label="second", var=getTickCount( "second" ) );

```


