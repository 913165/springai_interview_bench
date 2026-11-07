# Interview Bench

A local interview-question library. Spring Boot serves the pages and the API. Questions are stored in a JSON file outside the JAR, so you can edit them without a database or a rebuild.

## Requirements

- Java 21 or newer
- Maven 3.9+ (or the Maven Wrapper, if `mvn` is not on your PATH)

## Build

From the project directory:

```text
mvn clean package
```

If `mvn` is not installed:

```text
mvnw.cmd clean package
```

```text
./mvnw clean package
```

The runnable archive is `target/interview-platform.jar`.

## Run on Windows

Open a terminal in the project directory, then:

```text
mvn clean package
java -jar target\interview-platform.jar
```

## Run on Linux or macOS

```text
mvn clean package
java -jar target/interview-platform.jar
```

The application listens on [http://127.0.0.1:8091](http://127.0.0.1:8091).

## Where questions are stored

`./data/questions.json`, relative to the directory you start the process from.

Change the location in `src/main/resources/application.properties` or with an override:

```text
java -jar target/interview-platform.jar --interview.data-file=D:\notes\questions.json
```

The file is created from the built-in samples only when it is missing. An existing file is left as it is. If a write is interrupted, the previous file is kept as `questions.json.bak` and restored the next time the file itself is missing.

## Admin key

Reading is open. Adding, editing, and deleting require the `X-Admin-Key` header. The local default is `local-admin`. Change `interview.admin-key` before anyone else can reach the process. The admin page is [http://127.0.0.1:8091/admin](http://127.0.0.1:8091/admin).

A question URL looks like `http://127.0.0.1:8091/questions/chatclient-fluent-api`.

## Useful commands

Search and filters:

```text
GET /api/questions?category=Core%20Java&difficulty=Advanced&search=hashCode
```
