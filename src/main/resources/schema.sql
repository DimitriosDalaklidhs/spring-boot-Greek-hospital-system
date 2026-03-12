-- ============================================================
-- Greek Hospital Management System — Database Schema
-- Reconstructed from DAO source files
-- Encoding: UTF-8 (Greek column/table names)
-- ============================================================

CREATE DATABASE IF NOT EXISTS hospital_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hospital_db;

-- ------------------------------------------------------------
-- Patients (ασθενεισ)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ασθενεισ (
    ΚΩΔ_ΑΣΘΕΝΗ     INT          NOT NULL AUTO_INCREMENT,
    ΑΜΚΑ           VARCHAR(11)  NOT NULL UNIQUE,
    ΟΝΟΜΑ          VARCHAR(100) NOT NULL,
    ΕΠΩΝΥΜΟ        VARCHAR(100) NOT NULL,
    ΗΜ_ΓΕΝΝΗΣΗΣ   DATE         NOT NULL,
    ΦΥΛΟ           TINYINT(1)   NOT NULL COMMENT '0=Male, 1=Female',
    PRIMARY KEY (ΚΩΔ_ΑΣΘΕΝΗ)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Hospitals (νοσοκομεια)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS νοσοκομεια (
    ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ      INT          NOT NULL AUTO_INCREMENT,
    ΟΝΟΜΑΣΙΑ_ΝΟΣΟΚΟΜΕΙΟΥ VARCHAR(200) NOT NULL,
    PRIMARY KEY (ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Medical Tests catalogue (ιατρικεσ_εξετασεισ)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ιατρικεσ_εξετασεισ (
    ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ      INT            NOT NULL AUTO_INCREMENT,
    ΟΝΟΜΑΣΙΑ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ VARCHAR(200)   NOT NULL,
    ΚΟΣΤΟΣ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ   DECIMAL(10,2)  NOT NULL,
    PRIMARY KEY (ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Hospitalizations (νοσηλειεσ_ασθενων)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS νοσηλειεσ_ασθενων (
    ΑΑ_ΝΟΣΗΛΕΙΑΣ    INT  NOT NULL AUTO_INCREMENT,
    ΚΩΔ_ΑΣΘΕΝΗ      INT  NOT NULL,
    ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ INT  NOT NULL,
    ΗΜ_ΕΙΣΟΔΟΥ      DATE NOT NULL,
    ΗΜ_ΕΞΟΔΟΥ       DATE DEFAULT NULL,
    PRIMARY KEY (ΑΑ_ΝΟΣΗΛΕΙΑΣ),
    FOREIGN KEY (ΚΩΔ_ΑΣΘΕΝΗ)      REFERENCES ασθενεισ(ΚΩΔ_ΑΣΘΕΝΗ),
    FOREIGN KEY (ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ) REFERENCES νοσοκομεια(ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Patient medical test records (ιατρικεσ_εξετασεισ_ασθενων)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ιατρικεσ_εξετασεισ_ασθενων (
    ΑΑ                      INT  NOT NULL AUTO_INCREMENT,
    ΚΩΔ_ΑΣΘΕΝΗ              INT  NOT NULL,
    ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ         INT  NOT NULL,
    ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ       INT  NOT NULL,
    ΗΜ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ        DATE NOT NULL,
    PRIMARY KEY (ΑΑ),
    FOREIGN KEY (ΚΩΔ_ΑΣΘΕΝΗ)          REFERENCES ασθενεισ(ΚΩΔ_ΑΣΘΕΝΗ),
    FOREIGN KEY (ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ)     REFERENCES νοσοκομεια(ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ),
    FOREIGN KEY (ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ)  REFERENCES ιατρικεσ_εξετασεισ(ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Auth users (for JWT login)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id       INT          NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL COMMENT 'DOCTOR, CLERK, ADMIN',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Seed data — one hospital + one test so the app is usable immediately
-- ------------------------------------------------------------
INSERT IGNORE INTO νοσοκομεια (ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ, ΟΝΟΜΑΣΙΑ_ΝΟΣΟΚΟΜΕΙΟΥ)
VALUES (1, 'Γενικό Νοσοκομείο Αθηνών');

INSERT IGNORE INTO ιατρικεσ_εξετασεισ (ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ, ΟΝΟΜΑΣΙΑ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ, ΚΟΣΤΟΣ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ)
VALUES (1, 'Γενική Αίματος', 15.00),
       (2, 'Ακτινογραφία', 40.00),
       (3, 'MRI', 200.00);

-- Default ADMIN user (password: admin123 — change before production!)
-- BCrypt hash of "admin123"
INSERT IGNORE INTO users (username, password, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ADMIN');