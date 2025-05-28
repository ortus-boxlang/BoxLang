### List Files in Directory (Script Syntax)

Returns a query

<a href="https://try.boxlang.io/?code=eJzLrfTJLC5RsFVIySxKTS7JLwLzNRRSKwoS81ICEksyNBSU9PSVFDR1FNISc4pTdRSUCktTiyqBItZcAJ%2BQE6M%3D" target="_blank">Run Example</a>

```java
myList = directoryList( expandPath( "./" ), false, "query" );

```

Result: [
  {
  name : ".DS_Store",
  size : 6148,
  type : "File",
  dateLastModified : {ts '2025-05-26 18:15:41'},
  attributes : "RWH",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : ".ortus",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 18:13:41'},
  attributes : "RWXH",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "Application.bx",
  size : 42,
  type : "File",
  dateLastModified : {ts '2025-05-24 04:42:10'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "assets",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 17:03:57'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "bifs",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 22:10:29'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "components",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 22:10:12'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "compressed_test.txt.gz",
  size : 340,
  type : "File",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "example.bxm",
  size : 899,
  type : "File",
  dateLastModified : {ts '2025-05-26 11:05:52'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "example.bxm",
  size : 876,
  type : "File",
  dateLastModified : {ts '2025-05-26 11:01:38'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "filepath",
  size : 23,
  type : "File",
  dateLastModified : {ts '2025-05-26 22:10:14'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "images",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "index.bxm",
  size : 10048,
  type : "File",
  dateLastModified : {ts '2025-05-26 22:11:54'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "MyDestinationDirectory",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "myNewFileName.txt",
  size : 57,
  type : "File",
  dateLastModified : {ts '2025-05-26 16:38:57'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "new",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "new_directory",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 18:13:41'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "Page.bx",
  size : 1253,
  type : "File",
  dateLastModified : {ts '2025-05-24 11:23:02'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "server.json",
  size : 108,
  type : "File",
  dateLastModified : {ts '2025-05-24 10:35:52'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "setup_db.sql",
  size : 480,
  type : "File",
  dateLastModified : {ts '2025-05-24 04:42:10'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "some",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 18:13:42'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "test.txt",
  size : 614,
  type : "File",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "testcase.txt",
  size : 0,
  type : "File",
  dateLastModified : {ts '2025-05-28 22:10:14'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
}
]

### Create a Directory (Script Syntax)




```java
directoryCreate( expandPath( "./new_directory" ) );

```


### Delete a Directory (Script Syntax)

Directory Delete


```java
directoryDelete( expandPath( "./my_directory" ) );

```


### Rename a Directory (Script Syntax)




```java
directoryRename( expandPath( "./my_directory" ), expandPath( "./new_directory" ) );

```


### List File in Directory (Tag Syntax)

Directory List


```java
<bx:directory action="list" directory="#expandPath( "./" )#" recurse="false" name="myList">
```


### Create a Directory (Tag Syntax)




```java
<bx:directory action="create" directory="#expandPath( "./new_directory" )#">
```


### Delete a Directory (Tag Syntax)




```java
<bx:directory action="delete" directory="#expandPath( "./my_directory" )#">
```


### Rename a Directory (Tag Syntax)




```java
<bx:directory action="rename" directory="#expandPath( "./my_directory" )#" newdirectory="#expandPath( "./new_directory" )#">
```


