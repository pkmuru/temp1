-- Seed data for tests and the EmbeddedPostgresRunner dev database.
-- Idempotent (ON CONFLICT DO NOTHING) — multiple Spring test contexts share one embedded
-- PostgreSQL, so this script may run more than once against the same database.

-- The three templates from the requirement: FA success, Field Leader success and the
-- failed-delivery notice, with {placeholder} merge fields.
INSERT INTO aat_app.email_template (code, name, subject, body) VALUES
('FA_DELIVERY_SUCCESS', 'Successful Delivery - FA',
 'STAAT Client Retention Packs',
 '<p>Hi {faName},</p><p>You have received a client reassignment via the AAT tool. This email contains the following {packCount} STAAT Client Retention Packs as HTML attachments.</p>{householdTable}<p>Thanks,<br/>AAT Delivery System</p>'),
('FL_DELIVERY_SUCCESS', 'Successful Delivery - Field Leader',
 'STAAT Client Retention Packs - Unassigned Clients',
 '<p>Hi {fieldLeaderName},</p><p>Your STAAT Client Retention Packs for unassigned or pended client relationships have been generated. This email contains {packCount} packs as HTML attachments.</p>{householdTable}<p>Thanks,<br/>AAT Delivery System</p>'),
('DELIVERY_FAILED', 'Failed Delivery Notification',
 'STAAT Client Retention Packs - Temporary Delivery Issue (Action Not Required)',
 '<p>Hi {faName},</p><p>We attempted to deliver your {packCount} STAAT Client Retention Packs but encountered a temporary issue ({failureReason}). We are retrying delivery, and no action is needed.</p>{householdTable}<p>Thank you,<br/>AAT Delivery System</p>')
ON CONFLICT (code) DO NOTHING;

-- Sample externally-provided retention packs, keyed by ACE id (datamesh data product).
INSERT INTO datamesh.staat_insight_document (ace_id, lead_client_name, file_name, html_content) VALUES
('ACE-1001', 'John Smith Household', 'John_Smith_Household.html',
 '<html><body><h1>John Smith Household</h1><p>STAAT insights...</p></body></html>'),
('ACE-1002', 'Jane Doe Household', 'Jane_Doe_Household.html',
 '<html><body><h1>Jane Doe Household</h1><p>STAAT insights...</p></body></html>'),
('ACE-1003', 'Brown Family', 'Brown_Family.html',
 '<html><body><h1>Brown Family</h1><p>STAAT insights...</p></body></html>')
ON CONFLICT (ace_id) DO NOTHING;
