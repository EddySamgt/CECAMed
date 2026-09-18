# CECAMed

CECAMed es una aplicación de escritorio para el control y la gestión médica. Centraliza pacientes, expedientes clínicos, consultas, citas y recepción, con almacenamiento local de documentos e integración opcional con Google Calendar.

La interfaz está construida con JavaFX y utiliza Spring Boot para configurar los servicios y el acceso a PostgreSQL. La aplicación arranca sin servidor web (`spring.main.web-application-type=none`).

## Funcionalidades

- Registro, búsqueda y actualización de pacientes.
- Expedientes clínicos, consultas y signos vitales.
- Gestión de documentos adjuntos en PDF, PNG y JPEG.
- Agenda de citas, reprogramación, cancelación y validación de disponibilidad.
- Configuración de horarios de atención y bloqueos de agenda.
- Recepción y seguimiento de la sala de espera.
- Panel principal e interfaz con temas claro y oscuro.
- Servicios de inventario, movimientos y reportes de kardex. Actualmente no existe una vista específica de inventario en la navegación.
- Integración opcional para sincronizar citas y bloqueos con Google Calendar y consultar disponibilidad externa.

## Tecnologías

| Componente | Tecnología / versión declarada |
| --- | --- |
| Lenguaje | Java 21 |
| Construcción | Gradle 8.10.2 con Kotlin DSL y Gradle Wrapper |
| Servicios y configuración | Spring Boot 3.3.4 |
| Persistencia | Spring Data JPA / Hibernate y PostgreSQL |
| Migraciones | Flyway |
| Interfaz | JavaFX 21.0.4, AtlantaFX 2.0.1, ControlsFX 11.2.1 e Ikonli 12.3.1 |
| Calendario externo | Google Calendar API v3 |
| Pruebas | JUnit 5, Mockito, AssertJ y H2 |
| Base de datos local con Docker | PostgreSQL 16 |

## Estructura del proyecto

```text
CECAMed/
├── cecamed-core-db/                 # Entidades, repositorios, auditoría y migraciones
│   └── src/
│       ├── main/java/com/cecamed/core/
│       ├── main/resources/db/migration/
│       └── test/                    # Pruebas de repositorios y migraciones
├── cecamed-services/                # Reglas de negocio y almacenamiento de archivos
│   └── src/
│       ├── main/java/com/cecamed/services/
│       │   ├── config/             # Configuración de almacenamiento
│       │   ├── dto/                # Objetos de entrada y salida
│       │   ├── exception/          # Excepciones de negocio y archivos
│       │   ├── mapper/             # Conversión entre entidades y DTO
│       │   ├── service/            # Contratos e implementaciones de servicios
│       │   └── storage/            # Almacenamiento local de adjuntos
│       └── test/
├── cecamed-calendar-integration/    # Cliente, configuración y servicios de Google Calendar
│   └── src/{main,test}/
├── cecamed-ui/                      # Aplicación de escritorio y punto de entrada
│   └── src/
│       ├── main/java/com/cecamed/ui/
│       │   ├── component/          # Componentes visuales reutilizables
│       │   ├── controller/         # Controladores de las vistas FXML
│       │   ├── navigation/         # Navegación entre pantallas
│       │   ├── session/            # Autenticación y sesión local
│       │   ├── theme/              # Gestión de temas visuales
│       │   └── util/               # Carga de FXML con Spring
│       ├── main/resources/
│       │   ├── application.properties
│       │   ├── css/
│       │   └── fxml/
│       └── test/                   # Pruebas de vistas, contexto y configuración
├── gradle/wrapper/                 # Distribución de Gradle configurada
├── build.gradle.kts                # Configuración compartida
├── settings.gradle.kts             # Declaración de módulos
├── gradle.properties
├── gradlew / gradlew.bat
├── compose.yaml                    # Servicio local de PostgreSQL
├── .env.example                    # Plantilla de configuración local
└── uploads/                        # Adjuntos locales; excluidos de Git
```

Las dependencias principales siguen esta dirección: `cecamed-ui` utiliza servicios, calendario y persistencia; `cecamed-services` utiliza calendario y persistencia; `cecamed-calendar-integration` utiliza persistencia. `cecamed-core-db` contiene el modelo compartido.

## Requisitos

- JDK 21 disponible mediante `JAVA_HOME` o la configuración de Java del IDE.
- Docker con Docker Compose para la base de datos local, o una instancia de PostgreSQL accesible.
- Entorno gráfico para abrir la aplicación y ejecutar las pruebas de JavaFX.
- Conexión a Internet durante la primera compilación para descargar Gradle y las dependencias.

No es necesario instalar Gradle por separado. Los comandos siguientes se ejecutan desde la raíz del repositorio y usan Bash; en Windows, utiliza `gradlew.bat` en lugar de `./gradlew` y adapta los comandos de copia y variables a tu terminal.

