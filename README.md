# BANK BLOOD

Учебный проект по дисциплине «Корпоративная разработка» (Git Flow: main / develop / feature).

Мы строим учётную систему банка крови. Люди отслеживают **пакет донорской крови** по мере
прохождения им лабораторного контроля и выдачи в больницу.

## Product

Учётная система банка крови (отделения переливания крови). Она следит за каждым пакетом крови:
где он находится, проверен ли он, годен ли к переливанию и кому выдан.

## Core item

**Пакет крови** — тип `BloodUnit` (в лабе 1 представлен идентификатором и статусом).

- Идентификатор — `BloodUnitId`, обёртка над UUID (`BloodUnitId.newId()`).
  Пустое значение, `null` или не-UUID не допускаются — конструктор бросает `IllegalArgumentException`.
- Номер для людей — `BloodUnitNumber`, строка вида `BU-2026-000123` (бизнес-ключ, в базе UNIQUE).
  Пустой или `null` номер тоже даёт `IllegalArgumentException`.
- Статус — `BloodUnitStatus`:

| Статус | Что означает |
|--------|--------------|
| `COLLECTED` | кровь взяли у донора, пакет зарегистрирован |
| `TESTED` | пакет отправлен на лабораторные анализы |
| `RELEASED` | анализы отрицательные, пакет годен к выдаче |
| `ISSUED` | пакет выдан в больницу под пациента |
| `DISCARDED` | брак по анализам или истёк срок годности — на утилизацию |

Переходы между статусами разрешает класс `BloodUnitPolicy`:

- `move(from, to)` при разрешённом переходе **возвращает новый статус**;
- при запрещённом переходе **бросает `IllegalStateException`**.

## Status table

Ровно четыре перехода: два разрешённых и два запрещённых. Эти же строки проверяются в тесте
`BloodUnitPolicyTest` через `@CsvSource`.

| From | To | Разрешён? | Причина |
|------|----|-----------|---------|
| `COLLECTED` | `TESTED` | да | пакет отправляют на лабораторный скрининг |
| `TESTED` | `RELEASED` | да | все анализы отрицательные, пакет годен к выдаче |
| `COLLECTED` | `ISSUED` | нет | нельзя выдавать непроверенную кровь |
| `ISSUED` | `RELEASED` | нет | выданный пакет нельзя вернуть в оборот |

## Forbidden — why

- **`COLLECTED → ISSUED`** — выдача пакета в обход анализов.
  Донорская кровь до скрининга может содержать ВИЧ, гепатит B и C, сифилис; не определены
  группа крови и резус-фактор. Перелить такой пакет — прямая угроза жизни реципиента и
  нарушение регламента работы банка крови.

- **`ISSUED → RELEASED`** — возврат уже выданного пакета обратно на склад как годного.
  Пакет находился вне холодильника в больнице, холодовая цепь разорвана. Повторно пускать
  его в выдачу другому пациенту нельзя — кровь могла испортиться.

## Lab 2 — Spring Boot

К проекту подключён Spring Boot. Продукт и статусы из лабы 1 не менялись — только добавилась
обвязка вокруг них. Правила (`Rule` и его реализации) остаются чистой Java, без единого
`import org.springframework` — Spring работает только на границе, в пакете `config`.

### Package diagram

```
  dto          client        handler         config
  (JSON later) (HTTP later)  (HTTP week 9)   Application
                                             BloodUnitService (@Service)
       \            \            /                |
        \            \          /           injects Rule
         \            \        /
                     domain
     BloodUnitId  BloodUnitStatus  BloodUnitPolicy
     Rule, TransitionRule, FinalStatusRule, RuleChain
                     (no Spring)
```

Стрелки направлены внутрь. `domain` не импортирует `org.springframework` — проверить можно
поиском по проекту, совпадений быть не должно.

### Что в каждом пакете

