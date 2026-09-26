# Urban Company Clone

A full-stack service booking application built with Spring Boot, designed to mimic the core functionality of Urban Company. Users can browse services, discover nearby providers, and book appointments, while providers manage their profile and incoming requests through a dedicated portal.

## 🚀 Features

- **User Management**: Registration and login for both customers and service providers, backed by Spring Security.
- **Service Catalog**: Browse available home services with descriptions and pricing.
- **Provider Discovery**: Find providers by service type and location (distance-based matching).
- **Booking Flow**: Add services to a cart, submit a service request, and track its status.
- **Provider Portal**: Providers can manage their profile and respond to incoming service requests.
- **Caching**: Redis-backed caching for faster data retrieval.
- **Server-Rendered UI**: Thymeleaf templates with vanilla CSS/JS, no separate frontend build.

## 🛠️ Tech Stack

- **Language / Runtime**: Java 21
- **Framework**: Spring Boot 3.5.0 (Web, Data JPA, Security, Validation, Thymeleaf)
- **Database**: MySQL (runtime), H2 (dev/test)
- **Caching**: Redis (Spring Data Redis)
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Utilities**: Lombok, Jackson
- **Build Tool**: Maven
- **Containerization**: Docker, Docker Compose
- **CI**: GitHub Actions (`.github/workflows/maven.yml`)

## 🏁 Getting Started

### Prerequisites

- `Java 21` or higher
- `Maven 3.6+`
- `MySQL 8.0+`
- `Redis` (running on `localhost:6379`)

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Dharamrana/urban-clone.git
   cd urban-clone
   ```

2. **Database Setup**
   Create a MySQL database, e.g.:
   ```sql
   CREATE DATABASE urban_company_db;
   ```

3. **Configure Application**
   Update the database credentials in `src/main/resources/application.properties` (or use `application-dev.properties` for local development):
   ```properties
   spring.datasource.username=your_mysql_username
   spring.datasource.password=your_mysql_password
   ```

4. **Build and Run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   On Windows, you can also use the included `run-app.bat`.

5. **Access the App**
   Visit `http://localhost:8080` in your browser.

### Run with Docker

```bash
docker compose up --build
```
This uses the provided `Dockerfile` and `docker-compose.yml` to build and run the app.

## 📂 Project Structure

```
urban-clone/
├── .github/
│   └── workflows/maven.yml         # CI build pipeline
├── data/                           # Local H2 database file(s)
├── src/
│   ├── main/
│   │   ├── java/com/urbancompany/clone/
│   │   │   ├── config/             # Security, Redis, cache, and user-details config
│   │   │   ├── controller/         # Web and API endpoints (auth, services, providers, requests)
│   │   │   ├── exception/          # Global error handling
│   │   │   ├── model/              # JPA entities (User, Service, ServiceProvider, ServiceRequest, ...)
│   │   │   ├── repository/         # Spring Data JPA repositories
│   │   │   ├── service/            # Business logic layer
│   │   │   └── UrbanCompanyCloneApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── static/             # CSS and JS assets
│   │       └── templates/          # Thymeleaf HTML views (incl. fragments/)
│   └── test/
│       ├── java/com/urbancompany/clone/   # Unit and integration tests
│       └── resources/application-test.properties
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── run-app.bat
└── README.md
```

## 🧪 Testing

Run the test suite with:
```bash
mvn test
```
Tests cover the authentication flow, booking/cart workflow, provider matching, and core controllers/services (see `src/test/java`).

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on submitting changes.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
