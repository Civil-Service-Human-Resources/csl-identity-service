# csl-identity-service

csl-identity-service includes the following technical implementation:

* Actuator is included to monitor the application health and other runtime parameters.

### Build using:

* Java 17
* Spring Boot 3.1.0
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
    * `` docker run -it --rm -p 8080:8080 csl-identity-service-tag ``

### REST Endpoints:

A postman collection is available in [docs/csl-identity-service.postman_collection.json](docs/postman_collection/csl-identity-service.postman_collection.json) to execute above endpoints.

### Azure Build Pipeline:

* Azure build pipeline is used for the docker container build for the deployment in higher environment.
* Azure build pipeline configuration is available in [azure-pipeline.yml](azure-pipelines.yml) file.

### oauth2_registered_client Entries:

* SQL file to make the entries in local dev env is available in [docs/db/mysql/dev/V10.1__insert-oauth2-registered-client.sql](docs/db/mysql/dev/V10.1__insert-oauth2-registered-client.sql)
* SQL templates to make the entries in other envs are available in [docs/db/mysql/oauth2_client_entry_templates](docs/db/mysql/oauth2_client_entry_templates)
