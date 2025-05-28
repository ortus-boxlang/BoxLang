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

<a href="https://try.boxlang.io/?code=eJyNjsEKwjAQRM%2FmK4acWvDSc4ggWMHPqGGR4JqW7aZSxH83xXrRi9fHzJthUiUZ4WFP4H4icA5E1pnQMZ%2B7cD3mFDT2qUQq7NOMmIasqOF3eJiNkGZJK%2FRFs3SfzgiNmQvBqBLTpZ1I5gr8ntvix147c5eodMi3ocLarj%2Be5ltk23LyT02z0BdpcE29" target="_blank">Run Example</a>

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


