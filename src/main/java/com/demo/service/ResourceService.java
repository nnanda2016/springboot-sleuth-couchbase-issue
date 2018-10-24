package com.demo.service;

import com.demo.dao.ResourceDAO;
import com.demo.exception.AppException;
import com.demo.model.ResourceDetail;
import com.demo.util.BeanNames;
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

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Component(BeanNames.RESOURCE_SERVICE)
public class ResourceService {
	
	private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);
	public static final String CLASS_NAME = ResourceService.class.getCanonicalName();
	
	private final ResourceDAO resourceDAO;
	private final ServiceHelper serviceHelper;
	private final Tracer tracer;
	
	@Autowired
	public ResourceService(
			@Qualifier(BeanNames.RESOURCE_DAO) final ResourceDAO resourceDAO,
			@Qualifier(BeanNames.SERVICE_HELPER) final ServiceHelper serviceHelper,
			final Tracer tracer) {
		this.resourceDAO = resourceDAO;
		this.serviceHelper = serviceHelper;
		this.tracer = tracer;
	}
	
//	@NewSpan(name = "ResourceService#get")
	public Mono<JsonNode> get(final ResourceDetail resourceDetail) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				final String txPath = CLASS_NAME + "#get";
				
				final Span span = (Span) context.getOrEmpty(Span.class).orElse(null);
				logger.info("[TxPath: {}][Span: {}]", txPath, span);
				
				return resourceDAO.fetch(resourceDetail)
						// README: Change Mono.defer() and verify
	            		.switchIfEmpty(Mono.defer(() -> {
	            				logger.error("[Span: {}][TxPath: {}#switchIfEmpty] No data was fetched from persistence store for the given id '{}' and resource name '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName());

	                            return Mono.error(new AppException("APP_404002", "No data was fetched from persistence store for the given id '" + resourceDetail.getResourceId() + " and resource name '" + resourceDetail.getResourceName() + "'."));
	            			})
	            		)
	            		.doOnSuccess(jsonNode -> {
	    	            	// Remove all fields except 'revision' from _meta node.
	    	            	serviceHelper.updateMetaNodeForResponse(jsonNode);
	    	            	
	    	            	// This logging is just for correlation
	    	            	logger.info("[Span: {}][TxPath: {}#doOnSuccess] Successfully fetched resource from persistence store for the given id '{}' and resource name '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName());
	    	            })
	            		.doOnError(t -> logger.error("[Span: {}][TxPath: {}#doOnError] Failed to fetch resource from persistence store for the given id '{}' and resource name '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName()));
			})
		;
		
//		return Mono.defer(() -> {
//			final Span newSpan = this.tracer.nextSpan().name("ResourceService#get");
//			try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
//				final String txPath = CLASS_NAME + "#get";
//	            
//	            return resourceDAO.fetch(resourceDetail)
//	            		.switchIfEmpty(Mono.defer(() -> {
//	            				logger.error("[TxPath: {}] No data was fetched from persistence store for the given id '{}' and resource name '{}'.", txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName());
//
//	                            return Mono.error(new AppException("APP_404002", "No data was fetched from persistence store for the given id '" + resourceDetail.getResourceId() + " and resource name '" + resourceDetail.getResourceName() + "'."));
//	            			})
//	            		)
//	            		.doOnSuccess(jsonNode -> {
//	    	            	// Remove all fields except 'revision' from _meta node.
//	    	            	serviceHelper.updateMetaNodeForResponse(jsonNode);
//	    	            	
//	    	            	// This logging is just for correlation
//	    	            	logger.info("[TxPath: {}]Successfully fetched resource from persistence store for the given id '{}' and resource name '{}'.", txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName());
//	    	            });
//			} finally {
//				newSpan.finish();
//			}
//		});
	}
	
//	@NewSpan(name = "ResourceService#delete")
    public Mono<Void> delete(final ResourceDetail resourceDetail) {
		final Span newSpan = this.tracer.nextSpan().name("ResourceService#delete");
		try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
			return resourceDAO.delete(resourceDetail)
	                .doOnError(e -> {
	                    final String resourceName = resourceDetail.getResourceName();
	                    final String resourceId = resourceDetail.getResourceId();

	                    logger.error("[TxPath: {}] Failed to delete document; resource name: '{}', resource id: '{}'.", CLASS_NAME + "#delete", resourceName, resourceId, e);
	                });
		} finally {
			newSpan.finish();
		}
    }
	
//	@NewSpan(name = "ResourceServiceImpl#createOrReplace")
    public Mono<Tuple2<Boolean, JsonNode>> createOrReplace(final String senderApplicationId, final ResourceDetail resourceDetail, final JsonNode request) {
    	final Span newSpan = this.tracer.nextSpan().name("ResourceService#createOrReplace");
		try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
			final String txPath = CLASS_NAME + "#createOrReplace";
			return resourceDAO.fetch(resourceDetail)
				.map(previousDoc -> serviceHelper.getUpdatedMetaNode(previousDoc, senderApplicationId, resourceDetail))
				.switchIfEmpty(Mono.just(serviceHelper.buildMetaNode(senderApplicationId, resourceDetail)))
				.map(metaNode -> serviceHelper.addMetaNodeToJsonNode(request, metaNode))
				.flatMap(updatedRequest -> resourceDAO.createOrReplace(resourceDetail, updatedRequest))
				// These splunk logging are just for correlation purpose.
				.doOnSuccess(responseTuple -> {
					serviceHelper.updateMetaNodeForResponse(responseTuple.getT2());
					// This logging is just for correlation
	            	logger.info("[TxPath: {}]Successfully created resource in persistence store for the given id '{}' and resource name '{}'.", txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName());
				})
				.doOnError(t -> logger.info("[TxPath: {}]Failed to create resource in persistence store for the given id '{}' and resource name '{}'.", txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName()))
			;
		} finally {
			newSpan.finish();
		}
    }
}
