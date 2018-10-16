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
To run the app, use

```
./gradlew clean build bootRun
```


