# MongoDB Mointoring 

This services monitors MongoDB server by periodically polling the MongoDB server and pushing the status data into
a timeseries DB, which can be further visualized with an appropriate tool.

This solution is designed for and with 

- [MongoDB](https://www.mongodb.com/) - the monitored database
- [Influx DB](https://www.influxdata.com/time-series-platform/influxdb/) - for storing the timeseries data
- [Grafana](https://grafana.net/) - for visualizing the status information
- [Vert.x](http://vertx.io/)

# Architecture
This solution is based on [Vert.x](http://vertx.io/), an asynchronous, non-blocking framework for developing 
web apps and microservices. Vert.x is polyglott and can usees so called Verticles as deployment units. Verticles
adhere to the actor model of concurrency.

This monitoring tool provides a vertical that periodically pools a mongo DB server, transforms its status data and
pushes them into an InfluxDB.

This data can be visualized with Grafana. 

All used tools and frameworks are Open Source.

# Setup and Run

1. Download and install [Influx DB](https://www.influxdata.com/time-series-platform/influxdb/)
2. Initialize the timeseries DB by creating a database and configure a retention policy
    ```
    CREATE DATABASE mongo
    CREATE RETENTION POLICY "one_month" ON "mongo" DURATION 30d REPLICATION 1 DEFAULT
    ```
3. Download and install [Grafana](https://grafana.net/) - for visualizing the status information
4. Create a configuration file
    ```
    {
      "interval": 1000,
      "mongoServer": [
        {
          "host": "localhost",
          "db_name": "test",
          "port": 27017,
          "collections": [
            "example"
          ]
        }
      ],
      "influx": {
        "dbname": "mongo",
        "host": "localhost",
        "port": 8086
      }
    }
    
    ```
   - the interval for polling the MongoDB server, in ms 
   - mongo server defines the host and port of the server to monitor. To collects statistics of a database, the `db_name`
    has to be specified. If you want to monitor multiple databases, you have to configure multiple mongoServers (its and 
    array). To collect statistics of the collections of the database, add the collection names to the `collections` field't
    array. 
   - the influx document contains the connection parameters the for the influs db server and db to store the measures.
   - to authenticate with MongoDB, use `username` and `password` property, see 
   also [Vert.x Mongo Client](http://vertx.io/docs/vertx-mongo-client/java/)
5. Download the [Vert.x](http://vertx.io/) full-distribution and put it's bin/ folder on the PATH so you can execute 
vertx from any path
6. Start Vertx using the configuration and deploy the Verticle
    ```
    vertx run -conf config.json MongoInfluxMonitoringVerticle.js
    ```

Note, this solution provides only basic capabilities but may be a good starting point for building your own custom 
monitoring solution.

- no authentication for InfluxDB
- not all statistics are captured, but you can add additional mappings if needed (just look at the code)
- it has been tested with Mongo server running on Windows, the serverStatus contains an extraInfo field that may not work on linux. If that
causes problems, remove the `pagefile` measure
