# Volk Sniffer

A file-crawling agent for the **Volk eDiscovery platform**. The sniffer watches one or more
directories (e.g. network shares), filters for MS Office documents, extracts their embedded
metadata (title, author, keywords, comments, created/modified dates) with Apache POI, and
stores the results in MongoDB — attributed to the file's owner.

Volk is made up of three repositories:

| Repo | Role |
| --- | --- |
| [volk-sniffer](https://github.com/armper/volk-sniffer) | Crawls file shares and ingests file + Office metadata into MongoDB |
| [volk-rest](https://github.com/armper/volk-rest) | Reactive REST API that serves the ingested data |
| [volk-ui](https://github.com/armper/volk-ui) | Angular front end for searching users and browsing their files |

```
directories ──> volk-sniffer ──> MongoDB <── volk-rest <── volk-ui (Angular)
                (Camel + POI)     (volk db)   (WebFlux, :8091)   (:4200)
```

## Tech stack

- Java 17, Spring Boot 3.5
- Apache Camel 4.14 (`camel-file` component drives the crawl)
- Apache POI 5.4 (OLE2 + OOXML metadata extraction)
- Spring Data MongoDB

## How it works

1. `FileSystemRoute` creates one Camel `file:` route per configured directory
   (`noop=true`, so files are left in place and not re-processed).
2. `OfficeDocumentFilter` accepts only the extensions listed under
   `file-types.office-document` and skips `~$` Office lock files.
3. For each accepted file, the route:
   - resolves the file's OS owner into a `SearchUser` (`DOMAIN\name` on Windows,
     plain user name on POSIX), creating it if it doesn't exist yet;
   - extracts Office metadata via `MsOfficeExtractor` — legacy OLE2 formats
     (`.doc`, `.xls`, `.ppt`) through the HPSF `SummaryInformation`, OOXML formats
     (`.docx`, `.xlsx`, `.pptx`, ...) through `POIXMLProperties` core properties;
   - records file-system attributes (size, created, last modified);
   - appends the resulting `SearchFile`/`OfficeDocument` to the owner's
     `searchFiles` and saves it to MongoDB.

## Configuration

`src/main/resources/application.yaml`:

| Property | Default | Description |
| --- | --- | --- |
| `volk.sniffer.directories` | `/tmp/volk-inbox` | Comma-separated list of directories to crawl (env: `VOLK_SNIFFER_DIRECTORIES`) |
| `volk.sniffer.recursive` | `false` | Whether to descend into subdirectories |
| `file-types.office-document` | `doc, docx, ...` | Extensions accepted by the filter |
| `spring.data.mongodb.database` | `volk` (profile `local`) | Target MongoDB database |

## Running

Requires Java 17+ and a MongoDB instance on `localhost:27017`
(`docker compose up -d` in the volk-rest repo starts one).

```bash
./mvnw spring-boot:run
# or
./mvnw package && java -jar target/sniffer-2.0.0.jar

# crawl specific shares
VOLK_SNIFFER_DIRECTORIES=/mnt/share1,/mnt/share2 java -jar target/sniffer-2.0.0.jar
```

Drop an Office document into a watched directory and it shows up in the `volk`
database (collection `searchUser`), ready to be served by volk-rest.

## History

Originally written in 2018 against Spring Boot 1.5 / Camel 2.20 / POI 3.17 with a
hardcoded UNC path. Modernized in 2026: Spring Boot 3.5 (Java 17), Camel 4.14,
POI 5.4, configurable watch directories, POSIX owner support, and removal of unused
PowerShell/WMI experiments and committed build artifacts.
