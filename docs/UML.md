# UML диаграммалары

## 1. Use Case

```mermaid
flowchart LR
    Issuer([Беруші]) --> U1(Анықтама беру)
    Issuer --> U2(Анықтаманы кері қайтару)
    Issuer --> U3(Есепті экспорттау CSV/JSON)
    Issuer --> U4(Статистика көру)
    Checker([Тексеруші]) --> U5(Файлды тексеру)
    Checker --> U6(Папканы көп ағынмен тексеру)
    U1 -. include .-> U7(RSA қолтаңба қою)
    U5 -. include .-> U8(Қолтаңбаны тексеру)
    U5 -. include .-> U9(Тізілімді тексеру)
```

## 2. Class diagram

```mermaid
classDiagram
    class Identifiable~ID~ {
        <<interface>>
        +id() ID
    }
    class Repository~T~ {
        <<interface>>
        +save(T)
        +findById(id) Optional
        +findAll() List
        +deleteById(id) boolean
    }
    class TransactionManager {
        <<interface>>
        +execute(work) R
    }
    class InMemoryRepository~T~
    class JdbcRegistryDao
    class JdbcAuditDao
    class Database {
        +execute(work) R
        +connection() Connection
    }
    class NoTransaction
    class DocumentSigner {
        <<interface>>
        +sign(text, key) String
        +verify(text, sig, key) boolean
    }
    class AbstractSigner {
        <<abstract>>
        #algorithm() String
    }
    class RsaSigner
    class KeyManager {
        +loadOrCreate(dir, password) KeyPair
    }
    class Report {
        <<abstract>>
        #render(entries) String
        +extension() String
        +export(entries, file) Path
    }
    class CsvReport
    class JsonReport
    class IssuanceService {
        +issue(doc) SignedDocument
        +revoke(id)
    }
    class VerificationService {
        +verify(signed) VerificationResult
        +verifyFile(path) VerificationResult
    }
    class BatchVerifier {
        +verifyAll(files, threads) BatchResult
    }
    class Analytics {
        +countBy(items, fn) Map
        +summary(entries, today) List
    }
    class EDocument {
        <<record>>
    }
    class RegistryEntry {
        <<record>>
    }
    class AuditEntry {
        <<record>>
    }

    RegistryEntry ..|> Identifiable
    AuditEntry ..|> Identifiable
    InMemoryRepository ..|> Repository
    JdbcRegistryDao ..|> Repository
    JdbcAuditDao ..|> Repository
    Database ..|> TransactionManager
    NoTransaction ..|> TransactionManager
    JdbcRegistryDao --> Database
    JdbcAuditDao --> Database
    AbstractSigner ..|> DocumentSigner
    RsaSigner --|> AbstractSigner
    CsvReport --|> Report
    JsonReport --|> Report
    IssuanceService --> DocumentSigner
    IssuanceService --> Repository
    IssuanceService --> TransactionManager
    VerificationService --> DocumentSigner
    VerificationService --> Repository
    BatchVerifier --> VerificationService
    IssuanceService ..> EDocument
```

## 3. Sequence: анықтама беру (транзакциямен)

```mermaid
sequenceDiagram
    actor U as Беруші
    participant C as ConsoleApp
    participant S as IssuanceService
    participant T as Database (транзакция)
    participant R as JdbcRegistryDao
    participant A as JdbcAuditDao
    U->>C: деректерді енгізу
    C->>S: issue(EDocument)
    S->>T: execute(...)
    T->>T: setAutoCommit(false)
    S->>R: findById(id) (қайталануын тексеру)
    S->>S: RSA қолтаңба қою
    S->>R: save(RegistryEntry)
    S->>A: save(AuditEntry ISSUED)
    alt қате болмаса
        T->>T: commit()
    else қате болса
        T->>T: rollback()
    end
    S-->>C: SignedDocument
    C-->>U: out/ID.edoc сақталды
```

## 4. Sequence: файлды тексеру

```mermaid
sequenceDiagram
    actor U as Тексеруші
    participant C as ConsoleApp
    participant V as VerificationService
    participant K as DocumentCodec
    participant G as RsaSigner
    participant R as Repository
    U->>C: файл жолы
    C->>V: verifyFile(path)
    V->>K: decode(мәтін)
    V->>G: verify(canonicalText, signature, publicKey)
    alt қолтаңба қате
        V-->>C: TAMPERED
    else қолтаңба дұрыс
        V->>R: findById(id)
        V-->>C: NOT_FOUND / REVOKED / EXPIRED / VALID
    end
    C-->>U: нәтиже
```

## 5. Activity: тексеру алгоритмі

```mermaid
flowchart TD
    A([Бастау]) --> B[Файлды оқу және талдау]
    B -->|пішім қате| T[TAMPERED]
    B --> C{Қолтаңба дұрыс па?}
    C -->|жоқ| T
    C -->|иә| D{Тізілімде бар ма?}
    D -->|жоқ| N[NOT_FOUND]
    D -->|иә| E{Кері қайтарылған ба?}
    E -->|иә| R[REVOKED]
    E -->|жоқ| F{Мерзімі өтті ме?}
    F -->|иә| X[EXPIRED]
    F -->|жоқ| V[VALID]
```

## 6. ER диаграммасы (SQLite)

```mermaid
erDiagram
    REGISTRY {
        TEXT id PK
        TEXT type
        TEXT issued_on
        TEXT valid_until
        TEXT status
    }
    AUDIT_LOG {
        TEXT id PK
        TEXT doc_id
        TEXT action
        TEXT at
    }
    REGISTRY ||--o{ AUDIT_LOG : "doc_id"
```
