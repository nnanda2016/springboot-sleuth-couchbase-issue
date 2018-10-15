package com.demo.util;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
public final class Utils {
	private Utils() {}
    
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    
    public static final Pattern HTTP_STATUS_CODE_WITHIN_ERROR_CODES_PATTERN = Pattern.compile("_(1|2|3|4|5){1}\\d{2}");
    
    public static final Function<String, Integer> FN_GET_HTTP_STATUS_CODE_FROM_ERROR_CODE = errorCode -> {
        if (StringUtils.isNotBlank(errorCode)) {
            final Matcher matcher = HTTP_STATUS_CODE_WITHIN_ERROR_CODES_PATTERN.matcher(errorCode);
            if (matcher.find()) {
                try {
                    return Integer.valueOf(StringUtils.substring(matcher.group(0), 1));
                } catch (final Exception e) {
                    logger.warn("Cannot determine HTTP status code from given error code '{}'", errorCode);
                }
            }
        }
        
        return Integer.valueOf(-1);
    };
    
    public static final BiFunction<HttpHeaders, String, Optional<String>> GET_FIRST_HEADER_VALUE_FROM_HTTP_HEADERS = (httpHeaders, headerName) ->
						Optional.ofNullable(httpHeaders.get(headerName))
							.filter(CollectionUtils::isNotEmpty)
							.flatMap(list -> Optional.ofNullable(list.get(0)))
							;
						
	public static final BiFunction<String, String, String> FN_DOCUMENT_KEY_SUPPLIER = (resourceName, id) ->
						new StringBuilder()
			.append(resourceName)
			.append("-")
			.append(id)
			.toString();

	public static final BiFunction<ServerRequest, String, Optional<List<String>>> GET_HEADER_VALUES = (httpRequest, headerName) ->
						Optional.ofNullable(httpRequest.headers().header(headerName));
						
	public static final BiFunction<ServerRequest, String, Optional<String>> GET_FIRST_HEADER_VALUE_FROM_SERVER_REQ = (httpRequest, headerName) ->
						GET_HEADER_VALUES.apply(httpRequest, headerName)
							.filter(CollectionUtils::isNotEmpty)
							.flatMap(list -> Optional.ofNullable(list.get(0)))
							;

}
