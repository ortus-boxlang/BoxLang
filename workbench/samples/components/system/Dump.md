### Dump a variable to the browser

Outputs structured debug information for any variable type.

```java
<bx:dump var="#myStruct#">

```

### Dump with a label

```java
<bx:dump var="#users#" label="User List">

```

### Limit recursion depth

Prevents dumping deeply nested structures by limiting how many levels are recursed into.
`depth` is 1-based: `-1` (the default) is unlimited, `0` shows nothing, `1` shows the top
level with no recursion, `2` recurses once, etc.

```java
<bx:dump var="#complexObject#" depth="3">

```

### Limit the number of rows/items shown

Limits how many keys, array elements, or query rows are shown per level, independently of
recursion depth. Same 1-based semantics as `depth`.

```java
<bx:dump var="#bigArray#" maxRows="10">

```

### Dump to console

```java
<bx:dump var="#debugInfo#" output="console" format="text">

```

### Dump to a file

```java
<bx:dump var="#errorData#" output="/tmp/debug.txt" format="text">

```

### Dump and abort

```java
<bx:dump var="#exception#" abort="true">

```

### Hide UDFs in dump output

```java
<bx:dump var="#myComponent#" showUDFs="false">

```
