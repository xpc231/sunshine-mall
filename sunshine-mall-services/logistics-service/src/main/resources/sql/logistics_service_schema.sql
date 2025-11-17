CREATE TABLE IF NOT EXISTS logistics_shipment (
  id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag TINYINT(1) DEFAULT 0,
  shipment_no VARCHAR(32) NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  carrier_code VARCHAR(32) NOT NULL,
  carrier_name VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  tracking_code VARCHAR(64) DEFAULT NULL,
  receiver_name VARCHAR(64) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL,
  receiver_address VARCHAR(255) NOT NULL,
  sender_address VARCHAR(255) NOT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shipment_no (shipment_no),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
);

CREATE TABLE IF NOT EXISTS logistics_event (
  id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag TINYINT(1) DEFAULT 0,
  shipment_no VARCHAR(32) NOT NULL,
  status VARCHAR(20) DEFAULT NULL,
  event_time DATETIME DEFAULT NULL,
  location VARCHAR(128) DEFAULT NULL,
  message VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_shipment_no (shipment_no),
  KEY idx_event_time (event_time)
);