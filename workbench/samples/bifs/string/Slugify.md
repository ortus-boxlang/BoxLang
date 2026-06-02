### Convert a string to a URL-friendly slug

```java
result = slugify( "Hello World! This is a Test." );
writeOutput( result );

```

Result: hello-world-this-is-a-test

### Handle special characters

```java
result = slugify( "Café & Restaurant — New York" );
writeOutput( result );

```

Result: cafe-restaurant-new-york

### Using the member function

```java
result = "My Blog Post Title".slugify();
writeOutput( result );

```

Result: my-blog-post-title
