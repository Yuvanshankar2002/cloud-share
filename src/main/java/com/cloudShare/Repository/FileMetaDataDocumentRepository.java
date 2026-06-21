package com.cloudShare.Repository;

import com.cloudShare.Entity.FileMetaDataDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetaDataDocumentRepository extends MongoRepository<FileMetaDataDocument, String> {
    List<FileMetaDataDocument> findByClerkId(String clerkId);
    Long countByClerkId(String clerkId);

}
