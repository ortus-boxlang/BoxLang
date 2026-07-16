### Create a scheduled task

Creates a new task that fires an HTTP GET request against a URL on a recurring interval.

```java
<bx:schedule
    action="create"
    task="dailyReport"
    url="https://example.com/tasks/daily-report.bx"
    interval="daily"
    startTime="08:00"
>

```

### Create a task that runs a BoxLang class

Instead of firing an HTTP request, point a task at a BoxLang class with `class` (mutually
exclusive with `url`). The class is instantiated once and reused for the life of the task, and
its `run()` method (or the method named by `method`) is invoked on every fire:

```java
<bx:schedule
    action="create"
    task="dailyReport"
    class="tasks.DailyReport"
    interval="daily"
    startTime="08:00"
>

```

```java
// tasks/DailyReport.bx
class {

    function run() {
        // do the work
    }

}
```

The class may optionally define any of `before()`, `after(result)`, `onSuccess(result)`, and
`onError(exception)` — each is only invoked if defined, following the same convention as a
BoxLang scheduler class's life-cycle methods:

```java
// tasks/DailyReport.bx
class {

    function before() {
        logger.info( "About to generate the daily report" );
    }

    function run() {
        // do the work and (optionally) return a result
        return generateReport();
    }

    function onSuccess( result ) {
        logger.info( "Daily report generated: #result#" );
    }

    function onError( exception ) {
        logger.error( "Daily report failed: #exception.message#" );
    }

    function after( result ) {
        logger.info( "Daily report task finished" );
    }

}
```

A custom method name can be used instead of `run()`:

```java
<bx:schedule
    action="create"
    task="cleanup"
    class="tasks.Cleanup"
    method="purgeOldFiles"
    interval="daily"
>

```

### Create a task with cron expression

```java
<bx:schedule
    action="create"
    task="cleanup"
    url="/tasks/cleanup.bx"
    cronTime="0 0 2 * * ?"
>

```

### Create a task that runs every 5 minutes

```java
<bx:schedule
    action="create"
    task="healthCheck"
    url="/tasks/health.bx"
    interval="300"
>

```

### Update an existing task

```java
<bx:schedule
    action="update"
    task="dailyReport"
    url="/tasks/daily-report-v2.bx"
    interval="daily"
    startTime="09:00"
>

```

### Run a task immediately

```java
<bx:schedule
    action="run"
    task="dailyReport"
>

```

### Pause and resume a task

```java
<bx:schedule action="pause" task="dailyReport">
<bx:schedule action="resume" task="dailyReport">

```

### List all scheduled tasks

```java
<bx:schedule action="list" result="tasks">
<bx:dump var="#tasks#">

```

### List tasks filtered by group

```java
<bx:schedule action="list" group="reports" result="reportTasks">

```

### Pause or resume all tasks

```java
<bx:schedule action="pauseall">
<bx:schedule action="resumeall">

```

### Delete a scheduled task

```java
<bx:schedule action="delete" task="oldTask">

```
