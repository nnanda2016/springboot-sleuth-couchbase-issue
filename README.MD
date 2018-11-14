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


**GET-by-ID (ID exists)**
```
curl -v -H "From:1234" -H "Accept:application/json;charset=UTF-8" -H "X-Client-Trace-Id:get-test-12345" http://localhost:5002/user/1
```
Sample log
```
2018-11-14 11:30:41.120 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][StartTimestamp: 1542223841114][Scope=HTTP_REQ][URL=http://localhost:5002/user/1][HttpMethod=GET][User-Agent=PostmanRuntime/7.4.0][Accept=application/json;charset=UTF-8][Content-Type=text/plain][From=1234][X-Client-Trace-Id=Local-12345][X-Server-Trace-Id=b26ab9db56fa9822]
2018-11-14 11:30:41.196 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath: com.demo.web.handler.GetByIdHandler#handle][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)]
2018-11-14 11:30:41.218 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath: com.demo.web.handler.GetByIdHandler#get][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)]
2018-11-14 11:30:41.240 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath: com.demo.web.handler.GetByIdHandler#fetch][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)]
2018-11-14 11:30:41.260 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath: com.demo.web.handler.GetByIdHandler#fetchByKey][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)]
2018-11-14 11:30:41.324 [Thread: app-worker-0] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath: com.demo.web.handler.GetByIdHandler#fetchByKey#doOnSuccess][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)]
2018-11-14 11:30:41.359 [Thread: app-worker-0] [DEBUG] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [TxPath: com.demo.web.handler.GetByIdHandler#jsonDocToJsonNode] Successfully converted Couchbase JsonDocument to JsonNode
2018-11-14 11:30:41.359 [Thread: app-worker-0] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath: com.demo.web.handler.GetByIdHandler#fetch#doOnSuccess][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)] Successfully fetched document for resource '[1,user]'
2018-11-14 11:30:41.359 [Thread: app-worker-0] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath: com.demo.web.handler.GetByIdHandler#get#doOnSuccess][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)] Successfully fetched resource from persistence store for the given id '1' and resource name 'user'.
2018-11-14 11:30:41.360 [Thread: app-worker-0] [INFO] ServerTraceId: b26ab9db56fa9822 SpanId: b26ab9db56fa9822 SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath: com.demo.web.handler.GetByIdHandler#handle#doOnSuccess][Tracer.span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)] Successfully fetched resource with id '1' and name 'user'.
2018-11-14 11:30:41.441 [Thread: reactor-http-nio-3] [INFO] ServerTraceId:  SpanId:  SpringAppProfile: local  [Span: RealSpan(b26ab9db56fa9822/b26ab9db56fa9822)][TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][Tracer.span: null][EndTimestamp: 1542223841441][Scope=HTTP_RES][Location=<NA>][Content-Type=application/json;charset=UTF-8][Content-Length=202][X-Server-Trace-Id=b26ab9db56fa9822]


```
**GET-by-ID (ID does not exist)**
```
curl -v -H "From:1234" -H "Accept:application/json;charset=UTF-8" -H "X-Client-Trace-Id:get-test-99011" http://localhost:5002/user/1000001
```
Sample log (I have skipped the stack trace)
```
2018-11-14 11:31:37.120 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: ca6490e9adc36b98 SpanId: ca6490e9adc36b98 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][Tracer.span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][StartTimestamp: 1542223897119][Scope=HTTP_REQ][URL=http://localhost:5002/user/1001][HttpMethod=GET][User-Agent=PostmanRuntime/7.4.0][Accept=application/json;charset=UTF-8][Content-Type=text/plain][From=1234][X-Client-Trace-Id=Local-12345][X-Server-Trace-Id=ca6490e9adc36b98]
2018-11-14 11:31:37.173 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: ca6490e9adc36b98 SpanId: ca6490e9adc36b98 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.web.handler.GetByIdHandler#handle][Tracer.span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)]
2018-11-14 11:31:37.187 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: ca6490e9adc36b98 SpanId: ca6490e9adc36b98 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.web.handler.GetByIdHandler#get][Tracer.span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)]
2018-11-14 11:31:37.204 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: ca6490e9adc36b98 SpanId: ca6490e9adc36b98 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.web.handler.GetByIdHandler#fetch][Tracer.span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)]
2018-11-14 11:31:37.223 [Thread: reactor-http-nio-3] [INFO] ServerTraceId: ca6490e9adc36b98 SpanId: ca6490e9adc36b98 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.web.handler.GetByIdHandler#fetchByKey][Tracer.span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)]
2018-11-14 11:31:37.243 [Thread: app-worker-1] [INFO] ServerTraceId: 776c46893a498de5 SpanId: 776c46893a498de5 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.web.handler.GetByIdHandler#fetchByKey#doOnSuccess][Tracer.span: RealSpan(776c46893a498de5/776c46893a498de5)]
2018-11-14 11:31:37.243 [Thread: app-worker-1] [ERROR] ServerTraceId: 776c46893a498de5 SpanId: 776c46893a498de5 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.web.handler.GetByIdHandler#fetch#doOnSuccess][Tracer.span: RealSpan(776c46893a498de5/776c46893a498de5)] Failed to fetch document for resource '[1001,user]'.
2018-11-14 11:31:37.249 [Thread: app-worker-1] [ERROR] ServerTraceId: 776c46893a498de5 SpanId: 776c46893a498de5 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.web.handler.GetByIdHandler#get#doOnError][Tracer.span: RealSpan(776c46893a498de5/776c46893a498de5)] Failed to fetch resource from persistence store for the given id '1001' and resource name 'user'.
2018-11-14 11:31:37.250 [Thread: app-worker-1] [INFO] ServerTraceId: 776c46893a498de5 SpanId: 776c46893a498de5 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.web.handler.GetByIdHandler#handle#doOnError][Tracer.span: RealSpan(776c46893a498de5/776c46893a498de5)] Failed to fetch resource with id '1001' and name 'user'...
2018-11-14 11:31:37.250 [Thread: app-worker-1] [INFO] ServerTraceId: 776c46893a498de5 SpanId: 776c46893a498de5 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][Tracer.span: RealSpan(776c46893a498de5/776c46893a498de5)][EndTimestamp: 1542223897250][Scope=HTTP_RES][Location=<NA>][Content-Type=<NA>][Content-Length=<NA>][X-Server-Trace-Id=ca6490e9adc36b98]
2018-11-14 11:31:37.283 [Thread: app-worker-1] [INFO] ServerTraceId: ca6490e9adc36b98 SpanId: ca6490e9adc36b98 SpringAppProfile: local  [Span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)][TxPath: com.demo.exception.GlobalExceptionHandler#renderErrorResponse][Tracer.span: RealSpan(ca6490e9adc36b98/ca6490e9adc36b98)]

```