- **`domain`** — чистая Java, без Spring.
  - `BloodUnitId`, `BloodUnitStatus`, `BloodUnitPolicy` — из лабы 1, без изменений.
  - `Rule` — интерфейс с одним методом `check(from, to)`: молчит, если переход разрешён,
    бросает исключение, если нет.
  - `TransitionRule` — первая реализация `Rule`, таблица переходов (та же, что в `BloodUnitPolicy`).
  - `FinalStatusRule` — вторая реализация `Rule`, отдельный стоп-фактор: из `ISSUED`
    и `DISCARDED` двигаться нельзя никуда.
  - `RuleChain` — третья реализация `Rule`, склеивает две проверки в одну цепочку.
- **`config`** — единственное место, где встречается Spring.
  - `Application` — точка входа, `@SpringBootApplication`.
  - `BloodUnitService` — `@Service`, получает готовый `Rule` через конструктор
    (Dependency Injection) и не знает, какая именно реализация за ним стоит.
  - `RuleConfig` — `@Configuration` с `@Bean`, который собирает `RuleChain` из
    `TransitionRule` и `FinalStatusRule` и отдаёт его как единый `Rule`.
- **`dto`**, **`handler`** — пока пустые, задел на будущие лабы (JSON, HTTP-обработчики).
  **`client`** с лабы 3 не пустой: там внешние порты (см. ниже).

### Запуск и проверка

```bash
mvn spring-boot:run
```
Приложение стартует и висит (веб-сервера нет, REST в этой лабе не требуется).

```bash
mvn -q verify
```
Должно пройти без вывода и без ошибок.

## Lab 3 — PostgreSQL, JPA и Flyway

Пакеты крови сохраняются в PostgreSQL (локально: база `css`, пользователь `css`, пароль `css`).
Таблицу создаёт Flyway: файл `src/main/resources/db/migration/V1__create_blood_unit.sql`
запускается при старте приложения, до проверки Hibernate. Hibernate таблицы не создаёт:
`ddl-auto: validate` только сверяет сущность с таблицей. Изменение схемы всегда новый файл
(`V2__...sql`), старый `V1` после запуска не правим.

У пакета два идентификатора:

| | `id` | `business_key` |
|---|---|---|
| Что это | UUID, `BloodUnitId.newId()` | номер для людей, `BloodUnitNumber` |
| В базе | PRIMARY KEY | UNIQUE NOT NULL |

Список в `CHECK` совпадает со статусами из таблицы выше
(`COLLECTED`, `TESTED`, `RELEASED`, `ISSUED`, `DISCARDED`).

Новые пакеты: `persistence` (JPA: сущность и репозиторий) и `client` (внешние порты, пока консоль).
`domain` не импортирует ни `jakarta.persistence`, ни `org.springframework`.

- `BloodUnitEntity` (в `persistence`) — одна строка таблицы `blood_unit`, только названия колонок.
  Статус хранится текстом, как в `CHECK`. Сущность не покидает `persistence`.
- `BloodUnitRepository` (интерфейс в `domain`) — что нужно сервису от хранилища.
  Его реализует `BloodUnitJpa` в `persistence` (`EntityManager.persist` и `flush`).
- `HospitalNotifier` (порт в `domain`) — сообщить больнице, что пакет выдан.
  Реализует `ConsoleHospitalNotifier` в `client`.
- `DuplicateBloodUnit` — unchecked-исключение «такой номер уже есть». Сервис ловит
  `DataIntegrityViolationException` и бросает его заново, поэтому транзакция откатывается.

Тесты: `BloodUnitServiceTest` (порт замокан), `BloodUnitDbTest`:
`secondStatementRollsBack` — count 0, `secondRequestKeepsTheFirst` — count 1.
Перед каждым тестом строки удаляются (`DELETE FROM`), таблицу тест не трогает.
Запуск: `mvn -q verify` (PostgreSQL должен быть запущен, схема в базе пустая или уже на `V1`).

Если в базе `css` уже есть таблица, созданная вручную, Flyway откажется запускать `V1`
(«non-empty schema but no schema history»). Тогда пересоздай базу: `DROP DATABASE css;`
и `CREATE DATABASE css OWNER css;`, потом запусти приложение.

## Lab 4 — REST API

Сервис отвечает по HTTP. Один ресурс, два глагола. Версия в пути: `/api/v1`.
Id в пути это UUID из лабы 3, а не человеческий номер.

