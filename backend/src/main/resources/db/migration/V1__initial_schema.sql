-- 1. USERS TABLE
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'student',
    password VARCHAR(255) NOT NULL,
    avatar VARCHAR(500) NOT NULL DEFAULT '',
    campus VARCHAR(100) NOT NULL DEFAULT '',
    bio VARCHAR(300) NOT NULL DEFAULT '',
    phone VARCHAR(20) NOT NULL DEFAULT '',
    rating NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    lender_rating NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_lender_reviews INTEGER NOT NULL DEFAULT 0,
    renter_rating NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_renter_reviews INTEGER NOT NULL DEFAULT 0,
    item_quality_average NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_item_quality_reviews INTEGER NOT NULL DEFAULT 0,
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_role CHECK (role IN ('student', 'admin')),
    CONSTRAINT ck_users_rating CHECK (rating >= 0 AND rating <= 5),
    CONSTRAINT ck_users_lender_rating CHECK (lender_rating >= 0 AND lender_rating <= 5),
    CONSTRAINT ck_users_renter_rating CHECK (renter_rating >= 0 AND renter_rating <= 5),
    CONSTRAINT ck_users_item_quality_average CHECK (item_quality_average >= 0 AND item_quality_average <= 5)
);

CREATE INDEX idx_users_created_at ON users (created_at DESC);

-- 2. ITEMS TABLE
CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(30) NOT NULL,
    price_per_day NUMERIC(10,2) NOT NULL,
    condition VARCHAR(20) NOT NULL DEFAULT 'good',
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    location VARCHAR(200) NOT NULL DEFAULT '',
    rating NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_items_category CHECK (category IN ('textbooks', 'electronics', 'bikes', 'cameras', 'furniture', 'clothing', 'sports', 'instruments', 'other')),
    CONSTRAINT ck_items_condition CHECK (condition IN ('new', 'like-new', 'good', 'fair', 'poor')),
    CONSTRAINT ck_items_price CHECK (price_per_day >= 0),
    CONSTRAINT ck_items_rating CHECK (rating >= 0 AND rating <= 5)
);

CREATE INDEX idx_items_owner ON items(owner_id);
CREATE INDEX idx_items_category ON items(category);
CREATE INDEX idx_items_condition ON items(condition);
CREATE INDEX idx_items_available ON items(is_available);
CREATE INDEX idx_items_created_at ON items(created_at DESC);

-- 3. ITEM IMAGES TABLE
CREATE TABLE item_images (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_item_images_item ON item_images(item_id);

-- 4. RENTALS TABLE
CREATE TABLE rentals (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    renter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_price NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    message VARCHAR(500) NOT NULL DEFAULT '',
    offer_price NUMERIC(10,2),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_rentals_dates CHECK (end_date > start_date),
    CONSTRAINT ck_rentals_total_price CHECK (total_price >= 0),
    CONSTRAINT ck_rentals_offer_price CHECK (offer_price IS NULL OR offer_price >= 0),
    CONSTRAINT ck_rentals_status CHECK (status IN ('pending', 'approved', 'rejected', 'active', 'completed', 'cancelled'))
);

CREATE INDEX idx_rentals_renter_status ON rentals(renter_id, status);
CREATE INDEX idx_rentals_owner_status ON rentals(owner_id, status);
CREATE INDEX idx_rentals_item ON rentals(item_id);
CREATE INDEX idx_rentals_item_status_dates ON rentals(item_id, status, start_date, end_date);
CREATE INDEX idx_rentals_created_at ON rentals(created_at DESC);

-- 5. CONVERSATIONS TABLE
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL UNIQUE REFERENCES rentals(id) ON DELETE CASCADE,
    last_message VARCHAR(100) NOT NULL DEFAULT '',
    last_message_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. CONVERSATION PARTICIPANTS TABLE
CREATE TABLE conversation_participants (
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_user ON conversation_participants(user_id);
CREATE INDEX idx_conversations_last_message_at ON conversations(last_message_at DESC);

-- 7. MESSAGES TABLE
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content VARCHAR(2000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at);
CREATE INDEX idx_messages_unread ON messages(conversation_id, sender_id, is_read);

-- 8. REVIEWS TABLE
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL REFERENCES rentals(id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewee_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    item_rating INTEGER,
    type VARCHAR(20) NOT NULL,
    comment VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reviews_rental_reviewer UNIQUE (rental_id, reviewer_id),
    CONSTRAINT ck_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT ck_reviews_item_rating CHECK (item_rating IS NULL OR (item_rating >= 1 AND item_rating <= 5)),
    CONSTRAINT ck_reviews_type CHECK (type IN ('lender', 'renter'))
);

CREATE INDEX idx_reviews_reviewee ON reviews(reviewee_id);
CREATE INDEX idx_reviews_rental ON reviews(rental_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);

-- 9. NOTIFICATIONS TABLE
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(255) NOT NULL DEFAULT '',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_notifications_type CHECK (type IN ('rental_request', 'rental_status', 'review_received', 'message', 'system'))
);

CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- 10. REPORTS TABLE
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rental_id BIGINT NOT NULL REFERENCES rentals(id) ON DELETE CASCADE,
    reason VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    evidence_image VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    admin_notes TEXT NOT NULL DEFAULT '',
    admin_action VARCHAR(30) NOT NULL DEFAULT 'none',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_reports_reason CHECK (reason IN ('Late Return', 'Item Damage', 'Fake Product/Description', 'Inappropriate Behavior', 'Payment Issues', 'No Show', 'Other')),
    CONSTRAINT ck_reports_status CHECK (status IN ('pending', 'reviewed', 'resolved', 'dismissed')),
    CONSTRAINT ck_reports_admin_action CHECK (admin_action IN ('none', 'warned', 'listing_removed', 'account_suspended', 'resolved'))
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reported_user ON reports(reported_user_id);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);
