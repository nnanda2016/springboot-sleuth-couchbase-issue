package com.demo.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;

/**
 * Custom {@link JsonDeserializer} for deserializing {@link ZonedDateTime} in the format {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}.
 *
 * @author Suraj Mohanan Kodiyath
 */
public class ZonedDateTimeJsonDeserializer extends JsonDeserializer<ZonedDateTime> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ZonedDateTime deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
	    final String value = p.readValueAs(String.class);
		return StringUtils.isNotEmpty(value) ? ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME) : null;
	}
}
