### cfparam in CFML

A very basic CFML cfparam example


```java
<bx:param name="userID" default="0"/>
```

Result: 

### cfparam in cfscript

A very basic cfscript cfparam example

<a href="https://try.boxlang.io/?code=eJxLqrAqSCxKzFXIS8xNtVUqLU4t8nRRUkhJTUsszSmxNbDmSiKoAgArtxbV" target="_blank">Run Example</a>

```java
bx:param name="userID" default=0;
bx:param name="userID" default=0;

```

Result: 

### Tag syntax using a regex

Throws an error if the value is not one of a list of possible values


```java
<bx:param name="sortdir" default="ASC" type="regex" pattern="ASC|DESC"/>
```

Result: 

