CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  open_id VARCHAR(128) NULL,
  username VARCHAR(64) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  model VARCHAR(128) NOT NULL,
  image_url VARCHAR(512) NULL,
  summary VARCHAR(512) NOT NULL,
  parameters_json JSON NULL,
  attachment_id BIGINT NULL,
  unit VARCHAR(32) NOT NULL DEFAULT '套',
  standard_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product_relation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  main_product_id BIGINT NOT NULL,
  related_product_id BIGINT NOT NULL,
  recommended_quantity INT NOT NULL DEFAULT 1,
  recommendation_note VARCHAR(512) NULL,
  default_selected TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS solution (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  industry VARCHAR(64) NOT NULL,
  summary VARCHAR(512) NOT NULL,
  architecture TEXT NULL,
  image_url VARCHAR(512) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  location VARCHAR(128) NOT NULL,
  industry VARCHAR(64) NOT NULL,
  summary VARCHAR(512) NOT NULL,
  image_url VARCHAR(512) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS inquiry (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contact_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  company VARCHAR(128) NOT NULL,
  project_name VARCHAR(128) NOT NULL,
  project_location VARCHAR(128) NOT NULL,
  requirement TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NEW',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quotation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  inquiry_id BIGINT NULL,
  quote_no VARCHAR(64) NOT NULL UNIQUE,
  project_name VARCHAR(128) NOT NULL,
  customer_name VARCHAR(128) NULL,
  version INT NOT NULL DEFAULT 1,
  discount_rate DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS quotation_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quotation_id BIGINT NOT NULL,
  measurement VARCHAR(128) NOT NULL,
  category_name VARCHAR(64) NOT NULL,
  product_id BIGINT NULL,
  product_name VARCHAR(128) NOT NULL,
  model VARCHAR(128) NULL,
  unit VARCHAR(32) NOT NULL DEFAULT '套',
  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  skipped TINYINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS config_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_name VARCHAR(128) NOT NULL,
  description VARCHAR(512) NULL,
  measurement_json JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS file_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  original_name VARCHAR(256) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  bucket_name VARCHAR(128) NOT NULL DEFAULT 'product-show',
  content_type VARCHAR(128) NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS qr_code_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(32) NOT NULL,
  business_id BIGINT NOT NULL,
  scene VARCHAR(256) NOT NULL,
  file_id BIGINT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS browse_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(32) NOT NULL,
  business_id BIGINT NOT NULL,
  visitor_id VARCHAR(128) NULL,
  user_agent VARCHAR(256) NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS message_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recipient VARCHAR(128) NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  payload_json JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_document_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  file_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'RECORDED',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_question_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  question TEXT NOT NULL,
  answer TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PLACEHOLDER',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

INSERT INTO role (id, role_code, role_name) VALUES
  (1, 'VISITOR', '游客客户'),
  (2, 'SALES', '销售人员'),
  (3, 'ENGINEER', '技术工程师'),
  (4, 'ADMIN', '管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO app_user (id, username, display_name, role_code) VALUES
  (1, 'admin', '系统管理员', 'ADMIN'),
  (2, 'sales-demo', '销售演示账号', 'SALES'),
  (3, 'engineer-demo', '技术演示账号', 'ENGINEER')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), role_code = VALUES(role_code);

INSERT INTO product_category (id, name, sort_order) VALUES
  (1, '传感器', 10),
  (2, '采集仪', 20),
  (3, '通信', 30),
  (4, '安装附件', 40),
  (5, '供电系统', 50),
  (6, '安装耗材', 60)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order);

INSERT INTO product (id, category_id, name, model, summary, parameters_json, image_url, unit, standard_price) VALUES
  (1, 1, '智能位移传感器', 'PS-DIS-300', '用于边坡、坝体和矿山结构位移连续监测。', JSON_OBJECT('量程','300mm','精度','0.1mm','防护','IP67'), 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80', '套', 2800.00),
  (2, 2, '多通道数据采集仪', 'PS-DAQ-16', '支持多测点接入、边缘缓存和远程配置。', JSON_OBJECT('通道','16','通信','4G/以太网','供电','DC12V'), 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', '台', 8600.00),
  (3, 3, '工业 4G 通信网关', 'PS-GW-4G', '面向野外项目的数据回传和设备远程维护。', JSON_OBJECT('网络','4G Cat.4','协议','MQTT/HTTP','温度','-20~70C'), 'https://images.unsplash.com/photo-1597852074816-d933c7d2b988?auto=format&fit=crop&w=900&q=80', '台', 1900.00),
  (4, 5, '太阳能供电箱', 'PS-SOLAR-120', '适合无市电区域的监测站持续供电。', JSON_OBJECT('功率','120W','电池','80Ah','防护','IP65'), 'https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=900&q=80', '套', 5200.00)
ON DUPLICATE KEY UPDATE name = VALUES(name), model = VALUES(model), summary = VALUES(summary), standard_price = VALUES(standard_price);

INSERT INTO product_relation (id, main_product_id, related_product_id, recommended_quantity, recommendation_note, default_selected) VALUES
  (1, 1, 2, 1, '位移传感器需要采集仪接入测点数据。', 1),
  (2, 1, 3, 1, '野外项目建议配置通信网关。', 1),
  (3, 2, 4, 1, '无市电点位建议配置太阳能供电箱。', 0)
ON DUPLICATE KEY UPDATE recommendation_note = VALUES(recommendation_note), default_selected = VALUES(default_selected);

INSERT INTO solution (id, title, industry, summary, architecture, image_url) VALUES
  (1, '边坡自动化监测方案', '边坡监测', '位移、雨量、视频和采集网关组合，适合施工期与运营期监测。', '传感器层 -> 采集层 -> 通信层 -> 监测平台 -> 告警通知', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80'),
  (2, '尾矿库安全监测方案', '尾矿库监测', '围绕坝体位移、浸润线、库水位与视频巡检形成闭环。', '现场监测站 -> 工业网关 -> 云平台 -> 安全看板', 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80'),
  (3, '水库大坝监测方案', '水库大坝监测', '支持渗压、扬压力、变形和环境量多源数据采集。', '监测仪器 -> 数据采集 -> 专线/4G -> 数据中心', 'https://images.unsplash.com/photo-1437482078695-73f5ca6c96e2?auto=format&fit=crop&w=1200&q=80')
ON DUPLICATE KEY UPDATE title = VALUES(title), summary = VALUES(summary);

INSERT INTO project_case (id, name, location, industry, summary, image_url) VALUES
  (1, '华北矿区边坡监测项目', '河北 唐山', '智慧矿山', '部署 48 个监测点，形成自动化预警和日报分析。', 'https://images.unsplash.com/photo-1473649085228-583485e6e4d7?auto=format&fit=crop&w=1200&q=80'),
  (2, '西南水库大坝安全监测', '云南 昆明', '水库大坝监测', '完成渗压、位移、雨量和视频联动监测。', 'https://images.unsplash.com/photo-1437482078695-73f5ca6c96e2?auto=format&fit=crop&w=1200&q=80'),
  (3, '山区地质灾害预警示范点', '四川 雅安', '地质灾害监测', '实现雨量阈值和位移趋势联合预警。', 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80')
ON DUPLICATE KEY UPDATE name = VALUES(name), summary = VALUES(summary);

INSERT INTO config_template (id, template_name, description, measurement_json) VALUES
  (1, '边坡基础监测模板', '适合中小型边坡项目的基础配置。', JSON_ARRAY(JSON_OBJECT('测项','坡体位移','产品','智能位移传感器','数量',8,'单位','套','备注','按测点数量调整'))),
  (2, '尾矿库安全监测模板', '包含位移、采集、通信和供电建议。', JSON_ARRAY(JSON_OBJECT('测项','坝体位移','产品','智能位移传感器','数量',12,'单位','套','备注','重点坝段加密')))
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name), description = VALUES(description);

INSERT INTO ai_document_record (id, title, status) VALUES
  (1, '智能位移传感器技术说明', 'RECORDED'),
  (2, '边坡自动化监测方案白皮书', 'RECORDED'),
  (3, '尾矿库安全监测常见问答', 'RECORDED')
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status);
