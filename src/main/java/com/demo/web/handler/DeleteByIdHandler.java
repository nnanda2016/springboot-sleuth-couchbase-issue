package com.demo.web.handler;

import com.demo.exception.AppException;
import com.demo.model.ResourceDetail;
import com.demo.service.ResourceService;
import com.demo.util.BeanNames;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import brave.Span;
import brave.Tracer;
import reactor.core.publisher.Mono;

/**
 * A handler for {@code DELETE} operations.
 *
 * @author Niranjan Nanda
 */
@Component
public class DeleteByIdHandler {
	private final ResourceService resourceService;
	private final Tracer tracer;

	@Autowired
	public DeleteByIdHandler(@Qualifier(BeanNames.RESOURCE_SERVICE)final ResourceService resourceService, final Tracer tracer) {
		this.resourceService = resourceService;
		this.tracer = tracer;
	}

	/**
	 * Handles the delete request.
	 *
	 * @param request	The HTTP request
	 * @return	Server response with {@code 204} status code.
	 */
//	@NewSpan("DeleteByIdHandler#handle")
	public Mono<ServerResponse> handle(final ServerRequest request) {
		final Span newSpan = this.tracer.nextSpan().name("DeleteByIdHandler#handle");
		try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
			final ResourceDetail resourceDetail = (ResourceDetail) request.attribute("RESOURCE_DETAIL")
	                .orElseThrow(() -> new AppException("APP_400003", "Resource ID and name cannot be determined from request path '" + request.path() + "'."));

			return resourceService.delete(resourceDetail)
					.then(ServerResponse
							.status(HttpStatus.NO_CONTENT)
							.build());
		} finally {
			newSpan.finish();
		}
	}
}
