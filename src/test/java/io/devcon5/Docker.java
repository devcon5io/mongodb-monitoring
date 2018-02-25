package io.devcon5;

import org.testcontainers.containers.GenericContainer;

/**
 * Helper class for dealing with TestContainers
 */
public final class Docker {

    /**
     * Convenience for <code> new GenericContainer(imageName)</code>
     * @param imageName
     *  the name of the docker image
     * @return
     *  a new GenericContainer
     */
    public static GenericContainer run(String imageName){
        return new GenericContainer(imageName);
    }
}
