package kz.egov.docverify.model;

/** Generic интерфейс: идентификаторы бар кез келген нысан (Repository<T, ID> үшін). */
public interface Identifiable<ID> {
    ID id();
}
