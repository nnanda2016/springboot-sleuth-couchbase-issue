package com.demo.web.filter;

import com.demo.util.Utils;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.instrument.web.TraceWebFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import brave.Span;
import brave.Tracer;
import reactor.core.publisher.Mono;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Component
public class HttpGatewayTracingFilter  implements WebFilter, Ordered {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpGatewayTracingFilter.class);
	public static final String CLASS_NAME = HttpGatewayTracingFilter.class.getCanonicalName();
	
	public static final String CLIENT_TRACE_ID_HEADER = "X-Client-Trace-Id";
	public static final String SERVER_TRACE_ID_HEADER = "X-Server-Trace-Id";
	
	@Autowired
    private Tracer tracer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getOrder() {
		return TraceWebFilter.ORDER + 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
		return Mono.subscriberContext()
			.filter(context -> !StringUtils.equalsAnyIgnoreCase(exchange.getRequest().getPath().value(), "/health/check"))
			.flatMap(context -> {
				final String clientTraceIdHeader = exchange.getRequest().getHeaders().getFirst(CLIENT_TRACE_ID_HEADER);
	            if (StringUtils.isNotBlank(clientTraceIdHeader)) {
	                // Add X-Client-Trace-Id to response
	                exchange.getResponse().getHeaders().add(CLIENT_TRACE_ID_HEADER, clientTraceIdHeader);
	            }
	            
	            final String traceIdString = tracer.currentSpan().context().traceIdString();

	            // Add X-Server-Trace-Id to response
	            exchange.getResponse().getHeaders().add(SERVER_TRACE_ID_HEADER, traceIdString);
	            
	            final ServerHttpRequest httpRequest = exchange.getRequest();
	            final HttpHeaders requestHeaders = httpRequest.getHeaders();
	            
	            final String txPath = CLASS_NAME + "#filter";
	            final long txStartTime = System.currentTimeMillis();
	            
	            final Span spanFromReactorContext = (Span) context.getOrEmpty(Span.class).orElse(null);
	            
	            logger.info("[Span: {}][TxPath={}][Tracer.span: {}][StartTimestamp: {}][Scope=HTTP_REQ][URL={}][HttpMethod={}][User-Agent={}][Accept={}][Content-Type={}][From={}][X-Client-Trace-Id={}][X-Server-Trace-Id={}]",
	            		context.getOrEmpty(Span.class).orElse(null),
	            		txPath,
	            		tracer.currentSpan(),
	            		Objects.toString(txStartTime),
	            		Objects.toString(httpRequest.getURI()),
	            		httpRequest.getMethodValue(),
	            		Utils.GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS.apply(requestHeaders, HttpHeaders.USER_AGENT).orElse("<NA>"),
	            		Utils.GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS.apply(requestHeaders, HttpHeaders.ACCEPT).orElse("<NA>"),
	            		Utils.GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS.apply(requestHeaders, HttpHeaders.CONTENT_TYPE).orElse("<NA>"),
	            		Utils.GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS.apply(requestHeaders, HttpHeaders.FROM).orElse("<NA>"),
	            		Utils.GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS.apply(requestHeaders, CLIENT_TRACE_ID_HEADER).orElse("<NA>"),
	            		traceIdString);
	            
	            return chain.filter(exchange)
	            		.doOnSuccessOrError((s, e) -> {
	            			final Span newSpan = tracer.newChild(spanFromReactorContext.context()).name("HttpGatewayTracingFilter#filter#response");
	            			try (final Tracer.SpanInScope ws = this.tracer.withSpanInScope(newSpan.start())) {
	            				final ServerHttpResponse httpResponse = exchange.getResponse();
		                        final HttpHeaders responseHeaders = httpResponse.getHeaders();
		                        try {
//		                        	ThreadContext.put("traceId", traceIdString);
		                        	
		                        	logger.info("[Span: {}][TxPath={}][Tracer.span: {}][NewSpan: {}][EndTimestamp: {}][Scope=HTTP_RES][Location={}][Content-Type={}][Content-Length={}][X-Server-Trace-Id={}]",
		                        			context.getOrEmpty(Span.class).orElse(null),
		                        			txPath,
		                        			tracer.currentSpan(),
		                        			newSpan,
		            	            		Objects.toString(System.currentTimeMillis()),
		            	            		Utils.GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS.apply(responseHeaders, HttpHeaders.LOCATION).orElse("<NA>"),
		            	            		Utils.GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS.apply(responseHeaders, HttpHeaders.CONTENT_TYPE).orElse("<NA>"),
		            	            		Utils.GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS.apply(responseHeaders, HttpHeaders.CONTENT_LENGTH).orElse("<NA>"),
		            	            		traceIdString);
		                        			
		                        } finally {
//		                        	ThreadContext.remove("traceId");
		                        }
	            			} finally {
	            				newSpan.finish();
	            			}
	                        
	                    });
			})
		;
	}
}
