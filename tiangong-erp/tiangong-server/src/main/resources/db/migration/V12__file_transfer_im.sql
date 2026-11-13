-- =====================================================================
-- V12：文件传输系统 + IM 消息系统
-- =====================================================================

-- 文件表（自研记录，含秒传 MD5；物理文件由 x-file-storage 管理）
CREATE TABLE IF NOT EXISTS erp_file (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  file_name     VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_key      VARCHAR(128) NOT NULL COMMENT '存储唯一键（x-file-storage url）',
  md5           VARCHAR(64)  NOT NULL COMMENT '文件 MD5（秒传标识）',
  file_size     BIGINT       NOT NULL DEFAULT 0 COMMENT '字节数',
  content_type  VARCHAR(128)          DEFAULT '',
  url           VARCHAR(500) NOT NULL COMMENT '访问 URL',
  biz_type      VARCHAR(32)           DEFAULT '' COMMENT '业务类型（订单/报销等）',
  biz_id        BIGINT       NULL,
  uploader_id   BIGINT       NOT NULL,
  uploader_name VARCHAR(64)           DEFAULT '',
  creator       VARCHAR(64)           DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater       VARCHAR(64)           DEFAULT '',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       BIT(1)       NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_md5_name (md5, file_name),
  KEY idx_uploader (uploader_id),
  KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '文件记录';

-- 分片上传会话（临时目录管理）
CREATE TABLE IF NOT EXISTS file_chunk_session (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  upload_id    VARCHAR(64)  NOT NULL COMMENT '上传会话 ID',
  file_name    VARCHAR(255) NOT NULL,
  md5          VARCHAR(64)  NOT NULL,
  total_chunks INT          NOT NULL,
  total_size   BIGINT       NOT NULL,
  received     VARCHAR(1000)         COMMENT '已收分片索引 JSON 数组',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_upload (upload_id),
  KEY idx_md5 (md5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '分片上传会话';

-- IM 消息（单聊）
CREATE TABLE IF NOT EXISTS im_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  session_id  VARCHAR(64)  NOT NULL COMMENT '会话 ID（两人 id 排序拼接 a_b）',
  sender_id   BIGINT       NOT NULL,
  receiver_id BIGINT       NOT NULL,
  content     VARCHAR(2000) NOT NULL,
  msg_type    VARCHAR(8)   NOT NULL DEFAULT 'text' COMMENT 'text/file/image',
  read_flag   BIT(1)       NOT NULL DEFAULT b'0',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session (session_id, create_time),
  KEY idx_receiver (receiver_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'IM 消息';

-- IM 会话（最后一次消息冗余，用于会话列表）
CREATE TABLE IF NOT EXISTS im_session (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  session_key  VARCHAR(64)  NOT NULL COMMENT '会话键（两人 id 排序拼接）',
  user_a       BIGINT       NOT NULL COMMENT '小 id 一方',
  user_b       BIGINT       NOT NULL COMMENT '大 id 一方',
  last_content VARCHAR(200)          DEFAULT '',
  last_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_key (session_key),
  KEY idx_a (user_a, last_time),
  KEY idx_b (user_b, last_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'IM 会话';

-- x-file-storage 的 FileDetail 记录表（中间件自动维护）
CREATE TABLE IF NOT EXISTS file_detail (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  platform      VARCHAR(32)           DEFAULT '',
  filename      VARCHAR(255)          DEFAULT '',
  original_filename VARCHAR(255)      DEFAULT '',
  path          VARCHAR(512)          DEFAULT '',
  size          BIGINT                DEFAULT 0,
  url           VARCHAR(500)          DEFAULT '',
  file_md5      VARCHAR(64)           DEFAULT '',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_md5 (file_md5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'x-file-storage 文件记录';

-- 文件中心 / IM 消息（顶级菜单）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, component) VALUES
  (15190, '文件中心', '', 2, 50, 0, 'file-center', 'more/file-center/index'),
  (15191, '文件上传', 'file:upload', 3, 1, 15190, '', ''),
  (15192, '文件下载', 'file:download', 3, 2, 15190, '', ''),
  (15193, '文件删除', 'file:delete', 3, 3, 15190, '', ''),
  (15194, '文件列表', 'file:list', 3, 4, 15190, '', ''),
  (15195, 'IM 消息', '', 2, 51, 0, 'im', 'im/index'),
  (15196, 'IM 查看', 'im:query', 3, 1, 15195, '', ''),
  (15197, 'IM 聊天', 'im:chat', 3, 2, 15195, '', '');
