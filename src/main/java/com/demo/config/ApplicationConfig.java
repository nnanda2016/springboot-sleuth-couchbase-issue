package com.demo.config;


import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.demo.util.BeanNames;
import com.demo.util.ZonedDateTimeJsonDeserializer;
import com.demo.util.ZonedDateTimeJsonSerializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Configuration
public class ApplicationConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
	
	@Bean(name = BeanNames.JACKSON_OBJECT_MAPPER)
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ObjectMapper getObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        final JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeJsonSerializer());
        javaTimeModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeJsonDeserializer());
        mapper.registerModule(javaTimeModule);
        return mapper;
    }
	
	@Bean(name = BeanNames.COUCHBASE_DATA_BUCKET)
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
	public Bucket dataCouchbaseBucketBean() {
		final String bucketName = "sample";
		final String bucketUsername = "Administrator";
		final String bucketPassword = "password";
		
        final Bucket dataBucket = this.getCouchbaseBucket(bucketUsername, bucketPassword, bucketName);

        logger.info("Data bucket opened successfully.");

		return dataBucket;
	}
	
	private Bucket getCouchbaseBucket(final String bucketUserName, final String bucketUserPassword, final String bucketName) {
		final Cluster cluster = this.getCouchbaseCluster(bucketUserName, bucketUserPassword);
		try {
			return cluster.openBucket(bucketName);
		} catch (final Exception e) {
		    logger.error("Exception while opening bucket '{}'...", bucketName, e);

            throw new IllegalStateException("Failed to obtain the couchbase bucket; bucket name: " + bucketName
                    + "; user name: " + bucketUserName, e);
		}
	}

	private Cluster getCouchbaseCluster(final String authUserName, final String authPassword) {
		final DefaultCouchbaseEnvironment couchbaseEnv = this.buildCouchbaseEnvironment();

		// Create an instance of cluster and authenticate with the cluster.
		final Cluster cbCluster = CouchbaseCluster
		        .create(couchbaseEnv, "localhost")
				.authenticate(authUserName, authPassword);

		logger.info("Authentication with Couchbase cluster was successful.");
		return cbCluster;
	}

	private DefaultCouchbaseEnvironment buildCouchbaseEnvironment() {
		final DefaultCouchbaseEnvironment.Builder envBuilder = DefaultCouchbaseEnvironment.builder()
				.connectTimeout(TimeUnit.SECONDS.toMillis(30))
				.kvTimeout(TimeUnit.SECONDS.toMillis(30))
				.queryTimeout(TimeUnit.SECONDS.toMillis(30))
				.keepAliveErrorThreshold(1)
				.continuousKeepAliveEnabled(true)
				.keepAliveInterval(10000)
				.keepAliveTimeout(30000)
				;

		return envBuilder.build();
	}
}
