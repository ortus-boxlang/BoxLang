### Simple Example

Returns the 2nd element in the list

<a href="https://try.boxlang.io/?code=eJzLySwucU8tcSzRUFBKy8%2FXSUos0snJL0rN1cksKC7NVdJRMFLQtOYCABBIDHQ%3D" target="_blank">Run Example</a>

```java
listGetAt( "foo,bar,lorem,ipsum", 2 );

```

Result: bar

### Example with Delimiter

Returns the 3rd element in the list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJzLySwucU8tcSzRUFBKy8%2FXSUosqsnJL0rN1cksKC7NrclNrSnOz00tycjMS1fSUTDWUVCqUVLQtOYCAGHmE2k%3D" target="_blank">Run Example</a>

```java
listGetAt( "foo,bar|lorem,ipsum|me|something", 3, "|" );

```

Result: me

### Example with IncludeEmptyValues

Returns the 4th element in the list, treating the empty element as a value

<a href="https://try.boxlang.io/?code=eJzLySwucU8tcSzRUFBKy8%2FXSUos0tHJyS9KzdXJLCguzVXSUTDRUVDSAdIlRaWpCprWXAC3TA9q" target="_blank">Run Example</a>

```java
listGetAt( "foo,bar,,lorem,ipsum", 4, ",", true );

```

Result: lorem

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxNzrsKwzAMBdDdX3HxkgRMNZROoUOhj6Wl0D%2BIbUENeWHLpJ9fp3TIJAkuOneJQfiZZc5Sow9JbiynsurOGGuc0QZ7A71OiZnRoGlBhBdLjmOCVeV48GA54ppHJ2EasQR5o6IKnvswFCCqJPFevuMITR2RJUfkdauW1T%2FnYa7xj%2Bw2LQ6Fpi2tVvvymdkJe0y%2F3nDqC4%2BwPHA%3D" target="_blank">Run Example</a>

```java
writeOutput( listGetAt( "a,,b,c,", 3, ",", true ) ); // Returns b
// Member Function with '/' delimiter
strList = "/a//b/c//d";
writeDump( strList.listGetAt( 5, "/", true ) );
 // Expected output c

```


