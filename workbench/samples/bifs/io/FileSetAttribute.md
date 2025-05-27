### Create a temporary file and then change read-only mode



<a href="https://try.boxlang.io/?code=eJzLrXTLzElVsFVITy0JSc0tAPE0YByXzKLU5JL8okoNTR0FpZLU4hKQtJKCpjVXeVFmSap%2FaUlBaYmGglJmsQJIIDEpJ9VKQUlBDWQASKlnXlq%2BhkIuxA5NPWdHv%2FAgzxBXkAFpQKHg1BLHkpKizKTSklSYMqBFRamJKf55OZVYLFJ41DaJSAsARH5HRA%3D%3D" target="_blank">Run Example</a>

```java
myFile = getTempFile( getTempDirectory(), "testFile" );
writeOutput( "is writable: " & getFileInfo( myFile ).CANWRITE );
fileSetAttribute( myFile, "readOnly" );
writeOutput( " → " & getFileInfo( myFile ).CANWRITE );

```

Result: is writable: YES → NO

### Additional Examples


```java
filesetattribute( "example.txt", "readonly" );

```


