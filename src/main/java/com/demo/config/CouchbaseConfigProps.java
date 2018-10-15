package com.demo.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@ConfigurationProperties(prefix = "app.couchbase")
public class CouchbaseConfigProps {
	private List<String> hosts;
	private int connectionTimeoutInSecs;
	private int keyValueTimeoutInSecs;
	private int queryTimeoutInSecs;
	private int maxRetryAttempt;
	private boolean sslEnabled;
	private String bucketName;
	private String bucketUserName;
	private String bucketUserPassword;

	/**
	 * Returns the value of hosts.
	 *
	 * @return the hosts
	 */
	public List<String> getHosts() {
		return hosts;
	}

	/**
	 * Sets the value of hosts.
	 *
	 * @param hosts the hosts to set
	 */
	public void setHosts(final List<String> hosts) {
		this.hosts = hosts;
	}

	/**
	 * Returns the value of connectionTimeoutInSecs.
	 *
	 * @return the connectionTimeoutInSecs
	 */
	public int getConnectionTimeoutInSecs() {
		return connectionTimeoutInSecs;
	}

	/**
	 * Sets the value of connectionTimeoutInSecs.
	 *
	 * @param connectionTimeoutInSecs the connectionTimeoutInSecs to set
	 */
	public void setConnectionTimeoutInSecs(final int connectionTimeoutInSecs) {
		this.connectionTimeoutInSecs = connectionTimeoutInSecs;
	}

	/**
	 * Returns the value of keyValueTimeoutInSecs.
	 *
	 * @return the keyValueTimeoutInSecs
	 */
	public int getKeyValueTimeoutInSecs() {
		return keyValueTimeoutInSecs;
	}

	/**
	 * Sets the value of keyValueTimeoutInSecs.
	 *
	 * @param keyValueTimeoutInSecs the keyValueTimeoutInSecs to set
	 */
	public void setKeyValueTimeoutInSecs(final int keyValueTimeoutInSecs) {
		this.keyValueTimeoutInSecs = keyValueTimeoutInSecs;
	}

	/**
	 * Returns the value of queryTimeoutInSecs.
	 *
	 * @return the queryTimeoutInSecs
	 */
	public int getQueryTimeoutInSecs() {
		return queryTimeoutInSecs;
	}

	/**
	 * Sets the value of queryTimeoutInSecs.
	 *
	 * @param queryTimeoutInSecs the queryTimeoutInSecs to set
	 */
	public void setQueryTimeoutInSecs(final int queryTimeoutInSecs) {
		this.queryTimeoutInSecs = queryTimeoutInSecs;
	}

	/**
	 * Returns the value of maxRetryAttempt.
	 *
	 * @return the maxRetryAttempt
	 */
	public int getMaxRetryAttempt() {
		return maxRetryAttempt;
	}

	/**
	 * Sets the value of maxRetryAttempt.
	 *
	 * @param maxRetryAttempt the maxRetryAttempt to set
	 */
	public void setMaxRetryAttempt(final int maxRetryAttempt) {
		this.maxRetryAttempt = maxRetryAttempt;
	}

	/**
	 * Returns the value of sslEnabled.
	 *
	 * @return the sslEnabled
	 */
	public boolean isSslEnabled() {
		return sslEnabled;
	}

	/**
	 * Sets the value of sslEnabled.
	 *
	 * @param sslEnabled the sslEnabled to set
	 */
	public void setSslEnabled(final boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}

	/**
	 * Returns the value of bucketName.
	 *
	 * @return the bucketName
	 */
	public String getBucketName() {
		return bucketName;
	}

	/**
	 * Sets the value of bucketName.
	 *
	 * @param bucketName the bucketName to set
	 */
	public void setBucketName(final String bucketName) {
		this.bucketName = bucketName;
	}

	/**
	 * Returns the value of bucketUserName.
	 *
	 * @return the bucketUserName
	 */
	public String getBucketUserName() {
		return bucketUserName;
	}

	/**
	 * Sets the value of bucketUserName.
	 *
	 * @param bucketUserName the bucketUserName to set
	 */
	public void setBucketUserName(final String bucketUserName) {
		this.bucketUserName = bucketUserName;
	}

	/**
	 * Returns the value of bucketUserPassword.
	 *
	 * @return the bucketUserPassword
	 */
	public String getBucketUserPassword() {
		return bucketUserPassword;
	}

	/**
	 * Sets the value of bucketUserPassword.
	 *
	 * @param bucketUserPassword the bucketUserPassword to set
	 */
	public void setBucketUserPassword(final String bucketUserPassword) {
		this.bucketUserPassword = bucketUserPassword;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CouchbaseConfigProps {hosts=");
		builder.append(hosts);
		builder.append(", connectionTimeoutInSecs=");
		builder.append(connectionTimeoutInSecs);
		builder.append(", keyValueTimeoutInSecs=");
		builder.append(keyValueTimeoutInSecs);
		builder.append(", queryTimeoutInSecs=");
		builder.append(queryTimeoutInSecs);
		builder.append(", maxRetryAttempt=");
		builder.append(maxRetryAttempt);
		builder.append(", sslEnabled=");
		builder.append(sslEnabled);
		builder.append(", bucketName=");
		builder.append(bucketName);
		builder.append(", bucketUserName=");
		builder.append(bucketUserName);
		builder.append(", bucketUserPassword=");
		builder.append(bucketUserPassword);
		builder.append("}");
		return builder.toString();
	}
	
}
