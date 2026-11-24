# Электрондық анықтаманың түпнұсқалығын тексеру

Java тілінде жасалған оқу жобасы: электрондық анықтама жалған емес, кері қайтарылмаған және мерзімі өтпеген екенін тексереді.

## Мәселе

Анықтама көбіне файл немесе скриншот түрінде беріледі. Алушы (жұмыс беруші, банк) оның жалған емес екенін тез біле алмайды. Жоба қарапайым үлгі көрсетеді: "беруші" анықтамаға RSA қолтаңбасын қояды және тізілімге жазады, ал тексеруші файлды тексеріп, бес нәтиженің бірін алады:

| Нәтиже | Мағынасы |
|---|---|
| `VALID` (ЖАРАМДЫ) | қолтаңба дұрыс, тізілімде бар, мерзімі өтпеген |
| `TAMPERED` (ӨЗГЕРТІЛГЕН) | файлдағы бір әріп өзгерсе де қолтаңба бұзылады |
| `NOT_FOUND` (ТІЗІЛІМДЕ ЖОҚ) | қолтаңба дұрыс, бірақ тізілімде жоқ |
| `REVOKED` (КЕРІ ҚАЙТАРЫЛҒАН) | беруші анықтаманы жойған |
| `EXPIRED` (МЕРЗІМІ ӨТКЕН) | жарамдылық мерзімі біткен |

## Талаптардың орындалуы

| Талап | Қайда іске асырылған |
|---|---|
| ООП: интерфейс | `DocumentSigner`, `Repository<T, ID>`, `TransactionManager`, `Identifiable<ID>` |
| ООП: абстрактты класс, мұрагерлік | `AbstractSigner` → `RsaSigner`; `Report` → `CsvReport`, `JsonReport` |
| ООП: полиморфизм | `for (Report r : List.of(new CsvReport(), new JsonReport())) r.export(...)` |
| enum, record | `DocumentType`, `VerificationStatus`, `RegistryStatus`; `EDocument`, `RegistryEntry` |
| Коллекциялар | `List`, `Set`, `Map` (`ConcurrentHashMap`, `TreeMap`) |
| Generics | `Repository<T extends Identifiable<ID>, ID>`, `InMemoryRepository<T, ID>`, `Analytics.countBy` |
| Stream API | `Analytics`: `groupingBy`, `filter`, `sorted`, `mapToLong().average()` |
| SQLite + JDBC, DAO | `Database`, `JdbcRegistryDao`, `JdbcAuditDao` (`PreparedStatement`) |
| Транзакция | `Database.execute(...)`: тізілім мен журнал бірге сақталады, қате болса `rollback` |
| Экспорт CSV/JSON | `report/CsvReport`, `report/JsonReport` (мәзір: 6) |
| Көп ағын | `BatchVerifier`: `ExecutorService`, `Callable`, `Future` (мәзір: 7) |
| Тесттер | JUnit 5, 53 тест (`src/test`), оның 5-і SQLite тесттері |
| UML | `docs/UML.md`: use case, class, sequence, ER, activity |
| Командалық жұмыс | `docs/COMMITS.md`: тармақтар (branch) және Pull Request |

## Орнату

Қажет: **JDK 17+** және VS Code ішіндегі **Extension Pack for Java** (Maven сонымен бірге келеді).

1. Репозиторийді клондаңыз және папканы VS Code-та ашыңыз (File → Open Folder).
2. Extension Pack for Java жобаны өзі импорттайды (SQLite кітапханасын жүктеу үшін интернет керек).
3. `src/main/java/kz/egov/docverify/ui/Main.java` файлын ашып, `main` үстіндегі **Run** батырмасын басыңыз.
4. Құпиясөзді енгізіңіз (бірінші рет жаңа RSA кілті жасалады), содан кейін мәзірден **8** (демо) таңдаңыз.

Терминалдан: `mvn compile exec:java`. Тесттер: `mvn test`.

### Әріптер дұрыс көрінбесе

Консоль UTF-8 режиміне өзі ауысады (Windows-та `chcp 65001`). Әріптер әлі де бұзылса:

- мәзірде **9** таңдаңыз: ASCII режимі қазақ әріптерін латынға ауыстырады, немесе
- VS Code-тағы **Run and Debug** тізімінен "Main (ASCII режимі)" таңдаңыз.

## Қолдану мысалы

```
Таңдауыңыз: 1
Анықтама ID (мысалы, DOC-001): DOC-001
Түр нөмірі: 1
Азаматтың аты-жөні: Айгерім Сәрсенова
ЖСН (12 сан): 040101500123
Мазмұны: Айлық табысы: 450000 теңге
Жарамдылық мерзімі (күн саны, мысалы 30): 30
Анықтама берілді. Файл сақталды: out\DOC-001.edoc
```

Содан кейін `out/DOC-001.edoc` ішіндегі `450000` санын өзгертіп, мәзірден **2** таңдасаңыз, нәтиже `TAMPERED` болады.

## Жоба құрылымы

```
src/main/java/kz/egov/docverify/
  model/     EDocument, RegistryEntry, enum-дар, record-тар
  crypto/    DocumentSigner, AbstractSigner, RsaSigner, KeyManager
  storage/   Repository, Database, JdbcRegistryDao, DocumentCodec
  service/   IssuanceService, VerificationService, BatchVerifier, Analytics
  report/    Report, CsvReport, JsonReport
  ui/        Main, ConsoleApp, Ui
src/test/java/...   JUnit 5 тесттері
docs/        UML.md, SECURITY.md, COMMITS.md, research.md
```

## Қауіпсіздік ескертпесі

Жоба оқу мақсатында жасалған. Қолтаңба құжаттың **түпнұсқалығы мен өзгермегенін** қорғайды, мазмұнын **жасырмайды** (шифрлау жоқ). Толық түсіндірме: [docs/SECURITY.md](docs/SECURITY.md). Жабық кілт `keys/` папкасында құпиясөзбен шифрланып сақталады және GitHub-қа түспейді (`.gitignore`).

## Скриншот

Іске қосқаннан кейін скриншотты `docs/images/demo.png` ретінде сақтап, осы жерге қосыңыз:

`![Демо](docs/images/demo.png)`

## Лицензия

MIT, қараңыз [LICENSE](LICENSE).
