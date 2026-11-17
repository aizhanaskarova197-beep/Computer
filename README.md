# eGov Document Verifier

Электрондық анықтамалардың түпнұсқалығын тексеру: цифрлық қолтаңба (RSA) және күшін жою тізілімі. Java 17, Maven, JUnit 5.

## Мәселе

Электрондық анықтама жиі файл немесе скриншот түрінде жіберіледі. Қабылдаушы жақ құжаттың өзгертілмегенін, күші жойылмағанын және мерзімі өтпегенін тез тексере алмайды. Толық талдау: [docs/problem-analysis.md](docs/problem-analysis.md).

## Шешім

1. Беруші құжатқа RSA қолтаңба қояды (`SHA256withRSA`, стандартты JCA) және құжатты тізілімге жазады. Нәтиже: `.edoc` файлы.
2. Тексеруші `.edoc` файлын тексереді. Нәтиже: `VALID`, `TAMPERED`, `NOT_FOUND`, `REVOKED` немесе `EXPIRED`.
3. Файлдағы кез келген өзгеріс (сома, мерзім, аты-жөні) қолтаңбаны бұзады.

## Мүмкіндіктер

- 4 түрлі анықтаманы жасау және қол қою
- файлды тексеру: қолтаңба, тізілім, күшін жою, мерзім
- құжаттың күшін жою
- тізілімді көру
- демо-сценарий: шығару, тексеру, өзгерту, күшін жою
- сәтсіз тексерулерден уақытша бұғаттау (3 әрекет → 5 минут, одан кейін 10, 20 минут) және оның демосы

## Талаптар

- JDK 17 немесе жоғары
- VS Code және **Extension Pack for Java**
- Maven (тек командалық жолда `mvn` қолдану үшін, VS Code-та міндетті емес)

## VS Code-та іске қосу

1. Репозиторийді компьютерге клондаңыз (GitHub Desktop) және VS Code-та **File → Open Folder** арқылы ашыңыз.
2. **Extension Pack for Java** орнатыңыз. Төменгі жолақта Java жобасы жүктелгенше күтіңіз.
3. `src/main/java/kz/egov/docverify/ui/Main.java` файлын ашып, `main` әдісінің үстіндегі **Run** батырмасын басыңыз.
4. Терминалда мәзір шығады. Демо үшін `5`, бұғаттау демосы үшін `6` енгізіңіз.

Командалық жолмен (Maven орнатылған болса):

```bash
mvn package
java -jar target/egov-docverify-1.0.0.jar
```

Тесттер: VS Code-тағы **Testing** панелінен немесе `mvn test` командасымен.

Бағдарлама жоба папкасында `keys/`, `data/` және `output/` папкаларын өзі жасайды. Үшеуі де `.gitignore` ішінде.

## Қолдану мысалы

```
=== eGov Document Verifier ===
1. Issue a document
2. Verify a document file
3. Revoke a document
4. Show registry
5. Run demo (issue, verify, tamper, revoke)
6. Run lockout demo (repeated failed checks)
0. Exit
Choose an option: 5
1) Document issued: output/demo-original.edoc
2) Verifying the original file:
Result : VALID  [OK]
3) Someone changed the amount 1250000 -> 9999999 in a copy: output/demo-forged.edoc
Result : TAMPERED  [WARNING]
4) The issuer revoked the original document.
Result : REVOKED  [WARNING]
```

Мәзір мәтіндері ағылшын тілінде: Windows консолі кирилл және қазақ әріптерін бұрмалауы мүмкін.

## Жоба құрылымы

```
src/main/java/kz/egov/docverify/
  model/      EDocument, SignedDocument, RegistryEntry, VerificationResult, DocumentType, VerificationStatus
  crypto/     DocumentSigner (interface), RsaDocumentSigner, KeyManager
  storage/    DocumentCodec (.edoc форматы), DocumentRegistry (CSV)
  service/    IssuanceService, VerificationService
  security/   RateLimiter (уақытша бұғаттау)
  exception/  DocVerifyException
  ui/         ConsoleApp, Main
src/test/java/...   50 JUnit 5 тесті
docs/               UML.md, problem-analysis.md, research.md
```

UML диаграммалары: [docs/UML.md](docs/UML.md).

## Қауіпсіздік ескертпесі

- Қолтаңба стандартты JCA арқылы жасалады (`SHA256withRSA`, RSA 2048). Өз криптографиялық алгоритм ойлап табылмаған.
- Жабық кілт `keys/private.key` файлында ашық түрде сақталады, ол репозиторийге түспейді. Кілтті ешкімге бермеңіз.
- Тізілімде жеке деректер сақталмайды: тек ID, түрі, мерзімдері және күйі.
- Қатарынан 3 сәтсіз тексеруден кейін тексеру 5 минутқа бұғатталады, әр келесі бұғат екі есе ұзағырақ (`RateLimiter`). Бұл жалған файлдарды іріктеп тексеруден қорғайды.

## Шектеулер

- Оқу жобасы, өнеркәсіптік қолдануға арналмаған.
- ГОСТ Р 34.10-2015, сертификат тізбегі және CRL/OCSP қолданылмайды.
- Беруші мен тексеруші бір бағдарламада және бір тізілімді қолданады.

## Скриншот

Скриншоттар кейін қосылады (`docs/images/`).

## Лицензия

MIT, [LICENSE](LICENSE) файлын қараңыз.
