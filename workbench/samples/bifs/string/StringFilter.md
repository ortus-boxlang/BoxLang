### Full function

Return only the letters in the string that meet the callback condition.

<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUKqqCgyMUrLmSk7MyUlKTM4GimkoOOZVKmTmFShoKtjaKVRzcRallpQW5YGFbEF6gOprrbny83Iqo0CGBJcUZealu2XmAA3VUMiBGK6jADdS05qrvCizJNW%2FtKSgtERDAaoRKAwAnFYr7w%3D%3D" target="_blank">Run Example</a>

```java
letters = "zzQQZ";
callback = ( Any inp ) => {
	return inp == "z";
};
onlyZs = StringFilter( letters, callback );
writeOutput( onlyZs );

```

Result: zzZ

### Member function

Return only the letters in the string that meet the callback condition.


```java
letters = "zzQQZ";
callback = ( Any inp ) => {
	return inp == "z";
};
onlyZs = letters.filter( callback );
writeOutput( onlyZs );

```

Result: zzZ

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyNjTEKAjEQRWtziiFVAjZbhwiK7D0yMUhwzC6TCYuIdzfiWm1j%2Bz7%2FPUoiiSt40AExXk7aqRiIMMTb2EqUPJU%2BGjiWB%2BQyNwEL%2FgBPteMkjcsKfRdg%2F76c4lQbdQJVOJfrmKkXDNC3tIeN3jq1cJZ0bvfZwHq3P9GwMWmcUP%2FpGT70DSRqTHU%3D" target="_blank">Run Example</a>

```java
letters = "abbcdB";
callbackFunction = ( Any input ) => {
	return input == "b";
};
result = stringFilter( letters, callbackFunction );
writeDump( result );
result1 = stringFilter( "bob", callbackFunction );
writeDump( result1 );

```


