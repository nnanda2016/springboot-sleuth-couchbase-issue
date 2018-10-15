package com.demo.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom {@link JsonSerializer} for serializing {@link ZonedDateTime} in the format {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}.
 *
 * @author Suraj Mohanan Kodiyath
 */
public class ZonedDateTimeJsonSerializer extends JsonSerializer<ZonedDateTime> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(final ZonedDateTime value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
		gen.writeString(value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
	}
}
