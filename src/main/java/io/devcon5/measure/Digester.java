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

    default Measurement[] decode(Object o){
        if(o instanceof Buffer){
            return BinaryEncoding.decoder().decode((Buffer)o);
        } else if(o instanceof JsonArray){
            return JsonEncoding.decoder().decode((JsonArray)o);
        }
        throw new IllegalArgumentException("Unsupported type " + (o == null ? "null" : o.getClass()));
    }
}
