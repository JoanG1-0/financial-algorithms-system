# Sprint 0 - Infraestructura y Setup

## Resumen de historias implementadas

| US   | Historia                  | Estado | Puntos |
|------|---------------------------|--------|--------|
| US-01 | Maven Multi-Module       | ✅     | 3      |
| US-02 | Docker Compose           | ✅     | 3      |
| US-03 | PostgreSQL por servicio  | ✅     | 2      |
| US-04 | Pipeline Jenkins        | ✅     | 5      |
| US-34 | SonarQube                | ✅     | 3      |
| US-35 | Allure Reports           | ✅     | 3      |

---

## Requisitos previos

- **Java 17** (JAVA_HOME apuntando a JDK 17)
- **Docker Desktop** (para docker-compose)
- **Maven** (opcional: el proyecto usa Maven Wrapper `mvnw.cmd`)

---

## US-01: Maven Multi-Module

### ¿Qué se hizo?
- POM padre con `packaging=pom` y 5 módulos
- Versiones centralizadas: Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.1
- Dependency management (BOMs de Spring)
- Plugins comunes: Surefire, Failsafe, JaCoCo, Allure
- Maven Wrapper (`mvnw.cmd`) para builds reproducibles

### Comandos
```powershell
# Compilar todo
.\mvnw.cmd clean install

# Solo compilar (sin tests)
.\mvnw.cmd clean install -DskipTests

# Generar reporte Allure
.\mvnw.cmd allure:report
.\mvnw.cmd allure:serve
```

### Criterios de aceptación ✅
- [x] `mvn clean install` compila todos los módulos
- [x] No hay versiones duplicadas
- [x] Un cambio en el padre impacta a todos

---

## US-02 y US-03: Docker Compose + PostgreSQL

### ¿Qué se hizo?
- `docker-compose.yml` con PostgreSQL, Jenkins, SonarQube
- Script `docker/init-db/01-create-databases.sh` que crea:
  - `etl_db` (ETL Service)
  - `algorithm_db` (Algorithm Service)
  - `report_db` (Report Service)
- Volúmenes persistentes para cada servicio
- Red común `financial-network`

### Comandos
```powershell
# Levantar todo
docker-compose up -d

# Ver logs
docker-compose logs -f

# Detener
docker-compose down
```

### Puertos
| Servicio   | Puerto | URL                    |
|------------|--------|------------------------|
| PostgreSQL | 5432   | localhost:5432         |
| Jenkins    | 8080   | http://localhost:8080  |
| SonarQube  | 9000   | http://localhost:9000  |

### Conexión PostgreSQL (pgAdmin, DBeaver, etc.)
- **Host:** localhost
- **Puerto:** 5432
- **Usuario:** postgres
- **Password:** postgres
- **Bases de datos:** etl_db, algorithm_db, 
- **Visualizacion de bases de datos:** DBeaver Community

---

## US-04: Pipeline Jenkins

### ¿Qué se hizo?
- `Jenkinsfile` con pipeline declarativo
- Stages: Checkout → Build → Test → Verify
- Usa Maven Wrapper (`mvnw.cmd`)

### Configuración en Jenkins
1. Levantar Jenkins: `docker-compose up -d`
2. Acceder a http://localhost:8080
3. Desbloquear Jenkins (ver contraseña en logs: `docker-compose logs jenkins`)
4. Instalar plugins: **Git**, **Maven**, **Pipeline**, **Allure**
5. Crear item **Pipeline**
6. Configurar: "Pipeline script from SCM" → Git URL del repo
7. Guardar y ejecutar

---

## US-34: SonarQube (pendiente integración completa)

### Levantar SonarQube
```powershell
docker-compose up -d sonarqube
```

Acceder: http://localhost:9000 (usuario: admin, password: admin)

### Integración con Jenkins
- Agregar plugin **SonarQube Scanner** en Jenkins
- Configurar SonarQube server en Manage Jenkins → System
- Añadir stage `sonar:sonar` al pipeline cuando el token esté configurado

---

## US-35: Allure Reports

### ¿Qué se hizo?
- Dependencia `allure-junit5` en POM padre
- Configuración Surefire: `allure.results.directory`
- Plugin `allure-maven` en parent
- Jenkinsfile: stage `post { always { allure ... } }`

### Generar reporte local
```powershell
.\mvnw.cmd test
.\mvnw.cmd allure:serve
```

---

## Estructura del proyecto

```
financial-algorithms-system/
├── pom.xml                 # POM padre
├── mvnw.cmd                # Maven Wrapper (Windows)
├── .mvn/wrapper/           # Configuración wrapper
├── docker-compose.yml      # Infraestructura
├── Jenkinsfile             # Pipeline CI
├── docker/
│   └── init-db/            # Scripts PostgreSQL
├── etl-service/
├── algorithm-service/
├── report-service/
├── gateway-service/
└── config-server/
```

---

## Próximos pasos (post Sprint 0)

1. **US-34 completo:** Configurar SonarQube en Jenkins, Quality Gate
2. **Sprint 1:** Desarrollo de funcionalidad de negocio
3. **application.yml** en cada microservicio con URLs de BD
4. **Flyway/Liquibase** para migraciones de esquema