| Запрос | Что делает | Ответы |
|---|---|---|
| `POST /api/v1/blood-units` | регистрирует пакет (статус `COLLECTED`) | 201 + `Location`, 400, 409 |
| `GET /api/v1/blood-units/{id}` | читает пакет по UUID | 200, 400, 404 |

Тело запроса: `{"businessKey": "BU-2026-000123", "title": "Цельная кровь, 450 мл"}`, оба поля обязательные,
название не длиннее 120 символов. Ответ: `id`, `businessKey`, `status`, `title`.

Необязательные поля (остаются на `/api/v1`, старый клиент может их не присылать):
- `bloodGroup`: `O`, `A`, `B` или `AB`. Это международные буквы, цифрами их называют так:
  I группа это `O`, II это `A`, III это `B`, IV это `AB`;
- `rhFactor`: `POSITIVE` (положительный, +) или `NEGATIVE` (отрицательный, -).

В API храним буквы, а не цифры 1-4: цифры путаются (первая по алфавиту или первая группа?), и замена слова
на число считается несовместимым изменением контракта.

Если поле не указано, в ответе его нет. Неверное слово даёт 400 `VALIDATION` и список допустимых слов
(`bloodGroup must be one of [O, A, B, AB]`). В таблице для них колонки `blood_group` и `rh_factor`
с `CHECK`, их добавляет миграция `V2__add_blood_group_and_rh.sql` (`V1` не менялась).

Любая ошибка имеет один вид: `code`, `message`, `correlationId`. Стека и имён классов в ответе нет.
Если клиент прислал заголовок `X-Correlation-Id`, он возвращается в теле ошибки, иначе номер создаётся.

| Ситуация | Статус | code |
|---|---|---|
| пустой номер или название, название длиннее 120, тело не JSON, id не UUID | 400 | `VALIDATION` |
| UUID правильный, но такого пакета нет | 404 | `NOT_FOUND` |
| номер пакета уже есть (первая строка остаётся, count = 1) | 409 | `DUPLICATE` |
| неверный Content-Type | 415 | `UNSUPPORTED` |
| неожиданная ошибка | 500 | `INTERNAL` |

Новые пакеты и классы:
- `handler` — `BloodUnitController`, `CreateBloodUnit`, `BloodUnitResponse`, `ApiError`, `ApiErrors`
  (единственное место, которое знает про коды 400, 404, 409), `ApiDocs` (название API для Swagger).
- `domain` — `MissingBloodUnit` (unchecked, как `DuplicateBloodUnit`) и `BloodUnitView` (как пакет выглядит
  для вызывающего). В `domain` нет Spring, `ResponseEntity` и `jakarta.validation`.
- `BloodUnitService.register` теперь возвращает `BloodUnitView`, добавлен `require(id)`.

Swagger: запусти приложение и открой `/swagger-ui.html` (описание в JSON: `/v3/api-docs`).
Используется springdoc 2.6.0: он собран под Spring Boot 3.3, как у нас.

Тесты: `BloodUnitControllerTest` (`@WebMvcTest`, сервис замокан, без базы) проверяет 201, 400, 404, 409,
415, 500 и заголовок `X-Correlation-Id`. Тесты лабы 3 (count 0 и count 1) остаются без изменений.

## Lab 5 — Безопасность: Spring Security и JWT

Теперь API знает, кто звонит и можно ли ему. Войти можно один раз (`POST /api/v1/auth/login`), получить токен
на 15 минут и присылать его в каждом запросе: `Authorization: Bearer <токен>`. Пароль ходит только при входе.

Две роли (названия наши, под банк крови):
- `LAB_TECHNICIAN` — лаборант, младшая роль;
- `CHIEF_PHYSICIAN` — главврач, старшая роль.

Закрытое действие — **выдача пакета в больницу**: `POST /api/v1/blood-units/{id}/issue` (`RELEASED` -> `ISSUED`).
Это самый дорогой переход статуса из лабы 1, поэтому его может сделать только главврач.

### Таблица прав

