package com.demo.web.handler;

import com.demo.exception.AppException;
import com.demo.model.ResourceDetail;
import com.demo.service.ResourceService;
import com.demo.util.BeanNames;
import com.demo.util.Utils;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.ServerResponse.BodyBuilder;

import brave.Span;
import brave.Tracer;
import reactor.core.publisher.Mono;

/**
 * A handler which is used to perform PUT by id requests.
 *
 * @author Niranjan Nanda
 */
@Component
public class PutWithIdHandler {

	private static final Logger logger = LoggerFactory.getLogger(PutWithIdHandler.class);
	private static final String CLASS_NAME = PutWithIdHandler.class.getCanonicalName();

	private final ResourceService resourceService;
	private final Tracer tracer;
	
	@Autowired
    public PutWithIdHandler(@Qualifier(BeanNames.RESOURCE_SERVICE) final ResourceService resourceService, final Tracer tracer) {
		this.resourceService = resourceService;
		this.tracer = tracer;
	}

//	@NewSpan("PutWithIdHandler#handle")
	public Mono<ServerResponse> handle(final ServerRequest request) {
		final Span newSpan = this.tracer.nextSpan().name("PutWithIdHandler#handle");
		try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
			final ResourceDetail resourceDetail = (ResourceDetail) request.attribute("RESOURCE_DETAIL")
	                .orElseThrow(() -> new AppException("APP_400003", "Resource ID and name cannot be determined from request path '" + request.path() + "'."));

			final String senderAppId = Utils.GET_FIRST_HEADER_VALUE_FROM_SERVER_REQ.apply(request, HttpHeaders.FROM)
					.orElseThrow(() -> new AppException("APP_400002", "Required header '" + HttpHeaders.FROM + "' has unacceptable value in the request."))
					;

			final Mono<ObjectNode> requestBody = request.bodyToMono(ObjectNode.class);

			final String txPath = CLASS_NAME + "#handle";

	        return requestBody
					.onErrorMap(error -> {
						// If the request has no body present, throw exception
						logger.error("[TxPath: {}] There was an exception while serializing request body to corresponding resource. ", txPath, error); 
						return new AppException("APP_400006", "There was an exception while serializing request body to corresponding resource. The associated error message is '" + error.getMessage() + "'.");
					})
					// If there is any error while parsing the request, wrap the exception and re-throw a 400 status exception.
	                .switchIfEmpty(Mono.defer(() -> Mono.error(new AppException("APP_400005", "Resource must be provided in the request body for 'PUT' operation.")))
	                )

					.flatMap(requestContent -> resourceService.createOrReplace(senderAppId, resourceDetail, requestContent)) // Invoke the service to create-or-replace the provided request.
					.doOnSuccess(responseContent -> logger.info("Successfully '{}' resource.", responseContent.getT1() ? "Created" : "Replaced"))
	                .doOnError(e -> {
	                    logger.error("[TxPath: {}] Failed to create or replace a document: '{}'", txPath, resourceDetail, e);
	                })
					.flatMap(responseContent -> {
						/*
						 * If the document is created, set the status 201, else 200.
						 * Set the location header if the document is created or updated.
						 * If the ACCEPT header is not provided respond with an empty response.
						 */
						final BodyBuilder builder = responseContent.getT1() ? ServerResponse.created(request.uri())
								: ServerResponse.ok().location(request.uri());

						if (CollectionUtils.isNotEmpty(request.headers().accept())) {
							// Convert the response from ResourceService to ServerResponse
							return builder.contentType(MediaType.APPLICATION_JSON_UTF8).syncBody(responseContent.getT2()); 
						}

						return builder.build();
					});
		} finally {
			newSpan.finish();
		}
	}
}
