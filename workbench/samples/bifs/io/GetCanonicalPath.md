### getCanonicalPath Example

 Returns the canonical path of the input path.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSE8tcU7My8%2FLTE7MCUgsyQCLOCUWp4ak5hbkJJakggU1FYDQmgsAiaEUTg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( getCanonicalPath( getBaseTemplatePath() ) );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLSixOjS9ILMlQsFVITy1xAnJDUnMLchJLUgOAohqa1lzJiXn5eZnJiTkQJc4wLlheIQluAFBpSmlugYYCQgNQSEFfX0E%2FIz83Vb88NalYvzipQj%2BnNDk11VQfaFZRam5%2BSapeclouFwDd4y8X" target="_blank">Run Example</a>

```java
base_path = getBaseTemplatePath();
canonical = getCanonicalPath( base_path );
dump( canonical );
 // /var/task

```


