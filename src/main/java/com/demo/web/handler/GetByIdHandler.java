package com.demo.web.handler;

import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.util.retry.RetryBuilder;
import com.demo.exception.AppException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import brave.Span;
import brave.Tracer;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import rx.Observable;
import rx.RxReactiveStreams;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Component
public class GetByIdHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GetByIdHandler.class);
	public static final String CLASS_NAME = GetByIdHandler.class.getCanonicalName();
	
	private final Bucket dataBucket;
	private final Scheduler appWorkerScheduler;
	private final ObjectMapper jacksonObjectMapper;
	private final Tracer tracer;
	
	@Autowired
	public GetByIdHandler(final Tracer tracer) {
		this.jacksonObjectMapper = new ObjectMapper();
		jacksonObjectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		
		final String bucketName = "sample";
		final String bucketUsername = "Administrator";
		final String bucketPassword = "password";
		
		dataBucket = this.getCouchbaseBucket(bucketUsername, bucketPassword, bucketName);

        logger.info("Couchbase data bucket opened successfully.");

        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("app-worker-%d").build();
		this.appWorkerScheduler = Schedulers.fromExecutorService(Executors.newFixedThreadPool(10, threadFactory));
		
		this.tracer = tracer;
	}
	
//	@NewSpan("GetByIdHandler#handle")
	public Mono<ServerResponse> handle(final ServerRequest request) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				
				final String txPath = CLASS_NAME + "#handle";
				
				logger.info("[Span: {}][TxPath: {}][Tracer.span: {}]", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan());
				
				final Span newSpan = this.tracer.nextSpan().name("GetByIdHandler#handle");
				try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
					// This is just to find resource name and id from the request. For example,
					// if the URI is /user/101, then "user" becomes resource nameand "101" becomes id 
					final Tuple2<String, String> resourceTuple = getResourceDetailFromRequest(request);
					
					final String resourceName = resourceTuple.getT2();
			        final String id = resourceTuple.getT1();
					
			        return this.get(resourceTuple)
			        		// These logging are just for correlation purpose 
							.doOnSuccess(jsonNode -> logger.info("[Span: {}][TxPath: {}#doOnSuccess][Tracer.span: {}] Successfully fetched resource with id '{}' and name '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), id, resourceName))
							.doOnError(t -> logger.info("[Span: {}][TxPath: {}#doOnError][Tracer.span: {}] Failed to fetch resource with id '{}' and name '{}'...", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), id, resourceName, t))
			        		.flatMap(jsonNode -> ServerResponse.ok()
			        			.contentType(MediaType.APPLICATION_JSON_UTF8)
			        			.syncBody(jsonNode));
				} finally {
					newSpan.finish();
				}
			})
		;
	}
	
	public Mono<JsonNode> get(final Tuple2<String, String> resourceDetail) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				final String txPath = CLASS_NAME + "#get";
				
				logger.info("[Span: {}][TxPath: {}][Tracer.span: {}]", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan());
				
				final Span newSpan = this.tracer.nextSpan().name("ResourceService#get");
				try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
					return this.fetch(resourceDetail)
		            		.switchIfEmpty(Mono.error(new AppException("APP_404002", "No data was fetched from persistence store for the given id '" + resourceDetail.getT1() + "' and resource name '" + resourceDetail.getT2() + "'.")))
		            		.doOnSuccess(jsonNode -> logger.info("[Span: {}][TxPath: {}#doOnSuccess][Tracer.span: {}] Successfully fetched resource from persistence store for the given id '{}' and resource name '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), resourceDetail.getT1(), resourceDetail.getT2()))
		            		.doOnError(t -> logger.error("[Span: {}][TxPath: {}#doOnError][Tracer.span: {}] Failed to fetch resource from persistence store for the given id '{}' and resource name '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), resourceDetail.getT1(), resourceDetail.getT2()));
				} finally {
					newSpan.finish();
				}
			})
		;
	}
	
	public Mono<JsonNode> fetch(final Tuple2<String, String> resourceDetail) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				final String txPath = CLASS_NAME + "#fetch";
				
				logger.info("[Span: {}][TxPath: {}][Tracer.span: {}]", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan());
				
				final Span newSpan = this.tracer.nextSpan().name("ResourceDAO#fetch");
				try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
					// Document key in Couchbase = resourceName-resourceId
					final String documentKey = resourceDetail.getT2() + "-" + resourceDetail.getT1();
					
					return this.fetchByKey(dataBucket, documentKey)
							.map(this::jsonDocToJsonNode)
							// These logging are just for correlation purpose 
							.doOnSuccess(jsonNode ->  {
								if (jsonNode == null) {
									logger.error("[Span: {}][TxPath: {}#doOnSuccess][Tracer.span: {}] Failed to fetch document for resource '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), resourceDetail);
								} else {
									logger.info("[Span: {}][TxPath: {}#doOnSuccess][Tracer.span: {}] Successfully fetched document for resource '{}'", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), resourceDetail);
								}
							})
							.doOnError(t -> logger.error("[Span: {}][TxPath: {}#doOnError][Tracer.span: {}] Failed to fetch document for resource '{}'...", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), resourceDetail, t))
							;
				} finally {
					newSpan.finish();
				}
			})
		;
	}
	
	private Mono<JsonDocument> fetchByKey(final Bucket bucket, final String docKey) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				final String txPath = CLASS_NAME + "#fetchByKey";
				
				logger.info("[Span: {}][TxPath: {}][Tracer.span: {}]", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan());
				
				@SuppressWarnings("unchecked")
				final Observable<JsonDocument> fetchedJsonDocObservable = bucket.async().get(docKey)
						.retryWhen(RetryBuilder.anyOf(RequestCancelledException.class, TemporaryFailureException.class)
								.max(3)
								.delay(Delay.exponential(TimeUnit.MILLISECONDS, 30000, 0, 1))
								.doOnRetry((count, error, delay, timeUnit) -> logger.error("[Span: {}][TxPath: {}][Tracer.span: {}] An error occurred while fetching the document. [Document key={}][Retry iteration: {}][Time with delay: {}]...", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), docKey, count, delay, error))
								.build()
							)
					.doOnError(t -> logger.error("[Span: {}][TxPath: {}][Tracer.span: {}] Exception while fetching document with key '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), docKey))
					.onErrorReturn(e -> null)
				;
				
				final Span newSpan = this.tracer.nextSpan().name("CouchbaseUtils#fetchByKey");
				try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
					return Mono.from(RxReactiveStreams.toPublisher(fetchedJsonDocObservable))
							.publishOn(this.appWorkerScheduler)
							.doOnError(t -> logger.info("[Span: {}][TxPath: {}#doOnError][Tracer.span: {}] Exception: ", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), t))
							.doOnSuccess(jsonDoc -> logger.info("[Span: {}][TxPath: {}#doOnSuccess][Tracer.span: {}]", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan()));
				} finally {
					newSpan.finish();
				}
			})
		;
	}
	
	private JsonNode jsonDocToJsonNode(final JsonDocument jsonDoc) {
		if(jsonDoc == null || jsonDoc.content() == null) {
			return null;
		}
		
		final String txPath = CLASS_NAME + "#jsonDocToJsonNode";
        
        final JsonObject content = jsonDoc.content();
        final String stringContent = Objects.toString(content);
        
        try {
            final JsonNode jsonNode = jacksonObjectMapper.readTree(stringContent);

            logger.debug("[TxPath: {}] Successfully converted Couchbase JsonDocument to JsonNode", txPath);

            return jsonNode;
        } catch (final Exception e) {
            logger.debug("Exception when reading Couchbase JsonDocument: '{}' as JsonNode", jsonDoc);
            logger.error("Exception when reading Couchbase JsonDocument as JsonNode, document key: '{}'", jsonDoc.id(), e);

            throw new RuntimeException(e);
        }
	}
	
	private Bucket getCouchbaseBucket(final String bucketUserName, final String bucketUserPassword, final String bucketName) {
		final Cluster cluster = this.getCouchbaseCluster(bucketUserName, bucketUserPassword);
		try {
			return cluster.openBucket(bucketName);
		} catch (final Exception e) {
		    logger.error("Exception while opening bucket '{}'...", bucketName, e);

            throw new IllegalStateException("Failed to obtain the couchbase bucket; bucket name: " + bucketName
                    + "; user name: " + bucketUserName, e);
		}
	}

	private Cluster getCouchbaseCluster(final String authUserName, final String authPassword) {
		final DefaultCouchbaseEnvironment couchbaseEnv = DefaultCouchbaseEnvironment.builder()
				.connectTimeout(TimeUnit.SECONDS.toMillis(30))
				.kvTimeout(TimeUnit.SECONDS.toMillis(30))
				.queryTimeout(TimeUnit.SECONDS.toMillis(30))
				.keepAliveErrorThreshold(1)
				.continuousKeepAliveEnabled(true)
				.keepAliveInterval(10000)
				.keepAliveTimeout(30000)
				.build();

		// Create an instance of cluster and authenticate with the cluster.
		final Cluster cbCluster = CouchbaseCluster
		        .create(couchbaseEnv, "localhost")
				.authenticate(authUserName, authPassword);

		logger.info("Authentication with Couchbase cluster was successful.");
		return cbCluster;
	}
	
	private Tuple2<String, String> getResourceDetailFromRequest(final ServerRequest request) {
		final String requestPath = request.path();
		final String[] pathElementsArray = StringUtils.split(requestPath, "/");
		final int pathElementCount = pathElementsArray.length;
		
		if (pathElementCount <= 0) {
			throw new AppException("APP_400003", "Resource ID and name cannot be determined from request path '" + requestPath + "'.");
		}
		
		if (pathElementCount >= 2) {
			return Tuples.of(pathElementsArray[pathElementCount - 1], pathElementsArray[pathElementCount - 2]);
		} else {
			return Tuples.of(StringUtils.EMPTY, pathElementsArray[0]); // Empty string is a place holder.
		}
		
	}
}
