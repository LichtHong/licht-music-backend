-- MySQL dump 10.13  Distrib 8.4.3, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: licht_music
-- ------------------------------------------------------
-- Server version	8.4.3

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `t_channel`
--

DROP TABLE IF EXISTS `t_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_channel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '频道名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_channel`
--

LOCK TABLES `t_channel` WRITE;
/*!40000 ALTER TABLE `t_channel` DISABLE KEYS */;
INSERT INTO `t_channel` VALUES (1,'游戏','2024-12-03 09:04:54','2024-12-03 09:04:54',_binary '\0');
/*!40000 ALTER TABLE `t_channel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_channel_topic_rel`
--

DROP TABLE IF EXISTS `t_channel_topic_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_channel_topic_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `channel_id` bigint unsigned NOT NULL COMMENT '频道ID',
  `topic_id` bigint unsigned NOT NULL COMMENT '话题ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道-话题关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_channel_topic_rel`
--

LOCK TABLES `t_channel_topic_rel` WRITE;
/*!40000 ALTER TABLE `t_channel_topic_rel` DISABLE KEYS */;
INSERT INTO `t_channel_topic_rel` VALUES (1,1,1,'2024-12-03 09:04:54','2024-12-03 09:04:54');
/*!40000 ALTER TABLE `t_channel_topic_rel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_fan`
--

DROP TABLE IF EXISTS `t_fan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_fan` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `fan_user_id` bigint unsigned NOT NULL COMMENT '粉丝的用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_id_fan_user_id` (`user_id`,`fan_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户粉丝表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_fan`
--

LOCK TABLES `t_fan` WRITE;
/*!40000 ALTER TABLE `t_fan` DISABLE KEYS */;
INSERT INTO `t_fan` VALUES (1,2,1,'2024-12-29 07:05:21');
/*!40000 ALTER TABLE `t_fan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_follower`
--

DROP TABLE IF EXISTS `t_follower`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_follower` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `follower_user_id` bigint unsigned NOT NULL COMMENT '关注的用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_id_follower_user_id` (`user_id`,`follower_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_follower`
--

LOCK TABLES `t_follower` WRITE;
/*!40000 ALTER TABLE `t_follower` DISABLE KEYS */;
INSERT INTO `t_follower` VALUES (1,1,2,'2024-12-29 07:05:21');
/*!40000 ALTER TABLE `t_follower` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_permission`
--

DROP TABLE IF EXISTS `t_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `type` tinyint unsigned NOT NULL COMMENT '类型(1：目录 2：菜单 3：按钮)',
  `menu_url` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单路由',
  `menu_icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单图标',
  `sort` int unsigned NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `permission_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限标识',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态(0：启用，1：禁用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除，1：已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_permission`
--

LOCK TABLES `t_permission` WRITE;
/*!40000 ALTER TABLE `t_permission` DISABLE KEYS */;
INSERT INTO `t_permission` VALUES (1,0,'发布帖子',3,'','',1,'app:post:publish',0,'2024-11-19 11:18:47','2024-11-19 11:18:47',_binary '\0'),(2,0,'发布评论',3,'','',2,'app:comment:publish',0,'2024-11-19 11:18:47','2024-11-19 11:18:47',_binary '\0');
/*!40000 ALTER TABLE `t_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_post`
--

DROP TABLE IF EXISTS `t_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_post` (
  `id` bigint unsigned NOT NULL COMMENT '主键ID',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `is_content_empty` bit(1) NOT NULL DEFAULT b'0' COMMENT '内容是否为空(0：不为空 1：空)',
  `creator_id` bigint unsigned NOT NULL COMMENT '发布者ID',
  `topic_id` bigint unsigned DEFAULT NULL COMMENT '话题ID',
  `topic_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '话题名称',
  `is_top` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶(0：未置顶 1：置顶)',
  `type` tinyint DEFAULT '0' COMMENT '类型(0：图文 1：视频)',
  `img_uri` varchar(660) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '帖子图片链接(逗号隔开)',
  `video_uri` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频链接',
  `content_uuid` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '帖子内容UUID',
  `visible` tinyint DEFAULT '0' COMMENT '可见范围(0：公开,所有人可见 1：仅对自己可见)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0：待审核 1：正常展示 2：被删除(逻辑删除) 3：被下架)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_topic_id` (`topic_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_post`
--

LOCK TABLES `t_post` WRITE;
/*!40000 ALTER TABLE `t_post` DISABLE KEYS */;
INSERT INTO `t_post` VALUES (1864092252448489473,'图文帖子测试标题',_binary '\0',1,1,'Sky·光遇',_binary '\0',0,'http://localhost:9000/licht-music/26809bb32ea04b74ad77876049304051.png',NULL,'6d1ce5c8-78f0-4883-83cd-bd6dd57ff9f6',0,'2024-12-04 07:40:01','2024-12-24 05:22:48',1),(1871284788057350157,'图文帖子测试标题',_binary '\0',1,1,'Sky·光遇',_binary '\0',0,'http://localhost:9000/licht-music/26809bb32ea04b74ad77876049304051.png',NULL,'39d2210b-91cc-4853-bc75-83c9699f31c6',0,'2024-12-24 04:00:37','2024-12-24 06:31:12',2);
/*!40000 ALTER TABLE `t_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_role`
--

DROP TABLE IF EXISTS `t_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名',
  `role_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色唯一标识',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0：启用，1：禁用)',
  `sort` int unsigned NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后一次更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除，1：已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_role`
--

LOCK TABLES `t_role` WRITE;
/*!40000 ALTER TABLE `t_role` DISABLE KEYS */;
INSERT INTO `t_role` VALUES (1,'普通用户','common_user',0,1,'','2024-11-19 11:18:47','2024-11-19 11:18:47',_binary '\0');
/*!40000 ALTER TABLE `t_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_role_permission_rel`
--

DROP TABLE IF EXISTS `t_role_permission_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_role_permission_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_role_permission_rel`
--

LOCK TABLES `t_role_permission_rel` WRITE;
/*!40000 ALTER TABLE `t_role_permission_rel` DISABLE KEYS */;
INSERT INTO `t_role_permission_rel` VALUES (1,1,1,'2024-11-19 11:18:47','2024-11-19 11:18:47',_binary '\0'),(2,1,2,'2024-11-19 11:18:47','2024-11-19 11:18:47',_binary '\0');
/*!40000 ALTER TABLE `t_role_permission_rel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_topic`
--

DROP TABLE IF EXISTS `t_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_topic` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '话题名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_topic`
--

LOCK TABLES `t_topic` WRITE;
/*!40000 ALTER TABLE `t_topic` DISABLE KEYS */;
INSERT INTO `t_topic` VALUES (1,'Sky·光遇','2024-12-03 09:04:54','2024-12-03 09:04:54',_binary '\0');
/*!40000 ALTER TABLE `t_topic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user`
--

DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `password` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码',
  `nickname` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '昵称',
  `avatar` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `background_img` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '背景图',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号',
  `sex` tinyint DEFAULT '0' COMMENT '性别(0：女 1：男)',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0：启用 1：禁用)',
  `introduction` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个人简介',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除，1：已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=1001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user`
--

LOCK TABLES `t_user` WRITE;
/*!40000 ALTER TABLE `t_user` DISABLE KEYS */;
INSERT INTO `t_user` VALUES (1,'licht','$2a$10$VMXycRKelJ33MniiGXvv8uZJnKLOmqIJPFJrmI3nATNnRYBSvkhvq','Licht','http://localhost:9000/licht-music/fa151c602ac2499b900a73f3234de909.jpg','2003-04-14','http://localhost:9000/licht-music/a65b2dfb5a134709a859fbba36be9626.png','15632338751',1,0,NULL,'2024-11-21 20:34:11','2024-12-24 03:50:15',_binary '\0'),(2,'id_10002',NULL,'Licht10002',NULL,NULL,NULL,'13178399626',NULL,0,NULL,'2024-11-22 15:32:14','2024-11-22 15:32:14',_binary '\0'),(3,'id_10003',NULL,'Licht10003',NULL,NULL,NULL,'15185158411',NULL,0,NULL,'2024-11-24 03:04:25','2024-11-24 03:04:25',_binary '\0'),(1000,'id_1000000',NULL,'Licht1000000',NULL,NULL,NULL,'13306070924',NULL,0,NULL,'2024-12-04 07:08:06','2024-12-04 07:08:06',_binary '\0');
/*!40000 ALTER TABLE `t_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_user_role_rel`
--

DROP TABLE IF EXISTS `t_user_role_rel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user_role_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_user_role_rel`
--

LOCK TABLES `t_user_role_rel` WRITE;
/*!40000 ALTER TABLE `t_user_role_rel` DISABLE KEYS */;
INSERT INTO `t_user_role_rel` VALUES (1,1,1,'2024-11-21 20:34:11','2024-11-21 20:34:11',_binary '\0'),(2,2,1,'2024-11-22 15:32:14','2024-11-22 15:32:14',_binary '\0'),(3,3,1,'2024-11-24 03:04:25','2024-11-24 03:04:25',_binary '\0'),(4,1000,1,'2024-12-04 07:08:06','2024-12-04 07:08:06',_binary '\0');
/*!40000 ALTER TABLE `t_user_role_rel` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-12-30  0:37:06
