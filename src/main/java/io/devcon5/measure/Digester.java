package io.devcon5.measure;

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
}
