### Create a scheduled task

Creates a new task that fires an HTTP URL on a recurring interval.

```java
<bx:schedule
    action="create"
    task="dailyReport"
    url="/tasks/daily-report.bx"
    interval="daily"
    startTime="08:00"
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
