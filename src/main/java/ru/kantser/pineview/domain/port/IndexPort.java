package ru.kantser.pineview.domain.port;

import org.openapitools.db_control.client.model.IndexModel;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IndexPort {
    public CompletableFuture<List<IndexModel>> fetchIndexes();

}