## Instalación y ejecución local

### 1. Configurar el entorno

Si todavía no tienes un archivo `.env`, copia la plantilla:

```bash
cp .env.example .env
```

Edita `.env` y sustituye la contraseña de ejemplo:

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cecamed
SPRING_DATASOURCE_USERNAME=cecamed
SPRING_DATASOURCE_PASSWORD=replace-with-a-local-password
CECAMED_STORAGE_UPLOAD_DIR=./uploads/cecamed/documents
```

Spring importa el archivo `.env` de la raíz con sintaxis de propiedades y codificación UTF-8 mediante el cargador `.utf8`. Guarda el archivo en UTF-8 para conservar tildes, diéresis y eñes. Usa entradas `CLAVE=valor`, sin `export` ni comillas envolventes. Las tareas `run` y `bootRun` ya establecen la raíz del proyecto como directorio de trabajo; configura lo mismo si ejecutas desde el IDE.

| Variable | Uso |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL. La plantilla apunta a la base `cecamed`. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL; Compose también lo utiliza para inicializar la base. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña obligatoria de PostgreSQL. |
| `CECAMED_STORAGE_UPLOAD_DIR` | Directorio de adjuntos; por defecto, `./uploads/cecamed/documents`. |

Sin la URL de la plantilla, `application.properties` utiliza `jdbc:postgresql://localhost:5432/cecamed_db_eddy` y el usuario `postgres`. Para el entorno de Compose, conserva los valores de `.env.example` salvo la contraseña.

### 2. Iniciar PostgreSQL

```bash
docker compose up -d postgres
docker compose ps
```

Espera a que el servicio aparezca como saludable (`healthy`). Compose crea la base `cecamed`, publica PostgreSQL en `127.0.0.1:5432` y conserva los datos en el volumen `postgres_data`. El contenedor ejecuta únicamente la base de datos; la interfaz se ejecuta en el equipo local.

Si utilizas una instalación propia de PostgreSQL, crea una base vacía y configura su URL y credenciales en `.env`. El usuario debe poder crear las tablas y demás objetos de las migraciones.

### 3. Ejecutar la aplicación

```bash
./gradlew :cecamed-ui:bootRun
```

También está disponible `./gradlew :cecamed-ui:run`. El punto de entrada es `com.cecamed.ui.CecamedApplication`.

Durante el arranque, Flyway aplica las migraciones pendientes y Hibernate valida el esquema. Las migraciones iniciales crean el esquema y horarios de atención: lunes a viernes de 08:00 a 17:00 y sábado de 08:00 a 12:00, en intervalos de 30 minutos.

### 4. Acceder

La implementación actual de `AuthenticationService` contiene estas cuentas predefinidas:

| Usuario | Contraseña | Rol |
| --- | --- | --- |
| `medico` | `medico123` | Médico |
| `admin` | `admin123` | Médico |
| `recepcion` | `recepcion123` | Recepción |

Estas cuentas están definidas en código y no se administran desde PostgreSQL. El usuario `admin` comparte el rol médico; no existe un rol administrativo independiente. La autenticación actual debe sustituirse antes de utilizar el sistema en producción.

Para detener PostgreSQL conservando su volumen:

```bash
docker compose down
```

## Compilación y pruebas

```bash
# Compilar los módulos y ejecutar las pruebas predeterminadas
./gradlew build

# Ejecutar solamente las pruebas predeterminadas
./gradlew test

# Ejecutar las pruebas de un módulo
./gradlew :cecamed-services:test
```

Las pruebas predeterminadas utilizan H2 o dobles de prueba según el módulo. Las pruebas etiquetadas para PostgreSQL, el entorno local y JavaFX se ejecutan mediante tareas específicas:

| Comando | Alcance y requisitos |
| --- | --- |
| `./gradlew :cecamed-ui:uiTest` | Carga las vistas FXML y muestra la ventana de acceso; requiere un entorno gráfico. |
| `./gradlew :cecamed-ui:envCheck` | Comprueba la carga de `.env`, la conexión, el esquema y UTF-8 en PostgreSQL. No aplica migraciones por defecto; requiere una base ya inicializada. |
| `./gradlew :cecamed-ui:envCheck -PapplyMigrations=true` | Aplica las migraciones pendientes sobre la base configurada en `.env` antes de comprobarla. |
| `./gradlew :cecamed-core-db:postgresTest :cecamed-ui:postgresTest` | Valida migraciones, repositorios y el contexto de aplicación contra una base de pruebas PostgreSQL. |

Para las tareas `postgresTest`, crea previamente una base dedicada, por ejemplo `cecamed_test`, y exporta sus credenciales:

