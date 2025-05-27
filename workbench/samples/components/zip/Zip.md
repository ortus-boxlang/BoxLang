### Extract / Unzip a zip file into a folder

Uses `action="unzip"` in the cfzip tag to unzip into the temp directory.


```java
<bx:zip action="unzip" destination="#getTempDirectory()#" file="#zipFilePath#">
```


### Extract / Unzip a zip file into a folder (Script Syntax)

Uses `action="unzip"` in the cfzip tag to unzip into the temp directory.


```java
bx:zip action="unzip" file="zipFileName" destination=getTempDirectory();

```


### List contents of a zip folder (Script Syntax)

Uses `action="list"` in the cfzip tag to list the zip contents.


```java
bx:zip action="list" file="zipFileName" name="zipList";

```


