### Tag version




```java
<bx:timer label="Nap time" type="inline">
Begin some long running process ...
<bx:set sleep( 2000 ) >
done.
</bx:timer>
```

Result: The time elapsed while executing the code inside the <cftimer> block should be displayed inline.

### Script version



<a href="https://try.boxlang.io/?code=eJxVjEEKwjAQRdfNKT6zqpsQXCrdeADv0OpQAulkSCaoiHc3dufq8x6PvzxPFjcuSPPCaaLrrPgJgr2UJ8rNUpSObzc8SjTuQpuNoAuvUVDzxkhZVpQmEvtqyTeuFd57EA5nN9TErCOOIYSd%2F3%2FuWdjv4cd9AXLfLXM%3D" target="_blank">Run Example</a>

```java
bx:timer label="Nap time" type="outline" {
	writeoutput( "Begin some long running process ... " );
	sleep( 2000 );
	writeoutput( "done." );
}

```

Result: The time elapsed while executing the code inside the cftimer block should be displayed in the output with an outline around any output generated within the cftimer call..

