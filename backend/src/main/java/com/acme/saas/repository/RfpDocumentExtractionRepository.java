package com.acme.saas.repository;

import com.acme.saas.domain.RfpDocumentExtraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RfpDocumentExtractionRepository extends JpaRepository<RfpDocumentExtraction, UUID> {
    Optional<RfpDocumentExtraction> findByDocumentId(UUID documentId);
}
