-- RFP Documents table
CREATE TABLE IF NOT EXISTS rfp_documents (
  id UUID PRIMARY KEY,
  filename TEXT NOT NULL,
  original_filename TEXT NOT NULL,
  content_type TEXT NOT NULL,
  file_size BIGINT NOT NULL,
  storage_path TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'UPLOADED',
  error_message TEXT,
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  processed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_rfp_documents_status ON rfp_documents(status);
CREATE INDEX IF NOT EXISTS idx_rfp_documents_uploaded_at ON rfp_documents(uploaded_at DESC);

