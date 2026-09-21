### Define application settings

Sets application-level configuration at runtime.

```java
<bx:application name="MyApp">

```

### Configure application with session settings

```java
<bx:application
    name="MyApp"
    sessionManagement="true"
    sessionTimeout="#createTimeSpan(0, 0, 30, 0)#"
>

```

### Set application datasource

```java
<bx:application
    name="MyApp"
    datasource="myDSN"
>

```
