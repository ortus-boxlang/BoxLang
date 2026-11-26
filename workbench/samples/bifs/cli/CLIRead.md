### Read a line of input

Run the script and type a name when prompted. The prompt is printed before reading. In this example the user types `Luis`.

```java
name = cliRead( "What is your name? " )
println( "Hello " & name )
```

Result: Hello Luis

### Read without a prompt

The prompt argument is optional. In this example the user types `42`.

```java
value = cliRead()
println( value + 1 )
```

Result: 43
