CREATE TABLE IF NOT EXISTS product_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  model VARCHAR(128) NOT NULL,
  summary VARCHAR(1024) NOT NULL,
  parameters_json TEXT,
  image_url VARCHAR(1024),
  price DECIMAL(12,2) NOT NULL DEFAULT 0,
  carousel_images_json TEXT,
  detail_html TEXT,
  attachments_json TEXT,
  uploaded_by VARCHAR(64) NOT NULL DEFAULT '管理员',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS solution (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  industry VARCHAR(64) NOT NULL,
  summary VARCHAR(1024) NOT NULL,
  architecture TEXT,
  image_url VARCHAR(1024),
  recommended_product_ids_json TEXT,
  carousel_images_json TEXT,
  detail_html TEXT,
  attachments_json TEXT,
  uploaded_by VARCHAR(64) NOT NULL DEFAULT '管理员',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  location VARCHAR(128) NOT NULL,
  industry VARCHAR(64) NOT NULL,
  summary VARCHAR(1024) NOT NULL,
  image_url VARCHAR(1024),
  carousel_images_json TEXT,
  detail_html TEXT,
  attachments_json TEXT,
  uploaded_by VARCHAR(64) NOT NULL DEFAULT '管理员',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS personnel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL UNIQUE,
  open_id VARCHAR(128) NOT NULL UNIQUE,
  union_id VARCHAR(128),
  display_name VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
  phone VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT '启用',
  remark VARCHAR(1024),
  first_login_at VARCHAR(32),
  last_login_at VARCHAR(32),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inquiry (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contact_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  company VARCHAR(128) NOT NULL,
  project_name VARCHAR(128) NOT NULL,
  project_location VARCHAR(128) NOT NULL,
  requirement TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT '待分配',
  assigned_sales_id VARCHAR(64),
  assigned_sales_name VARCHAR(64),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quotation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  inquiry_id BIGINT,
  quote_no VARCHAR(80) NOT NULL UNIQUE,
  project_name VARCHAR(128) NOT NULL,
  customer_name VARCHAR(128) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  discount_rate DECIMAL(8,2) NOT NULL DEFAULT 100,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  owner_id VARCHAR(64) NOT NULL,
  owner_name VARCHAR(64) NOT NULL,
  sent_at VARCHAR(32),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quotation_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quote_id BIGINT NOT NULL,
  measurement VARCHAR(128) NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS config_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_name VARCHAR(128) NOT NULL,
  description VARCHAR(1024),
  measurements_json TEXT,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_model_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider VARCHAR(128) NOT NULL,
  provider_code VARCHAR(64) NOT NULL,
  adapter_type VARCHAR(64) NOT NULL,
  base_url VARCHAR(512),
  chat_model VARCHAR(128),
  embedding_model VARCHAR(128),
  api_key TEXT,
  key_status VARCHAR(32) NOT NULL DEFAULT '未配置',
  enabled TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(1024),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS file_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  original_name VARCHAR(256) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  bucket_name VARCHAR(128) NOT NULL DEFAULT 'local',
  file_size BIGINT NOT NULL DEFAULT 0,
  url VARCHAR(1024) NOT NULL,
  uploaded_by VARCHAR(64) NOT NULL DEFAULT '管理员',
  uploaded_at VARCHAR(32),
  module VARCHAR(64) NOT NULL DEFAULT 'common',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_document_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  source_type VARCHAR(64) NOT NULL DEFAULT 'RECORD',
  status VARCHAR(32) NOT NULL DEFAULT 'RECORDED',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_question_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question TEXT NOT NULL,
  answer TEXT,
  status VARCHAR(64) NOT NULL,
  provider VARCHAR(128),
  chat_model VARCHAR(128),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO product_category (id, name, sort_order) VALUES
  (1, '传感器', 10),
  (2, '采集仪', 20),
  (3, '通信', 30),
  (4, '安装附件', 40),
  (5, '供电系统', 50),
  (6, '安装耗材', 60);

INSERT INTO product (id, category_id, name, model, summary, parameters_json, image_url, price, carousel_images_json, detail_html, attachments_json, uploaded_by) VALUES
  (1, 1, '智能位移传感器', 'PS-DIS-300', '用于边坡、坝体和矿山结构位移连续监测。', '{"量程":"300mm","精度":"0.1mm","防护":"IP67"}', 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80', 6800.00, '["https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80"]', '<h2>智能位移传感器</h2><p>适用于工程监测现场长期运行，支持多点部署、远程采集和平台化管理。</p>', '[]', '管理员'),
  (2, 1, '翻斗式雨量传感器', 'PS-RAIN-01', '采集现场降雨数据，用于预警阈值判断和日报分析。', '{"分辨率":"0.2mm","输出":"RS485","防护":"IP65"}', 'https://dummyimage.com/960x540/edf7f1/16835b&text=PS-RAIN-01', 1800.00, '["https://dummyimage.com/960x540/edf7f1/16835b&text=PS-RAIN-01"]', '<h2>翻斗式雨量传感器</h2><p>适合野外雨量监测和多源数据联动预警。</p>', '[]', '管理员'),
  (3, 2, '多通道数据采集仪', 'PS-DAQ-16', '支持多测点接入、边缘缓存和远程配置。', '{"通道":"16","通信":"4G / Ethernet","供电":"DC12V"}', 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80', 12800.00, '["https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80"]', '<h2>多通道数据采集仪</h2><p>面向多测项监测站的数据采集和边缘处理。</p>', '[]', '管理员'),
  (4, 2, '边缘计算采集终端', 'PS-EDGE-8', '适合小型监测站的数据采集、本地缓存和边缘计算。', '{"通道":"8","存储":"32GB","协议":"MQTT / HTTP"}', 'https://dummyimage.com/960x540/f7f0ff/722ed1&text=PS-EDGE-8', 9800.00, '["https://dummyimage.com/960x540/f7f0ff/722ed1&text=PS-EDGE-8"]', '<h2>边缘计算采集终端</h2><p>适合分散点位和轻量化监测站。</p>', '[]', '管理员'),
  (5, 3, '工业 4G 通信网关', 'PS-GW-4G', '面向野外项目的数据回传和设备远程维护。', '{"网络":"4G Cat.4","接口":"RS485 / LAN","温度":"-20~70C"}', 'https://images.unsplash.com/photo-1597852074816-d933c7d2b988?auto=format&fit=crop&w=900&q=80', 3600.00, '["https://images.unsplash.com/photo-1597852074816-d933c7d2b988?auto=format&fit=crop&w=900&q=80"]', '<h2>工业 4G 通信网关</h2><p>支持野外监测站稳定联网和远程维护。</p>', '[]', '管理员'),
  (6, 3, 'LoRa 无线网桥', 'PS-LORA-BR', '用于山地和坝区短距离无线传输，降低布线成本。', '{"频段":"470MHz","距离":"3km","防护":"IP66"}', 'https://dummyimage.com/960x540/f6ffed/389e0d&text=PS-LORA', 2600.00, '["https://dummyimage.com/960x540/f6ffed/389e0d&text=PS-LORA"]', '<h2>LoRa 无线网桥</h2><p>适合复杂地形下的低功耗无线传输。</p>', '[]', '管理员'),
  (7, 4, '不锈钢安装支架', 'PS-BRACKET-S', '用于传感器、采集箱和天线的现场固定。', '{"材质":"304","高度":"1.2m","安装":"地基 / 墙面"}', 'https://dummyimage.com/960x540/fff7e6/d46b08&text=BRACKET', 520.00, '["https://dummyimage.com/960x540/fff7e6/d46b08&text=BRACKET"]', '<h2>不锈钢安装支架</h2><p>用于现场设备固定和标准化安装。</p>', '[]', '管理员'),
  (8, 4, '室外防护设备箱', 'PS-BOX-500', '保护采集仪、电源和通信设备，适合野外长期部署。', '{"尺寸":"500x400x220","防护":"IP65","材质":"冷轧钢"}', 'https://dummyimage.com/960x540/fff1f0/c41d7f&text=PS-BOX', 880.00, '["https://dummyimage.com/960x540/fff1f0/c41d7f&text=PS-BOX"]', '<h2>室外防护设备箱</h2><p>提升野外设备可靠性和维护便利性。</p>', '[]', '管理员'),
  (9, 5, '太阳能供电箱', 'PS-SOLAR-120', '适合无市电区域的监测站持续供电。', '{"功率":"120W","电池":"80Ah","防护":"IP65"}', 'https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=900&q=80', 4200.00, '["https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=900&q=80"]', '<h2>太阳能供电箱</h2><p>适合无人值守点位的独立供电。</p>', '[]', '管理员'),
  (10, 5, '备用锂电电源', 'PS-BAT-50', '用于阴雨天或市电中断时的监测站备用供电。', '{"容量":"50Ah","输出":"12V","寿命":"2000 cycles"}', 'https://dummyimage.com/960x540/f9f0ff/531dab&text=BATTERY', 2300.00, '["https://dummyimage.com/960x540/f9f0ff/531dab&text=BATTERY"]', '<h2>备用锂电电源</h2><p>作为监测站备用供电模块。</p>', '[]', '管理员'),
  (11, 6, '防水接头与线缆包', 'PS-CABLE-KIT', '用于传感器到采集仪的现场接线和防水处理。', '{"线缆":"100m","接头":"20pcs","防护":"IP67"}', 'https://dummyimage.com/960x540/f0f5ff/1d39c4&text=CABLE', 960.00, '["https://dummyimage.com/960x540/f0f5ff/1d39c4&text=CABLE"]', '<h2>防水接头与线缆包</h2><p>覆盖常见现场接线耗材。</p>', '[]', '管理员'),
  (12, 6, '基础安装耗材包', 'PS-INSTALL-PACK', '覆盖单个监测站的常用安装耗材和标识材料。', '{"含量":"螺栓 / 胶带 / 标签","适用":"1 station","类型":"通用"}', 'https://dummyimage.com/960x540/f5f5f5/595959&text=INSTALL', 680.00, '["https://dummyimage.com/960x540/f5f5f5/595959&text=INSTALL"]', '<h2>基础安装耗材包</h2><p>用于单站点标准化安装。</p>', '[]', '管理员');

INSERT INTO solution (id, title, industry, summary, architecture, image_url, recommended_product_ids_json, carousel_images_json, detail_html, attachments_json, uploaded_by) VALUES
  (1, '边坡自动化监测方案', '边坡监测', '位移、雨量、视频和采集网关组合，适合施工期与运营期监测。', '传感器层 -> 采集层 -> 通信层 -> 监测平台 -> 告警通知', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80', '[1,3,5,9]', '["https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80"]', '<h2>边坡自动化监测方案</h2><p>覆盖现场感知、数据采集、通信回传、平台分析和预警通知。</p>', '[]', '管理员'),
  (2, '尾矿库安全监测方案', '尾矿库监测', '围绕坝体位移、浸润线、库水位与视频巡检形成闭环。', '现场监测站 -> 工业网关 -> 云平台 -> 安全看板', 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80', '[1,3,9,11]', '["https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80"]', '<h2>尾矿库安全监测方案</h2><p>用于尾矿库风险识别和安全运营。</p>', '[]', '管理员');

INSERT INTO project_case (id, name, location, industry, summary, image_url, carousel_images_json, detail_html, attachments_json, uploaded_by) VALUES
  (1, '华北矿区边坡监测项目', '河北 唐山', '智慧矿山', '部署 48 个监测点，形成自动化预警和日报分析。', 'https://images.unsplash.com/photo-1473649085228-583485e6e4d7?auto=format&fit=crop&w=1200&q=80', '["https://images.unsplash.com/photo-1473649085228-583485e6e4d7?auto=format&fit=crop&w=1200&q=80"]', '<h2>华北矿区边坡监测项目</h2><p>实现监测数据自动采集、异常预警和日报输出。</p>', '[]', '管理员'),
  (2, '西南水库大坝安全监测', '云南 昆明', '水库大坝监测', '完成渗压、位移、雨量和视频联动监测。', 'https://images.unsplash.com/photo-1437482078695-73f5ca6c96e2?auto=format&fit=crop&w=1200&q=80', '["https://images.unsplash.com/photo-1437482078695-73f5ca6c96e2?auto=format&fit=crop&w=1200&q=80"]', '<h2>西南水库大坝安全监测</h2><p>提升大坝安全监测和巡检效率。</p>', '[]', '管理员');

INSERT INTO personnel (id, user_id, open_id, union_id, display_name, role, phone, status, remark, first_login_at, last_login_at) VALUES
  (1, 'customer-1', 'openid-customer-1', '', '普通客户', 'CUSTOMER', '13800000000', '启用', '首次登录自动建档的客户身份。', '2026-06-03 09:20', '2026-06-03 09:20'),
  (2, 'sales-1', 'openid-sales-1', '', '王销售', 'SALES', '13900000001', '启用', '后台授权为销售，只能查看自己的报价。', '2026-06-03 09:30', '2026-06-03 09:30'),
  (3, 'engineer-1', 'openid-engineer-1', '', '技术工程师', 'ENGINEER', '13900000002', '启用', '后台授权为技术工程师，可查看全部报价。', '2026-06-02 18:10', '2026-06-02 18:10'),
  (4, 'admin-1', 'openid-admin-1', '', '管理员', 'ADMIN', '13900000003', '启用', '系统管理员，可管理人员权限和询价分配。', '2026-06-01 08:40', '2026-06-01 08:40');

INSERT INTO inquiry (id, contact_name, phone, company, project_name, project_location, requirement, status, assigned_sales_id, assigned_sales_name, created_at) VALUES
  (1001, '张工', '13800001001', '华北矿业集团', '北山边坡自动化监测', '河北 唐山', '需要边坡位移、雨量和通信设备报价。', '待分配', '', '未分配', '2026-06-03 10:12:00'),
  (1002, '陈工', '13800001002', '西南水务', '水库大坝安全监测', '云南 昆明', '需要水库大坝安全监测设备方案。', '已分配', 'sales-1', '王销售', '2026-06-02 16:05:00');

INSERT INTO quotation (id, quote_no, project_name, customer_name, version, discount_rate, total_amount, status, owner_id, owner_name, created_at) VALUES
  (2001, 'Q20260603001', '北山边坡自动化监测', '华北矿业集团', 1, 95, 69008.00, 'DRAFT', 'sales-1', '王销售', '2026-06-03 10:20:00'),
  (2002, 'Q20260602003', '西南水库大坝监测', '西南水务', 1, 95, 128600.00, 'GENERATED', 'sales-1', '王销售', '2026-06-02 15:50:00');

INSERT INTO quotation_item (quote_id, measurement, product_name, quantity, unit_price, amount) VALUES
  (2001, '标准监测站 / 传感器', '智能位移传感器', 2, 6800.00, 13600.00),
  (2001, '标准监测站 / 采集仪', '多通道数据采集仪', 1, 12800.00, 12800.00),
  (2001, '标准监测站 / 通信', '工业 4G 通信网关', 1, 3600.00, 3600.00),
  (2002, '大坝监测站 / 采集仪', '多通道数据采集仪', 2, 12800.00, 25600.00);

INSERT INTO config_template (id, template_name, description, measurements_json) VALUES
  (1, '标准设备报价书', '适合小程序报价工具生成正式设备报价单。', '[{"name":"标准监测站","categoryName":"传感器","productName":"智能位移传感器","quantity":2,"unitPrice":6800}]'),
  (2, '边坡基础监测模板', '适合中小型边坡项目的基础配置。', '[{"name":"坡体位移","categoryName":"采集仪","productName":"多通道数据采集仪","quantity":1,"unitPrice":12800}]');

INSERT INTO ai_model_config (id, provider, provider_code, adapter_type, base_url, chat_model, embedding_model, api_key, key_status, enabled, remark) VALUES
  (1, 'DeepSeek', 'DeepSeek', 'OpenAI Compatible', 'https://api.deepseek.com', 'deepseek-v3', 'BAAI/bge-m3', '', '未配置', 0, '适合国内网络环境下做通用问答。'),
  (2, 'Gemini', 'Gemini', 'Native', 'https://generativelanguage.googleapis.com', 'gemini-2.5-flash', 'gemini-embedding-001', '', '未配置', 0, '适合知识库问答和向量化。'),
  (3, '阿里云百炼/Qwen', 'DashScope', 'OpenAI Compatible', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'qwen-plus', 'text-embedding-v4', '', '未配置', 0, '国内优先推荐，兼容 OpenAI 调用方式。'),
  (4, '百度千帆/ERNIE', 'Qianfan', 'OpenAI Compatible', 'https://qianfan.baidubce.com/v2', 'ernie-4.0-turbo-8k', 'bge-large-zh', '', '未配置', 0, '按千帆控制台实际开通模型填写。'),
  (5, '火山方舟/Doubao', 'VolcArk', 'OpenAI Compatible', 'https://ark.cn-beijing.volces.com/api/v3', 'doubao-seed-1-6', 'doubao-embedding', '', '未配置', 0, '适合接入豆包和方舟模型。'),
  (6, '腾讯混元', 'Hunyuan', 'OpenAI Compatible', 'https://api.hunyuan.cloud.tencent.com/v1', 'hunyuan-turbo', 'hunyuan-embedding', '', '未配置', 0, '如账号接口不是兼容模式，后续使用原生适配。'),
  (7, '智谱 GLM', 'Zhipu', 'OpenAI Compatible', 'https://open.bigmodel.cn/api/paas/v4', 'glm-4-flash', 'embedding-3', '', '未配置', 0, '适合 GLM 系列模型。'),
  (8, '月之暗面/Kimi', 'Moonshot', 'OpenAI Compatible', 'https://api.moonshot.cn/v1', 'moonshot-v1-8k', '', '', '未配置', 0, '适合长文本问答。'),
  (9, 'MiniMax', 'MiniMax', 'OpenAI Compatible', 'https://api.minimax.chat/v1', 'MiniMax-Text-01', '', '', '未配置', 0, '按 MiniMax 控制台实际模型名称调整。'),
  (10, '讯飞星火', 'Spark', 'Native', 'https://spark-api-open.xf-yun.com', 'generalv3.5', '', '', '未配置', 0, '预留原生适配入口。'),
  (11, '硅基流动', 'SiliconFlow', 'OpenAI Compatible', 'https://api.siliconflow.cn/v1', 'Qwen/Qwen2.5-72B-Instruct', 'BAAI/bge-m3', '', '未配置', 0, '适合统一接入多个国产模型。'),
  (12, 'OpenAI Compatible', 'Custom', 'OpenAI Compatible', '', '', '', '', '未配置', 0, '自定义兼容接口。');

INSERT INTO ai_document_record (id, title, source_type, status) VALUES
  (1, '智能位移传感器技术说明', 'RECORD', 'RECORDED'),
  (2, '边坡自动化监测方案白皮书', 'RECORD', 'RECORDED'),
  (3, '尾矿库安全监测常见问答', 'RECORD', 'RECORDED');
