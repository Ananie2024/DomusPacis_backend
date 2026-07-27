-- ============================================================
-- Domus Pacis Platform — Initial Schema
-- Managed by Flyway. Do not edit manually.
-- ============================================================

-- ── Users & Auth ─────────────────────────────────────────────
CREATE TABLE users (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customers (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36),
    full_name       VARCHAR(255)    NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    nationality     VARCHAR(100),
    id_number       VARCHAR(100),
    address_street  VARCHAR(255),
    address_city    VARCHAR(100),
    address_province VARCHAR(100),
    address_country VARCHAR(100),
    address_postal_code VARCHAR(50),
    segment         VARCHAR(50),
    notes           TEXT,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    INDEX idx_customer_user_id (user_id),
    INDEX idx_customer_email (email),
    INDEX idx_customer_phone (phone),
    INDEX idx_customer_fullname (full_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_log (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    actor_id        VARCHAR(36),
    actor_name      VARCHAR(255),
    action_label    VARCHAR(100),
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(36),
    outcome         VARCHAR(20),
    failure_reason  VARCHAR(500),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    timestamp       DATETIME(6)     NOT NULL,
    INDEX idx_audit_actor (actor_id),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE security_audit_log (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    event_type      VARCHAR(50)     NOT NULL,
    username        VARCHAR(255),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    outcome         VARCHAR(20),
    detail          VARCHAR(1000),
    timestamp       DATETIME(6)     NOT NULL,
    INDEX idx_sec_audit_user (username),
    INDEX idx_sec_audit_event (event_type),
    INDEX idx_sec_audit_timestamp (timestamp),
    INDEX idx_sec_audit_ip (ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Service Assets (single-table inheritance) ────────────────
CREATE TABLE service_assets (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    asset_type      VARCHAR(30)     NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    capacity        INT,
    price_per_unit  DECIMAL(12,2)   NOT NULL,
    pricing_unit    VARCHAR(20)     NOT NULL,
    is_available    BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    -- Room
    room_number     VARCHAR(20),
    room_type       VARCHAR(20),
    floor           INT,
    -- Conference Hall
    hall_code       VARCHAR(30),
    projector_available BOOLEAN,
    audio_system_available BOOLEAN,
    max_seating_layout VARCHAR(20),
    -- Wedding Garden
    is_indoor       BOOLEAN,
    has_stage       BOOLEAN,
    -- Retreat Center
    number_of_beds  INT,
    includes_chapel BOOLEAN,
    includes_catering BOOLEAN,
    INDEX idx_asset_type (asset_type),
    INDEX idx_asset_available (is_available),
    INDEX idx_asset_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE service_asset_images (
    asset_id        VARCHAR(36)     NOT NULL,
    image_path      VARCHAR(500),
    INDEX idx_sai_asset (asset_id),
    FOREIGN KEY (asset_id) REFERENCES service_assets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE room_amenities (
    room_id         VARCHAR(36)     NOT NULL,
    amenity         VARCHAR(255),
    INDEX idx_ra_room (room_id),
    FOREIGN KEY (room_id) REFERENCES service_assets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wedding_decoration_packages (
    garden_id       VARCHAR(36)     NOT NULL,
    package_name    VARCHAR(255),
    INDEX idx_wdp_garden (garden_id),
    FOREIGN KEY (garden_id) REFERENCES service_assets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Bookings ──────────────────────────────────────────────────
CREATE TABLE bookings (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    customer_id         VARCHAR(36)     NOT NULL,
    service_asset_id    VARCHAR(36)     NOT NULL,
    check_in_date       DATE            NOT NULL,
    check_out_date      DATE            NOT NULL,
    number_of_guests    INT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    special_requests    TEXT,
    total_amount        DECIMAL(12,2),
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    INDEX idx_booking_customer (customer_id),
    INDEX idx_booking_asset (service_asset_id),
    INDEX idx_booking_status (status),
    INDEX idx_booking_checkin (check_in_date),
    INDEX idx_booking_checkout (check_out_date),
    INDEX idx_booking_dates (check_in_date, check_out_date, status),
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    FOREIGN KEY (service_asset_id) REFERENCES service_assets(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Inventory ────────────────────────────────────────────────
CREATE TABLE suppliers (
    id                      VARCHAR(36)     NOT NULL PRIMARY KEY,
    name                     VARCHAR(255)    NOT NULL,
    contact_person           VARCHAR(255),
    phone                    VARCHAR(50),
    email                    VARCHAR(255),
    address                  TEXT,
    tax_identification_number VARCHAR(50),
    is_active                BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at               DATETIME(6)     NOT NULL,
    updated_at               DATETIME(6)     NOT NULL,
    INDEX idx_supplier_name (name),
    INDEX idx_supplier_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_items (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    name                 VARCHAR(255)    NOT NULL,
    category             VARCHAR(30)     NOT NULL,
    unit                 VARCHAR(30)     NOT NULL,
    current_stock        DECIMAL(12,3)   NOT NULL DEFAULT 0,
    reorder_level        DECIMAL(12,3)   NOT NULL DEFAULT 0,
    unit_cost            DECIMAL(12,2),
    supplier_id          VARCHAR(36),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_inv_category (category),
    INDEX idx_inv_stock (current_stock),
    INDEX idx_inv_supplier (supplier_id),
    INDEX idx_inv_name (name),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE menu_items (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    name                 VARCHAR(255)    NOT NULL,
    category             VARCHAR(20)     NOT NULL,
    description          TEXT,
    unit_price           DECIMAL(12,2)   NOT NULL,
    is_available         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_menu_category (category),
    INDEX idx_menu_available (is_available),
    INDEX idx_menu_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE menu_item_ingredients (
    menu_item_id     VARCHAR(36)     NOT NULL,
    inventory_item_id VARCHAR(36)    NOT NULL,
    INDEX idx_mii_menu (menu_item_id),
    INDEX idx_mii_inv (inventory_item_id),
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_movements (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    item_id              VARCHAR(36)     NOT NULL,
    movement_type        VARCHAR(20)     NOT NULL,
    quantity             DECIMAL(12,3)   NOT NULL,
    movement_date        DATE            NOT NULL,
    reference_note       VARCHAR(500),
    recorded_by          VARCHAR(36),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_stock_item (item_id),
    INDEX idx_stock_date (movement_date),
    INDEX idx_stock_type (movement_type),
    INDEX idx_stock_by (recorded_by),
    FOREIGN KEY (item_id) REFERENCES inventory_items(id) ON DELETE RESTRICT,
    FOREIGN KEY (recorded_by) REFERENCES employees(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE food_orders (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    customer_id          VARCHAR(36)     NOT NULL,
    booking_id           VARCHAR(36),
    status               VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    total_amount         DECIMAL(12,2)   NOT NULL DEFAULT 0,
    delivery_location    VARCHAR(255),
    ordered_at           DATETIME(6),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_fo_customer (customer_id),
    INDEX idx_fo_booking (booking_id),
    INDEX idx_fo_status (status),
    INDEX idx_fo_ordered (ordered_at),
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE food_order_items (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    food_order_id        VARCHAR(36)     NOT NULL,
    menu_item_id         VARCHAR(36)     NOT NULL,
    quantity             INT             NOT NULL,
    unit_price           DECIMAL(12,2)   NOT NULL,
    subtotal             DECIMAL(12,2)   NOT NULL,
    INDEX idx_foi_order (food_order_id),
    INDEX idx_foi_menuitem (menu_item_id),
    FOREIGN KEY (food_order_id) REFERENCES food_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Finance ──────────────────────────────────────────────────
CREATE TABLE invoices (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    booking_id           VARCHAR(36)     NOT NULL UNIQUE,
    invoice_number       VARCHAR(50)     NOT NULL UNIQUE,
    issued_at            DATETIME(6),
    due_date             DATE,
    subtotal             DECIMAL(12,2)   NOT NULL,
    tax_amount           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    total_amount         DECIMAL(12,2)   NOT NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    tax_record_id        VARCHAR(36),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_invoice_booking (booking_id),
    INDEX idx_invoice_number (invoice_number),
    INDEX idx_invoice_status (status),
    INDEX idx_invoice_issued (issued_at),
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    booking_id           VARCHAR(36)     NOT NULL UNIQUE,
    amount               DECIMAL(12,2)   NOT NULL,
    currency             VARCHAR(10)     NOT NULL DEFAULT 'RWF',
    method               VARCHAR(20)     NOT NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    transaction_reference VARCHAR(255),
    paid_at              DATETIME(6),
    notes                TEXT,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_payment_booking (booking_id),
    INDEX idx_payment_status (status),
    INDEX idx_payment_paid_at (paid_at),
    INDEX idx_payment_method (method),
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE expenses (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    category             VARCHAR(30)     NOT NULL,
    description          TEXT,
    amount               DECIMAL(12,2)   NOT NULL,
    expense_date         DATE            NOT NULL,
    approved_by          VARCHAR(36),
    receipt_reference    VARCHAR(255),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_expense_category (category),
    INDEX idx_expense_date (expense_date),
    INDEX idx_expense_approved (approved_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE financial_reports (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    report_type          VARCHAR(20)     NOT NULL,
    period               VARCHAR(20)     NOT NULL,
    total_revenue        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    total_expenses       DECIMAL(14,2)  NOT NULL DEFAULT 0,
    net_income           DECIMAL(14,2)  NOT NULL DEFAULT 0,
    generated_at         DATETIME(6),
    generated_by         VARCHAR(36),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_report_type (report_type),
    INDEX idx_report_period (period),
    INDEX idx_report_gen_by (generated_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE revenue_transactions (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    source_type          VARCHAR(30)     NOT NULL,
    source_id            VARCHAR(36),
    amount               DECIMAL(12,2)   NOT NULL,
    currency             VARCHAR(10)     DEFAULT 'RWF',
    transaction_date     DATE            NOT NULL,
    description          VARCHAR(500),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_rev_source_type (source_type),
    INDEX idx_rev_date (transaction_date),
    INDEX idx_rev_source_id (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Tax ──────────────────────────────────────────────────────
CREATE TABLE tax_records (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    period_year          INT             NOT NULL,
    period_month         INT             NOT NULL,
    tax_type             VARCHAR(20)     NOT NULL,
    taxable_amount       DECIMAL(14,2)  NOT NULL,
    tax_rate             DECIMAL(6,4)   NOT NULL,
    tax_due              DECIMAL(14,2)  NOT NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    filed_at             DATETIME(6),
    reference_note       VARCHAR(500),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_tax_period (period_year, period_month),
    INDEX idx_tax_type (tax_type),
    INDEX idx_tax_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tax_rule_configs (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    tax_type             VARCHAR(20)     NOT NULL,
    rate                 DECIMAL(6,4)   NOT NULL,
    description          VARCHAR(500),
    effective_from       DATE            NOT NULL,
    effective_to         DATE,
    is_active            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_tax_rule_type (tax_type),
    INDEX idx_tax_rule_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Staff ────────────────────────────────────────────────────
CREATE TABLE employees (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    user_id              VARCHAR(36),
    full_name            VARCHAR(255)    NOT NULL,
    national_id          VARCHAR(50),
    phone                VARCHAR(50),
    role_id              VARCHAR(36),
    department           VARCHAR(100),
    contract_type        VARCHAR(20)     NOT NULL,
    hire_date            DATE,
    base_salary          DECIMAL(12,2),
    bank_account         VARCHAR(100),
    is_active            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_emp_user (user_id),
    INDEX idx_emp_dept (department),
    INDEX idx_emp_contract (contract_type),
    INDEX idx_emp_national (national_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (role_id) REFERENCES employee_roles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE employee_roles (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    title                VARCHAR(100)    NOT NULL,
    description          TEXT,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_emp_role_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE employee_role_permissions (
    role_id              VARCHAR(36)     NOT NULL,
    permission           VARCHAR(255),
    INDEX idx_erp_role (role_id),
    FOREIGN KEY (role_id) REFERENCES employee_roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE work_schedules (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    employee_id          VARCHAR(36)     NOT NULL,
    week_start_date      DATE            NOT NULL,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_sched_emp (employee_id),
    INDEX idx_sched_week (employee_id, week_start_date),
    UNIQUE KEY uq_sched_emp_week (employee_id, week_start_date),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shifts (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    schedule_id          VARCHAR(36)     NOT NULL,
    day_of_week          VARCHAR(10)     NOT NULL,
    start_time           TIME            NOT NULL,
    end_time             TIME            NOT NULL,
    INDEX idx_shift_schedule (schedule_id),
    FOREIGN KEY (schedule_id) REFERENCES work_schedules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payroll_records (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    employee_id          VARCHAR(36)     NOT NULL,
    period_year          INT             NOT NULL,
    period_month         INT             NOT NULL,
    gross_salary         DECIMAL(12,2)   NOT NULL,
    deductions           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    net_salary           DECIMAL(12,2)   NOT NULL,
    tax_withheld         DECIMAL(12,2)   NOT NULL DEFAULT 0,
    status               VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    paid_at              DATETIME(6),
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL,
    INDEX idx_payroll_emp (employee_id),
    INDEX idx_payroll_period (period_year, period_month),
    INDEX idx_payroll_status (status),
    UNIQUE KEY uq_payroll_emp_period (employee_id, period_year, period_month),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Testimonials ─────────────────────────────────────────────
CREATE TABLE testimonials (
    id                  VARCHAR(36)     NOT NULL PRIMARY KEY,
    quote                TEXT,
    author_name          VARCHAR(255),
    author_role          VARCHAR(100),
    approved             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at           DATETIME(6)     NOT NULL,
    updated_at           DATETIME(6)     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
