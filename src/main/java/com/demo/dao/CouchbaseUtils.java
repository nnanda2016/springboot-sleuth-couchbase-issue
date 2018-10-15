package com.demo.dao;

import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.transcoder.JsonTranscoder;
import com.couchbase.client.java.util.retry.RetryBuilder;
import com.demo.config.CouchbaseConfigProps;
import com.demo.util.BeanNames;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
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
@Component(BeanNames.COUCHBASE_UTILS)
public class CouchbaseUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(CouchbaseUtils.class);
	public static final String CLASS_NAME = CouchbaseUtils.class.getCanonicalName();
	
	private static final String DELETE_BY_KEY_FAILED_MESSAGE = "[TxPath: {}] Failed to delete document with key '{}' from bucket '{}'.";
    private static final String DELETE_BY_KEY_SUCCESS_MESSAGE = "[TxPath: {}] Successfully deleted document for key '{}' from bucket '{}'.";
    
    public static final Predicate<JsonDocument> NON_NULL_JSON_DOC = jsonDocument -> Objects.nonNull(jsonDocument) && Objects.nonNull(jsonDocument.content());
	
	private final ObjectMapper jacksonObjectMapper;
	private final CouchbaseConfigProps couchbaseConfigProperties;
	
	private final JsonTranscoder jsonTranscoder;
	private final Tuple2<Integer, Long> retryTuple;
	
	private final Scheduler appWorkerScheduler;
	
	@Autowired
	public CouchbaseUtils(
			final CouchbaseConfigProps couchbaseConfigProperties,
			@Qualifier(BeanNames.APP_WORKER_THREADPOOL_EXECUTOR) final ExecutorService appWorkerThreadPool,
			@Qualifier(BeanNames.JACKSON_OBJECT_MAPPER) final ObjectMapper objectMapper) {
		this.couchbaseConfigProperties = couchbaseConfigProperties;
		this.jacksonObjectMapper = objectMapper;
		this.jsonTranscoder = new JsonTranscoder();
		this.retryTuple = this.getRetryTuple();
		this.appWorkerScheduler = Schedulers.fromExecutorService(appWorkerThreadPool);
	}
	
	@NewSpan(name = "CouchbaseUtils#fetchByKey")
	public Mono<JsonDocument> fetchByKey(final Bucket bucket, final String docKey) {
    	
    	final String txPath = CLASS_NAME + "#fetchByKey";
    	
    	@SuppressWarnings("unchecked")
		final Observable<JsonDocument> fetchedJsonDocObservable = bucket.async().get(docKey)
				.retryWhen(RetryBuilder.anyOf(RequestCancelledException.class, TemporaryFailureException.class)
						.max(this.retryTuple.getT1())
						.delay(Delay.exponential(TimeUnit.MILLISECONDS, this.retryTuple.getT2(), 0, 1))
						.doOnRetry((count, error, delay, timeUnit) -> {
							logger.error("[TxPath: {}] An error occurred while fetching the document. [Document key={}][Retry iteration: {}][Time with delay: {}]...", txPath, docKey, count, delay, error);
						})
						.build()
					)
			.doOnError(t -> logger.error("[TxPath: {}] Exception while fetching document with key '{}'.", txPath, docKey))
			.onErrorReturn(e -> null)
		;
		
		return Mono.from(RxReactiveStreams.toPublisher(fetchedJsonDocObservable)).publishOn(this.appWorkerScheduler);
	}
	
	@NewSpan(name = "CouchbaseUtils#fetchMultiple")
	public Flux<JsonNode> fetchMultiple(final Bucket bucket, final List<String> docKeys) {
		final String txPath = CLASS_NAME + "#fetchMultiple";
    	
		@SuppressWarnings("unchecked")
		final Observable<JsonNode> jsonNodeObservable = Observable.from(docKeys)
			.flatMap(docKey -> bucket.async().get(docKey)
					.retryWhen(RetryBuilder.anyOf(RequestCancelledException.class, TemporaryFailureException.class)
										.max(this.retryTuple.getT1())
										.delay(Delay.exponential(TimeUnit.MILLISECONDS, this.retryTuple.getT2(), 0, 1))
										.doOnRetry((count, error, delay, timeUnit) -> {
											logger.error("[TxPath: {}] An error occurred while fetching the document. [Document key={}][Retry iteration: {}][Time with delay: {}]...", txPath, docKey, count, delay, error);
										})
										.build()
					)
					.doOnError(t -> {
						logger.error("[TxPath: {}] Exception while fetching document with id '{}'...", txPath, docKey, t);
					})
					.onErrorReturn(t -> null)
					.filter(NON_NULL_JSON_DOC::test)
					.map(this::jsonDocToJsonNode)
			)
			;
		
		return Flux.from(RxReactiveStreams.toPublisher(jsonNodeObservable)).publishOn(this.appWorkerScheduler);
	}
	
	@SuppressWarnings("unchecked")
	public Mono<JsonDocument> asyncDelete(final Bucket bucket, final String docKey) {
		final String bucketName = bucket.name();
		
		final String txPath = CLASS_NAME + "#asyncDelete";
    	
		final Observable<JsonDocument> deletedDocObservable = bucket.async().remove(docKey)
			.retryWhen(RetryBuilder.anyOf(RequestCancelledException.class, TemporaryFailureException.class)
				.max(this.retryTuple.getT1())
				.delay(Delay.exponential(TimeUnit.MILLISECONDS, this.retryTuple.getT2(), 0, 1))
				.doOnRetry((count, error, delay, timeUnit) -> {
					logger.error("[TxPath: {}] An error occurred while fetching the document. [Document key={}][Retry iteration: {}][Time with delay: {}]...", txPath, docKey, count, delay, error);
				})
				.build()
			)
		;
		
		return Mono.from(RxReactiveStreams.toPublisher(deletedDocObservable))
				.publishOn(this.appWorkerScheduler)
				.doOnError(t -> {
					logger.error(DELETE_BY_KEY_FAILED_MESSAGE, txPath, docKey, bucketName, t);
				})
				.doOnSuccess(jsonDoc -> {
					logger.debug(DELETE_BY_KEY_SUCCESS_MESSAGE, txPath, docKey, bucketName);
				});
	}
	
	
	@NewSpan(name = "CouchbaseUtils#createOrReplace")
	public Tuple2<Boolean, JsonDocument> createOrReplace(final Bucket bucket, final JsonDocument jsonDoc) {
		
		final String txPath = CLASS_NAME + "#createOrReplace";
        try {
        	final JsonDocument replacedDocument = bucket.replace(jsonDoc);
        	logger.info("[TxPath: {}] Document with key '{}' was replaced successfully.", txPath, jsonDoc.id());
        	return Tuples.of(Boolean.TRUE, replacedDocument);
        } catch (final DocumentDoesNotExistException e) {
            logger.error("[TxPath: {}] DocumentDoesNotExistException when trying to replace the document by key '{}', trying to insert the document.", txPath, jsonDoc.id(), e);
            
            // Try inserting the doc.
            return Tuples.of(Boolean.FALSE, this.insert(bucket, jsonDoc));
        } catch (final CASMismatchException e) {
        	logger.error("[TxPath: {}] CASMismatchException when trying to replace the document by key '{}'", txPath, jsonDoc.id(), e);
        	
        	// If there is a CASMismatchException, re-throw a different exception
            throw e;
        	
        } catch (final Exception e) {
        	logger.error("[TxPath: {}] Exception when trying to replace the document by key '{}'", txPath, jsonDoc.id(), e);

            // If the exception is not a CASMismatchException, re-throw a default AppSupportException
            throw e;
        }
	}
	
	@NewSpan(name = "CouchbaseUtils#insert")
	public JsonDocument insert(final Bucket bucket, final JsonDocument jsonDoc) {
		final String txPath = CLASS_NAME + "#insert";
		
		try {
			final JsonDocument insetedDocument = bucket.insert(jsonDoc);
			logger.info("[TxPath: {}] Document with key '{}' was inserted successfully.", txPath, jsonDoc.id());
			
            return insetedDocument;
		} catch (final Exception e) {
            logger.error("[TxPath: {}] Failed to create new document with id: '{}'", txPath, jsonDoc.id(), e);
            throw e;
		}
	}
	
	@NewSpan(name = "CouchbaseUtils#jsonDocToJsonNode")
	public JsonNode jsonDocToJsonNode(final JsonDocument jsonDoc) {
		if(NON_NULL_JSON_DOC.negate().test(jsonDoc)) {
			return null;
		}
		
        final String txPath = CLASS_NAME + "jsonDocToJsonNode";
        
        final JsonObject content = jsonDoc.content();
        JsonObject meta = content.getObject("_meta");

        if (meta == null) {
            meta = JsonObject.create();
            content.put("_meta", meta);
        }
        
        // Set the cas to the revision in 'meta'
        meta.put("revision", Objects.toString(jsonDoc.cas()));
        
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
	
	@NewSpan(name = "CouchbaseUtils#jsonNodeToJsonDocument")
	public JsonDocument jsonNodeToJsonDocument(
			final JsonNode jsonNode, 
			final String docKey,
			final Long docRevision,
			final int ttlInSec) {
		final String txPath = CLASS_NAME + "#jsonNodeToJsonDoc";
		
		try {
			final String content = jacksonObjectMapper.writer().writeValueAsString(jsonNode);
            final JsonObject jsonObject = jsonTranscoder.stringToJsonObject(content);

            // If the revision is set to 0 then bucket.replace does not validate the cas value.
            return jsonTranscoder.newDocument(docKey, ttlInSec, jsonObject, Objects.nonNull(docRevision) ? docRevision.longValue() : -1);
		} catch (final Exception e) {
			logger.error("[TxPath: {}] Could not convert JsonNode to Couchbase JsonDocument. [DocKey: {}][DocTTLInSec: {}][DocRevision: {}]..", txPath, docKey, ttlInSec, docRevision, e);
			
			throw new RuntimeException(e);
		}
	}
	
	private Tuple2<Integer, Long> getRetryTuple() {
		final Integer maxRetryConfigured = couchbaseConfigProperties.getMaxRetryAttempt();
    	final int maxRetries = Objects.nonNull(maxRetryConfigured) && maxRetryConfigured.intValue() > 0 ? maxRetryConfigured : 3;
    	
    	final Long timeoutInMillisConfigured = couchbaseConfigProperties.getConnectionTimeoutInSecs() * 1000L;
    	final Long timeoutInMillis = Objects.nonNull(timeoutInMillisConfigured) && timeoutInMillisConfigured.longValue() > 0L ? timeoutInMillisConfigured : 30000L;
		
		return Tuples.of(maxRetries, timeoutInMillis);
	}
}
