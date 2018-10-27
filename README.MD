This application demonstrates the issue with Spring Cloud Sleuth. Summary of issues:
1. The issue is occurring both in case of `@NewSpan` annotation and programmatic way of creating spans.
2. For success cases, when the response is written the `traceId` and `spanId` are missing in SLF4J's MDC.
3. Whenever there's an exception, Sleuth creates a new span. This breaks the continuity of trace and span. ZipKin UI shows these separate spans.


#### Install Couchbase Locally
To run this app, we need Couchbase which is supplied as part of this application. Following commands will help running fully configured CB which can be used for this app. 

```
cd cb-setup/example
docker-compose up -d
```
You can verify the CB installation by opening URL http://localhost:8091 

#### Run the App
I have tested this app in JDK 8. To run the app, use following command. The log files are generated under `./build/logs` folder.

```
./gradlew clean build bootRun
```

#### Testing
I have tested this app in JDK 8. Use following commands from the base of this project.


**GET-by-ID (Success case)**
```
curl -v -H "From:1234" -H "Accept:application/json;charset=UTF-8" -H "X-Client-Trace-Id:get-test-12345" http://localhost:5002/user/1
```
Sample log
```
2018-10-27 00:09:19.300 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][StartTimestamp: 1540624159289][Scope=HTTP_REQ][URL=http://localhost:5002/user/1][HttpMethod=GET][User-Agent=PostmanRuntime/7.3.0][Accept=application/json;charset=UTF-8][Content-Type=<NA>][From=1234][X-Client-Trace-Id=Local-12345][X-Server-Trace-Id=f08f86f829cf8904]
2018-10-27 00:09:19.402 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath: com.demo.web.handler.GetByIdHandler#handle][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)]
2018-10-27 00:09:19.425 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath: com.demo.service.ResourceService#get][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)]
2018-10-27 00:09:19.442 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath: com.demo.dao.ResourceDAO#fetch][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)]
2018-10-27 00:09:19.463 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath: com.demo.dao.ResourceDAO#fetchByKey][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)]
2018-10-27 00:09:19.528 [Thread: app-worker-0] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath: com.demo.dao.ResourceDAO#fetchByKey#doOnSuccess][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)]
2018-10-27 00:09:19.560 [Thread: app-worker-0] [DEBUG] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [TxPath: com.demo.dao.ResourceDAO#jsonDocToJsonNode] Successfully converted Couchbase JsonDocument to JsonNode
2018-10-27 00:09:19.561 [Thread: app-worker-0] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath: com.demo.dao.ResourceDAO#fetch#doOnSuccess][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)] Successfully fetched document for resource 'ResourceDetail {resourceId=1, resourceName=user}'
2018-10-27 00:09:19.561 [Thread: app-worker-0] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath: com.demo.service.ResourceService#get#doOnSuccess][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)] Successfully fetched resource from persistence store for the given id '1' and resource name 'user'.
2018-10-27 00:09:19.561 [Thread: app-worker-0] [INFO] ServerTraceId: f08f86f829cf8904 SpanId: f08f86f829cf8904 SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath: com.demo.web.handler.GetByIdHandler#handle#doOnSuccess][Tracer.span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)] Successfully fetched resource with id '1' and name 'user'.
2018-10-27 00:09:19.633 [Thread: reactor-http-nio-3] [INFO] ServerTraceId:  SpanId:  SpringAppProfile: local  [Span: RealSpan(f08f86f829cf8904/f08f86f829cf8904)][TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][Tracer.span: null][EndTimestamp: 1540624159633][Scope=HTTP_RES][Location=<NA>][Content-Type=application/json;charset=UTF-8][Content-Length=200][X-Server-Trace-Id=f08f86f829cf8904]

```
**GET-by-ID (Failure case)**
```
curl -v -H "From:1234" -H "Accept:application/json;charset=UTF-8" -H "X-Client-Trace-Id:get-test-12345" http://localhost:5002/user/1000001
```
Sample log
```
2018-10-27 00:10:59.214 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: 651bd326507ec3ca SpanId: 651bd326507ec3ca SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][Tracer.span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][StartTimestamp: 1540624259214][Scope=HTTP_REQ][URL=http://localhost:5002/user/100001][HttpMethod=GET][User-Agent=PostmanRuntime/7.3.0][Accept=application/json;charset=UTF-8][Content-Type=<NA>][From=1234][X-Client-Trace-Id=Local-12345][X-Server-Trace-Id=651bd326507ec3ca]
2018-10-27 00:10:59.327 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: 651bd326507ec3ca SpanId: 651bd326507ec3ca SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.web.handler.GetByIdHandler#handle][Tracer.span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)]
2018-10-27 00:10:59.347 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: 651bd326507ec3ca SpanId: 651bd326507ec3ca SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.service.ResourceService#get][Tracer.span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)]
2018-10-27 00:10:59.367 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: 651bd326507ec3ca SpanId: 651bd326507ec3ca SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.dao.ResourceDAO#fetch][Tracer.span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)]
2018-10-27 00:10:59.389 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: 651bd326507ec3ca SpanId: 651bd326507ec3ca SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.dao.ResourceDAO#fetchByKey][Tracer.span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)]
2018-10-27 00:10:59.417 [Thread: app-worker-1] [INFO] ServerTraceId: 831f535e0c1495c6 SpanId: 831f535e0c1495c6 SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.dao.ResourceDAO#fetchByKey#doOnSuccess][Tracer.span: RealSpan(831f535e0c1495c6/831f535e0c1495c6)]
2018-10-27 00:10:59.417 [Thread: app-worker-1] [ERROR] ServerTraceId: 831f535e0c1495c6 SpanId: 831f535e0c1495c6 SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.dao.ResourceDAO#fetch#doOnSuccess][Tracer.span: RealSpan(831f535e0c1495c6/831f535e0c1495c6)] Failed to fetch document for resource 'ResourceDetail {resourceId=100001, resourceName=user}'.
2018-10-27 00:10:59.427 [Thread: app-worker-1] [ERROR] ServerTraceId: 831f535e0c1495c6 SpanId: 831f535e0c1495c6 SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.service.ResourceService#get#doOnError][Tracer.span: RealSpan(831f535e0c1495c6/831f535e0c1495c6)] Failed to fetch resource from persistence store for the given id '100001' and resource name 'user'.
2018-10-27 00:10:59.428 [Thread: app-worker-1] [INFO] ServerTraceId: 831f535e0c1495c6 SpanId: 831f535e0c1495c6 SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.web.handler.GetByIdHandler#handle#doOnError][Tracer.span: RealSpan(831f535e0c1495c6/831f535e0c1495c6)] Failed to fetch resource with id '100001' and name 'user'...

Error has been observed by the following operator(s):
	|_	Mono.error ⇢ com.demo.service.ResourceService.lambda$get$2(ResourceService.java:51)
	|_	Mono.switchIfEmpty ⇢ com.demo.service.ResourceService.lambda$get$2(ResourceService.java:51)
	|_	Mono.doOnSuccess ⇢ com.demo.service.ResourceService.lambda$get$2(ResourceService.java:52)
	|_	Mono.doOnError ⇢ com.demo.service.ResourceService.lambda$get$2(ResourceService.java:56)
	|_	Mono.flatMap ⇢ com.demo.service.ResourceService.get(ResourceService.java:43)
	|_	Mono.doOnSuccess ⇢ com.demo.web.handler.GetByIdHandler.lambda$handle$4(GetByIdHandler.java:62)
	|_	Mono.doOnError ⇢ com.demo.web.handler.GetByIdHandler.lambda$handle$4(GetByIdHandler.java:63)
	|_	Mono.flatMap ⇢ com.demo.web.handler.GetByIdHandler.lambda$handle$4(GetByIdHandler.java:64)
	|_	Mono.flatMap ⇢ com.demo.web.handler.GetByIdHandler.handle(GetByIdHandler.java:46)
	|_	Mono.map ⇢ org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter.handle(HandlerFunctionAdapter.java:62)
	|_	Mono.flatMap ⇢ org.springframework.web.reactive.DispatcherHandler.handle(DispatcherHandler.java:160)

2018-10-27 00:10:59.431 [Thread: app-worker-1] [INFO] ServerTraceId: 831f535e0c1495c6 SpanId: 831f535e0c1495c6 SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][Tracer.span: RealSpan(831f535e0c1495c6/831f535e0c1495c6)][EndTimestamp: 1540624259431][Scope=HTTP_RES][Location=<NA>][Content-Type=<NA>][Content-Length=<NA>][X-Server-Trace-Id=651bd326507ec3ca]
2018-10-27 00:10:59.478 [Thread: app-worker-1] [INFO] ServerTraceId: 651bd326507ec3ca SpanId: 651bd326507ec3ca SpringAppProfile: local  [Span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)][TxPath: com.demo.exception.GlobalExceptionHandler#renderErrorResponse][Tracer.span: RealSpan(651bd326507ec3ca/651bd326507ec3ca)]

```
