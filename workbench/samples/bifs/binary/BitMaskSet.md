### Bitwise Mask Set

Performs masking operation on each of the corresponding bits

<a href="https://try.boxlang.io/?code=eJxLyizxTSzODk4t0VAw01Ew1FEwAJIKmtZcAGsZBno%3D" target="_blank">Run Example</a>

```java
bitMaskSet( 6, 1, 0, 1 );

```

Result: 7

### Using non zero start parameter

Bit shift the mask 2 places

<a href="https://try.boxlang.io/?code=eJxLyizxTSzODk4t0VAwNNBRMNRRMAKSCprWXABx9Aan" target="_blank">Run Example</a>

```java
bitMaskSet( 10, 1, 2, 1 );

```

Result: 14

### Using non zero mask start and length parameters



<a href="https://try.boxlang.io/?code=eJxLyizxTSzODk4t0VAwNNBRMNJRMASSCprWXABx%2FAao" target="_blank">Run Example</a>

```java
bitMaskSet( 10, 2, 1, 2 );

```

Result: 12

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSMos8U0szg5OBbKNTE11IAQIKWgqaFpzlSMrVrJJKrJTwhDGMMMQiA10FEwgRgAAn0IgKQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( bitMaskSet( 255, 255, 5, 5 ) );
writeOutput( "<br>" );
writeOutput( bitMaskSet( 255, 15, 0, 4 ) );

```


