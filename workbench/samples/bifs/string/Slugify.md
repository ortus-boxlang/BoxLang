### Convert a string to a URL-friendly slug

```java
writeOutput( slugify( "My Blog Post Title" ) )

```

Result: my-blog-post-title

### Accents are removed

```java
writeOutput( slugify( "This is ä ü test ß" ) )

```

Result: this-is-a-u-test-ss

### Other characters become hyphens

Every character that is not a-z or 0-9 is replaced with a hyphen, and consecutive hyphens are not collapsed.

```java
writeOutput( slugify( "Hello, World!" ) )

```

Result: hello--world-

### Limit the length of the slug

```java
writeOutput( slugify( "This is a   test", 10 ) )

```

Result: this-is-a-

### Allow additional characters

```java
writeOutput( slugify( "snake_case name", 0, "_" ) )

```

Result: snake_case-name

### Member function

```java
writeOutput( "My Blog Post Title".slugify() )

```

Result: my-blog-post-title
