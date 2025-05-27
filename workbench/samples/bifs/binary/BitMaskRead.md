### Bitwise Mask Read

Uses the bitMaskRead function to read each of the corresponding bits specified in the mask

<a href="https://try.boxlang.io/?code=eJxLyizxTSzODkpNTNFQMNZRMNBRMFTQtOYCAGABBko%3D" target="_blank">Run Example</a>

```java
bitMaskRead( 3, 0, 1 );

```

Result: 1

### Using non zero start parameter

Bit shift the mask 2 places

<a href="https://try.boxlang.io/?code=eJxLyizxTSzODkpNTNFQMDTQUTDSUTBU0LTmAgBmzAZ6" target="_blank">Run Example</a>

```java
bitMaskRead( 10, 2, 1 );

```

Result: 0

### Using non zero read mask start and length parameters



<a href="https://try.boxlang.io/?code=eJxLyizxTSzODkpNTNFQMDTQUTDUUTBW0LTmAgBmzgZ7" target="_blank">Run Example</a>

```java
bitMaskRead( 10, 1, 3 );

```

Result: 5

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSMos8U0szg5KTUzRUDAyNdVRACEFTQVNa65yZIVKNklFdkoYwpj6DXQUTCD6AeqjHy8%3D" target="_blank">Run Example</a>

```java
writeOutput( bitMaskRead( 255, 5, 5 ) );
writeOutput( "<br>" );
writeOutput( bitMaskRead( 255, 0, 4 ) );

```


