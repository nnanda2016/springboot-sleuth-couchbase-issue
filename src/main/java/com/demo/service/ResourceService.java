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
	private final Tracer tracer;
	
	@Autowired
	public ResourceService(
			@Qualifier(BeanNames.RESOURCE_DAO) final ResourceDAO resourceDAO,
			final Tracer tracer) {
		this.resourceDAO = resourceDAO;
		this.tracer = tracer;
	}
	
	public Mono<JsonNode> get(final ResourceDetail resourceDetail) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				final String txPath = CLASS_NAME + "#get";
				
				logger.info("[Span: {}][TxPath: {}][Tracer.span: {}]", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan());
				
				final Span newSpan = this.tracer.nextSpan().name("ResourceService#get");
				try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
					return resourceDAO.fetch(resourceDetail)
		            		.switchIfEmpty(Mono.error(new AppException("APP_404002", "No data was fetched from persistence store for the given id '" + resourceDetail.getResourceId() + " and resource name '" + resourceDetail.getResourceName() + "'.")))
		            		.doOnSuccess(jsonNode -> {
		    	            	// This logging is just for correlation
		    	            	logger.info("[Span: {}][TxPath: {}#doOnSuccess][Tracer.span: {}] Successfully fetched resource from persistence store for the given id '{}' and resource name '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), resourceDetail.getResourceId(), resourceDetail.getResourceName());
		    	            })
		            		.doOnError(t -> logger.error("[Span: {}][TxPath: {}#doOnError][Tracer.span: {}] Failed to fetch resource from persistence store for the given id '{}' and resource name '{}'.", context.getOrEmpty(Span.class).orElse(null), txPath, tracer.currentSpan(), resourceDetail.getResourceId(), resourceDetail.getResourceName()));
				} finally {
					newSpan.finish();
				}
			})
		;
	}
}
