package com.demo.web.route;

import com.demo.web.handler.GetByIdHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.*;


/**
 * Configures the web routes.
 * 
 * @author Niranjan Nanda
 */
@Configuration
@EnableWebFlux
public class RouterConfigs {
	
	private static final String HEALTH_CHECK = "/health/check";
	private static final String GENERIC_GET_BY_ID = "/*/{id}";
	
	@Autowired
	private GetByIdHandler getByIdHandler;
	
	@Bean
	public RouterFunction<ServerResponse> routes() {
		return RouterFunctions
			.route(RequestPredicates.GET(HEALTH_CHECK), request -> ServerResponse.ok().build())
			.andNest(emptyPathPredicate(), nestedRoutes())
			;
	}
	
	private RequestPredicate emptyPathPredicate() {
		return RequestPredicates.path("");
	}
	
	private RouterFunction<ServerResponse> nestedRoutes() {
		return RouterFunctions
				.route(RequestPredicates.GET(GENERIC_GET_BY_ID), getByIdHandler::handle) // Handler to route GET by id requests
				;
	}
}
