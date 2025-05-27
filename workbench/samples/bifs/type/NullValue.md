### ColdFusion polyfill

Using java data type null instead

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyCz2K83J0VDIA5JhiTmlqRqaCkBozQUAFlIMpA%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( isNull( nullValue() ) );

```

Result: YES

### Additional Examples

<a href="https://try.boxlang.io/?code=eJw1i7EKwjAURfd%2BxR3TpVlFiW66FAcFHaWmKQ0kTUjea%2BjfGwWnA%2BfcKyWyMaDZZqx2NAFTSOhZV7mwc8gcY0gEdcRMFPNeylJKtwUmfptOBy%2FLQHo%2Brepyd8%2Bpv%2B3OKTd%2Be%2F3uCteKx%2BDYiPbQjOyjgM1fKfAftajpAx%2BpLzA%3D" target="_blank">Run Example</a>

```java
// see this video for Lucee null support => https://www.youtube.com/watch?v=GSlWfLR8Frs
my_null = NullValue();
dump( isNull( my_null ) );

```