| Действие | LAB_TECHNICIAN | CHIEF_PHYSICIAN | Без токена |
|---|---|---|---|
| `POST /api/v1/auth/login` | все | все | можно |
| Создать и прочитать пакет | да | да | 401 |
| Отправить на анализы `POST .../{id}/test`, допустить `POST .../{id}/release` | да | да | 401 |
| **Выдать пакет `POST .../{id}/issue`** (закрытое действие) | **403** | да | 401 |
| `GET /api/me` | да | да | 401 |
| `GET /actuator/health` | все | все | можно |
| Swagger: `/swagger-ui.html`, `/v3/api-docs/**` | все | все | можно |
| Страница входа и её css/js (`/`, `/index.html`, `/*.css`, `/*.js`) | все | все | можно |
| Всё остальное (в том числе `/actuator/info`, `/actuator/env`) | 403 | 403 | 401 |

Правила в цепочке фильтров (`SecurityConfig`) стоят в том же порядке, срабатывает первое подходящее.
Узкое правило про `issue` стоит выше широкого `/api/v1/**`: иначе лаборант получил бы 200.
Последним стоит `denyAll`: новый адрес, про который забыли написать правило, будет закрыт, а не открыт всем.

Как отличать коды: **401** значит «я тебя не знаю» (токена нет, он битый, подделан или просрочен),
**403** значит «я тебя знаю, но тебе нельзя» (роль не подходит, новый токен не поможет).

### Вход и токен

`POST /api/v1/auth/login`, тело `{"username": "...", "password": "..."}`, оба поля обязательные.

| Что отправили | Статус | Ответ |
|---|---|---|
| верные имя и пароль | 200 | `accessToken`, `tokenType` = `Bearer`, `expiresIn` = `900` (секунд) |
| неверный пароль **или** такого имени нет | 401 | `UNAUTHENTICATED`, `bad username or password` (текст один и тот же) |
| пустое имя или пароль | 400 | `VALIDATION`, `username must not be blank` |

Текст один и тот же специально: иначе по ответу можно узнать, какие имена пользователей существуют.

Токен: HS256, издатель `bloodbank`, живёт 15 минут. Внутри: `iss`, `sub` (имя), `iat`, `exp` и `roles`
(например `["CHIEF_PHYSICIAN"]`, без приставки `ROLE_`). Пароля и других личных данных в токене нет:
токен подписан, но не зашифрован, его середину может прочитать любой.

Если подделать роль в токене, подпись перестанет совпадать, и ответом будет 401: секрет знает только наш сервер.

### Ошибки 401 и 403

Они случаются в фильтрах, до контроллера, поэтому тело пишет `SecurityErrors`, а вид тот же, что у всех ошибок
(`code`, `message`, `correlationId`):

| Что случилось | Статус | code | message |
|---|---|---|---|
| нет токена, токен битый, подделан, просрочен или чужого издателя | 401 | `UNAUTHENTICATED` | `valid token required` |
| роль не подходит | 403 | `FORBIDDEN` | `not allowed for your role` |
| правила не разрешают переход статуса (например, выдать пакет, который ещё не допущен) | 409 | `ILLEGAL_TRANSITION` | текст правила |

`X-Correlation-Id` работает как в лабе 4: нормальный возвращаем, странный или отсутствующий заменяем на свой.

### Закрытое действие: как оно устроено

1. Цепочка фильтров: токен лаборанта на `issue` даёт 403. До контроллера и сервиса запрос не доходит.
2. Второй замок: на методе `BloodUnitService.issue` стоит `@PreAuthorize("hasRole('CHIEF_PHYSICIAN')")`.
   Если кто-то добавит второй контроллер и забудет правило в URL, метод всё равно не пустит лаборанта.
3. Кто выдал, сервис узнаёт из токена (`Authentication.getName()`), а не из JSON. Поля `actor` или `role`
   в теле запроса игнорируются.
4. Сервис читает пакет, спрашивает правила (`RuleChain`), обновляет статус одной командой
   `UPDATE ... WHERE status = <старый>` и только потом сообщает больнице. Если правила против, строка не меняется.
5. В лог идёт имя пользователя и смена статуса, токен в лог не попадает.

