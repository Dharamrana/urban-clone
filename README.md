# Urban Company Clone

A full-stack service booking application built with Spring Boot, designed to mimic the core functionality of Urban Company. This application allows users to browse services, find providers, and book appointments seamlessly.

## 🚀 Features

- **User Management**: Secure registration and login for users and service providers.
- **Service Catalog**: Browse various home services with detailed descriptions.
- **Provider Discovery**: Find qualified service providers based on their expertise and location.
- **Booking System**: Intuitive flow to request services, manage a cart, and book appointments.
- **Provider Portal**: A dedicated interface for providers to manage their profile and service requests.
- **Caching**: Integrated Redis for high-performance data retrieval.
- **Responsive UI**: Built with Thymeleaf, HTML, CSS, and JavaScript for a clean user experience.

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.5
- **Security**: Spring Security
- **Database**: MySQL (Primary), H2 (Development/Testing)
- **Caching**: Redis
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Build Tool**: Maven

## 🏁 Getting Started

### Prerequisites

- `Java 17` or higher
- `Maven 3.6+`
- `MySQL 8.0+`
- `Redis` (Running on localhost:6379)

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/devcancode11/gand-fadh-project.git
   cd urban-company-clone
   ```

2. **Database Setup**
   Create a MySQL database named `urban_company_db`:
   ```sql
   CREATE DATABASE urban_company_db;
   ```

3. **Configure Application**
   Open `src/main/resources/application.properties` and update the database credentials:
   ```properties
   spring.datasource.username=your_mysql_username
   spring.datasource.password=your_mysql_password
   ```

4. **Build and Run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access the App**
   Visit `http://localhost:8080` in your browser.

## 📂 Project Structure

```
urban-company-clone/
├── src/
│   ├── main/
│   │   ├── java/com/urbancompany/clone/
│   │   │   ├── config/      # Security, Redis, and App configurations
│   │   │   ├── controller/  # Web and API endpoints
│   │   │   ├── exception/   # Global error handling
│   │   │   ├── model/       # JPA Entities (User, Service, etc.)
│   │   │   ├── repository/   # Data Access Layer
│   │   │   └── service/      # Business Logic Layer
│   │   └── resources/
│   │       ├── static/       # CSS, JS, and Images
│   │       └── templates/    # Thymeleaf HTML templates
│   └── test/                # Unit and Integration tests
├── pom.xml                  # Maven dependencies
└── README.md
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
