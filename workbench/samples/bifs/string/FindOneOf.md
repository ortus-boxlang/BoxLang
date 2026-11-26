### Find first instance starting from beginning of string.

We're not passing a start index in this example.

<a href="https://try.boxlang.io/?code=eJxdjTEKgDAQBHtfsaRSEH%2FgG1JoL0ETcxDPkDsRf29shS1mh4UVLcT7ouci3pU1YoSZo0dxxKiZ8gfBpSQ4Kqbns1oXOdUqg2nuQurtpfnSFoF4s%2BxtaGGITQ%2F5H3ToXqPMKOs%3D" target="_blank">Run Example</a>

```java
string_to_search = "The rain in Spain falls mainly in the plains."
writeOutput( findOneOf( "in", string_to_search ) )

```

Result: 7

### Find first instance starting from the twelfth character.

Let's pass in a starting index of 12. The search will start at the twelfth character, just before the word 'Spain'.

<a href="https://try.boxlang.io/?code=eJwrLinKzEuPL8mPL05NLErOULBVUArJSFUoSszMUwCi4AIQIy0xJ6dYIRfIzKkEiZYAVRTkALnFekpc5UWZJan%2BpSUFpSUaCmmZeSn%2Bean%2BaRoKSpl5SjoKxWgW6CgYGiloKmgCAEmfKZo%3D" target="_blank">Run Example</a>

```java
string_to_search = "The rain in Spain falls mainly in the plains."
writeOutput( findOneOf( "in", string_to_search, 12 ) )

```

Result: 16

### Example showing this function will search all characters from the 'set' argument in the 'string' argument.

This function is case-sensitive so 't' does NOT match the first 'T'. It's the same for 'H' NOT matching the first 'h'. But 'e' matches the 'e' at the third position.
Since this is the first match, this is the index that is returned.

<a href="https://try.boxlang.io/?code=eJxdjTEKgDAQBHtfsaSKIIIPsLdLob0EvWggxpA7EX9vbIUtZoeFZck%2BbrOcM5PNy44eatoJ2fqIkjF94GwIjKNgeD4rZZFCqdyq6s5eyFySLtFwPq4mknEaSgZSDfj30KBDjfoFSMopsg%3D%3D" target="_blank">Run Example</a>

```java
string_to_search = "The rain in Spain falls mainly in the plains."
writeOutput( findOneOf( "tHe", string_to_search, 1 ) )

```

Result: 3

### Characters that are not found

```java
writeOutput( findOneOf( "c", "a a a a b b b b" ) & " " & findOneOf( "b", "a a a a b b b b" ) )

```

Result: 0 9

### Member function

```java
writeOutput( "The rain in Spain".findOneOf( "xyz" ) )

```

Result: 0
