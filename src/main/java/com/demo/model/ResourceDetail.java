package com.demo.model;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
public class ResourceDetail {
	private String resourceId;
	private String resourceName;

	/**
	 * Returns the value of resourceId.
	 *
	 * @return the resourceId
	 */
	public String getResourceId() {
		return resourceId;
	}

	/**
	 * Sets the value of resourceId.
	 *
	 * @param resourceId the resourceId to set
	 */
	public void setResourceId(final String resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * Returns the value of resourceName.
	 *
	 * @return the resourceName
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * Sets the value of resourceName.
	 *
	 * @param resourceName the resourceName to set
	 */
	public void setResourceName(final String resourceName) {
		this.resourceName = resourceName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ResourceDetail {resourceId=");
		builder.append(resourceId);
		builder.append(", resourceName=");
		builder.append(resourceName);
		builder.append("}");
		return builder.toString();
	}
	
}
