# csl-identity-service

csl-identity-service details here

This service includes the following technical implementation:

* Actuator is included to monitor the application health and other runtime parameters.

### Build using:

* Java 17
* [Spring Boot 3.0](docs/HELP.md)
* Maven Wrapper 3.8.6
* Docker 4.15.0
* Other dependencies are available in [pom.xml](pom.xml)

### Running Locally:

The application requires Java 17 installed to build and run.

Run the application either using IDE e.g. Intellij or Maven or Docker as follows:

* Intellij: Use any one of the following option:
  * `` Run -> Run 'CslIdentityService' ``
  * `` Run -> Debug 'CslIdentityService' ``

* Maven:
  * `` ./mvnw clean install ``
  * `` ./mvnw spring-boot:run ``

* Docker:
    * `` docker build -t csl-identity-service-tag . ``
    * `` docker run -it --rm -p 9003:9003 csl-identity-service-tag ``

### REST Endpoints:

A postman collection is available in [docs/csl-identity-service.postman_collection.json](docs/postman_collection/csl-identity-service.postman_collection.json) to execute above endpoints.

### Azure Build Pipeline:

* Azure build pipeline is used for the docker container build for the deployment in higher environment.
* Azure build pipeline configuration is available in [azure-pipeline.yml](azure-pipelines.yml) file.
