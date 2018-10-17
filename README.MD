This application demonstrates the issue with Spring Cloud Sleuth. Summary of issues:
1. The issues are occurring both in case of `@NewSpan` annotation and programmatic way of creating spans.
2. For Get-by-ID success case, `spanId` is lost in the filter which intercepts the response.
3. For most of other cases, the continuity of trace and span is lost. Even ZipKin UI reports two separate spans.

**Install Couchbase Locally**
To run this app, we need Couchbase. I tried with a local in-JVM key-value store for data, but could not replicate this issue. Looks like remote invocations are causing such issues. I have used Docker to install couchbase locally.

```
docker run -d --name db -p 8091-8094:8091-8094 -p 11210:11210 couchbase
docker logs db
```
Once CB starts, open `http://localhost:8091` and setup 
- a demo cluster (with default settings)
- a demo bucket with name `DEMO1`
- a demo bucket admin (under `Securities` tab) with name `bucket-admin` and password `demo@123`. 

These settings are used in `application.yml`, so, if you are changing these, please update your local `application.yml`.

**Run the App**
I have tested this app in JDK 8. To run the app, use following command. The log files are generated under `./build/logs` folder.

```
./gradlew clean build bootRun
```

**Testing**
I have tested this app in JDK 8. Use following commands from the base of this project.

**PUT-with-ID (Success case)**
```
curl -v -X PUT -H "From:1234" -H "Accept:application/json;charset=UTF-8" -H "Content-Type:application/json;charset=UTF-8" -H "X-Client-Trace-Id:put-test-12345" -d @data/u1001.json http://localhost:5002/users/u1001
```

Sample Log
```
2018-10-16 17:05:51.827 [Thread: reactor-http-nio-6] [INFO] ServerTraceId: 803ee94a65b71fb3 SpanId: 803ee94a65b71fb3 SpringAppProfile: local  [TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][StartTimestamp: 1539734751827][Scope=HTTP_REQ][URL=http://localhost:5002/users/u1001][HttpMethod=PUT][User-Agent=curl/7.58.0][Accept=application/json;charset=UTF-8][Content-Type=application/json;charset=UTF-8][From=1234][X-Client-Trace-Id=put-test-12345][X-Server-Trace-Id=803ee94a65b71fb3]
2018-10-16 17:05:52.030 [Thread: app-worker-1] [ERROR] ServerTraceId: 2a09385f5153884a SpanId: 2a09385f5153884a SpringAppProfile: local  [TxPath: com.demo.dao.ResourceDAO#fetch] Failed to fetch document for resource 'ResourceDetail {resourceId=u1001, resourceName=users}'.
2018-10-16 17:05:52.057 [Thread: app-worker-1] [ERROR] ServerTraceId: 803ee94a65b71fb3 SpanId: 613d1b6ece4fef41 SpringAppProfile: local  [TxPath: com.demo.dao.CouchbaseUtils#createOrReplace] DocumentDoesNotExistException when trying to replace the document by key 'users-u1001', trying to insert the document.
2018-10-16 17:05:52.063 [Thread: app-worker-1] [INFO] ServerTraceId: 803ee94a65b71fb3 SpanId: a7e272aa3041caef SpringAppProfile: local  [TxPath: com.demo.dao.CouchbaseUtils#insert] Document with key 'users-u1001' was inserted successfully.
2018-10-16 17:05:52.067 [Thread: app-worker-1] [DEBUG] ServerTraceId: 803ee94a65b71fb3 SpanId: b39cbaa634b2408f SpringAppProfile: local  [TxPath: com.demo.dao.CouchbaseUtils#jsonDocToJsonNode] Successfully converted Couchbase JsonDocument to JsonNode
2018-10-16 17:05:52.073 [Thread: app-worker-1] [INFO] ServerTraceId: 803ee94a65b71fb3 SpanId: 803ee94a65b71fb3 SpringAppProfile: local  [TxPath: com.demo.service.ResourceService#createOrReplace]Successfully created resource in persistence store for the given id 'u1001' and resource name 'users'.
2018-10-16 17:05:52.073 [Thread: app-worker-1] [INFO] ServerTraceId: 803ee94a65b71fb3 SpanId: 803ee94a65b71fb3 SpringAppProfile: local  Successfully 'Replaced' resource.
2018-10-16 17:05:52.093 [Thread: reactor-http-nio-6] [INFO] ServerTraceId:  SpanId:  SpringAppProfile: local  [TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][EndTimestamp: 1539734752093][Scope=HTTP_RES][Location=http://localhost:5002/users/u1001][Content-Type=application/json;charset=UTF-8][Content-Length=206][X-Server-Trace-Id=803ee94a65b71fb3]

```

