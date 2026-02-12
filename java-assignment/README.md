# Java Code Assignment

This is a short code assignment that explores various aspects of software development, including API implementation, documentation, persistence layer handling, and testing.

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file.

For a detailed technical description of the implementation, requirements compliance, and test strategy, see [DOCUMENTATION](DOCUMENTATION.md). For a senior architect review, gap analysis, and improvement suggestions, see [ARCHITECTURE_REVIEW_AND_IMPROVEMENTS](ARCHITECTURE_REVIEW_AND_IMPROVEMENTS.md).

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

To compile and run this demo you will need:

- JDK 17+

In addition, you will need either a PostgreSQL database, or Docker to run one.

### Configuring JDK 17+

Make sure that `JAVA_HOME` environment variables has been set, and that a JDK 17+ `java` command is on the path.

## Building the demo

Execute the Maven build on the root of the project:

```sh
./mvnw package
```

## Running the demo

### Live coding with Quarkus

The Maven Quarkus plugin provides a development mode that supports
live coding. To try this out:

```sh
./mvnw quarkus:dev
```

In this mode you can make changes to the code and have the changes immediately applied, by just refreshing your browser.

    Hot reload works even when modifying your JPA entities.
    Try it! Even the database schema will be updated on the fly.

## (Optional) Run Quarkus in JVM mode

When you're done iterating in developer mode, you can run the application as a conventional jar file.

First compile it:

```sh
./mvnw package
```

Next we need to make sure you have a PostgreSQL instance running (Quarkus automatically starts one for dev and test mode). To set up a PostgreSQL database with Docker:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

Connection properties for the Agroal datasource are defined in the standard Quarkus configuration file,
`src/main/resources/application.properties`.

Then run it:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```
    Have a look at how fast it boots.
    Or measure total native memory consumption...


## See the demo in your browser

Navigate to:

<http://localhost:8080/index.html>

## API documentation (Swagger UI)

With the app running (e.g. `./mvnw quarkus:dev`):

- **Swagger UI:** <http://localhost:8080/q/swagger-ui> — explore and try Store, Product, and Warehouse endpoints.
- **OpenAPI JSON:** <http://localhost:8080/q/openapi> — machine-readable API spec.

## Postman

A Postman collection is provided for all APIs:

- **File:** `postman/Java-Assignment-API.postman_collection.json`
- **Import:** In Postman, **Import** → choose this file (or the `postman` folder).
- **Variable:** The collection uses `baseUrl` (default `http://localhost:8080`). Change it in the collection variables if your server runs elsewhere.

The collection includes: **Store** (list, get, create, update, patch, delete), **Product** (list, get, create, update, delete), **Warehouse** (list, get, create, replace, archive).

Have fun, and join the team of contributors!

## Troubleshooting

Using **IntelliJ**, in case the generated code is not recognized and you have compilation failures, you may need to add `target/.../jaxrs` folder as "generated sources".

## Ready for GitHub

- **Build & tests:** `./mvnw clean test` — all 58 tests and JaCoCo should pass.
- **Run locally:** `./mvnw quarkus:dev` (or use PostgreSQL and `./mvnw package` then `java -jar target/quarkus-app/quarkus-run.jar`).
- **APIs:** Store (`/store`), Product (`/product`), Warehouse (`/warehouse`), Location (used by warehouse). Error responses use a common structured format (status, message, traceId).
- Before publishing: ensure no secrets in `application.properties`, and that `target/` is in `.gitignore`.