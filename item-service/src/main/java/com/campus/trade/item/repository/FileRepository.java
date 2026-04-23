package com.campus.trade.item.repository;

import com.campus.trade.item.model.FileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface FileRepository extends MongoRepository<FileDocument, String> {

    List<FileDocument> findByUrlIn(Collection<String> urls);
}
