package com.demo.model;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * This class represents the metadata information of any resource.
 *
 * @author Suraj Mohanan Kodiyath
 */
public class Meta implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;
    private String id;
    private String resourceName;
    private transient Long revision;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private String createdBy;
    private String updatedBy;

    /**
     * Returns the value of id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of id.
     *
     * @param id
     *            the id to set
     */
    public void setId(final String id) {
        this.id = id;
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
     * @param resourceName
     *            the resourceName to set
     */
    public void setResourceName(final String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * Returns the value of revision.
     *
     * @return the revision
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Long getRevision() {
        return revision;
    }

    /**
     * Sets the value of revision.
     *
     * @param revision
     *            the revision to set
     */
    public void setRevision(final Long revision) {
        this.revision = revision;
    }

    /**
     * Returns the value of createdDate.
     *
     * @return the createdDate
     */
    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Sets the value of createdDate.
     *
     * @param createdDate
     *            the createdDate to set
     */
    public void setCreatedDate(final ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Returns the value of updatedDate.
     *
     * @return the updatedDate
     */
    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

    /**
     * Sets the value of updatedDate.
     *
     * @param updatedDate
     *            the updatedDate to set
     */
    public void setUpdatedDate(final ZonedDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    /**
     * Returns the value of createdBy.
     *
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the value of createdBy.
     *
     * @param createdBy
     *            the createdBy to set
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns the value of updatedBy.
     *
     * @return the updatedBy
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the value of updatedBy.
     *
     * @param updatedBy
     *            the updatedBy to set
     */
    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Meta {id=");
        builder.append(id);
        builder.append(", resourceName=");
        builder.append(resourceName);
        builder.append(", createdDate=");
        builder.append(createdDate);
        builder.append(", updatedDate=");
        builder.append(updatedDate);
        builder.append(", createdBy=");
        builder.append(createdBy);
        builder.append(", updatedBy=");
        builder.append(updatedBy);
        builder.append("}");
        return builder.toString();
    }

}
