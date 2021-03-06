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

var eb = vertx.eventBus();
var config = Vertx.currentContext().config();
var DIGEST_ADDR = Java.type('io.devcon5.measure.Digester').DIGEST_ADDR;

var MongoClient = require("vertx-mongo-js/mongo_client");

////// init Mongo Clients
var mongoClients = config.servers.map(function (config) {
    return {
        client: MongoClient.createNonShared(vertx, config),
        collections: config.collections,
        dbName: config.db_name
    };
});

vertx.setPeriodic(config.interval, function (timerId) {

    mongoClients.forEach(function (mongo) {

        /*
         * Run serverStatus for each configured mongoDB
         */
        mongo.client.runCommand("serverStatus", {"serverStatus": 1}, function (status, status_err) {

            if (status_err === null) {

                //get the meta-data for this server, also used for colStats and dbStats
                var tags = {
                    host: status.host,
                    version: status.version,
                    storageEngine: status.storageEngine.name
                };

                eb.publish(DIGEST_ADDR, mapServerStatus(status, tags));
                tags.dbname = mongo.dbName;
                /*
                 * Collect DB Statistics from the current database
                 */
                mongo.client.runCommand("dbStats", {"dbStats": 1}, function (dbStats, dbStats_err) {
                    if (dbStats_err === null) {
                        eb.publish(DIGEST_ADDR, mapDBStats(dbStats, tags));
                    } else {
                        console.error("collecting dbStats failed " + dbStats_err);
                    }
                });
                /*
                 * Collect Collection Statistics for each configured collection
                 */
                mongo.collections.forEach(function (colName) {
                    mongo.client.runCommand("collStats", {"collStats": colName}, function (stats, stats_err) {
                        if (stats_err === null) {
                            //add additional stats
                            tags.colName = colName;
                            tags.ns = stats.ns;
                            eb.publish(DIGEST_ADDR, mapColStats(stats, tags));
                        } else {
                            console.error("colLStats failed for '" + colName + "' : " + status_err);
                        }
                    });
                });
            } else {
                console.error("Server status failed" + status_err);
            }
        });
    });
});
console.log("MongoCollector verticle started")

/**
 * Function to map serverStatus document to a datapoints and tags. The result is an object with the datapoints
 * property and a tags property
 * @param status
 * @returns {*[]}
 */
function mapServerStatus(status, tags) {

    var timestamp = new Date(status.localTime.$date).getTime() * 1000000 //ns;

    var ret = [
        measure("connections", tags, timestamp, {
            "current": status.connections.current,
            "available": status.connections.available,
            "totalCreated": status.connections.totalCreated
        }),
        measure("mem", tags, timestamp, {
            "resident": status.mem.resident,
            "virtual": status.mem.virtual,
            "mapped": status.mem.mapped
        }),
        measure("metrics_document", tags, timestamp, {
            "deleted": status.metrics.document.deleted,
            "inserted": status.metrics.document.inserted,
            "returned": status.metrics.document.returned,
            "updated": status.metrics.document.updated
        }),
        measure("wiredTiger_cache", tags, timestamp, {
            "max_bytes": status.wiredTiger.cache["maximum bytes configured"],
            "current_bytes": status.wiredTiger.cache["bytes currently in the cache"]
        }),
        measure("wiredTiger_sessions", tags, timestamp, {
            "session_count": status.wiredTiger.session["open session count"],
            "cursor_count": status.wiredTiger.session["open cursor count"]
        }),
        measure("tcmalloc_generic", tags, timestamp, {
            "current_allocated_bytes": status.tcmalloc.generic.current_allocated_bytes,
            "heap_size": status.tcmalloc.generic.heap_size
        }),
        measure("tcmalloc_detail", tags, timestamp, {
            "pageheap_free_bytes": status.tcmalloc.tcmalloc["pageheap_free_bytes"],
            "pageheap_unmapped_bytes": status.tcmalloc.tcmalloc["pageheap_unmapped_bytes"],
            "max_total_thread_cache_bytes": status.tcmalloc.tcmalloc["max_total_thread_cache_bytes"],
            "current_total_thread_cache_bytes": status.tcmalloc.tcmalloc["current_total_thread_cache_bytes"],
            "central_cache_free_bytes": status.tcmalloc.tcmalloc["central_cache_free_bytes"],
            "transfer_cache_free_bytes": status.tcmalloc.tcmalloc["transfer_cache_free_bytes"],
            "thread_cache_free_bytes": status.tcmalloc.tcmalloc["thread_cache_free_bytes"],
            "aggressive_memory_decommit": status.tcmalloc.tcmalloc["aggressive_memory_decommit"]
        })];

    if(status.extra_info.usagePageFileMB){
        ret.push(measure("pagefile", tags, timestamp, {
            "usage": status.extra_info.usagePageFileMB,
            "available": status.extra_info.totalPageFileMB,
            "total": status.extra_info.totalPageFileMB
        }));
    }
    return ret;
}

/**
 * Function to map collection statistics to datapoints
 * @param stats
 *  the stats document returned from mongo
 * @param tags
 *  tags object to be used for the datapoints. Additional tags are added
 * @returns {*[]} an array of datapoint strings
 */
function mapColStats(stats, tags) {

    var timestamp = new Date().getTime() * 1000000 //ns;

    return [
        measure("colstats", tags, timestamp, {
            "count": stats.count,
            "size": stats.size,
            "average_obj_size": stats.avgObjSize,
            "storageSize": stats.storageSize,
            "totalIndexSize": stats.totalIndexSize
        })];
}

/**
 * Function to map db statistics to datapoints
 * @param stats
 *  the stats document returned from mongo
 * @param tags
 *  tags object to be used for the datapoints. Additional tags are added
 * @returns {*[]} an array of datapoint strings
 */
function mapDBStats(stats, tags) {

    var timestamp = new Date().getTime() * 1000000 //ns;

    return [
        measure("dbstats", tags, timestamp, {
            "collections": stats.collections,
            "objects": stats.objects,
            "average_obj_size": stats.avgObjSize,
            "dataSize": stats.dataSize,
            "storageSize": stats.storageSize,
            "numExtents": stats.numExtents,
            "indexes": stats.indexes,
            "indexSize": stats.indexSize,
        })];
}


////// Helper functions


function measure(name, tags, timestamp, fieldObj) {

    return {
        name: name,
        timestamp: timestamp,
        tags: tags,
        values: fieldObj
    };
}

