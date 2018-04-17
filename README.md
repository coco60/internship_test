# Restful API in Scala with Spring

## About this Restful API

This application uses maven and the framework Spring. 
Several libraries have been used : [Json4s](http://json4s.org) and Jackson to manipulate JSON files. 
To validate a json file according to a schema this application uses the library [json-schema-validator](https://github.com/java-json-tools/json-schema-validator). 
The file with all the schemas will be stored in you home directory, its name will be schemaLibrary.txt .


## Build and run the service

To run the service, after downloading the project, you need to :
- get maven
- Open a terminal and go to the directory of the project
- enter the command : mvn spring-boot:run. You can start to send request !

Example of request : 
- to upload a schema : curl http://localhost:8080/schema/config-schema -X POST -d @config-schema.json -i
- to validate a Json with the schema uploaded previsouly : curl http://localhost:8080/validate/config-schema -X POST -d @config.json -i