```bash
export TEST_POSTGRES_URL=jdbc:postgresql://localhost:5432/cecamed_test
export TEST_POSTGRES_USERNAME=cecamed
export TEST_POSTGRES_PASSWORD='contraseña-de-la-base-de-pruebas'
./gradlew :cecamed-core-db:postgresTest :cecamed-ui:postgresTest
```

Estas pruebas aplican migraciones y realizan operaciones de persistencia; utiliza una base separada de los datos de trabajo. Los reportes HTML se generan en `<módulo>/build/reports/tests/<tarea>/index.html`.

## Google Calendar

La integración está deshabilitada por defecto y no es necesaria para iniciar la aplicación. Su configuración se encuentra en `cecamed-ui/src/main/resources/application.properties` y en `GoogleCalendarProperties`.

Para configurar una integración real, añade las propiedades correspondientes a tu `.env` local, por ejemplo:

```properties
cecamed.google.calendar.enabled=true
cecamed.google.calendar.calendar-id=identificador-del-calendario
cecamed.google.calendar.credentials-path=/ruta/privada/credentials.json
cecamed.google.calendar.time-zone=America/Guatemala
```

El cliente carga credenciales mediante `GoogleCredentials.fromStream`. Necesita un archivo compatible con ese mecanismo y acceso al calendario indicado. El código actual no implementa una pantalla de autorización OAuth para el usuario; un archivo de secretos de cliente OAuth por sí solo no completa ese proceso. Aunque existe la propiedad `tokens-directory-path`, la configuración actual no implementa un flujo de autorización que guarde tokens en ese directorio.

Si la integración está habilitada pero el cliente no puede inicializarse, las citas quedan pendientes de sincronización. Los errores de sincronización se registran y el servicio dispone de una operación para reintentar citas pendientes o fallidas.

## Datos, adjuntos y migraciones

- **Base de datos:** PostgreSQL almacena pacientes, expedientes, consultas, citas, horarios, bloqueos, inventario y metadatos de documentos.
- **Adjuntos:** los archivos se guardan en el directorio configurado con `CECAMED_STORAGE_UPLOAD_DIR`. Por defecto se permiten `.pdf`, `.png`, `.jpg` y `.jpeg`, con un máximo de 15 MiB por archivo. El almacenamiento genera nombres únicos y calcula una suma SHA-256.
- **Migraciones:** están en `cecamed-core-db/src/main/resources/db/migration/`. Para evolucionar el esquema, añade una migración versionada, por ejemplo `V3__descripcion.sql`, en lugar de modificar una migración ya aplicada.
- **Respaldos:** conserva tanto la base de datos como el directorio de adjuntos; un respaldo de PostgreSQL no incluye los archivos físicos.
- **Archivos locales:** `.env`, las credenciales de Google, los tokens y los adjuntos están excluidos de Git mediante `.gitignore`.

## Problemas frecuentes

| Problema | Qué revisar |
| --- | --- |
| No se encuentra Java 21 | Verifica `java -version`, `JAVA_HOME` y el JDK configurado en el IDE. |
| `Permission denied` al usar el wrapper | En Linux/macOS, ejecuta `chmod +x gradlew`. |
| Conexión rechazada a PostgreSQL | Revisa `docker compose ps`, la URL JDBC y si el puerto 5432 está ocupado por otra instancia. |
| Error de autenticación de PostgreSQL | Comprueba usuario y contraseña. Cambiar `.env` no cambia las credenciales de una base ya inicializada en el volumen de Docker. |
| No se carga `.env` | Ejecuta desde la raíz del repositorio y comprueba que el archivo exista con formato `CLAVE=valor`. |
| Error de validación del esquema | Revisa los mensajes de Flyway y confirma que las migraciones se aplicaron a la base correcta. `envCheck` no las aplica salvo que se indique `-PapplyMigrations=true`. |
| JavaFX no puede abrir una ventana | Ejecuta en una sesión con entorno gráfico; en Linux revisa también las bibliotecas gráficas nativas requeridas por JavaFX. |
| No se pueden guardar adjuntos | Comprueba los permisos del directorio configurado, el tamaño y el formato del archivo. |

## Desarrollo

Mantén las entidades y repositorios en `cecamed-core-db`, las reglas de negocio en `cecamed-services`, la comunicación con Google en `cecamed-calendar-integration` y la presentación en `cecamed-ui`. Las vistas utilizan FXML y sus controladores se cargan mediante `SpringFXMLLoader` para integrarse con Spring.

Antes de proponer cambios, ejecuta las pruebas relacionadas con el módulo afectado. Si cambias FXML, ejecuta también `uiTest`; si cambias el esquema o consultas específicas de PostgreSQL, ejecuta las tareas `postgresTest` contra una base dedicada.

## Licencia

El repositorio no incluye actualmente un archivo de licencia. Las condiciones de uso y distribución deben acordarse con sus responsables.
