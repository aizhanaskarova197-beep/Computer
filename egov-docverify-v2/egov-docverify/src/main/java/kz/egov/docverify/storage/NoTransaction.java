package kz.egov.docverify.storage;

import java.util.concurrent.Callable;

import kz.egov.docverify.exception.DocVerifyException;

/** Транзакциясыз нұсқа (жадтағы репозиторийлер мен тесттер үшін). */
public class NoTransaction implements TransactionManager {
    @Override
    public <R> R execute(Callable<R> work) {
        try {
            return work.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocVerifyException("Әрекет орындалмады: " + e.getMessage(), e);
        }
    }
}
