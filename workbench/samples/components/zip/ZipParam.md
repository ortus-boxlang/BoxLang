### Add a file to a ZIP archive

Defines a source file or directory to include in a ZIP operation.

```java
<bx:zip action="zip" file="/tmp/archive.zip" source="/tmp/myfile.txt">

```

### Add a directory recursively

```java
<bx:zip action="zip" file="/tmp/archive.zip" source="/tmp/myfolder" recurse="true">

```

### Add content directly as a ZIP entry

Creates a ZIP entry from in-memory content without a source file.

```java
<bx:zip action="zip" file="/tmp/archive.zip">
    <bx:zipParam content="Hello World" entryPath="greeting.txt">
</bx:zip>

```

### Add content with a specific charset

```java
<bx:zip action="zip" file="/tmp/archive.zip">
    <bx:zipParam content="Bonjour le monde" entryPath="french.txt" charset="UTF-8">
</bx:zip>

```

### Filter files when zipping a directory

```java
<bx:zip action="zip" file="/tmp/archive.zip" source="/tmp/logs">
    <bx:zipParam filter="*.log">
</bx:zip>

```

### Add a prefix to ZIP entry paths

```java
<bx:zip action="zip" file="/tmp/archive.zip" source="/tmp/data">
    <bx:zipParam prefix="backup/2024/">
</bx:zip>

```
