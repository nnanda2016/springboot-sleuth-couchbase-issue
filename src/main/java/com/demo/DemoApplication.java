package com.demo;

import com.demo.config.ApplicationConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import reactor.core.publisher.Hooks;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import(ApplicationConfig.class)
public class DemoApplication {
	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);
	public static void main(final String[] args) {
		if (logger.isDebugEnabled()) {
			Hooks.onOperatorDebug();
		}
		SpringApplication.run(DemoApplication.class, args);
	}
}
