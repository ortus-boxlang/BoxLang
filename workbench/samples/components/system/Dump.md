### Dump a variable to the browser

Outputs structured debug information for any variable type.

```java
<bx:dump var="#myStruct#">

```

### Dump with a label

```java
<bx:dump var="#users#" label="User List">

```

### Limit dump depth

Prevents dumping deeply nested structures by limiting the number of levels.

```java
<bx:dump var="#complexObject#" top="3">

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
