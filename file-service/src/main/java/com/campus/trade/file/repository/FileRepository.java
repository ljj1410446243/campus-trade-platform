package com.campus.trade.file.repository;

import com.campus.trade.file.model.FileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FileRepository extends MongoRepository<FileDocument, String> {
}
