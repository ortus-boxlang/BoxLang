### Simple listLast Example

A very basic listLast example

<a href="https://try.boxlang.io/?code=eJzLySwu8UksLtFQUMrPS9UpKc%2FXKckoSk3VScsvLVJS0LTmAgDV5gtf" target="_blank">Run Example</a>

```java
listLast( "one,two,three,four" );

```

Result: four

### listLast Example with multiple delimiters

A more advanced listLast example

<a href="https://try.boxlang.io/?code=eJzLySwu8UksLtFQUMrPS9UvKc%2BPKckoSk3VT8svLVLSUVCK0VdS0LTmAgAjqQyw" target="_blank">Run Example</a>

```java
listLast( "one/two\three/four", "\/" );

```

Result: four

### Additional Examples

<a href="https://try.boxlang.io/?code=eJw9jTsKwzAQRHudYlFlwxIdwKQIJIGAUukEjtlCIFlCu%2Bvk%2BFG%2B1Uwxb55zEGKuieD0mF9p7i0KFZWqMkCKLH7m3mxQjoD%2BEBCTLkSIKMRiYYRxMs7BlfKNGpx1XSSW1bA033HYg8VLh8pGP9JOH81Rcx3gO9z9Ze%2FHJ1DsMeE%3D" target="_blank">Run Example</a>

```java
// Simple Example
writeoutput( listLast( "Susi ,LAS,,boxlang,,,test" ) );
// Member Function
strList = ",I,,love,boxlang,,";
writeDump( strList.listLast() );

```


