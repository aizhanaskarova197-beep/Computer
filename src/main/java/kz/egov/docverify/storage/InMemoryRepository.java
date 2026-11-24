package kz.egov.docverify.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import kz.egov.docverify.model.Identifiable;

/** Generic класс: жадтағы репозиторий (тесттер үшін, ағындарға қауіпсіз). */
public class InMemoryRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {
    private final Map<ID, T> items = new ConcurrentHashMap<>();

    @Override
    public void save(T item) {
        items.put(item.id(), item);
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(items.values());
    }

    @Override
    public boolean deleteById(ID id) {
        return items.remove(id) != null;
    }
}
