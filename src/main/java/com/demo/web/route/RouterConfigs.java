package com.demo.web.route;

import com.demo.web.filter.ResourceParsingFilter;
import com.demo.web.handler.DeleteByIdHandler;
import com.demo.web.handler.GetByIdHandler;
import com.demo.web.handler.PutWithIdHandler;

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
	private static final String GENERIC_PUT_WITH_ID = "/*/{id}";
	private static final String GENERIC_DELETE_BY_ID = "/*/{id}";
	
	@Autowired
	private ResourceParsingFilter resourceParsingFilter;
	
	@Autowired
	private GetByIdHandler getByIdHandler;

	@Autowired
	private DeleteByIdHandler deleteByIdHandler;

	@Autowired
	private PutWithIdHandler putWithIdHandler;
	
	@Bean
	public RouterFunction<ServerResponse> routes() {
		return RouterFunctions
			.route(healthCheckPredicate(), request -> ServerResponse.ok().build())
			.andNest(emptyPathPredicate(), nestedRoutes())
			;
	}

	private RequestPredicate healthCheckPredicate() {
		return RequestPredicates.GET(HEALTH_CHECK);
	}
	
	private RequestPredicate emptyPathPredicate() {
		return RequestPredicates.path("");
	}
	
	private RouterFunction<ServerResponse> nestedRoutes() {
		return RouterFunctions
				.route(RequestPredicates.GET(GENERIC_GET_BY_ID), getByIdHandler::handle) // Handler to route GET by id requests
				.andRoute(RequestPredicates.PUT(GENERIC_PUT_WITH_ID), putWithIdHandler::handle) // Handler to route PUT with id requests
				.andRoute(RequestPredicates.DELETE(GENERIC_DELETE_BY_ID), deleteByIdHandler::handle) // Handler to route DELETE by id requests
				.filter(resourceParsingFilter)
				;
	}
}
