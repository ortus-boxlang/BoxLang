### Bitwise Mask Clear

Uses the bitMaskClear function to clear (setting to 0) each of the corresponding bits

<a href="https://try.boxlang.io/?code=eJxLyizxTSzOds5JTSzSUDDWUTDQUTBU0LTmAgBqAQa1" target="_blank">Run Example</a>

```java
bitMaskClear( 3, 0, 1 );

```

Result: 2

### Using non zero start parameter



<a href="https://try.boxlang.io/?code=eJxLyizxTSzOds5JTSzSUDDWUTAEIgVNay4AagkGtg%3D%3D" target="_blank">Run Example</a>

```java
bitMaskClear( 3, 1, 1 );

```

Result: 1

### Using non zero mask start and length parameters



<a href="https://try.boxlang.io/?code=eJxLyizxTSzOds5JTSzSUDA00FEw1FEwUtC05gIAcTQG5Q%3D%3D" target="_blank">Run Example</a>

```java
bitMaskClear( 10, 1, 2 );

```

Result: 8

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSMos8U0sznbOSU0s0lAwMjXVUQAhBU0FTWuucmSVSjZJRXZKGMJYDDDQUTCBGAAAOmkgBQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( bitMaskClear( 255, 5, 5 ) );
writeOutput( "<br>" );
writeOutput( bitMaskClear( 255, 0, 4 ) );

```


