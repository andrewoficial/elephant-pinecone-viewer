package ru.kantser.pineview.domain.port;

import ru.kantser.pineview.domain.error.ImportException;
import ru.kantser.pineview.domain.model.ImportItem;
import ru.kantser.pineview.domain.model.ImportSource;
import java.util.List;
import java.util.Set;

public interface ImportPort {
    
    /**
     * Парсит файл в список элементов для импорта.
     * 
     * @param source источник данных (файл, поток, URL)
     * @return список валидных элементов
     * @throws ImportException если формат не распознан или данные битые
     */
    List<ImportItem> parse(ImportSource source) throws ImportException;


    /**
     * Поддерживаемые расширения для этого парсера.
     * Используется для авто-определения формата.
     */
    Set<String> supportedExtensions();
}