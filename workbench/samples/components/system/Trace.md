### Add a trace message

Records debugging information visible in the runtime's trace output.

```java
<bx:trace text="Entering processOrder function" type="Information">

```

### Trace with a category

```java
<bx:trace text="Query executed in 45ms" category="database" type="Information">

```

### Trace with extra info

```java
<bx:trace
    text="User login attempt"
    category="auth"
    type="Warning"
    extrainfo="#{ username: 'john', ip: '192.168.1.1' }#"
>

```

### Trace and abort the request

```java
<bx:trace text="Fatal error encountered" type="Error" abort="true">

```

### Trace different severity levels

```java
<bx:trace text="Step 1 complete" type="Information">
<bx:trace text="Unexpected value detected" type="Warning">
<bx:trace text="Operation failed" type="Error">

```
