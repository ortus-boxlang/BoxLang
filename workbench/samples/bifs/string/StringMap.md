### Full function

Map each element of the string to a new value.

<a href="https://try.boxlang.io/?code=eJw1jTEKAjEQRWtzis9WE7S3CCt4ALHwBNk4rgMhhskEEfHuq6jtezxeZjPWhhFDnNKZL%2FMQXMq31pXfkLAvD0ip8Bh3eLqVsnUtSNeohNiSCP38Glv44F7B3VWMj91qN8LJVMp8iJWQv7MN%2FgP%2FCRae5Skm" target="_blank">Run Example</a>

```java
letters = "abcdefg";
closure = ( Any inp ) => {
	return char( ascii( inp ) + 7 );
};
writeOutput( StringMap( letters, closure ) );

```

Result: hijklmn

### Member function

Map each element of the string to a new value.


```java
letters = "abcdefg";
closure = ( Any inp ) => {
	return char( ascii( inp ) + 7 );
};
writeOutput( letters.map( closure ) );

```

Result: hijklmn

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrQwuKcrMS1ewVVDySM3JyVcIzy%2FKSVGy5krOyS8uLUoFSmgoOOZVKpQl5ihoKtjaKVRzcRallpQW5SlogMTUFJQSlTStuWqtucqLMktSXUpzCzQUIKb6JgKZuVArdBRgRmoqANUDADxKJig%3D" target="_blank">Run Example</a>

```java
myString = "Hello World";
closure = ( Any val ) => {
	return (val & "a");
};
writeDump( StringMap( myString, closure ) );

```


