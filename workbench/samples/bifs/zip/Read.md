### Read a file from a ZIP archive

Read a file from a ZIP archive and return its contents as a string.

```java

read( "test.zip", "example.txt" );

```

### Read a file from a ZIP archive with a charset

Specify the character encoding used to read the file from the archive.

```java

read( "test.zip", "example.txt", "UTF-8" );

```

### Additional Examples

Read a file located in a directory inside the ZIP archive.

```java

read( "test.zip", "docs/readme.txt" );

```

### Read a file using named arguments.

```java

read( source="test.zip", entryPath="docs/readme.txt" );

read( source="test.zip", entryPath="docs/readme.txt", charset="UTF-8" );

```