**GET-by-ID (Failure case)**
```
curl -v -H "From:1234" -H "Accept:application/json;charset=UTF-8" -H "X-Client-Trace-Id:get-test-12345" http://localhost:5002/users/u1010
```
Sample log
```
2018-10-16 16:58:00.251 [Thread: reactor-http-nio-4] [INFO] ServerTraceId: 015223e43053cf5e SpanId: 015223e43053cf5e SpringAppProfile: local  [TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][StartTimestamp: 1539734280251][Scope=HTTP_REQ][URL=http://localhost:5002/users/u1010][HttpMethod=GET][User-Agent=curl/7.58.0][Accept=application/json;charset=UTF-8][Content-Type=<NA>][From=1234][X-Client-Trace-Id=Local-12345][X-Server-Trace-Id=015223e43053cf5e]
2018-10-16 16:58:00.412 [Thread: app-worker-0] [ERROR] ServerTraceId: a5de9d0f54903781 SpanId: a5de9d0f54903781 SpringAppProfile: local  [TxPath: com.demo.dao.ResourceDAO#fetch] Failed to fetch document for resource 'ResourceDetail {resourceId=u1010, resourceName=users}'.
2018-10-16 16:58:00.415 [Thread: app-worker-0] [ERROR] ServerTraceId: a5de9d0f54903781 SpanId: a5de9d0f54903781 SpringAppProfile: local  [TxPath: com.demo.service.ResourceService#get] No data was fetched from persistence store for the given id 'u1010' and resource name 'users'.
2018-10-16 16:58:00.417 [Thread: app-worker-0] [INFO] ServerTraceId: a5de9d0f54903781 SpanId: a5de9d0f54903781 SpringAppProfile: local  [TxPath: com.demo.web.handler.GetByIdHandler#handle] Failed to fetch resource with id 'u1010' and name 'users'...
2018-10-16 16:58:00.421 [Thread: app-worker-0] [INFO] ServerTraceId: a5de9d0f54903781 SpanId: a5de9d0f54903781 SpringAppProfile: local  [TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][EndTimestamp: 1539734280421][Scope=HTTP_RES][Location=<NA>][Content-Type=<NA>][Content-Length=<NA>][X-Server-Trace-Id=015223e43053cf5e]
```

**DELETE (Failure case)**
```
curl -v -X DELETE -H "From:1234" -H "Accept:application/json;charset=UTF-8" -H "X-Client-Trace-Id:delete-test-12345" http://localhost:5002/users/u1010
```

Sample log
```
2018-10-16 17:07:59.895 [Thread: reactor-http-nio-7] [INFO] ServerTraceId: cd805d7a0a6ff13d SpanId: cd805d7a0a6ff13d SpringAppProfile: local  [TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][StartTimestamp: 1539734879895][Scope=HTTP_REQ][URL=http://localhost:5002/users/u1010][HttpMethod=DELETE][User-Agent=curl/7.58.0][Accept=application/json;charset=UTF-8][Content-Type=<NA>][From=1234][X-Client-Trace-Id=delete-test-12345][X-Server-Trace-Id=cd805d7a0a6ff13d]
2018-10-16 17:07:59.979 [Thread: app-worker-2] [ERROR] ServerTraceId: c31a366be52e4432 SpanId: c31a366be52e4432 SpringAppProfile: local  [TxPath: com.demo.dao.CouchbaseUtils#asyncDelete] Failed to delete document with key 'users-u1010' from bucket 'DEMO1'.
2018-10-16 17:07:59.981 [Thread: app-worker-2] [ERROR] ServerTraceId: c31a366be52e4432 SpanId: c31a366be52e4432 SpringAppProfile: local  [TxPath: com.demo.dao.ResourceDAO#delete] Failed to delete document with id 'u1010'...
2018-10-16 17:07:59.986 [Thread: app-worker-2] [INFO] ServerTraceId: c31a366be52e4432 SpanId: c31a366be52e4432 SpringAppProfile: local  [TxPath=com.demo.web.filter.HttpGatewayTracingFilter#filter][EndTimestamp: 1539734879986][Scope=HTTP_RES][Location=<NA>][Content-Type=<NA>][Content-Length=<NA>][X-Server-Trace-Id=cd805d7a0a6ff13d]

```
