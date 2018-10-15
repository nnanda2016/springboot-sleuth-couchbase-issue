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
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;

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
	
	@Autowired
	public ResourceService(
			@Qualifier(BeanNames.RESOURCE_DAO) final ResourceDAO resourceDAO,
			@Qualifier(BeanNames.SERVICE_HELPER) final ServiceHelper serviceHelper) {
		this.resourceDAO = resourceDAO;
		this.serviceHelper = serviceHelper;
	}
	
	@NewSpan(name = "ResourceService#get")
	public Mono<JsonNode> get(final ResourceDetail resourceDetail) {
		return Mono.defer(() -> {
            final String txPath = CLASS_NAME + "#get";
            
            return resourceDAO.fetch(resourceDetail)
            		.switchIfEmpty(Mono.defer(() -> {
            				logger.error("[TxPath: {}] No data was fetched from persistence store for the given id '{}' and resource name '{}'.", txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName());

                            return Mono.error(new AppException("APP_404002", "No data was fetched from persistence store for the given id '" + resourceDetail.getResourceId() + " and resource name '" + resourceDetail.getResourceName() + "'."));
            			})
            		)
            		.doOnSuccess(jsonNode -> {
    	            	// Remove all fields except 'revision' from _meta node.
    	            	serviceHelper.updateMetaNodeForResponse(jsonNode);
    	            	
    	            	// This logging is just for correlation
    	            	logger.info("[TxPath: {}]Successfully fetched resource from persistence store for the given id '{}' and resource name '{}'.", txPath, resourceDetail.getResourceId(), resourceDetail.getResourceName());
    	            })
            		;
		});
	}
	
	@NewSpan(name = "ResourceService#delete")
    public Mono<Void> delete(final ResourceDetail resourceDetail) {
        return resourceDAO.delete(resourceDetail)
                .doOnError(e -> {
                    final String resourceName = resourceDetail.getResourceName();
                    final String resourceId = resourceDetail.getResourceId();

                    logger.error("[TxPath: {}] Failed to delete document; resource name: '{}', resource id: '{}'.", CLASS_NAME + "delete", resourceName, resourceId, e);
                });
    }
	
	@NewSpan(name = "ResourceServiceImpl#createOrReplace")
    public Mono<Tuple2<Boolean, JsonNode>> createOrReplace(final String senderApplicationId, final ResourceDetail resourceDetail, final JsonNode request) {
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
		
    }
}
