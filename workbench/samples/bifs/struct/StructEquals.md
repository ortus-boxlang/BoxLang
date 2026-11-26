### Compare two structs that differ



<a href="https://try.boxlang.io/?code=eJxLzs9Ly0w3VLBVqFbg4nRUsFIw0OHidALRXLXWXMlgaSOQNKqsIUi2vCizJNW%2FtKSgtERDobikqDS5xLWwNDGnWEMBotFQRwFmgqaCpjUXABwCHhk%3D" target="_blank">Run Example</a>

```java
config1 = { 
	A : 0,
	B : 0
}
config2 = {
	A : 0,
	B : 1
}
writeOutput( structEquals( config1, config2 ) )

```

Result: false

### Compare two structs with the same content

Key order does not matter; the structs only need the same keys and equal values.

```java
a = { x : 1, y : 2 }
b = { y : 2, x : 1 }
writeOutput( a.equals( b ) )
```

Result: true
