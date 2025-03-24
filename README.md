# Phone Country Detector

Приложение для определения страны по номеру телефона. Загружает данные о кодах стран из Wikipedia при запуске и предоставляет RESTful API с JSON-ответами.

## Описание

Приложение валидирует введенный номер телефона и определяет страну на основе телефонного кода. Данные о кодах стран хранятся в PostgreSQL и обновляются из Wikipedia при каждом запуске. Взаимодействие осуществляется через REST API.

## Функциональные возможности

- **Валидация номера телефона**: Проверка корректности введенного номера.
- **Определение страны**:
    - Успешный результат: `{"country": "Russia"}`.
    - Ошибка: `{"message": "Invalid phone number format"}`.
- **Загрузка кодов стран**: Обновление данных из Wikipedia при каждом запуске.
- **REST API**: Работа с приложением через HTTP-запросы.
- **Запуск на порту 8088**.

## Технологический стек

- **Backend**: Java 17, Spring Boot 3.x, PostgreSQL, Jsoup, Swagger/OpenAPI.
- **Frontend**: HTML, CSS, JavaScript.
- **Сборка**: Gradle, Docker.
- **Тестирование**: JUnit 5, Mockito.

## Установка и запуск

### Через Gradle (без Docker)

1. Запустите PostgreSQL (локально или через Docker):
   ```bash
   docker run -d --name phonecountry-db -e POSTGRES_DB=phonecountry -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16-alpine
   ```
2. Обновите `application.yml`, если необходимо:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/phonecountry
       username: postgres
       password: postgres
     jpa:
       hibernate:
         ddl-auto: update
   ```
3. Соберите проект:
   ```bash
   ./gradlew build
   ```
4. Запустите приложение:
   ```bash
   java -jar build/libs/phonecountry-0.0.1-SNAPSHOT.jar
   ```

Приложение будет доступно по адресу: `http://localhost:8088`.

### Через Docker Compose

1. Соберите и запустите приложение:
   ```bash
   docker-compose up --build
   ```
2. Остановите приложение:
   ```bash
   docker-compose down
   ```

## API Endpoints

### Определение страны по номеру телефона

- **URL**: `/api/phone/country`
- **Метод**: `POST`
- **Заголовки**: `Content-Type: application/json`
- **Тело запроса**: `{ "phoneNumber": "<номер>" }`

Пример запроса:

```bash
curl -X POST -H "Content-Type: application/json" -d '{"phoneNumber": "71423423412"}' http://localhost:8088/api/phone/country
```

Ответ:

```json
{"country": "Russia"}
```

## Тестирование

1. Запуск тестов:
   ```bash
   ./gradlew test
   ```
2. Отчеты о тестировании доступны по пути: `build/reports/tests/test/index.html`.

## Структура проекта

```
phone-country-app/
├── src/
│   ├── main/
│   │   ├── java/ru/dsec/phonecountry/  # Код
│   │   └── resources/                  # Конфигурация и статика (HTML, CSS, JS)
│   └── test/                           # Тесты
├── Dockerfile                          # Конфигурация Docker
├── docker-compose.yml                  # Конфигурация Docker Compose
└── README.md                           # Документация
```

## Дополнительно

- Swagger UI: `http://localhost:8088/swagger-ui.html`
- Репозиторий: `https://github.com/<your-username>/phone-country-app`

Приложение поддерживает как Gradle, так и Docker, а также содержит тесты с отчетами в HTML.

