### Full function

Do all letters in the string meet the callback condition?

<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUIqKSqyqUrLmSk7MyUlKTM4GimkoOOZVKmTmFShoKtjaKVRzcRallpQW5YGFbIF6QOprrbmAOqJAZgSXFGXmpbuWpRZVaijkQIzWUYAbqGnNVV6UWZLqX1pSUFqioQDRBhQFABXtKps%3D" target="_blank">Run Example</a>

```java
letters = "ZZazz"
callback = ( Any inp ) -> {
	return inp == "z"
}
allZs = StringEvery( letters, callback )
writeOutput( allZs )

```

Result: false

### All characters match

String comparison with `==` ignores case.

```java
letters = "zzZZz"
callback = ( inp ) -> inp == "z"
writeOutput( stringEvery( letters, callback ) )

```

Result: true

### An empty string always returns true

```java
writeOutput( stringEvery( "", ( inp ) -> inp == "z" ) )

```

Result: true
