# Urban Company Clone - Development Guide

## Project Overview
A Spring Boot web application that mimics Urban Company, allowing users to request services (carpenter, massage, electrician, etc.) and find the nearest available service providers using geolocation.

## Tech Stack
- **Backend**: Java 17 + Spring Boot 3.2.5
- **Database**: MySQL 8.x (H2 for tests)
- **Caching**: Redis
- **Frontend**: HTML + Thymeleaf templates
- **Build**: Maven

## Prerequisites

### Java 17+
### MySQL 8.x
Create database: `CREATE DATABASE urban_company_db;`

### Redis
Start Redis server: `redis-server` (or `redis-server --daemonize yes` on Linux)

### Maven 3.9+

## Build & Run

### Development Profile (H2 + sample data)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Profile (MySQL + Redis)
```bash
mvn spring-boot:run
```

### Run Tests
```bash
mvn test
```

## API Endpoints

### Services
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/services` | List all active services |
| GET | `/api/services/{id}` | Get service by ID |
| GET | `/api/services/search?name=X` | Search services by name |
| POST | `/api/services` | Create a new service |
| PUT | `/api/services/{id}` | Update a service |
| DELETE | `/api/services/{id}` | Delete a service |

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/users` | Create a user |
| PUT | `/api/users/{id}` | Update a user |
| DELETE | `/api/users/{id}` | Delete a user |

### Service Providers
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/providers` | List all providers |
| GET | `/api/providers/{id}` | Get provider by ID |
| GET | `/api/providers/nearest?serviceId=1&lat=28.6&lng=77.2` | Find nearest providers (Haversine formula) |
| GET | `/api/providers/nearby?lat=28.6&lng=77.2&radiusKm=10` | Find nearby providers within radius |
| POST | `/api/providers` | Create a provider |
| PUT | `/api/providers/{id}` | Update a provider |
| DELETE | `/api/providers/{id}` | Delete a provider |

### Service Requests
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/requests` | List all requests |
| GET | `/api/requests/{id}` | Get request by ID |
| GET | `/api/requests/user/{userId}` | Requests by user |
| GET | `/api/requests/provider/{providerId}` | Requests by provider |
| POST | `/api/requests` | Create a service request |
| PUT | `/api/requests/{id}/assign/{providerId}` | Assign a provider |
| PUT | `/api/requests/{id}/status?status=COMPLETED` | Update status |
| PUT | `/api/requests/{id}/complete?finalPrice=500` | Complete with price |
| DELETE | `/api/requests/{id}/cancel` | Cancel a request |

### Web Pages
| Path | Description |
|------|-------------|
| `/` | Home page with service search |
| `/services` | All available services |
| `/providers?serviceId=X&lat=Y&lng=Z` | Nearest providers for a service |
| `/provider/{id}` | Provider detail page |
| `/request` | Booking form |
| `/requests` | User's service requests |
| `/about` | About page |
| `/contact` | Contact page |

## Database Schema

### Tables
- `users` - Customer information
- `services` - Service categories (Carpenter, Electrician, etc.)
- `service_providers` - Provider profiles with location, ratings, availability
- `service_requests` - Booking requests linking users, services, and providers
- `provider_services` - Many-to-many mapping: providers <-> services
- `provider_certifications` - List of provider certifications

### Geolocation Logic
The Haversine formula calculates great-circle distance between two coordinates:
```java
// In ServiceProviderService.java:calculateDistance()
double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
           Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
           Math.sin(dLon/2) * Math.sin(dLon/2);
double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
double distance = 6371 * c; // Earth's radius in km
```

## Redis Caching
Caches are configured with per-cache TTL:
- `services` (1 hour), `service` (30 min), `serviceSearch` (30 min)
- `users` (1 hour), `user` (30 min), `userByEmail` (30 min)
- `providers` (30 min), `provider` (30 min), `providersByService` (10 min)
- `nearbyProviders` (5 min), `requests` (5 min)

Cache evicts on any create/update/delete operation.

## Development Commands

### Compile
```bash
mvn compile -q
```

### Run Application
```bash
mvn spring-boot:run
```

### Run with Dev Profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run Tests
```bash
mvn test
```
