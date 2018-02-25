package io.devcon5.measure;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

/**
 * Marker interface for Digesters. Digesters should be implemented as Verticles that receive measurements sent
 * via Vert.x eventbus on the addess specified as DIGEST_ADDR
 *
 */
public interface Digester {

    /**
     * The address digesters receive measurements.
     */
    String DIGEST_ADDR = "persist";

    /**
     * Decodes an encoded measure using an appropriate decoder.
     * The default implementation supports the following types:
     * <ul>
     *     <li>{@link io.vertx.core.buffer.Buffer} - {@link io.devcon5.measure.BinaryEncoding}</li>
     *     <li>{@link io.vertx.core.json.JsonArray - {@link io.devcon5.measure.JsonEncoding}}</li>
     * </ul>
     * @param o
     *  the measurements in a specific encoding
     *
     * @return
     *  the decoded measurements
     */
    default Measurement[] decode(Object o){
        if(o instanceof Buffer){
            return BinaryEncoding.decoder().decode((Buffer)o);
        } else if(o instanceof JsonArray){
            return JsonEncoding.decoder().decode((JsonArray)o);
        }
        throw new IllegalArgumentException("Unsupported type " + (o == null ? "null" : o.getClass()));
    }
}
