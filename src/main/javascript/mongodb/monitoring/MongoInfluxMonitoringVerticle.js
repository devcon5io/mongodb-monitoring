var MongoClient = require("vertx-mongo-js/mongo_client");

var config = Vertx.currentContext().config();

////// init Mongo Clients
var mongoClients = config.mongoServer.map(function (config) {
    return {
        client: MongoClient.createNonShared(vertx, config),
        collections: config.collections,
        dbName : config.db_name
    };
});

///// Influx DB Configuration
var influxDB = {
    http: vertx.createHttpClient(),
    post: function (data) {
        influxDB.http.post(config.influx.port, config.influx.host, "/write?db=" + config.influx.dbname, responseHandler)
            .end(Array.isArray(data) ? data.join("\n") : data);
    }
};

vertx.setPeriodic(config.interval, function (timerId) {

    mongoClients.forEach(function (mongo) {

        /*
         * Run serverStatus for each configured mongoDB
         */
        mongo.client.runCommand("serverStatus", {"serverStatus": 1}, function (status, status_err) {

            if (status_err == null) {

                //get the meta-data for this server, also used for colStats and dbStats
                var tags = {
                    host: status.host,
                    version: status.version,
                    storageEngine: status.storageEngine.name
                };

                //post to influx
                influxDB.post(mapServerStatus(status, tags));
                tags.dbname = mongo.dbName;
                /*
                 * Collect DB Statistics from the current database
                 */
                mongo.client.runCommand("dbStats", {"dbStats": 1}, function (dbStats, dbStats_err) {
                    if (dbStats_err == null) {
                        influxDB.post(mapDBStats(dbStats, tags));
                    } else {
                        console.error("Server status failed" + dbStats_err);
                    }
                });
                /*
                 * Collect Collection Statistics for each configured collection
                 */
                mongo.collections.forEach(function (colName) {
                    mongo.client.runCommand("collStats", {"collStats": colName}, function (stats, stats_err) {
                        if (stats_err == null) {
                            //add additional stats
                            tags.colName = colName;
                            tags.ns = stats.ns;
                            influxDB.post(mapColStats(stats, tags));
                        } else {
                            console.error("Server status failed" + status_err);
                        }
                    });
                });
            } else {
                console.error("Server status failed" + status_err);
            }
        });
    });
});
console.log("ServerStatus verticle started")
/**
 * Function to map serverStatus document to a datapoints and tags. The result is an object with the datapoints
 * property and a tags property
 * @param status
 * @returns {*[]}
 */
function mapServerStatus(status, tags) {

    var timestamp = new Date(status.localTime.$date).getTime() * 1000000 //ns;

    return [
        measure("connections", tags, timestamp, {
            "current": status.connections.current,
            "available": status.connections.available,
            "totalCreated": status.connections.totalCreated
        }),
        measure("pagefile", tags, timestamp, {
            "usage": status.extra_info.usagePageFileMB,
            "available": status.extra_info.totalPageFileMB,
            "total": status.extra_info.totalPageFileMB
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
        })
    ];
}

/**
 * Function to map collection statistics to datapoints
 * @param stats
 *  the stats document returned from mongo
 * @param tags
 *  tags object to be used for the datapoints. Additional tags are added
 * @returns {*[]} an array of datapoint strings
 */
function mapColStats(stats, tags ) {

    var timestamp = new Date().getTime() * 1000000 //ns;

    return [
        measure("colstats", tags, timestamp, {
            "count": stats.count,
            "size": stats.size,
            "average_obj_size": stats.avgObjSize,
            "storageSize": stats.storageSize,
            "totalIndexSize": stats.totalIndexSize
        })
    ];
}

/**
 * Function to map db statistics to datapoints
 * @param stats
 *  the stats document returned from mongo
 * @param tags
 *  tags object to be used for the datapoints. Additional tags are added
 * @returns {*[]} an array of datapoint strings
 */
function mapDBStats(stats, tags ) {

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
        })
    ];
}


////// Helper functions

/**
 * Creates a Influx LineProtocol measure of the format
 * <pre>
 *     measure_name[,tag_name=tag_value]* field_name=field_value[,field_name=field_value]* timestamp
 * </pre>
 * @param name
 *  the name of the measure, used for measure_name
 * @param tags
 *  an object with tags. Each property name is used as tag_name and the according value as tag_value
 * @param timestamp
 *  the timestamp in nanoseconds
 * @param fieldObj
 *  on object withe fields. Each property name is used as field_name and the according value as field_value
 * @returns {string}
 *  a string representing a measure for influx
 */
function measure(name, tags, timestamp, fieldObj) {
    return name + "," + flatten(tags) + " " + flatten(fieldObj) + " " + timestamp;
}

/**
 * Flattens an object into a key=value pair representation, with each pair separated by a comma
 * @param obj
 *  an object, i.e. { "aKey" : "aValue", "bKey":"bValue"}
 * @returns {string}
 *  a comma separated string of the key-value pairs , i.e. aKey=aValue,bKey=bValue
 */
function flatten(obj) {
    var result = [];
    for (var key in obj) {
        if (obj.hasOwnProperty(key)) {
            result.push(key + "=" + obj[key]);
        }
    }
    return result.join(",");
}

function responseHandler(response) {
    if (response.statusCode() >= 400) {
        console.warn("< " + (response.statusCode() + " " + response.statusMessage()));
        response.bodyHandler(function (data) {
            console.warn(data);
        })
    }
}
