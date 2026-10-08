CREATE TABLE blood_unit (
   id            uuid PRIMARY KEY,
   business_key  text NOT NULL UNIQUE,
   status        text NOT NULL,
   title         text NOT NULL,
   created_at    timestamptz NOT NULL DEFAULT now(),
   CONSTRAINT blood_unit_status_known
   CHECK (status IN ('COLLECTED', 'TESTED', 'RELEASED', 'ISSUED', 'DISCARDED'))
);
