### bx:param in BL

A very basic BL bx:param example


```java
<bx:param name="userID" default="0"/>
```


### bx:param in script

A very basic script bx:param example

<a href="https://try.boxlang.io/?code=eJxLqrAqSCxKzFXIS8xNtVUqLU4t8nRRUkhJTUsszSmxNbDmSiKoAgArtxbV" target="_blank">Run Example</a>

```java
bx:param name="userID" default=0;
bx:param name="userID" default=0;

```


### Tag syntax using a regex

Throws an error if the value is not one of a list of possible values


```java
<bx:param name="sortdir" default="ASC" type="regex" pattern="ASC|DESC"/>
```


