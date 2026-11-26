### Output some information about a temporary file



<a href="https://try.boxlang.io/?code=eJxtkNFrgzAQxt%2F7V9zyMBRK514HY7hFaWC1oEJhb%2BkS14DRopHS%2FfW7s1E62rwc9%2BX7XS6fPaem1vAKP9qV2h6pC6aGm05%2Fu7Y7B%2BESmNO9o2sG4aLCKpqqvYCp7wKwl3HhwvS5lkrux9nBw%2BRffcRZnsQc3oANDYMXYCyER2CddzMkd51xd8ldLsrkBj15O6Fro5Ru%2FoOiWAvOk4zApnVwhR5GO1so6eg1jqU0VqdtZ6ULYB7xGRflZstFKhKOSVRDXU8pFOaX0Gywe93dgIX4SuAJnqMo8gXxaBVFhNPieju444AIw4MrzWQWbxJaEVXTA11dRYr60mtzWKiBbJSXfRAoruD2LeHgJHuQDmrZO7CtMpXRigTCxzimeQcyQk%2FfbCuYdhy%2FTZbNO47%2FA4oqq3I%3D" target="_blank">Run Example</a>

```java
myFile = getTempFile( getTempDirectory(), "testFile" )
fileInfo = getFileInfo( myFile )
isReadable = (!fileInfo.CANREAD ? "un" : "") & "readable"
isWritable = (!fileInfo.CANWRITE ? "un" : "") & "writable"
isHidden = (!fileInfo.ISHIDDEN ? "not " : "") & "hidden"
date = DateTimeFormat( fileInfo.LASTMODIFIED, "full" )
fileSize = NumberFormat( fileInfo.SIZE / 1000 / 1000, "0.00" )
writeOutput( """" & fileInfo.NAME & """ is " & isReadable & ", " & isWritable & " and " & isHidden & ". " )
writeOutput( "It was at last modified at " & date & " and has a size of " & fileSize & " MB" )

```

Result: "testFile9217639658547923751.tmp" is readable, writable and not hidden. It was at last modified at Friday, November 3, 2017 3:58:08 PM UTC and has a size of 0.00 MB

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLy8xJVbBVSE8tCUnNLXAD8jRgHJfMotTkkvyiSg1NHQWllNTcfCUFTa6U0twCDYWyxCJboDKQes%2B8tHwNhTSQOUB1OYlJqTm2Su4IOVRNOHUgKQcAXYowUA%3D%3D" target="_blank">Run Example</a>

```java
file = getTempFile( getTempDirectory(), "demo" )
dump( var=getFileInfo( file ), label="GetFileInfo" )
dump( var=FileInfo( file ), label="FileInfo" )

```

### Check if a path is a symbolic link

`fileInfo()` and `getFileInfo()` return `isLink` and `linkTarget`. `linkTarget` is an empty string when the path is not a link. The other keys describe what the link points to.

```java
base = getTempDirectory() & "linkDemo"
directoryCreate( base & "/releases/v2" )
fileWrite( base & "/releases/v2/app.txt", "app" )
fileCreateSymlink( base & "/current", "releases/v2" )

link = fileInfo( base & "/current" )
writeOutput( link.isLink & " " & link.linkTarget & ", " )

file = fileInfo( base & "/releases/v2/app.txt" )
writeOutput( file.isLink & " [" & file.linkTarget & "]" )

```

Result: true releases/v2, false []

