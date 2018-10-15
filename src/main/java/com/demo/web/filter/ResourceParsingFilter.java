package com.demo.web.filter;

import com.demo.exception.AppException;
import com.demo.model.ResourceDetail;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Component
public class ResourceParsingFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<ServerResponse> filter(final ServerRequest request, final HandlerFunction<ServerResponse> next) {
		final String requestPath = request.path();
		final String[] pathElementsArray = StringUtils.split(requestPath, "/");
		final int pathElementCount = pathElementsArray.length;
		
		if (pathElementCount <= 0) {
			throw new AppException("APP_400003", "Resource ID and name cannot be determined from request path '" + requestPath + "'.");
		}
		
		final ResourceDetail resourceDetail = new ResourceDetail();
		if (pathElementCount >= 2) {
			resourceDetail.setResourceId(pathElementsArray[pathElementCount - 1]);
			resourceDetail.setResourceName(pathElementsArray[pathElementCount - 2]);
		} else {
			resourceDetail.setResourceName(pathElementsArray[0]);
		}
		
		// Store resourceDetail in request
		request.attributes().put("RESOURCE_DETAIL", resourceDetail);

		return next.handle(request);
	}
}
