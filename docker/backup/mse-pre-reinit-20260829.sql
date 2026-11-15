-- MySQL dump 10.13  Distrib 8.4.11, for Linux (x86_64)
--
-- Host: localhost    Database: mse
-- ------------------------------------------------------
-- Server version	8.4.11

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
-- Current Database: `mse`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `mse` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `mse`;

--
-- Table structure for table `demo_event`
--

DROP TABLE IF EXISTS `demo_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_event` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `event_type` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `payload` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='已消费租户事件（共享主库）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_event`
--

LOCK TABLES `demo_event` WRITE;
/*!40000 ALTER TABLE `demo_event` DISABLE KEYS */;
INSERT INTO `demo_event` VALUES (2092629315752243201,1,'ORDER_PLACED','{\"orderId\":2092629260186103809,\"productName\":\"事务+MQ全链路\"}',NULL,'2026-08-26 23:04:43',NULL,'2026-08-26 23:04:43',0),(2092629393871155201,2092610735790788610,'ORDER_PLACED','{\"orderId\":2092629393573359618,\"productName\":\"租户2正常单\"}',NULL,'2026-08-26 23:05:01',NULL,'2026-08-26 23:05:01',0),(2092646384954658818,1,'ORDER_PLACED','{\"orderId\":2092646384690417665,\"productName\":\"正常终验单L\"}',NULL,'2026-08-27 00:12:32',NULL,'2026-08-27 00:12:32',0),(2092646403065663489,2092610735790788610,'ORDER_PLACED','{\"orderId\":2092646402973388801,\"productName\":\"独立库正常单L\"}',NULL,'2026-08-27 00:12:37',NULL,'2026-08-27 00:12:37',0),(2092646877277933569,2092610735790788610,'ORDER_PLACED','{\"orderId\":2092646876988526593,\"productName\":\"独立库路由终验M\"}',NULL,'2026-08-27 00:14:30',NULL,'2026-08-27 00:14:30',0),(2092648519398244354,2092610735790788610,'ORDER_PLACED','{\"orderId\":2092648519117225986,\"productName\":\"diagN\"}',NULL,'2026-08-27 00:21:01',NULL,'2026-08-27 00:21:01',0),(2092649001529286657,2092610735790788610,'ORDER_PLACED','{\"orderId\":2092649001206325250,\"productName\":\"diagO\"}',NULL,'2026-08-27 00:22:56',NULL,'2026-08-27 00:22:56',0),(2092649325568651265,1,'ORDER_PLACED','{\"orderId\":2092649325182775297,\"productName\":\"M4终验-共享库\"}',NULL,'2026-08-27 00:24:13',NULL,'2026-08-27 00:24:13',0),(2092649343578992641,2092610735790788610,'ORDER_PLACED','{\"orderId\":2092649343411220481,\"productName\":\"M4终验-独立库\"}',NULL,'2026-08-27 00:24:18',NULL,'2026-08-27 00:24:18',0);
/*!40000 ALTER TABLE `demo_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_order`
--

DROP TABLE IF EXISTS `demo_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_order` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `order_no` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `product_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order_no` (`tenant_id`,`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例订单（@TenantDatasource 混合路由）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_order`
--

LOCK TABLES `demo_order` WRITE;
/*!40000 ALTER TABLE `demo_order` DISABLE KEYS */;
INSERT INTO `demo_order` VALUES (2092613191568363522,1,'ORDEF27A3DFD52E47E6','共享库订单A',99.50,NULL,2,'2026-08-26 22:00:38',2,'2026-08-26 22:00:38',0),(2092613192197509122,2092610735790788610,'ORDE7E5AE9C20E44B33','独立库订单B',200.00,NULL,2092610736122138625,'2026-08-26 22:00:39',2092610736122138625,'2026-08-26 22:00:39',0),(2092624464444067842,1,'ORDF218172FE5904BBD','微服务模式订单A',11.50,NULL,2,'2026-08-26 22:45:26',2,'2026-08-26 22:45:26',0),(2092624465660416002,2092610735790788610,'ORD2D5AA43481414DE8','微服务独立库订单D',22.00,NULL,2092610736122138625,'2026-08-26 22:45:26',2092610736122138625,'2026-08-26 22:45:26',0),(2092625006964731906,1,'ORD5EAB651CAE2D4C0A','微服务共享库订单E',31.00,NULL,2,'2026-08-26 22:47:35',2,'2026-08-26 22:47:35',0),(2092628522449924097,1,'ORD6979D7C11FD848FA','事务演示-正常',10.00,NULL,2,'2026-08-26 23:01:34',2,'2026-08-26 23:01:34',0),(2092628761764327425,1,'ORD8CADC4E5BF634FC6','事务演示-正常',10.00,NULL,2,'2026-08-26 23:02:31',2,'2026-08-26 23:02:31',0),(2092629032825417730,1,'ORDCBE224F3B7554ADE','MQ重试演示',10.00,NULL,2,'2026-08-26 23:03:35',2,'2026-08-26 23:03:35',0),(2092629260186103809,1,'ORD65FC1925E7694B4C','事务+MQ全链路',10.00,NULL,2,'2026-08-26 23:04:29',2,'2026-08-26 23:04:29',0),(2092646384690417665,1,'ORDFA80F5AED6704E66','正常终验单L',1.00,NULL,2,'2026-08-27 00:12:32',2,'2026-08-27 00:12:32',0),(2092646402973388801,2092610735790788610,'ORD0E1EF9E7A44444E0','独立库正常单L',5.00,NULL,2092610736122138625,'2026-08-27 00:12:37',2092610736122138625,'2026-08-27 00:12:37',0),(2092646876988526593,2092610735790788610,'ORD89F49D26AE9E42B6','独立库路由终验M',5.00,NULL,2092610736122138625,'2026-08-27 00:14:30',2092610736122138625,'2026-08-27 00:14:30',0),(2092648519117225986,2092610735790788610,'ORDF42E477654E0406E','diagN',1.00,NULL,2092610736122138625,'2026-08-27 00:21:01',2092610736122138625,'2026-08-27 00:21:01',0),(2092649001206325250,2092610735790788610,'ORDBAD6E1C634C04EDD','diagO',1.00,NULL,2092610736122138625,'2026-08-27 00:22:56',2092610736122138625,'2026-08-27 00:22:56',0),(2092649325182775297,1,'ORD99749695B82C4712','M4终验-共享库',1.00,NULL,2,'2026-08-27 00:24:13',2,'2026-08-27 00:24:13',0);
/*!40000 ALTER TABLE `demo_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_stock`
--

DROP TABLE IF EXISTS `demo_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_stock` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `remain` int NOT NULL DEFAULT '0' COMMENT '剩余可下单数量',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例库存（共享主库）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_stock`
--

LOCK TABLES `demo_stock` WRITE;
/*!40000 ALTER TABLE `demo_stock` DISABLE KEYS */;
INSERT INTO `demo_stock` VALUES (2092628520642179073,1,92,2,'2026-08-26 23:01:33',2,'2026-08-27 00:24:13',0),(2092629393153929218,2092610735790788610,87,2092610736122138625,'2026-08-26 23:05:01',2092610736122138625,'2026-08-27 00:24:21',0);
/*!40000 ALTER TABLE `demo_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT '0',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `type` tinyint NOT NULL COMMENT '1 目录 2 菜单 3 按钮',
  `permission` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
  `path` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由地址',
  `component` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `sort` int NOT NULL DEFAULT '0',
  `visible` tinyint NOT NULL DEFAULT '0' COMMENT '0 显示 1 隐藏',
  `status` tinyint NOT NULL DEFAULT '0',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,0,'系统管理',1,NULL,'/system',NULL,'Setting',10,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(100,1,'用户管理',2,'system:user:list','user','system/user/index','User',1,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(101,100,'用户新增',3,'system:user:create',NULL,NULL,NULL,1,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(102,100,'用户删除',3,'system:user:delete',NULL,NULL,NULL,2,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(110,1,'角色管理',2,'system:role:list','role','system/role/index','Peoples',2,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(111,110,'角色新增',3,'system:role:create',NULL,NULL,NULL,1,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(112,110,'角色删除',3,'system:role:delete',NULL,NULL,NULL,2,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(200,0,'平台管理',1,NULL,'/platform',NULL,'Monitor',20,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(210,200,'租户管理',2,'tenant:list','tenant','platform/tenant/index','OfficeBuilding',1,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(220,200,'套餐管理',2,'tenant-package:list','tenant-package','platform/tenant-package/index','Goods',2,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(230,200,'菜单管理',2,'system:menu:list','menu','platform/menu/index','Menu',3,0,0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0);
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `sort` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,1,'普通用户','common',1,0,'仅查看',NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0);
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,1,1,1,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(2,1,1,100,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(3,1,1,110,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_tenant`
--

DROP TABLE IF EXISTS `sys_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant` (
  `id` bigint NOT NULL COMMENT '租户编号',
  `code` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编码（全局唯一）',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户名',
  `package_id` bigint DEFAULT NULL COMMENT '套餐编号',
  `ds_key` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '独立库数据源 key（混合模式；NULL=共享库）',
  `contact_user_name` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_tenant`
--

LOCK TABLES `sys_tenant` WRITE;
/*!40000 ALTER TABLE `sys_tenant` DISABLE KEYS */;
INSERT INTO `sys_tenant` VALUES (1,'demo','示例租户',1,NULL,'张三',NULL,0,'2030-12-31 23:59:59','共享库租户',NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(2092610735790788610,'tenant2','独立库租户',NULL,'tenant2',NULL,NULL,0,NULL,NULL,1,'2026-08-26 21:50:53',1,'2026-08-26 22:00:55',0),(2092623968689876994,'tenant3','微服务新租户',NULL,NULL,NULL,NULL,0,NULL,NULL,1,'2026-08-26 22:43:28',1,'2026-08-26 22:43:28',0);
/*!40000 ALTER TABLE `sys_tenant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_tenant_package`
--

DROP TABLE IF EXISTS `sys_tenant_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant_package` (
  `id` bigint NOT NULL,
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐名',
  `menu_ids` varchar(2048) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '可用菜单编号集合（逗号分隔）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户套餐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_tenant_package`
--

LOCK TABLES `sys_tenant_package` WRITE;
/*!40000 ALTER TABLE `sys_tenant_package` DISABLE KEYS */;
INSERT INTO `sys_tenant_package` VALUES (1,'基础套餐','1,100,101,102,110','用户/角色管理',0,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0);
/*!40000 ALTER TABLE `sys_tenant_package` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `username` varchar(30) COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'BCrypt',
  `nickname` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `mobile` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `email` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `super_admin` tinyint NOT NULL DEFAULT '0' COMMENT '租户管理员标志',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_username` (`tenant_id`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,0,'platform','$2a$10$hvzCJfttG/KRR4rGXdYQOuQsx2jeFjI3uUX34jx3dhylud93VrHwG','平台管理员',NULL,NULL,NULL,0,1,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(2,1,'admin','$2a$10$OUQapwD84lrQG.44mZb.4.bd0rTlC.Tb6tn2HuRzIHJd0HbjyaCMq','租户管理员',NULL,NULL,NULL,0,1,NULL,'2026-08-26 21:50:06',NULL,'2026-08-26 21:50:06',0),(2092610736122138625,2092610735790788610,'boss','$2a$10$KPBTwweE0OnFMkiRELdZaOf8TOJQ9ZAwoe2Jjf8ZwPGuxgZ1WRXhK','boss',NULL,NULL,NULL,0,1,1,'2026-08-26 21:50:53',1,'2026-08-26 21:50:53',0),(2092623969176444929,2092623968689876994,'t3admin','$2a$10$yFNPDDLvNB3iMTMJ05qOK.OV4LDLqes06IV/wvUlv2qc9Cg4BSFvu','t3admin',NULL,NULL,NULL,0,1,1,'2026-08-26 22:43:28',1,'2026-08-26 22:43:28',0);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `undo_log`
--

DROP TABLE IF EXISTS `undo_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `undo_log` (
  `branch_id` bigint NOT NULL COMMENT '分支事务ID',
  `xid` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局事务ID',
  `context` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '上下文',
  `rollback_info` longblob NOT NULL COMMENT '回滚信息',
  `log_status` int NOT NULL COMMENT '0 正常 1 全局已完成',
  `log_created` datetime(6) NOT NULL COMMENT '创建时间',
  `log_modified` datetime(6) NOT NULL COMMENT '修改时间',
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Seata AT 模式回滚日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `undo_log`
--

LOCK TABLES `undo_log` WRITE;
/*!40000 ALTER TABLE `undo_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `undo_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'mse'
--

--
-- Current Database: `mse_tenant2`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `mse_tenant2` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `mse_tenant2`;

--
-- Table structure for table `demo_order`
--

DROP TABLE IF EXISTS `demo_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_order` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '0',
  `order_no` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `product_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order_no` (`tenant_id`,`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例订单（独立库租户）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_order`
--

LOCK TABLES `demo_order` WRITE;
/*!40000 ALTER TABLE `demo_order` DISABLE KEYS */;
INSERT INTO `demo_order` VALUES (2092613259893575682,2092610735790788610,'ORD7D2D7736F53943E0','独立库订单C',300.00,NULL,2092610736122138625,'2026-08-26 22:00:55',2092610736122138625,'2026-08-26 22:00:55',0),(2092625009623920641,2092610735790788610,'ORD54A285672DBB4BF5','微服务独立库订单F',32.00,NULL,2092610736122138625,'2026-08-26 22:47:36',2092610736122138625,'2026-08-26 22:47:36',0),(2092629393573359618,2092610735790788610,'ORD7B13DE7B05724039','租户2正常单',7.00,NULL,2092610736122138625,'2026-08-26 23:05:01',2092610736122138625,'2026-08-26 23:05:01',0),(2092629394223476738,2092610735790788610,'ORD065B7A9E1EEC4EB1','回滚演示单',10.00,NULL,2092610736122138625,'2026-08-26 23:05:01',2092610736122138625,'2026-08-26 23:05:01',0),(2092649343411220481,2092610735790788610,'ORDD909596DBCD841FC','M4终验-独立库',5.00,NULL,2092610736122138625,'2026-08-27 00:24:18',2092610736122138625,'2026-08-27 00:24:18',0),(2092649360737890305,2092610735790788610,'ORD72A64E02F1324519','M4终验-失败回滚',10.00,NULL,2092610736122138625,'2026-08-27 00:24:22',2092610736122138625,'2026-08-27 00:24:22',0);
/*!40000 ALTER TABLE `demo_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `undo_log`
--

DROP TABLE IF EXISTS `undo_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `undo_log` (
  `branch_id` bigint NOT NULL,
  `xid` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `context` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int NOT NULL,
  `log_created` datetime(6) NOT NULL,
  `log_modified` datetime(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Seata AT 模式回滚日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `undo_log`
--

LOCK TABLES `undo_log` WRITE;
/*!40000 ALTER TABLE `undo_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `undo_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'mse_tenant2'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-29 10:37:10
