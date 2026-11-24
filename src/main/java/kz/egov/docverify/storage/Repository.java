package kz.egov.docverify.storage;

import java.util.List;
import java.util.Optional;

import kz.egov.docverify.model.Identifiable;

/** Generic репозиторий интерфейсі: кез келген T нысаны үшін жұмыс істейді. */
public interface Repository<T extends Identifiable<ID>, ID> {
    void save(T item);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean deleteById(ID id);
}
