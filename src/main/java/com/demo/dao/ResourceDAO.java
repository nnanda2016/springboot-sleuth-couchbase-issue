package com.demo.dao;

import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.util.retry.RetryBuilder;
import com.demo.model.ResourceDetail;
import com.demo.util.BeanNames;
import com.demo.util.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import brave.Span;
import brave.Tracer;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import rx.Observable;
import rx.RxReactiveStreams;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Component(BeanNames.RESOURCE_DAO)
public class ResourceDAO {
	
	private static final Logger logger = LoggerFactory.getLogger(ResourceDAO.class);
	public static final String CLASS_NAME = ResourceDAO.class.getCanonicalName();
	
	private final Bucket dataBucket;
	private final Scheduler appWorkerScheduler;
	private final ObjectMapper jacksonObjectMapper;
	private final Tracer tracer;
	
	@Autowired
	public ResourceDAO(
			@Qualifier(BeanNames.COUCHBASE_DATA_BUCKET) final Bucket dataBucket,
			@Qualifier(BeanNames.JACKSON_OBJECT_MAPPER) final ObjectMapper objectMapper,
			final Tracer tracer) {
		this.dataBucket = dataBucket;
		this.jacksonObjectMapper = objectMapper;
		
		final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("app-worker-%d").build();
		this.appWorkerScheduler = Schedulers.fromExecutorService(Executors.newFixedThreadPool(10, threadFactory));
		
		this.tracer = tracer;
	}
	
	public Mono<JsonNode> fetch(final ResourceDetail resourceDetail) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				final String txPath = CLASS_NAME + "#fetch";
				
				logger.info("[Span: {}][TxPath: {}][Tracer.span: {}]", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan());
				
				final Span newSpan = this.tracer.nextSpan().name("ResourceDAO#fetch");
				try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
					final String documentKey = Utils.FN_DOCUMENT_KEY_SUPPLIER.apply(resourceDetail.getResourceName(), resourceDetail.getResourceId());
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
//        JsonObject meta = content.getObject("_meta");
//
//        if (meta == null) {
//            meta = JsonObject.create();
//            content.put("_meta", meta);
//        }
//        
//        // Set the cas to the revision in 'meta'
//        meta.put("revision", Objects.toString(jsonDoc.cas()));
        
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
}
