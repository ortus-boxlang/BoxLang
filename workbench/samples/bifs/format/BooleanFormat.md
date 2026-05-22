### 1 is definitely true



<a href="https://try.boxlang.io/?code=eJxLys%2FPSU3Mc8svyk0s0VAwVNC05gIATjEGUQ%3D%3D" target="_blank">Run Example</a>

```java
booleanFormat( 1 );

```

Result: true

### 0 is definitely false



<a href="https://try.boxlang.io/?code=eJxLys%2FPSU3Mc8svyk0s0VAwUNC05gIATiwGUA%3D%3D" target="_blank">Run Example</a>

```java
booleanFormat( 0 );

```

Result: false

### Negative -1 is true as well



<a href="https://try.boxlang.io/?code=eJxLys%2FPSU3Mc8svyk0s0VDQNVTQtOYCAFTRBn4%3D" target="_blank">Run Example</a>

```java
booleanFormat( -1 );

```

Result: true

### And even a number larger then 1 is true



<a href="https://try.boxlang.io/?code=eJxLys%2FPSU3Mc8svyk0s0VAwVdC05gIATkUGVQ%3D%3D" target="_blank">Run Example</a>

```java
booleanFormat( 5 );

```

Result: true

### String representation of true is interpreted as true

```java
booleanFormat( "true" );

```

Result: true

### String representation of false is interpreted as false

```java
booleanFormat( "false" );

```

Result: false

### YES is recognized as synonym for true

```java
booleanFormat( "YES" );

```

Result: true

### NO is recognized as synonym for false

```java
booleanFormat( "NO" );

```

Result: false

### An empty string results in false

```java
booleanFormat( "" );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUHDKz89JTcxzyy%2FKTSzRUDA0MlbQVNC0VtDXVwgpKk3lKsep1ACskAuk0i0xpziVCwC7Nho6" target="_blank">Run Example</a>

```java
writeDump( BooleanFormat( 123 ) ); // True
writeDump( BooleanFormat( 0 ) );
 // False

```

### Deprecated Alias

`trueFalseFormat()` is a deprecated alias for `booleanFormat()` and is planned for removal in the 2.0 release.


