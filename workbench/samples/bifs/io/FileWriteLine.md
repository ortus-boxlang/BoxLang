### Script Syntax




```java
myfile = fileOpen( "c:\temp\test1.txt", "write" );
fileWriteLine( myfile, "This line is new." );
fileClose( myfile );

```


### Additional Examples


```java
openFile = fileopen( filepath, "read" );
readfromfile = filereadline( openfile );
filewriteline( filepath, readfromfile );

```


