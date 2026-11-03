# Query execution observation

ON_QUERY_EXECUTE_ERROR is announced when a PendingQuery statement execution fails. It supplies pendingQuery, context, exception, and elapsed executionTime in milliseconds. The original RuntimeException is rethrown unchanged, even if an observer throws. It pairs with preQueryExecute by PendingQuery identity, allowing telemetry to close a failed execution even when an application catches the database exception. PRE before connection acquisition/build does not exist; consumers requiring that coverage should wrap the query boundary.

preQueryExecute now includes dbtype and cached=false for statement execution. Cached results announce preQueryExecute/postQueryExecute with the same pendingQuery, cached=true, and executionTime=0, without a new database round trip. Existing cached-result metadata and returned result behavior remain unchanged.

Focused SQLErrorHandlingTest checks original exception identity, context, one failure announcement, elapsed time, observer failure isolation, and cached event correlation. It adds no new DDL. Runtime deployment/compatibility matrices are separate from the source-build tests.
