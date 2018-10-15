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
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Configuration
@EnableConfigurationProperties(value = {CouchbaseConfigProps.class})
public class ApplicationConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
	
	@Autowired
	private CouchbaseConfigProps couchbaseConfigs;
	
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
	    

        final Bucket dataBucket = this.getCouchbaseBucket(
        		couchbaseConfigs.getBucketUserName(),
        		couchbaseConfigs.getBucketUserPassword(),
        		couchbaseConfigs.getBucketName());

        logger.info("Data bucket opened successfully.");

		return dataBucket;
	}
	
	@Bean(name = BeanNames.APP_WORKER_THREADPOOL_EXECUTOR, destroyMethod = "shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ExecutorService appWorkerExecutorService(@Value("${app.workerThreadPoolSize}") final int appWorkerThreadpoolSize) {
    	final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("app-worker-%d").build();
    	
    	if (appWorkerThreadpoolSize <= 0) {
    		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), threadFactory);
    	}
    	
    	return Executors.newFixedThreadPool(appWorkerThreadpoolSize, threadFactory);
    }
	
	private Bucket getCouchbaseBucket(final String bucketUserName, final String bucketUserPassword,
			final String bucketName) {
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
		if (CollectionUtils.isEmpty(couchbaseConfigs.getHosts())) {
			throw new IllegalStateException("Couchbase host values is null or empty in configuration.");
		}

		final DefaultCouchbaseEnvironment couchbaseEnv = this.buildCouchbaseEnvironment();

		// Create an instance of cluster and authenticate with the cluster.
		final Cluster cbCluster = CouchbaseCluster
		        .create(couchbaseEnv, couchbaseConfigs.getHosts())
				.authenticate(authUserName, authPassword);

		logger.info("Authentication with Couchbase cluster was successful.");
		return cbCluster;
	}

	private DefaultCouchbaseEnvironment buildCouchbaseEnvironment() {
		final long connectionTimeoutInSecs = Objects.nonNull(couchbaseConfigs.getConnectionTimeoutInSecs())
				? couchbaseConfigs.getConnectionTimeoutInSecs()
				: 30L;
		final long keyValueTimeoutInSecs = Objects.nonNull(couchbaseConfigs.getKeyValueTimeoutInSecs())
				? couchbaseConfigs.getKeyValueTimeoutInSecs()
				: 30L;
		final long queryTimeoutInSecs = Objects.nonNull(couchbaseConfigs.getQueryTimeoutInSecs())
				? couchbaseConfigs.getQueryTimeoutInSecs()
				: 30L;

		final DefaultCouchbaseEnvironment.Builder envBuilder = DefaultCouchbaseEnvironment.builder()
				.connectTimeout(TimeUnit.SECONDS.toMillis(connectionTimeoutInSecs))
				.kvTimeout(TimeUnit.SECONDS.toMillis(keyValueTimeoutInSecs))
				.queryTimeout(TimeUnit.SECONDS.toMillis(queryTimeoutInSecs))
				.keepAliveErrorThreshold(1)
				.continuousKeepAliveEnabled(true)
				.keepAliveInterval(10000)
				.keepAliveTimeout(30000)
				;

		return envBuilder.build();
	}
	
	
}
