### Output some information about a temporary file



<a href="https://try.boxlang.io/?code=eJxtkFFLwzAUhd%2F9Fdc8SAdj1ldFpJqWBVwHa2HgW2ZSF2ja0aaM%2Beu9N0vLZMtLck%2FOd3Nz7CkztYZX%2BNGu1PZAVTQW3HT627XdKZrNgTndO7pmMHu5q%2FAgmqo9k1moIrDnfugw%2FUZLJXe%2Be3Q%2FAouPJN%2BkCYc3YEPD4BkYm8EDsC64GaHbzrib6HYjyvSKPQa7Z5dGKd38J0WxFJynOZFN6%2BCC3Xs7kko6eo%2FjVhqrs7az0kUw9fhMinK15iITKcc4qqGupygK80tsPtid7q7IQnyl8AhPcRyHDfl4Eceep%2BH1enCHARmGC6ea0DxZpTQlqqYHurrIFfV50KbAUAPZqCCHLFBcwI3HhIOj7EE6qGXvwLbKVEYrEoj3iYwN92SEnj7aVjAO6T9OltW77%2F8HrTatjw%3D%3D" target="_blank">Run Example</a>

```java
myFile = getTempFile( getTempDirectory(), "testFile" );
fileInfo = getFileInfo( myFile );
isReadable = (!fileInfo.CANREAD ? "un" : "") & "readable";
isWritable = (!fileInfo.CANWRITE ? "un" : "") & "writable";
isHidden = (!fileInfo.ISHIDDEN ? "not " : "") & "hidden";
date = DateTimeFormat( fileInfo.LASTMODIFIED, "full" );
fileSize = NumberFormat( fileInfo.SIZE / 1000 / 1000, "0.00" );
writeOutput( """" & fileInfo.NAME & """ is " & isReadable & ", " & isWritable & " and " & isHidden & ". " );
writeOutput( "It was at last modified at " & date & " and has a size of " & fileSize & " MB" );

```

Result: "testFile9217639658547923751.tmp" is readable, writable and not hidden. It was at last modified at Friday, November 3, 2017 3:58:08 PM UTC and has a size of 0.00 MB

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLy8xJVbBVSE8tCUnNLXAD8jRgHJfMotTkkvyiSg1NHQWllNTcfCUFTWuulNLcAg2FssQiW6A6kAbPvLR8DYU0kEFAhTmJSak5tkruCDk0XTi1oKgHABOwMQs%3D" target="_blank">Run Example</a>

```java
file = getTempFile( getTempDirectory(), "demo" );
dump( var=getFileInfo( file ), label="GetFileInfo" );
dump( var=FileInfo( file ), label="FileInfo" );

```


