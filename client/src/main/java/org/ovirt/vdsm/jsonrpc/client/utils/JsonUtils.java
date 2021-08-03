package org.ovirt.vdsm.jsonrpc.client.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcRequest;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcResponse;
import org.ovirt.vdsm.jsonrpc.client.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for json marshalling.
 *
 */
public class JsonUtils {
    public static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final double GRACE_PERIOD = 0.5;
    public static final String ALL = "*";
    public static final String SUBSCRIPTION_ALL = "*|*|*|*";
    private static Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private static ObjectMapper mapper = new ObjectMapper();
    private static JsonFactory factory = mapper.getFactory();
    static {
        mapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
    }

    public static Map<String, Object> mapValues(JsonNode node) {
        Map<String, Object> map = null;
        try {
            map = mapper.readValue(mapper.writeValueAsBytes(node),
                    new TypeReference<HashMap<String, Object>>() {
                    });
        } catch (IOException e) {
            log.debug("Exception thrown during marshalling json", e);
        }
        return map;
    }

    public static byte[] jsonToByteArray(JsonNode json) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            try (JsonGenerator gen = factory.createGenerator(os, JsonEncoding.UTF8)) {
                gen.writeTree(json);
            }
        } catch (IOException e) {
            log.debug("Exception thrown during marshalling json", e);
        }
        return os.toByteArray();
    }

    public static byte[] jsonToByteArray(List<JsonRpcRequest> requests) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            try (JsonGenerator gen = factory.createGenerator(os, JsonEncoding.UTF8)) {
                gen.writeStartArray();
                for (final JsonRpcRequest request : requests) {
                    gen.writeTree(request.toJson());
                }
                gen.writeEndArray();
            }
        } catch (IOException e) {
            log.debug("Exception thrown during marshalling json", e);
        }
        return os.toByteArray();
    }

    public static <T> JsonRpcResponse buildErrorResponse(JsonNode id, T code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        return new ResponseBuilder(id).withError(error).build();
    }

    public static JsonRpcResponse buildFailedResponse(JsonRpcRequest request) {
        return buildErrorResponse(request.getId(),
                5022,
                "Message timeout which can be caused by communication issues");
    }

    public static String getAddress(String host, int port) {
        return host + ":" + port;
    }

    public static ByteBuffer cloneBuffer(ByteBuffer original) {
        int pos = original.position();
        original.clear();
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        clone.put(original);
        clone.flip();
        clone.position(pos);
        return clone;
    }

    public static boolean isEmpty(String value) {
        return value == null || "".equals(value.trim());
    }

    public static int reduceGracePeriod(int interval) {
        return interval - (int) (interval * GRACE_PERIOD);
    }

    public static int addGracePeriod(int interval) {
        return interval + (int) (interval * GRACE_PERIOD);
    }

    public static String swapHeartbeat(String heartbeat) {
        String[] heartbeats = heartbeat.split(",");
        return heartbeats[1] + "," + heartbeats[0];
    }

    public static long getTimeout(int timeout, TimeUnit unit) {
        return Clock.systemUTC().millis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
    }

    public static void logException(Logger logger, String message, Throwable throwable) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, throwable);
        } else {
            logger.error(message);
        }
    }

    public static String[] parse(String id) {
        String[] ids = id.split("\\|");
        if (ids.length != 4) {
            throw new IllegalArgumentException("wrong id format");
        }
        return ids;
    }
}
