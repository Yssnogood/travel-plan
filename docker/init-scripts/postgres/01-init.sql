-- PostgreSQL Initialization Script
-- Creates all necessary schemas and tables for Travel Management System

-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS travel_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;

-- Auth Schema Tables
CREATE TABLE IF NOT EXISTS auth_schema.roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth_schema.permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth_schema.role_permissions (
    role_id BIGINT REFERENCES auth_schema.roles(id) ON DELETE CASCADE,
    permission_id BIGINT REFERENCES auth_schema.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS auth_schema.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Schema Tables
CREATE TABLE IF NOT EXISTS user_schema.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    role_id BIGINT REFERENCES auth_schema.roles(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_schema.user_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES user_schema.users(id) ON DELETE CASCADE,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    country VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_schema.user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES user_schema.users(id) ON DELETE CASCADE,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    UNIQUE(user_id, preference_key)
);

-- Travel Schema Tables
CREATE TABLE IF NOT EXISTS travel_schema.travels (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration_days INT GENERATED ALWAYS AS (end_date - start_date) STORED,
    status VARCHAR(30) DEFAULT 'DRAFT',
    total_budget DECIMAL(12, 2),
    currency VARCHAR(3) DEFAULT 'EUR',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS travel_schema.destinations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    description TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS travel_schema.travel_destinations (
    id BIGSERIAL PRIMARY KEY,
    travel_id BIGINT REFERENCES travel_schema.travels(id) ON DELETE CASCADE,
    destination_id BIGINT REFERENCES travel_schema.destinations(id),
    arrival_date DATE,
    departure_date DATE,
    order_index INT DEFAULT 0,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS travel_schema.activities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    duration_hours DECIMAL(4, 2),
    price DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'EUR',
    destination_id BIGINT REFERENCES travel_schema.destinations(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS travel_schema.travel_activities (
    id BIGSERIAL PRIMARY KEY,
    travel_destination_id BIGINT REFERENCES travel_schema.travel_destinations(id) ON DELETE CASCADE,
    activity_id BIGINT REFERENCES travel_schema.activities(id),
    scheduled_date DATE,
    scheduled_time TIME,
    status VARCHAR(30) DEFAULT 'PLANNED'
);

CREATE TABLE IF NOT EXISTS travel_schema.accommodations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    address VARCHAR(500),
    destination_id BIGINT REFERENCES travel_schema.destinations(id),
    price_per_night DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'EUR',
    rating DECIMAL(2, 1),
    amenities TEXT[],
    contact_info JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS travel_schema.travel_accommodations (
    id BIGSERIAL PRIMARY KEY,
    travel_id BIGINT REFERENCES travel_schema.travels(id) ON DELETE CASCADE,
    accommodation_id BIGINT REFERENCES travel_schema.accommodations(id),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    booking_reference VARCHAR(100),
    total_price DECIMAL(10, 2),
    status VARCHAR(30) DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS travel_schema.transportations (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    provider VARCHAR(255),
    departure_location VARCHAR(255) NOT NULL,
    arrival_location VARCHAR(255) NOT NULL,
    departure_time TIMESTAMP,
    arrival_time TIMESTAMP,
    price DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'EUR',
    booking_reference VARCHAR(100),
    travel_id BIGINT REFERENCES travel_schema.travels(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment Schema Tables
CREATE TABLE IF NOT EXISTS payment_schema.payment_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_customer_id VARCHAR(255),
    provider_payment_method_id VARCHAR(255),
    last_four VARCHAR(4),
    brand VARCHAR(50),
    exp_month INT,
    exp_year INT,
    is_default BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_schema.payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    travel_id BIGINT,
    payment_method_id BIGINT REFERENCES payment_schema.payment_methods(id),
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    status VARCHAR(30) DEFAULT 'PENDING',
    provider VARCHAR(50) NOT NULL,
    provider_payment_id VARCHAR(255),
    provider_charge_id VARCHAR(255),
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_schema.refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT REFERENCES payment_schema.payments(id),
    amount DECIMAL(12, 2) NOT NULL,
    reason TEXT,
    status VARCHAR(30) DEFAULT 'PENDING',
    provider_refund_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_email ON user_schema.users(email);
CREATE INDEX idx_users_status ON user_schema.users(status);
CREATE INDEX idx_travels_created_by ON travel_schema.travels(created_by);
CREATE INDEX idx_travels_status ON travel_schema.travels(status);
CREATE INDEX idx_travels_dates ON travel_schema.travels(start_date, end_date);
CREATE INDEX idx_destinations_country ON travel_schema.destinations(country);
CREATE INDEX idx_payments_user ON payment_schema.payments(user_id);
CREATE INDEX idx_payments_status ON payment_schema.payments(status);
CREATE INDEX idx_payment_methods_user ON payment_schema.payment_methods(user_id);

-- Insert default roles
INSERT INTO auth_schema.roles (name, description) VALUES 
    ('ADMIN', 'System administrator with full access'),
    ('MANAGER', 'Manager with limited administrative access'),
    ('USER', 'Regular user with basic access')
ON CONFLICT (name) DO NOTHING;

-- Insert default permissions
INSERT INTO auth_schema.permissions (name, description, resource, action) VALUES
    ('users:read', 'Read user data', 'users', 'read'),
    ('users:write', 'Create and update users', 'users', 'write'),
    ('users:delete', 'Delete users', 'users', 'delete'),
    ('travels:read', 'Read travel data', 'travels', 'read'),
    ('travels:write', 'Create and update travels', 'travels', 'write'),
    ('travels:delete', 'Delete travels', 'travels', 'delete'),
    ('payments:read', 'Read payment data', 'payments', 'read'),
    ('payments:write', 'Create and update payments', 'payments', 'write'),
    ('payments:delete', 'Delete payments', 'payments', 'delete'),
    ('admin:access', 'Access admin dashboard', 'admin', 'access')
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to roles
INSERT INTO auth_schema.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth_schema.roles r, auth_schema.permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO auth_schema.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth_schema.roles r, auth_schema.permissions p
WHERE r.name = 'MANAGER' AND p.name NOT IN ('users:delete', 'payments:delete')
ON CONFLICT DO NOTHING;

INSERT INTO auth_schema.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth_schema.roles r, auth_schema.permissions p
WHERE r.name = 'USER' AND p.action = 'read'
ON CONFLICT DO NOTHING;

-- Create default admin user (password: admin123)
INSERT INTO user_schema.users (email, password_hash, first_name, last_name, role_id, email_verified, status)
SELECT 'admin@travel-plan.com', 
       '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqkQeq6a.TvwWJB9YFHB7c7JKxZHa', 
       'System', 
       'Admin',
       r.id,
       TRUE,
       'ACTIVE'
FROM auth_schema.roles r WHERE r.name = 'ADMIN'
ON CONFLICT (email) DO NOTHING;

COMMIT;
