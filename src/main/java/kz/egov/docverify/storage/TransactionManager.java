package kz.egov.docverify.storage;

import java.util.concurrent.Callable;

/** Бірнеше әрекетті бір транзакцияда орындау: не бәрі сәтті, не ештеңе. */
public interface TransactionManager {
    <R> R execute(Callable<R> work);
}
