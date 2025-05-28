### Simple iIf Example



<a href="https://try.boxlang.io/?code=eJzL9EzTUChOLSpLLdLzD9bzc%2FR1VbC1VVBySkzOz1PSUUhJ1VBQCirNy8vMS1cACyr4ByspaEJl%2FPJLFLDIKmhacwEAwp8Z9w%3D%3D" target="_blank">Run Example</a>

```java
iIf( server.OS.NAME == "Bacon", de( "Running Bacon OS" ), de( "Not Running Bacon OS" ) );
```

Result: Not Running Bacon OS

### Simple Example using Ternary Operator Instead

Instead of using iif, you should use the ternary operator

<a href="https://try.boxlang.io/?code=eJzT0ChOLSpLLdLzD9bzc%2FR1VbC1VVBySkzOz1PSVLBXUAoqzcvLzEtXAAsp%2BAcrKVgpKPnllyhgSGhacwEAduEWnQ%3D%3D" target="_blank">Run Example</a>

```java
((server.OS.NAME == "Bacon") ? "Running Bacon OS" : "Not Running Bacon OS");

```

Result: Not Running Bacon OS

