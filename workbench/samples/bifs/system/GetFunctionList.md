### Check to see if a function exists

Uses the keyExists member function on the returned struct.

```java
writeOutput( getFunctionList().keyExists( "reMatch" ) )
```

Result: true

### Inspect a function entry

```java
entry = getFunctionList().reMatch
writeOutput( entry.keyList() )
```

Result: module,namespace,isGlobal,className,arguments (the key order may vary)
