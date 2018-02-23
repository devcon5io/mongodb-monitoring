
Run Grafana
------------

    docker run -d --name=grafana \
        -p 3000:3000 \
        grafana/grafana
     
Run Influx
----------

    docker run -d --name=influx \
        -p 2003:2003 \
        -p 4242:4242 \
        -p 8083:8083 \
        -p 8086:8086 \
        -p 8088:8088 \
        -p 25826:25826/udp \
        influxdb/influxdb
        
To access influx from grafana, use the docker internal ip, i.e. `http://172.17.0.3:8086`

Run Artifactory
---------------

    docker run --name artifactory -d \
        -p 8081:8081 \
        docker.bintray.io/jfrog/artifactory-oss:latest


Run Sonarqube
-------------

    docker run -d --name sonarqube \
        -p 9000:9000 \
        -p 9092:9092 \
        sonarqube:6.5
