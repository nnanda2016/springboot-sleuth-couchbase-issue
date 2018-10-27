package com.demo.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;

import brave.Span;
import brave.Tracer;
import reactor.core.publisher.Mono;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Component
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	public static final String CLASS_NAME = GlobalExceptionHandler.class.getCanonicalName();
	
	private final Tracer tracer;
	
    @Autowired
	public GlobalExceptionHandler(final ApplicationContext applicationContext, final ServerCodecConfigurer serverCodecConfigurer, final Tracer tracer) {
		super (new DemoErrorAttributes(), new ResourceProperties(), applicationContext);
		super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        
        this.tracer = tracer;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(final ErrorAttributes errorAttributes) {
		return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
	}
	
	/**
	 * Render the error information as a JSON payload.
	 * @param request the current request
	 * @return a {@code Publisher} of the HTTP response
	 */
	private Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {
		return Mono.subscriberContext()
			.flatMap(context -> {
				final String txPath = CLASS_NAME + "#renderErrorResponse";
				
				final Map<String, Object> errorPropertiesMap = getErrorAttributes(request, false);
				
				final Span span = (Span) context.getOrEmpty(Span.class).orElse(null);
				logger.info("[Span: {}][TxPath: {}][Tracer.span: {}]", span, txPath, tracer.currentSpan());
				
				final HttpStatus httpStatus = (HttpStatus) errorPropertiesMap.get(HttpStatus.class.getCanonicalName());
				
				// Remove the HttpStatus from the map so that it does not get rendered in the response 
				errorPropertiesMap.remove(HttpStatus.class.getCanonicalName());
				return ServerResponse.status(httpStatus)
						.contentType(MediaType.APPLICATION_JSON_UTF8)
						.syncBody(errorPropertiesMap);
			})
		;
//		
//		return Mono.defer(() -> {
//			final Map<String, Object> errorPropertiesMap = getErrorAttributes(request, false);
//			
//			final HttpStatus httpStatus = (HttpStatus) errorPropertiesMap.get(HttpStatus.class.getCanonicalName());
//			
//			// Remove the HttpStatus from the map so that it does not get rendered in the response 
//			errorPropertiesMap.remove(HttpStatus.class.getCanonicalName());
//			
//			return ServerResponse.status(httpStatus)
//					.contentType(MediaType.APPLICATION_JSON_UTF8)
//					.syncBody(errorPropertiesMap);
//		});
	}
}
