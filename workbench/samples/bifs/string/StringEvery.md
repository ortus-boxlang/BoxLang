### Full function

Do all letters in the string meet the callback condition?

<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUIqKSqyqUrLmSk7MyUlKTM4GimkoOOZVKmTmFShoKtjaKVRzcRallpQW5YGFbIF6QOprrbmAOqJAZgSXFGXmpbuWpRZVaijkQIzWUYAbqGnNVV6UWZLqX1pSUFqioQDRBhQFABXtKps%3D" target="_blank">Run Example</a>

```java
letters = "ZZazz";
callback = ( Any inp ) => {
	return inp == "z";
};
allZs = StringEvery( letters, callback );
writeOutput( allZs );

```

Result: NO

### Member function

Do all letters in the string meet the callback condition?


```java
letters = "zzZZz";
callback = ( Any inp ) => {
	return inp == "z";
};
allZs = letters.every( callback );
writeOutput( allZs );

```

Result: YES

### Additional Examples

<a href="https://try.boxlang.io?code=eJyNzcEKgkAQxvFz%2BxQfe1Lo4lkMggx8DJVBlqZRxllLondvIbvUpeuf%2BX7DZEY6o4JvwONC6MY7tzL40vUtc9f2l3OU3sIo6SjDUVYEmaIhR3XAw%2B2ULKpssUoQpe2zdEpz5FQwmwYZ6oV0zcDvh3v86HnpbhqMTvE6ZdjW%2BccpviFfE%2Fk%2FmSLVF%2BFSTpA%3D" target="_blank">Run Example</a>

```java
letters = "I love boxlang";
callbackFunction = ( Any input ) => {
	return input == "e";
};
result = stringEvery( letters, callbackFunction );
writeDump( result );
result1 = stringEvery( "Eee", callbackFunction );
writeDump( result1 );

```

