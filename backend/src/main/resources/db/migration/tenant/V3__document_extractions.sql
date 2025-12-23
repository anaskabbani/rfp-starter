CREATE TABLE IF NOT EXISTS rfp_document_extractions (
  id UUID PRIMARY KEY,
  document_id UUID NOT NULL REFERENCES rfp_documents(id) ON DELETE CASCADE,
  extracted_text TEXT,
  tables_json JSONB,
  key_values_json JSONB,
  status TEXT NOT NULL DEFAULT 'PENDING',
  error_message TEXT,
  page_count INT,
  sheet_count INT,
  character_count INT,
  table_count INT,
  extracted_at TIMESTAMPTZ,
  CONSTRAINT fk_document FOREIGN KEY (document_id) REFERENCES rfp_documents(id)
);

CREATE INDEX IF NOT EXISTS idx_extractions_document_id ON rfp_document_extractions(document_id);
CREATE INDEX IF NOT EXISTS idx_extractions_status ON rfp_document_extractions(status);
