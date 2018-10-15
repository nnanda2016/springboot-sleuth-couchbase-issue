package com.demo.service;

import com.demo.model.Meta;
import com.demo.model.ResourceDetail;
import com.demo.util.BeanNames;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
@Component(BeanNames.SERVICE_HELPER)
public class ServiceHelper {
	public static final String CLASS_NAME = ServiceHelper.class.getCanonicalName();
	
	/**
     * Predicate to test if the given {@link JsonNode} is a Missing or Null node
     */
	private static final Predicate<JsonNode> NULL_NODE_PREDICATE = Predicates.or(JsonNode::isMissingNode, JsonNode::isNull);
	
	private static final Set<String> RESPONSE_META_FIELDS = ImmutableSet.of("revision");
	
	private final ObjectMapper jacksonObjectMapper;
	
	@Autowired
	public ServiceHelper(@Qualifier(BeanNames.JACKSON_OBJECT_MAPPER) final ObjectMapper objectMapper) {
		this.jacksonObjectMapper = objectMapper;
	}
	
	public void updateMetaNodeForResponse(final JsonNode jsonNode) {
		final JsonNode metaJsonNode = jsonNode.path("_meta");
		if (NULL_NODE_PREDICATE.negate().test(metaJsonNode)) {
			final Set<String> allMetaNodeFields = this.asStream(metaJsonNode.fieldNames(), false)
	                .collect(Collectors.toSet());
			final Set<String> fieldsToRemove = Sets.difference(allMetaNodeFields, RESPONSE_META_FIELDS);
	        
			// Remove meta node.
			((com.fasterxml.jackson.databind.node.ObjectNode) metaJsonNode).remove(fieldsToRemove);
		}
	}
	
	public JsonNode buildMetaNode(final String applicationId, final ResourceDetail resourceDetail) {
		final ZonedDateTime currentDate = ZonedDateTime.now(ZoneOffset.UTC);
        
		final Meta meta = new Meta();
        meta.setId(resourceDetail.getResourceId());
        meta.setResourceName(resourceDetail.getResourceName());
        meta.setCreatedBy(applicationId);
        meta.setCreatedDate(currentDate);
        meta.setUpdatedBy(applicationId);
        meta.setUpdatedDate(currentDate);
        return jacksonObjectMapper.valueToTree(meta);
	}
	
	public JsonNode getUpdatedMetaNode(final JsonNode jsonNode, final String applicationId, final ResourceDetail resourceDetail) {
		final JsonNode metaJsonNode = jsonNode.path("_meta");
		
        if (NULL_NODE_PREDICATE.negate().test(metaJsonNode)) {
            final com.fasterxml.jackson.databind.node.ObjectNode objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) metaJsonNode;
            objectNode
            	.put("updatedBy", applicationId)
            	.set("updatedDate", jacksonObjectMapper.valueToTree(ZonedDateTime.now(ZoneOffset.UTC)));

            objectNode.remove("revision");
            return objectNode;
        } else {
            return this.buildMetaNode(applicationId, resourceDetail);
        }
	}
	
	public JsonNode addMetaNodeToJsonNode(final JsonNode document, final JsonNode metaNode) {
		((com.fasterxml.jackson.databind.node.ObjectNode) document).set("_meta", metaNode);
		return document;
	}
	
	private <T> Stream<T> asStream(final Iterator<T> iterator, final boolean parallel) {
		if (iterator == null) {
			return Stream.empty();
		}

		final Iterable<T> iterable = () -> iterator;
		return StreamSupport.stream(iterable.spliterator(), parallel);
	}
}
