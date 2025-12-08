# Database Version Project Plan

## Goal Description
Create a new project `databaseVersion` based on `simplestVersion` that uses a persistent database (H2 file-based) for URL storage. It will also include a mechanism to simulate variable database latency using AOP, configurable via an API, to demonstrate performance impacts under load.

## User Review Required
> [!IMPORTANT]
> **Persistence**: To satisfy the requirement that data "survives restart" (simulated by `DirtiesContext`), we will use H2 in **file mode** (`jdbc:h2:file:./target/shortUrldb`) instead of in-memory. This ensures data remains on disk even when the Spring Context is reloaded.

## Proposed Changes

### Project Structure
- Create `c:\projects\learn\examples\shortUrl\databaseVersion`
- Copy content from `simplestVersion` excluding `target` and `test`.

### Dependencies
- Add to `pom.xml`:
    - `spring-boot-starter-data-jpa`
    - `com.h2database:h2`
    - `spring-boot-starter-aop`

### Persistence
#### [NEW] [ShortUrl.java](file:///c:/projects/learn/examples/shortUrl/databaseVersion/src/main/java/com/example/shorturl/model/ShortUrl.java)
- JPA Entity `ShortUrl`
- Fields: `shortUrl` (String, PK) - *Renamed from 'id' per feedback*, `originalUrl` (String)

#### [NEW] [ShortUrlRepository.java](file:///c:/projects/learn/examples/shortUrl/databaseVersion/src/main/java/com/example/shorturl/repository/ShortUrlRepository.java)
- Extends `JpaRepository<ShortUrl, String>`

### Service Layer
#### [MODIFY] [UrlService.java](file:///c:/projects/learn/examples/shortUrl/databaseVersion/src/main/java/com/example/shorturl/service/UrlService.java)
- Inject `ShortUrlRepository`
- **No HashMap**: Complete replacement with Repository.
- `shorten` method:
    - Generate unique short code.
    - Check if exists (`repository.existsById(shortUrl)`).
    - Save new entity `new ShortUrl(shortUrl, originalUrl)`.
    - *No delay on writes*.
- `resolve` method:
    - `repository.findById(shortUrl)` (will be intercepted by AOP).

### AOP & Delays
#### [NEW] [DelayManager.java](file:///c:/projects/learn/examples/shortUrl/databaseVersion/src/main/java/com/example/shorturl/aop/DelayManager.java)
- Stores delay configuration. Default: 20ms.

#### [NEW] [DelayController.java](file:///c:/projects/learn/examples/shortUrl/databaseVersion/src/main/java/com/example/shorturl/controller/DelayController.java)
- `POST /api/config/delay?min=X&max=Y` to configure delay range.

#### [NEW] [DelayAspect.java](file:///c:/projects/learn/examples/shortUrl/databaseVersion/src/main/java/com/example/shorturl/aop/DelayAspect.java)
- `@Aspect` class
- `@Around` advice targeting **only read operations**: `ShortUrlRepository.findById`.
- Simulates network/DB latency.

### Documentation
#### [MODIFY] [README.md](file:///c:/projects/learn/examples/shortUrl/README.md)
- Add link to `databaseVersion`

#### [NEW] [plan.md](file:///c:/projects/learn/examples/shortUrl/databaseVersion/plan/plan.md)
- Describe the project.

## Verification Plan

### Automated Tests
1.  **Functional Tests**:
    - `UrlServiceTest`: Verify logic.
    - `UrlControllerIntegrationTest`: Verify standard API flow.

2.  **Persistence Test ("Survives Restart")**:
    - `PersistenceIntegrationTest`:
        - Test: Create a short URL.
        - Trigger `@DirtiesContext` (simulating app restart).
        - Verify validation: Can resolve the same short URL? (Should pass due to file-based DB).

3.  **Performance/Delay Test**:
    - `ReadDelayIntegrationTest`:
    - Configure delay (e.g., 100ms).
    - Measure `GET /{shortId}` response time.
    - Verify it takes at least 100ms.
    - Reset delay to 0, verify it is fast again.

### Manual Verification
1.  Start app.
2.  Create URL -> Restart App -> Try to resolve URL (should work).
3.  Set delay -> Verify browser load is slower.
