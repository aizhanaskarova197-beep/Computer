package kz.egov.docverify.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.VerificationResult;

/** Көп ағынды тексеру: ExecutorService + Callable + Future. */
public class BatchVerifier {

    /** Нәтиже: файл -> тексеру нәтижесі, және қатысқан ағындар аттары. */
    public record BatchResult(Map<String, VerificationResult> results, Set<String> threadNames) {
    }

    private final VerificationService verifier;

    public BatchVerifier(VerificationService verifier) {
        this.verifier = verifier;
    }

    public BatchResult verifyAll(List<Path> files, int threads) {
        if (threads < 1) {
            throw new DocVerifyException("Ағын саны кемінде 1 болуы тиіс.");
        }
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        Map<String, VerificationResult> results = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Path f : files) {
                Callable<Void> task = () -> {
                    threadNames.add(Thread.currentThread().getName());
                    results.put(f.getFileName().toString(), verifier.verifyFile(f));
                    return null;
                };
                futures.add(pool.submit(task));
            }
            for (Future<Void> fu : futures) {
                fu.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocVerifyException("Тексеру үзілді.", e);
        } catch (ExecutionException e) {
            throw new DocVerifyException("Ағындағы қате: " + e.getCause().getMessage(), e.getCause());
        } finally {
            pool.shutdown();
        }
        return new BatchResult(new TreeMap<>(results), threadNames);
    }
}
