# UML диаграммалары

Диаграммалар Mermaid форматында жазылған: GitHub олардың суретін автоматты түрде көрсетеді.
Есепке сурет керек болса, кодты [mermaid.live](https://mermaid.live) сайтына қойып, PNG етіп жүктеп алуға болады.

## 1. Use Case диаграммасы

```mermaid
flowchart LR
    Issuer(["Issuer (issuing body)"])
    Verifier(["Verifier (employer, bank)"])

    subgraph System["eGov Document Verifier"]
        UC1("Issue a signed document")
        UC2("Revoke a document")
        UC3("Show registry")
        UC4("Verify a document file")
        UC5("Run demo")
    end

    Issuer --- UC1
    Issuer --- UC2
    Issuer --- UC3
    Issuer --- UC5
    Verifier --- UC4
```

Құжат иесі (азамат) файлды берушіден алып, тексерушіге көрсетеді. Бұл қадам жүйеден тыс өтеді, сондықтан диаграммада жоқ.

## 2. Класс диаграммасы

```mermaid
classDiagram
    direction LR

    class DocumentSigner {
        <<interface>>
        +sign(byte[] data) byte[]
        +verify(byte[] data, byte[] signature) boolean
    }
    class RsaDocumentSigner {
        -PrivateKey privateKey
        -PublicKey publicKey
        +sign(byte[] data) byte[]
        +verify(byte[] data, byte[] signature) boolean
        +verifyOnly(PublicKey key)$ RsaDocumentSigner
    }
    class KeyManager {
        +generate()$ KeyPair
        +save(KeyPair pair, Path dir)$ void
        +load(Path dir)$ KeyPair
        +exists(Path dir)$ boolean
        +loadOrCreate(Path dir)$ KeyPair
    }

    class EDocument {
        <<record>>
        +String id
        +DocumentType type
        +String ownerIin
        +String ownerName
        +Instant issuedAt
        +Instant expiresAt
        +String content
    }
    class SignedDocument {
        <<record>>
        +EDocument document
        +byte[] signature
        +signatureBase64() String
    }
    class RegistryEntry {
        <<record>>
        +String id
        +DocumentType type
        +Instant issuedAt
        +Instant expiresAt
        +boolean revoked
        +withRevoked() RegistryEntry
    }
    class VerificationResult {
        <<record>>
        +VerificationStatus status
        +EDocument document
        +isValid() boolean
    }
    class DocumentType {
        <<enumeration>>
        PENSION_CERTIFICATE
        PROPERTY_CERTIFICATE
        NO_CRIMINAL_RECORD
        EDUCATION_CERTIFICATE
    }
    class VerificationStatus {
        <<enumeration>>
        VALID
        TAMPERED
        NOT_FOUND
        REVOKED
        EXPIRED
    }

    class DocumentCodec {
        +canonicalBytes(EDocument d)$ byte[]
        +encode(SignedDocument s)$ String
        +decode(String text)$ SignedDocument
        +read(Path file)$ SignedDocument
        +write(Path file, SignedDocument s)$ void
    }
    class DocumentRegistry {
        -Map entries
        +register(EDocument d) void
        +contains(String id) boolean
        +isRevoked(String id) boolean
        +revoke(String id) boolean
        +list() List
        +save(Path file) void
        +load(Path file)$ DocumentRegistry
    }

    class IssuanceService {
        +issue(DocumentType type, String iin, String name, String content, int validDays) SignedDocument
    }
    class VerificationService {
        +verify(SignedDocument s) VerificationResult
        +verifyFile(Path file) VerificationResult
    }

    class RateLimiter {
        -int maxFailures
        -Duration baseBlock
        +isBlocked(String key) boolean
        +secondsLeft(String key) long
        +registerFailure(String key) void
        +reset(String key) void
    }
    class ConsoleApp {
        +run() void
    }
    class Main {
        +main(String[] args)$ void
    }
    class DocVerifyException {
        <<exception>>
    }

    DocumentSigner <|.. RsaDocumentSigner
    SignedDocument *-- EDocument
    EDocument --> DocumentType
    RegistryEntry --> DocumentType
    DocumentRegistry o-- RegistryEntry
    VerificationResult --> VerificationStatus
    VerificationResult --> EDocument

    IssuanceService --> DocumentSigner
    IssuanceService --> DocumentRegistry
    IssuanceService ..> DocumentCodec : uses
    IssuanceService ..> SignedDocument : creates
    VerificationService --> DocumentSigner
    VerificationService --> DocumentRegistry
    VerificationService --> RateLimiter
    ConsoleApp --> RateLimiter
    VerificationService ..> DocumentCodec : uses
    VerificationService ..> VerificationResult : returns
    DocumentCodec ..> SignedDocument

    ConsoleApp --> IssuanceService
    ConsoleApp --> VerificationService
    ConsoleApp --> DocumentRegistry
    Main ..> ConsoleApp : creates
    Main ..> KeyManager : uses
    DocVerifyException <.. DocumentCodec : throws
    DocVerifyException <.. IssuanceService : throws
```

## 3. Sequence диаграммасы: құжатты тексеру

```mermaid
sequenceDiagram
    actor V as Verifier
    participant UI as ConsoleApp
    participant VS as VerificationService
    participant C as DocumentCodec
    participant S as RsaDocumentSigner
    participant R as DocumentRegistry

    V->>UI: path to the .edoc file
    UI->>VS: verifyFile(path)
    VS->>C: read(path)
    C-->>VS: SignedDocument
    VS->>C: canonicalBytes(document)
    C-->>VS: bytes
    VS->>S: verify(bytes, signature)
    S-->>VS: true or false
    alt signature is invalid
        VS-->>UI: TAMPERED
    else signature is valid
        VS->>R: contains(id), isRevoked(id)
        R-->>VS: flags
        VS-->>UI: NOT_FOUND, REVOKED, EXPIRED or VALID
    end
    UI-->>V: result and document data
```

## 4. Activity диаграммасы: тексеру алгоритмі

```mermaid
flowchart TD
    A(["Open the .edoc file"]) --> B{"Format is correct?"}
    B -- no --> E1(["Error: malformed file"])
    B -- yes --> C{"Signature is valid?"}
    C -- no --> T(["TAMPERED"])
    C -- yes --> D{"ID is in the registry?"}
    D -- no --> N(["NOT_FOUND"])
    D -- yes --> R{"Revoked?"}
    R -- yes --> RV(["REVOKED"])
    R -- no --> X{"Validity period ended?"}
    X -- yes --> EX(["EXPIRED"])
    X -- no --> V(["VALID"])
```

## 5. Құжаттың өмірлік циклі (State диаграммасы)

```mermaid
stateDiagram-v2
    [*] --> Active : issue
    Active --> Revoked : revoke
    Active --> Expired : validity period ends
    Revoked --> [*]
    Expired --> [*]
```

`Expired` күйі тізілімде сақталмайды: ол тексеру кезінде ағымдағы уақыт бойынша есептеледі.
