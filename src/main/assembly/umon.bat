@echo off
set CLASSPATH=universalcollector.jar
vertx run -conf config.json -cp %CLASSPATH% io.devcon5.collector.Collector
