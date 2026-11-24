# Коммиттер тізімі және командалық жұмыс

Әр қадамда тек сол қадамның файлдарын репозиторийге көшіріңіз. GitHub Desktop: **Changes** → файлдарды белгілеу → **Summary** жазу → **Commit to main** → **Push origin**.
Жолдар `src/main/java/kz/egov/docverify/` (негізгі код) және `src/test/java/kz/egov/docverify/` (тесттер) бастап есептеледі.

| № | Файлдар | Summary |
|---|---|---|
| 1 | `pom.xml`, `.gitignore` | Add Maven build configuration and .gitignore |
| 2 | `README.md` | Add README with problem, requirements and usage |
| 3 | `docs/problem-analysis.md` | Add problem analysis |
| 4 | `model/DocumentType`, `VerificationStatus`, `RegistryStatus` | Add enums for document type and statuses |
| 5 | `exception/DocVerifyException` | Add custom exception |
| 6 | `model/Identifiable`, `EDocument` | Add Identifiable interface and EDocument with validation |
| 7 | `model/SignedDocument`, `RegistryEntry`, `AuditEntry`, `VerificationResult` | Add signed document, registry, audit and result records |
| 8 | `crypto/DocumentSigner`, `AbstractSigner`, `RsaSigner` | Add signer interface, abstract class and RSA implementation |
| 9 | `crypto/KeyManager` | Add RSA key generation with password-protected storage |
| 10 | `storage/Repository`, `InMemoryRepository`, `TransactionManager`, `NoTransaction` | Add generic repository and transaction interfaces |
| 11 | `storage/DocumentCodec` | Add .edoc text format codec |
| 12 | `storage/Database` | Add SQLite connection with transaction support |
| 13 | `storage/JdbcRegistryDao`, `JdbcAuditDao` | Add JDBC DAO for registry and audit log |
| 14 | `service/IssuanceService` | Add issuance service with transaction |
| 15 | `service/VerificationService` | Add verification service |
| 16 | `service/BatchVerifier` | Add multithreaded batch verification |
| 17 | `service/Analytics` | Add Stream API statistics |
| 18 | `report/Report`, `CsvReport`, `JsonReport` | Add abstract report with CSV and JSON export |
| 19 | `ui/Translit`, `Ui`, `ConsoleApp`, `Main`, `.vscode/` | Add Kazakh console menu and VS Code launch config |
| 20 | тест: `TestSupport`, `model/EDocumentTest`, `crypto/RsaSignerTest` | Add tests for EDocument and RSA signer |
| 21 | тест: `crypto/KeyManagerTest`, `storage/DocumentCodecTest` | Add tests for key manager and codec |
| 22 | тест: `storage/InMemoryRepositoryTest`, `service/AnalyticsTest`, `report/ReportTest` | Add tests for repository, analytics and reports |
| 23 | тест: `service/ServicesTest`, `service/BatchVerifierTest` | Add tests for services and batch verification |
| 24 | тест: `storage/JdbcDaoTest` | Add SQLite DAO and transaction tests |
| 25 | `docs/UML.md` | Add UML diagrams |
| 26 | `docs/SECURITY.md` | Add security explanation |
| 27 | `LICENSE` | Add MIT license |
| 28 | `docs/images/*.png`, README "Скриншот" бөлімі | Add screenshots to README |

Коммиттерді **әртүрлі күндерге** бөліңіз: талапта олар бірнеше аптаға жайылуы керек.

## Тармақтар және Pull Request (командалық жұмыс)

Командада әр адам өз модулін бөлек тармақта жасайды:

1. GitHub Desktop: **Current Branch** → **New Branch** → атауы, мысалы `feature/jdbc-dao`.
2. Өзгерістерді сол тармаққа коммит жасаңыз, **Publish branch** басыңыз.
3. **Create Pull Request** → сипаттама жазыңыз → командаласыңыз **Review** жасайды → **Merge pull request**.
4. Негізгі тармаққа оралып, **Fetch origin / Pull** жасаңыз.

Ұсынылатын бөлу: `feature/crypto` (3, 8-9), `feature/database` (10-13), `feature/services` (14-17), `feature/ui-reports` (18-19).

## Release v1.0
GitHub Desktop-та release жасалмайды. github.com сайтында: **Releases** → **Draft a new release** → тег `v1.0` → **Publish release**.
