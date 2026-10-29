CREATE TABLE issuers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id),
    tax_id varchar(13) NOT NULL,
    legal_name varchar(255) NOT NULL,
    trade_name varchar(255),
    main_address text,
    establishment_address text,
    special_taxpayer_code varchar(40),
    obliged_accounting varchar(2) DEFAULT 'NO',
    establishment_code varchar(3) NOT NULL,
    emission_point_code varchar(3) NOT NULL,
    environment varchar(12) NOT NULL DEFAULT 'TEST' CHECK(environment IN ('TEST', 'PRODUCTION')),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE document_sequences (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    issuer_id uuid NOT NULL REFERENCES issuers(id) ON DELETE CASCADE,
    document_type varchar(10) NOT NULL CHECK(document_type IN ('01', '04', '05', '06', '07')), -- 01: Factura, 04: Nota de Crédito
    next_number bigint NOT NULL CHECK(next_number > 0),
    UNIQUE(issuer_id, document_type)
);

CREATE TABLE electronic_documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    issuer_id uuid NOT NULL REFERENCES issuers(id),
    sale_id uuid REFERENCES sales(id),
    document_type varchar(10) NOT NULL,
    sequence_number bigint NOT NULL,
    access_key varchar(49),
    status varchar(30) NOT NULL CHECK(status IN (
        'GENERATED', 'SIGNED', 'SENT', 'RECEIVED', 'AUTHORIZED', 'REJECTED', 'FAILED'
    )),
    authorization_number varchar(100),
    authorized_at timestamptz,
    last_error text,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(issuer_id, document_type, sequence_number),
    UNIQUE(access_key)
);

CREATE TABLE electronic_document_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id uuid NOT NULL REFERENCES electronic_documents(id) ON DELETE CASCADE,
    stage varchar(40) NOT NULL,
    status varchar(40) NOT NULL,
    response_code varchar(100),
    response_payload text,
    occurred_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE electronic_document_files (
    document_id uuid NOT NULL REFERENCES electronic_documents(id) ON DELETE CASCADE,
    media_id uuid NOT NULL REFERENCES media_files(id) ON DELETE CASCADE,
    file_type varchar(20) NOT NULL CHECK(file_type IN ('XML_ORIGINAL', 'XML_SIGNED', 'XML_AUTHORIZED', 'RIDE_PDF')),
    PRIMARY KEY(document_id, file_type)
);

CREATE TABLE outbox_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type varchar(80) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload jsonb NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    locked_until timestamptz,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_outbox_due ON outbox_events(next_attempt_at) WHERE status = 'PENDING';
