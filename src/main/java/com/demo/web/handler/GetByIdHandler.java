package com.demo.web.handler;

import com.demo.exception.AppException;
import com.demo.model.ResourceDetail;
import com.demo.service.ResourceService;
import com.demo.util.BeanNames;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Component
public class GetByIdHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GetByIdHandler.class);
	public static final String CLASS_NAME = GetByIdHandler.class.getCanonicalName();
	
	private final ResourceService resourceService;
	
	@Autowired
	public GetByIdHandler(@Qualifier(BeanNames.RESOURCE_SERVICE) final ResourceService resourceService) {
		this.resourceService = resourceService;
	}
	
	@NewSpan("GetByIdHandler#handle")
	public Mono<ServerResponse> handle(final ServerRequest request) {
	        final ResourceDetail resourceDetail = (ResourceDetail) request.attribute("RESOURCE_DETAIL")
	                .orElseThrow(() -> new AppException("APP_400003", "Resource ID and name cannot be determined from request path '" + request.path() + "'."));

	        final String resourceName = resourceDetail.getResourceName();
	        final String id = resourceDetail.getResourceId();

	        final String txPath = CLASS_NAME + "#handle";
			
	        return resourceService.get(resourceDetail)
	        		// These logging are just for correlation purpose 
					.doOnSuccess(jsonNode -> logger.info("[TxPath: {}] Successfully fetched resource with id '{}' and name '{}'.", txPath, id, resourceName))
					.doOnError(t -> logger.info("[TxPath: {}] Failed to fetch resource with id '{}' and name '{}'...", txPath, id, resourceName, t))
	        		.flatMap(jsonNode -> ServerResponse.ok()
	        			.contentType(MediaType.APPLICATION_JSON_UTF8)
	        			.syncBody(jsonNode))
	        ;
	}
}
