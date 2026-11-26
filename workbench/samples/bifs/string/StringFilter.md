### Full function

Return only the letters in the string that meet the callback condition.

<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUKqqCgyMUrLmSk7MyUlKTM4GimkoOOZVKmTmFShoKtjaKVRzcRallpQW5YGFbEF6gOprrbny83Iqo0CGBJcUZealu2XmAA3VUMiBGK6jADdS05qrvCizJNW%2FtKSgtERDAaoRKAwAnFYr7w%3D%3D" target="_blank">Run Example</a>

```java
letters = "zzQQZ"
callback = ( Any inp ) -> {
	return inp == "z"
}
onlyZs = StringFilter( letters, callback )
writeOutput( onlyZs )

```

Result: zzZ

### Keep only the digits

```java
writeOutput( stringFilter( "a1b2c3", ( ch ) => isNumeric( ch ) ) )

```

Result: 123
