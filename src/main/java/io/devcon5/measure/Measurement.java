/*
 *     Universal Collector for Metrics
 *     Copyright (C) 2017-2018 DevCon5 GmbH, Switzerland
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.devcon5.measure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A Measurement represents a collection of values at a specific point in time. The measurement may contain
 * several tags that allow classification/categorizations of these timepoints.
 */
public class Measurement {

    /**
     * Timestamp in nanoseconds
     */
    final long timestamp;
    /**
     * The name of the measurement. Must not be null.
     */
    final String name;

    /**
     * A sorted list of key-value pairs representing tags
     */
    final SortedMap<String, String> tags;

    /**
     * The actual values of these measurements. The values could be
     * <ul>
     * <li>Boolean</li>
     * <li>Integer</li>
     * <li>Long</li>
     * <li>Float</li>
     * <li>Double</li>
     * <li>String</li>
     * </ul>
     */
    final Map<String, Object> values;

    Measurement(Measurement.Builder builder) {

        this.timestamp = builder.timestamp;
        this.name = builder.name;
        this.tags = Collections.unmodifiableSortedMap(new TreeMap<>(builder.tags));

        final Map<String, Object> values = new HashMap<>();
        values.putAll(builder.booleanValues);
        values.putAll(builder.numberValues);
        values.putAll(builder.stringValues);
        this.values = Collections.unmodifiableMap(new TreeMap<>(values));
    }

    /**
     * The point in time of this measurement in nanoseconds since 1.1.1970
     *
     * @return the timestamp in ns
     */
    public long getTimestamp() {

        return timestamp;
    }

    /**
     * The name of the measurment
     *
     * @return a non-null, non-empty string representing this measurement
     */
    public String getName() {

        return name;
    }

    /**
     * The tags for categorization/calssification of the measurement.
     *
     * @return an unmodifiable map containing the key-value pairs
     */
    public SortedMap<String, String> getTags() {

        return tags;
    }

    /**
     * The actual values of this measurement. The values could be of the following types.
     * <ul>
     * <li>Boolean</li>
     * <li>Integer</li>
     * <li>Long</li>
     * <li>Float</li>
     * <li>Double</li>
     * <li>String</li>
     * </ul>
     *
     * @return an unmodifiable map containing the key-value pairs.
     */
    public Map<String, Object> getValues() {

        return values;
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder("Measurement{");
        sb.append("name='").append(name).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", tags=").append(tags);
        sb.append(", values=").append(values);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Measurement that = (Measurement) o;
        return timestamp == that.timestamp
                && Objects.equals(name, that.name)
                && Objects.equals(tags, that.tags)
                && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {

        return Objects.hash(timestamp, name, tags, values);
    }

    /**
     * Creates a new builder for a Measurment
     *
     * @return a new, non-null builder
     */
    public static Builder builder() {

        return new Builder();
    }

    public static class Builder {

        private String name;
        private long timestamp;
        private Map<String, String> tags = new HashMap<>();

        private Map<String, Number> numberValues = new HashMap<>();
        private Map<String, Boolean> booleanValues = new HashMap<>();
        private Map<String, String> stringValues = new HashMap<>();

        public Builder name(String name) {

            this.name = name;
            return this;
        }

        public Builder timestamp(long nanos) {

            this.timestamp = nanos;
            return this;
        }

        public Builder tag(String name, String value) {

            if(value != null) {
                this.tags.put(name, value);
            }
            return this;
        }

        public Builder value(String name, String value) {

            this.stringValues.put(name, value);
            return this;
        }

        public Builder value(String name, Boolean value) {

            this.booleanValues.put(name, value);
            return this;
        }

        public Builder value(String name, Integer value) {

            this.numberValues.put(name, value);
            return this;
        }

        public Builder value(String name, Long value) {

            this.numberValues.put(name, value);
            return this;
        }

        public Builder value(String name, Double value) {

            this.numberValues.put(name, value);
            return this;
        }

        public Builder value(String name, Float value) {

            this.numberValues.put(name, value);
            return this;
        }

        //method is only required for decoding
        public Builder value(final String name, final Object rawValue) {

            if (rawValue instanceof Number) {
                this.numberValues.put(name, (Number) rawValue);
                return this;
            } else if (rawValue instanceof Boolean) {
                return value(name, (Boolean) rawValue);
            } else if (rawValue instanceof String) {
                return value(name, (String) rawValue);
            }
            throw new IllegalArgumentException("Unsupported value type " + (rawValue == null
                                                                            ? "null"
                                                                            : rawValue.getClass()));
        }

        /**
         * Validates the collected parameters and creates the Measurement.
         *
         * @return a non-null Measurement
         *
         * @throws java.lang.IllegalArgumentException
         *         if the validation of the parameters failed
         */
        public Measurement build() {

            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name is not set");
            }
            if (timestamp < 0) {
                throw new IllegalArgumentException("timestamp is invalid: " + timestamp);
            }
            if (timestamp == 0) {
                timestamp = System.currentTimeMillis() * 1_000_000;
            }
            if (numberValues.isEmpty() && booleanValues.isEmpty() && stringValues.isEmpty()) {
                throw new IllegalArgumentException("no values recorded");
            }

            return new Measurement(this);
        }
    }
}