Чтобы довести пакет до `RELEASED`, есть два обычных действия, доступные обеим ролям:
`POST .../{id}/test` (`COLLECTED` -> `TESTED`) и `POST .../{id}/release` (`TESTED` -> `RELEASED`).

### Пользователи и секрет

Таблицы пользователей в этой лабе нет. Два тестовых пользователя живут в памяти и **только в профиле `dev`**
(то есть на ноутбуке, не в проде):

| Имя | Пароль | Роль |
|---|---|---|
| `technician` | `tech-dev` | `LAB_TECHNICIAN` |
| `chief` | `chief-dev` | `CHIEF_PHYSICIAN` |

Пароли хранятся как хэши BCrypt (`{bcrypt}...`), а не текстом. Секрет для подписи токена:

| Файл | Секрет |
|---|---|
| `application-dev.yml` | `app.jwt.secret`: значение для ноутбука, не короче 32 символов |
| `application-prod.yml` | `app.jwt.secret: ${CSS_JWT_SECRET}`, без значения по умолчанию |
| `application.yml` | секрета нет |

Секрет короче 32 символов останавливает приложение при запуске. В `src/main/java` секрета нет.

### Как проверить руками

В Swagger (`/swagger-ui.html`): вызови `POST /api/v1/auth/login`, скопируй `accessToken`, нажми **Authorize**
и вставь токен. Дальше запросы идут с заголовком сами. Чтобы проверить 403, войди как `technician` и вызови `issue`.

Через curl (в PowerShell `curl.exe`):

```
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"chief\",\"password\":\"chief-dev\"}"
curl.exe -i http://localhost:8080/api/v1/blood-units/<id>                                    -> 401
curl.exe -i -X POST -H "Authorization: Bearer <токен лаборанта>" http://localhost:8080/api/v1/blood-units/<id>/issue   -> 403
curl.exe -i -X POST -H "Authorization: Bearer <токен главврача>" http://localhost:8080/api/v1/blood-units/<id>/issue   -> 200
```

### Что добавили и изменили

- Новые, `config`: `JwtConfig` (ключ, кодировщик и декодер), `AuthConfig` (BCrypt, dev-пользователи, проверка имени и пароля),
  `TokenService` (выпускает токен).
- Новые, `handler`: `SecurityConfig` (цепочка фильтров), `SecurityErrors` (401 и 403 в виде нашего `ApiError`),
  `CorrelationId` (одно правило для `ApiErrors` и `SecurityErrors`), `AuthController`, `LoginRequest`, `TokenResponse`,
  `MeController`, `MeResponse`.
- Новое, `domain`: `ForbiddenTransition` (правила не разрешают переход; Spring в `domain` по-прежнему нет).
- Изменены: `BloodUnitService` (`sendToTest`, `release`, закрытое `issue`), `BloodUnitRepository` и `BloodUnitJpa`
  (`updateStatus`), `BloodUnitController` (три действия, подписи статусов), `ApiErrors` (401 при входе, 403 от метода, 409),
  `ApiDocs` (кнопка Authorize в Swagger), `pom.xml` (security, resource-server, spring-security-test).

### Тесты

- `SecurityRulesTest` (веб-слой, сервис замокан): строки таблицы прав. Нет токена даёт 401, лаборант на `issue` даёт 403,
  главврач даёт 200, имя берётся из токена, `/api/me`. В каждом «нельзя» проверяется, что сервис не вызван.
- `SecurityFlowTest` (настоящий вход, декодер и PostgreSQL): вход, один текст для двух 401, 400, история
  «лаборант 403 / главврач успех / повторная выдача 409», подделанная роль, просроченный токен, чужой издатель,
  второй замок на методе, открытые адреса, Swagger называет 401 и 403.
- `TokenServiceTest`: токен выпускается и читается обратно (имя, роль без `ROLE_`, срок 15 минут, пароля нет).
- `BloodUnitControllerTest` (лаба 4): те же 201, 400, 404, 409, теперь с токеном, защита не отключена.
- `ActuatorTest`: `/actuator/health` открыт, остальное закрыто (401).