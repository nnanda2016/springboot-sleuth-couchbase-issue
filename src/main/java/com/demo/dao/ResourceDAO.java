package com.demo.dao;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.demo.exception.AppException;
import com.demo.model.ResourceDetail;
import com.demo.util.BeanNames;
import com.demo.util.Utils;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import brave.Span;
import brave.Tracer;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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
	private final CouchbaseUtils couchbaseUtils;
	private final Tracer tracer;
	
	@Autowired
	public ResourceDAO(
			@Qualifier(BeanNames.COUCHBASE_DATA_BUCKET) final Bucket dataBucket,
			@Qualifier(BeanNames.COUCHBASE_UTILS) final CouchbaseUtils couchbaseUtils,
			final Tracer tracer) {
		this.dataBucket = dataBucket;
		this.couchbaseUtils = couchbaseUtils;
		this.tracer = tracer;
	}
	
//	@NewSpan(name = "ResourceDAO#fetch")
	public Mono<JsonNode> fetch(final ResourceDetail resourceDetail) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				final String txPath = CLASS_NAME + "#fetch";
				
				logger.info("[Span: {}][TxPath: {}][Tracer.span: {}]", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan());
				
				final Span newSpan = this.tracer.nextSpan().name("ResourceDAO#fetch");
				try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
					final String documentKey = Utils.FN_DOCUMENT_KEY_SUPPLIER.apply(resourceDetail.getResourceName(), resourceDetail.getResourceId());
					return couchbaseUtils.fetchByKey(dataBucket, documentKey)
							.map(this.couchbaseUtils::jsonDocToJsonNode)
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
		
		
//		return Mono.defer(() -> {
//			final Span newSpan = this.tracer.nextSpan().name("ResourceDAO#fetch");
//			try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
//				final String documentKey = Utils.FN_DOCUMENT_KEY_SUPPLIER.apply(resourceDetail.getResourceName(), resourceDetail.getResourceId());
//				final String txPath = CLASS_NAME + "#fetch";
//				return couchbaseUtils.fetchByKey(dataBucket, documentKey)
//					.map(this.couchbaseUtils::jsonDocToJsonNode)
//					// These logging are just for correlation purpose 
//					.doOnSuccess(jsonNode ->  {
//						if (jsonNode == null) {
//							logger.error("[TxPath: {}] Failed to fetch document for resource '{}'.", txPath, resourceDetail);
//						} else {
//							logger.info("[TxPath: {}] Successfully fetched document for resource '{}'", txPath, resourceDetail);
//						}
//					})
//					.doOnError(t -> logger.error("[TxPath: {}] Failed to fetch document for resource '{}'...", txPath, resourceDetail, t))
//					;
//			} finally {
//				newSpan.finish();
//			}
//		});
	}
	
//	@NewSpan(name = "ResourceDAO#delete")
	public Mono<Void> delete(final ResourceDetail resourceDetail) {
		return Mono.defer(() -> {
			final Span newSpan = this.tracer.nextSpan().name("ResourceDAO#delete");
			try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
				final String documentKey = Utils.FN_DOCUMENT_KEY_SUPPLIER.apply(resourceDetail.getResourceName(), resourceDetail.getResourceId());
				final String txPath = CLASS_NAME + "#delete";
				
				return this.couchbaseUtils.asyncDelete(dataBucket, documentKey)
					// These logging are just for correlation purpose 
					.doOnSuccess(jsonNode -> logger.info("[TxPath: {}] Successfully deleted document with resource id '{}'.", txPath, resourceDetail.getResourceId()))
					.doOnError(t -> logger.error("[TxPath: {}] Failed to delete document with id '{}'...", txPath, resourceDetail.getResourceId(), t))
					.onErrorMap(t -> {
						if (t instanceof DocumentDoesNotExistException) {
							return new AppException("APP_404003", "Delete failed because no data was fetched from persistence store for the given id " + resourceDetail.getResourceId() + " and resource name '" + resourceDetail.getResourceId() + "'.");
						} else {
							return new AppException("APP_500006", "An error occurred while deleting the resource with id " + resourceDetail.getResourceId() + " and resource name '" + resourceDetail.getResourceId() + "'.");
						}
					})
					.flatMap(jsonDoc -> Mono.empty())
				;
			} finally {
				newSpan.finish();
			}
		});
	}
	
//	@NewSpan(name = "ResourceDAO#createOrReplace")
	public Mono<Tuple2<Boolean, JsonNode>> createOrReplace(final ResourceDetail resourceDetail, final JsonNode jsonNode) {
		return Mono.defer(() -> {
			final Span newSpan = this.tracer.nextSpan().name("ResourceDAO#createOrReplace");
			try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
				final String docKey = Utils.FN_DOCUMENT_KEY_SUPPLIER.apply(resourceDetail.getResourceName(), resourceDetail.getResourceId());
				final JsonDocument jsonDoc;
				try {
					jsonDoc = couchbaseUtils.jsonNodeToJsonDocument(jsonNode, docKey, (Long) null, 0);
				} catch (final Exception e) {
					return Mono.error(new AppException("APP_500005", "An error occurred while deserializing the request for key '" + resourceDetail.getResourceId() + "'."));
				}
				
				try {
					final Tuple2<Boolean, JsonDocument> createdDocumentTuple = couchbaseUtils.createOrReplace(dataBucket, jsonDoc);
					final JsonNode createdJsonNode = couchbaseUtils.jsonDocToJsonNode(createdDocumentTuple.getT2());
					return Mono.justOrEmpty(Tuples.of(createdDocumentTuple.getT1(), createdJsonNode));
				} catch (final CASMismatchException e) {
					return Mono.error(new AppException("APP_409001", "An exception occurred while saving the resource because there was a version mismatch between the one provided, and the one stored in the database."));
				} catch (final Exception e) {
					return Mono.error(new AppException("APP_500004", "An error occurred while saving the resource with key '" + resourceDetail.getResourceId() + "'."));
				}
			} finally {
				newSpan.finish();
			}
		});
	}
}
