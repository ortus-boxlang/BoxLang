### Script Syntax

Checking if a directory called 'icons' exists and then creating the directory if it does not exist.

<a href="https://try.boxlang.io/?code=eJzLTNNQUEzJLEpNLskvqnStyCwuKdZQSK0oSMxLCUgsydBQUNJPLC5OLSnWz8xN189Mzs8rVlLQBMNqLk64Tuei1MSSVKBqLIqtuWq5AEzXIoU%3D" target="_blank">Run Example</a>

```java
if( !directoryExists( expandPath( "/assets/img/icons" ) ) ) {
	directoryCreate( "assets/img/icons" );
}

```

Result: The directory 'icons' will be created under the img folder.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLySxKTS7JL6p0rcgsLinWUFBKgYkEJJZkKCloWnMBAAI%2BDNI%3D" target="_blank">Run Example</a>

```java
directoryExists( "directoryPath" );

```

Result: false

