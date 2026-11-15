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

/*!40000 DROP DATABASE IF EXISTS `mse`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `mse` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `mse`;

--
-- Table structure for table `ACT_EVT_LOG`
--

DROP TABLE IF EXISTS `ACT_EVT_LOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_EVT_LOG` (
  `LOG_NR_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` longblob,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IS_PROCESSED_` tinyint DEFAULT '0',
  PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_EVT_LOG`
--

LOCK TABLES `ACT_EVT_LOG` WRITE;
/*!40000 ALTER TABLE `ACT_EVT_LOG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_EVT_LOG` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_GE_BYTEARRAY`
--

DROP TABLE IF EXISTS `ACT_GE_BYTEARRAY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_GE_BYTEARRAY` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_BYTEAR_DEPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_GE_BYTEARRAY`
--

LOCK TABLES `ACT_GE_BYTEARRAY` WRITE;
/*!40000 ALTER TABLE `ACT_GE_BYTEARRAY` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_GE_BYTEARRAY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_GE_PROPERTY`
--

DROP TABLE IF EXISTS `ACT_GE_PROPERTY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_GE_PROPERTY` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_GE_PROPERTY`
--

LOCK TABLES `ACT_GE_PROPERTY` WRITE;
/*!40000 ALTER TABLE `ACT_GE_PROPERTY` DISABLE KEYS */;
INSERT INTO `ACT_GE_PROPERTY` VALUES ('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('common.schema.version','8.0.0.0',1),('eventregistry.schema.version','8.0.0.0',1),('next.dbid','1',1),('schema.history','create(8.0.0.0)',1),('schema.version','8.0.0.0',1);
/*!40000 ALTER TABLE `ACT_GE_PROPERTY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ACTINST`
--

DROP TABLE IF EXISTS `ACT_HI_ACTINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ACTINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ACTINST`
--

LOCK TABLES `ACT_HI_ACTINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ACTINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ACTINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ATTACHMENT`
--

DROP TABLE IF EXISTS `ACT_HI_ATTACHMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ATTACHMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `URL_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ATTACHMENT`
--

LOCK TABLES `ACT_HI_ATTACHMENT` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ATTACHMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ATTACHMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_COMMENT`
--

DROP TABLE IF EXISTS `ACT_HI_COMMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_COMMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_COMMENT`
--

LOCK TABLES `ACT_HI_COMMENT` WRITE;
/*!40000 ALTER TABLE `ACT_HI_COMMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_COMMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_DETAIL`
--

DROP TABLE IF EXISTS `ACT_HI_DETAIL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_DETAIL` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_DETAIL`
--

LOCK TABLES `ACT_HI_DETAIL` WRITE;
/*!40000 ALTER TABLE `ACT_HI_DETAIL` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_DETAIL` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_HI_ENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ENTITYLINK`
--

LOCK TABLES `ACT_HI_ENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_IDENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_HI_IDENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_IDENTITYLINK`
--

LOCK TABLES `ACT_HI_IDENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_HI_IDENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_IDENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_PROCINST`
--

DROP TABLE IF EXISTS `ACT_HI_PROCINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_PROCINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_PROCINST`
--

LOCK TABLES `ACT_HI_PROCINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_PROCINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_PROCINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_TASKINST`
--

DROP TABLE IF EXISTS `ACT_HI_TASKINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_TASKINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_TASKINST`
--

LOCK TABLES `ACT_HI_TASKINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_TASKINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_TASKINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_TSK_LOG`
--

DROP TABLE IF EXISTS `ACT_HI_TSK_LOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_TSK_LOG` (
  `ID_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ACT_HI_TSK_LOG_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_TSK_LOG`
--

LOCK TABLES `ACT_HI_TSK_LOG` WRITE;
/*!40000 ALTER TABLE `ACT_HI_TSK_LOG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_TSK_LOG` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_VARINST`
--

DROP TABLE IF EXISTS `ACT_HI_VARINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_VARINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(100) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_VARINST`
--

LOCK TABLES `ACT_HI_VARINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_VARINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_VARINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_BYTEARRAY`
--

DROP TABLE IF EXISTS `ACT_ID_BYTEARRAY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_BYTEARRAY` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_BYTEARRAY`
--

LOCK TABLES `ACT_ID_BYTEARRAY` WRITE;
/*!40000 ALTER TABLE `ACT_ID_BYTEARRAY` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_BYTEARRAY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_GROUP`
--

DROP TABLE IF EXISTS `ACT_ID_GROUP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_GROUP` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_GROUP`
--

LOCK TABLES `ACT_ID_GROUP` WRITE;
/*!40000 ALTER TABLE `ACT_ID_GROUP` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_GROUP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_INFO`
--

DROP TABLE IF EXISTS `ACT_ID_INFO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_INFO` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PASSWORD_` longblob,
  `PARENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_INFO`
--

LOCK TABLES `ACT_ID_INFO` WRITE;
/*!40000 ALTER TABLE `ACT_ID_INFO` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_INFO` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_MEMBERSHIP`
--

DROP TABLE IF EXISTS `ACT_ID_MEMBERSHIP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_MEMBERSHIP` (
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
  KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `ACT_ID_GROUP` (`ID_`),
  CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `ACT_ID_USER` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_MEMBERSHIP`
--

LOCK TABLES `ACT_ID_MEMBERSHIP` WRITE;
/*!40000 ALTER TABLE `ACT_ID_MEMBERSHIP` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_MEMBERSHIP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PRIV`
--

DROP TABLE IF EXISTS `ACT_ID_PRIV`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PRIV` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PRIV`
--

LOCK TABLES `ACT_ID_PRIV` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PRIV` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_PRIV` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PRIV_MAPPING`
--

DROP TABLE IF EXISTS `ACT_ID_PRIV_MAPPING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PRIV_MAPPING` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PRIV_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`),
  KEY `ACT_IDX_PRIV_USER` (`USER_ID_`),
  KEY `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `ACT_ID_PRIV` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PRIV_MAPPING`
--

LOCK TABLES `ACT_ID_PRIV_MAPPING` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PRIV_MAPPING` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_PRIV_MAPPING` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PROPERTY`
--

DROP TABLE IF EXISTS `ACT_ID_PROPERTY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PROPERTY` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PROPERTY`
--

LOCK TABLES `ACT_ID_PROPERTY` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PROPERTY` DISABLE KEYS */;
INSERT INTO `ACT_ID_PROPERTY` VALUES ('schema.version','8.0.0.0',1);
/*!40000 ALTER TABLE `ACT_ID_PROPERTY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_TOKEN`
--

DROP TABLE IF EXISTS `ACT_ID_TOKEN`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_TOKEN` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TOKEN_VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATE_` timestamp(3) NULL DEFAULT NULL,
  `IP_ADDRESS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_AGENT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATA_` varchar(2000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_TOKEN`
--

LOCK TABLES `ACT_ID_TOKEN` WRITE;
/*!40000 ALTER TABLE `ACT_ID_TOKEN` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_TOKEN` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_USER`
--

DROP TABLE IF EXISTS `ACT_ID_USER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_USER` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `FIRST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LAST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DISPLAY_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EMAIL_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PWD_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PICTURE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_USER`
--

LOCK TABLES `ACT_ID_USER` WRITE;
/*!40000 ALTER TABLE `ACT_ID_USER` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_USER` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_PROCDEF_INFO`
--

DROP TABLE IF EXISTS `ACT_PROCDEF_INFO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_PROCDEF_INFO` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
  CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_PROCDEF_INFO`
--

LOCK TABLES `ACT_PROCDEF_INFO` WRITE;
/*!40000 ALTER TABLE `ACT_PROCDEF_INFO` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_PROCDEF_INFO` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_DEPLOYMENT`
--

DROP TABLE IF EXISTS `ACT_RE_DEPLOYMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_DEPLOYMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_DEPLOYMENT`
--

LOCK TABLES `ACT_RE_DEPLOYMENT` WRITE;
/*!40000 ALTER TABLE `ACT_RE_DEPLOYMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_DEPLOYMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_MODEL`
--

DROP TABLE IF EXISTS `ACT_RE_MODEL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_MODEL` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_MODEL`
--

LOCK TABLES `ACT_RE_MODEL` WRITE;
/*!40000 ALTER TABLE `ACT_RE_MODEL` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_MODEL` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_PROCDEF`
--

DROP TABLE IF EXISTS `ACT_RE_PROCDEF`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_PROCDEF` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_PROCDEF`
--

LOCK TABLES `ACT_RE_PROCDEF` WRITE;
/*!40000 ALTER TABLE `ACT_RE_PROCDEF` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_PROCDEF` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_ACTINST`
--

DROP TABLE IF EXISTS `ACT_RU_ACTINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_ACTINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_ACTINST`
--

LOCK TABLES `ACT_RU_ACTINST` WRITE;
/*!40000 ALTER TABLE `ACT_RU_ACTINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_ACTINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_DEADLETTER_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_DEADLETTER_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_DEADLETTER_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_DEADLETTER_JOB`
--

LOCK TABLES `ACT_RU_DEADLETTER_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_DEADLETTER_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_DEADLETTER_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_ENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_RU_ENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_ENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_ENTITYLINK`
--

LOCK TABLES `ACT_RU_ENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_ENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_ENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EVENT_SUBSCR`
--

DROP TABLE IF EXISTS `ACT_RU_EVENT_SUBSCR`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EVENT_SUBSCR` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EVENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
  KEY `ACT_IDX_EVENT_SUBSCR_EXEC_ID` (`EXECUTION_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_PROC_ID` (`PROC_INST_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EVENT_SUBSCR`
--

LOCK TABLES `ACT_RU_EVENT_SUBSCR` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EVENT_SUBSCR` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EVENT_SUBSCR` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EXECUTION`
--

DROP TABLE IF EXISTS `ACT_RU_EXECUTION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EXECUTION` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint DEFAULT NULL,
  `IS_CONCURRENT_` tinyint DEFAULT NULL,
  `IS_SCOPE_` tinyint DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint DEFAULT NULL,
  `IS_MI_ROOT_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `CACHED_ENT_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `EVT_SUBSCR_COUNT_` int DEFAULT NULL,
  `TASK_COUNT_` int DEFAULT NULL,
  `JOB_COUNT_` int DEFAULT NULL,
  `TIMER_JOB_COUNT_` int DEFAULT NULL,
  `SUSP_JOB_COUNT_` int DEFAULT NULL,
  `DEADLETTER_JOB_COUNT_` int DEFAULT NULL,
  `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
  KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE,
  CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EXECUTION`
--

LOCK TABLES `ACT_RU_EXECUTION` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EXECUTION` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EXECUTION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EXTERNAL_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_EXTERNAL_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EXTERNAL_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EXTERNAL_JOB`
--

LOCK TABLES `ACT_RU_EXTERNAL_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EXTERNAL_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EXTERNAL_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_HISTORY_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_HISTORY_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_HISTORY_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_HISTORY_JOB`
--

LOCK TABLES `ACT_RU_HISTORY_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_HISTORY_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_HISTORY_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_IDENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_RU_IDENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `ACT_RU_TASK` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_IDENTITYLINK`
--

LOCK TABLES `ACT_RU_IDENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_IDENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_IDENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_JOB`
--

LOCK TABLES `ACT_RU_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_SUSPENDED_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_SUSPENDED_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_SUSPENDED_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_SUSPENDED_JOB`
--

LOCK TABLES `ACT_RU_SUSPENDED_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_SUSPENDED_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_SUSPENDED_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_TASK`
--

DROP TABLE IF EXISTS `ACT_RU_TASK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_TASK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `SUB_TASK_COUNT_` int DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
  KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_TASK`
--

LOCK TABLES `ACT_RU_TASK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_TASK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_TASK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_TIMER_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_TIMER_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_TIMER_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
  KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_TIMER_JOB`
--

LOCK TABLES `ACT_RU_TIMER_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_TIMER_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_TIMER_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_VARIABLE`
--

DROP TABLE IF EXISTS `ACT_RU_VARIABLE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_VARIABLE` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_VARIABLE`
--

LOCK TABLES `ACT_RU_VARIABLE` WRITE;
/*!40000 ALTER TABLE `ACT_RU_VARIABLE` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_VARIABLE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_CHANNEL_DEFINITION`
--

DROP TABLE IF EXISTS `FLW_CHANNEL_DEFINITION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_CHANNEL_DEFINITION` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `IMPLEMENTATION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CHANNEL_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_CHANNEL_DEFINITION`
--

LOCK TABLES `FLW_CHANNEL_DEFINITION` WRITE;
/*!40000 ALTER TABLE `FLW_CHANNEL_DEFINITION` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_CHANNEL_DEFINITION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_DEFINITION`
--

DROP TABLE IF EXISTS `FLW_EVENT_DEFINITION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_DEFINITION` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_EVENT_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_DEFINITION`
--

LOCK TABLES `FLW_EVENT_DEFINITION` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_DEFINITION` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_DEFINITION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_DEPLOYMENT`
--

DROP TABLE IF EXISTS `FLW_EVENT_DEPLOYMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_DEPLOYMENT` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_DEPLOYMENT`
--

LOCK TABLES `FLW_EVENT_DEPLOYMENT` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_DEPLOYMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_DEPLOYMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_RESOURCE`
--

DROP TABLE IF EXISTS `FLW_EVENT_RESOURCE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_RESOURCE` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_EVENT_RSRC_DPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `FLW_FK_EVENT_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `FLW_EVENT_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_RESOURCE`
--

LOCK TABLES `FLW_EVENT_RESOURCE` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_RESOURCE` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_RESOURCE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_RU_BATCH`
--

DROP TABLE IF EXISTS `FLW_RU_BATCH`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_RU_BATCH` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BATCH_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_RU_BATCH`
--

LOCK TABLES `FLW_RU_BATCH` WRITE;
/*!40000 ALTER TABLE `FLW_RU_BATCH` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_RU_BATCH` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_RU_BATCH_PART`
--

DROP TABLE IF EXISTS `FLW_RU_BATCH_PART`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_RU_BATCH_PART` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `BATCH_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESULT_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_BATCH_PART` (`BATCH_ID_`),
  CONSTRAINT `FLW_FK_BATCH_PART_PARENT` FOREIGN KEY (`BATCH_ID_`) REFERENCES `FLW_RU_BATCH` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_RU_BATCH_PART`
--

LOCK TABLES `FLW_RU_BATCH_PART` WRITE;
/*!40000 ALTER TABLE `FLW_RU_BATCH_PART` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_RU_BATCH_PART` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_agent`
--

DROP TABLE IF EXISTS `ai_agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `agent_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码（全局唯一）',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `avatar` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像（emoji 或 URL）',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `system_prompt` text COLLATE utf8mb4_general_ci COMMENT '系统提示词（支持 {{var}} 占位）',
  `model_id` bigint DEFAULT NULL COMMENT '绑定模型（ai_model.id，空=平台默认模型）',
  `temperature` double DEFAULT NULL COMMENT '温度覆盖',
  `welcome_message` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开场白',
  `tool_codes` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工具编码列表（逗号分隔）',
  `kb_ids` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '知识库 ID 列表（逗号分隔）',
  `orchestration_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'simple' COMMENT '编排模式：simple 简单配置 / flow 画布编排',
  `graph_json` longtext COLLATE utf8mb4_general_ci COMMENT '画布编排 DSL（mse-flow/v1 JSON，flow 模式生效）',
  `draft_graph_json` longtext COLLATE utf8mb4_general_ci COMMENT '画布编排草稿（编辑器保存目标，发布时校验并拷贝到 graph_json）',
  `scope` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tenant' COMMENT 'platform 平台公共 / tenant 租户自有',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（platform 时为 0）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 已发布 1 下线',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_code` (`agent_code`,`deleted`),
  KEY `idx_scope` (`scope`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能体（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_agent`
--

LOCK TABLES `ai_agent` WRITE;
/*!40000 ALTER TABLE `ai_agent` DISABLE KEYS */;
INSERT INTO `ai_agent` VALUES (8001,'general','通用助手','🤖','平台公共通用对话助手，开箱可用','你是 mse-ai 平台的通用助手。回答准确、简洁，必要时分点说明；不知道的事情不要编造。',NULL,NULL,NULL,NULL,NULL,'simple',NULL,NULL,'platform',0,0,NULL,NULL,'2026-09-21 13:10:44',NULL,'2026-09-21 13:10:44',0),(8002,'oa_approve_assist','OA 审批助手','📋','审批详情 AI 分析：申请摘要/风险点/审批建议','你是企业 OA 审批助手。基于用户提供的审批事项上下文（流程、表单数据、审批记录），输出三部分：【申请摘要】用 2-3 句话概括申请内容与关键数据；【风险点】列出需要审批人关注的风险与疑点（金额、天数、政策合规、材料完整性等），没有则写「未发现明显风险」；【审批建议】给出倾向性建议与理由，但必须注明「AI 分析仅供审批人参考，不构成审批结论」。不要编造上下文中不存在的信息；信息不足时明确指出需要补充什么。',NULL,NULL,NULL,NULL,NULL,'simple',NULL,NULL,'platform',0,0,NULL,NULL,'2026-09-21 13:10:44',NULL,'2026-09-21 13:10:44',0),(8003,'oa_process_designer','OA 流程生成器','🧭','自然语言生成审批流程草稿（简易设计器 JSON）','你是 OA 审批流程设计专家。根据用户的业务需求描述与系统上下文，产出 mse-oa「简易设计器」流程 JSON 草稿。输出要求：只输出一个 JSON 对象，形如 {\"children\":[...]}，不要任何解释文字与代码围栏。节点类型：APPROVER（审批，含 approveMode: ANY或签/ALL会签/SEQ依次/VOTE票签，assignee 规则，emptyStrategy: PASS自动通过/ADMIN转交管理员，rejectStrategy: TERMINATE/TO_STARTER/TO_LAST，timeoutStrategy 可选）；CC（抄送）；BRANCH（EXCLUSIVE 条件分支，branches 内非默认分支用 conditions 配条件，字段 op 可用 EQ/NE/GT/GTE/LT/LTE/IN/NOT_IN/BETWEEN/CONTAINS/EMPTY/NOT_EMPTY，最后一支为默认分支 isDefault）；SUBPROCESS（子流程，引用其它已发布模板 refModelId，仅在用户明确要求时使用）。assignee 规则 type 可用：USER（userIds）/ROLE（roleId）/DEPT（deptId，可配 includeChildren）/DEPT_LEADER/MANAGER_CHAIN（levels）/FORM_FIELD（formField=表单人员字段 id）/STARTER_CHOSEN/STARTER。上下文使用约束（重要）：请求会随上下文提供「表单字段」「部门（编号|名称|上级编号）」「角色（编号|名称）」与「已配置负责人的部门」清单——分支条件与 FORM_FIELD 只能引用提供的字段 id，ROLE/DEPT 只能引用提供的编号，禁止编造编号；声明「组织数据不可用」时审批人规则只用 STARTER_CHOSEN 或 FORM_FIELD；未列入已配置负责人清单的部门不要用 DEPT_LEADER/MANAGER_CHAIN；USER 规则尽量少用（人员变动即失效），优先 ROLE/DEPT/DEPT_LEADER。设计期可调用 oa_flow 工具按需补充数据：org_tree（部门树）/role_list（角色）/dept_users（部门人员）/dept_leaders（部门负责人）/search_users（按姓名搜人）/model_detail（查看现有流程编排 JSON）。「已有流程」清单用于避免设计重复流程，SUBPROCESS 的 refModelId 只能引用其中编号。审批链要符合中国企业审批惯例（直属主管→部门负责人→分管领导→总经理，金额/天数升级，结果抄送相关部门）；每个节点 id 唯一且非空。修复模式：若用户消息包含「上一版 JSON 存在以下问题」的错误清单，逐项修正后输出完整修正版 JSON（保留正确部分）。',NULL,NULL,NULL,NULL,NULL,'simple',NULL,NULL,'platform',0,0,NULL,NULL,'2026-09-21 13:10:44',NULL,'2026-09-21 13:10:44',0),(8004,'oa_form_fill','OA 智能填单','✍️','从材料文本提取表单字段值','你是表单填写助手。根据用户提供的表单字段定义与材料文本，提取各字段的值。输出要求：只输出一个 JSON 对象，键=字段 id，值=提取后的字段值（类型与字段定义一致，数组字段用 JSON 数组）；材料中无法确定的字段一律不要输出，禁止编造。',NULL,NULL,NULL,NULL,NULL,'simple',NULL,NULL,'platform',0,0,NULL,NULL,'2026-09-21 13:10:44',NULL,'2026-09-21 13:10:44',0),(8005,'oa_flow_bot','OA 流程机器人','🤖','待办问答 / 审批统计 / 代发催办（经 oa_flow 工具）','你是 OA 流程机器人，帮助用户查询和处理审批事项。你可以调用 oa_flow 工具：todo_query（查待办/已办/我发起/抄送，scope=todo|done|mine|cc）、instance_detail（实例详情）、model_catalog（可发起的流程模板目录）、stats（审批统计）、remind（催办，仅发起人本人）。回答用简洁中文列表；用户要求模糊时先查再答；涉及具体操作（催办）前与用户确认。',NULL,NULL,NULL,'oa_flow',NULL,'simple',NULL,NULL,'platform',0,0,NULL,NULL,'2026-09-21 13:10:44',NULL,'2026-09-21 13:10:44',0),(8006,'data_analyst','数据智能分析师','📊','text2sql 智能查询（受控通道：SQL 防护+租户隔离+限行）','你是数据分析 SQL 专家，帮助用户用自然语言查询企业数据。工作流程：1) 先调用 data_query 工具（action=meta_search）检索相关表与列，拿不准列名时用 metaTableId 查列清单；2) 再调用 data_query 工具（action=execute）执行只读 SELECT（不要写分页 LIMIT，系统自动处理）；3) 用 Markdown 表格或要点向用户解读结果，说明数据口径与限制。约束：只生成单条 SELECT；不查询敏感个人信息；中台库只查 ods_ 前缀表；执行失败时根据报错修正 SQL 重试（最多 2 次）。诚实性要求：回答必须基于工具返回的真实数据，禁止编造数据或假装已执行查询；工具未能成功执行时，直接向用户说明失败原因与已尝试的 SQL，不要输出未经执行的查询结果。',NULL,NULL,NULL,'data_query',NULL,'simple',NULL,NULL,'platform',0,0,NULL,NULL,'2026-09-21 13:10:44',NULL,'2026-09-21 13:10:44',0);
/*!40000 ALTER TABLE `ai_agent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_api_key`
--

DROP TABLE IF EXISTS `ai_api_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_api_key` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `app_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用 ID',
  `key_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API Key 哈希（SHA-256）',
  `key_prefix` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Key 前缀（识别用）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '业务归属租户',
  `agent_codes` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体白名单（空=全部已发布）',
  `rate_per_minute` int DEFAULT NULL COMMENT '每分钟调用上限（0/空=不限）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 密钥（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_api_key`
--

LOCK TABLES `ai_api_key` WRITE;
/*!40000 ALTER TABLE `ai_api_key` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_api_key` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_message`
--

DROP TABLE IF EXISTS `ai_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_message` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `session_id` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话 ID（ai_chat_session.id 字符串）',
  `role` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'user / assistant',
  `content` mediumtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `ext` mediumtext COLLATE utf8mb4_general_ci COMMENT '扩展信息 JSON（assistant 消息：datasets 工具数据结果 / sources 引用来源，历史回放恢复用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对话消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_message`
--

LOCK TABLES `ai_chat_message` WRITE;
/*!40000 ALTER TABLE `ai_chat_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_session`
--

DROP TABLE IF EXISTS `ai_chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_session` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `title` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话标题',
  `agent_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码',
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `last_active_time` datetime DEFAULT NULL COMMENT '最近活跃时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_agent` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对话会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_session`
--

LOCK TABLES `ai_chat_session` WRITE;
/*!40000 ALTER TABLE `ai_chat_session` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_kb_chunk`
--

DROP TABLE IF EXISTS `ai_kb_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_kb_chunk` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `kb_id` bigint NOT NULL COMMENT '知识库 ID',
  `document_id` bigint NOT NULL COMMENT '文档 ID',
  `seq` int NOT NULL DEFAULT '0' COMMENT '切片序号',
  `content` mediumtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '切片正文',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_kb_doc` (`kb_id`,`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库切片';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_kb_chunk`
--

LOCK TABLES `ai_kb_chunk` WRITE;
/*!40000 ALTER TABLE `ai_kb_chunk` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_kb_chunk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_kb_document`
--

DROP TABLE IF EXISTS `ai_kb_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_kb_document` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `kb_id` bigint NOT NULL COMMENT '知识库 ID',
  `file_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `file_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件类型',
  `size_bytes` bigint DEFAULT NULL COMMENT '文件大小',
  `storage_path` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原文件归档路径（支撑重新解析/重建索引）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 待处理 1 处理中 2 完成 3 失败',
  `chunk_count` int DEFAULT NULL COMMENT '切片数量',
  `error_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_kb_document`
--

LOCK TABLES `ai_kb_document` WRITE;
/*!40000 ALTER TABLE `ai_kb_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_kb_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_base`
--

DROP TABLE IF EXISTS `ai_knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_base` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `chunk_size` int DEFAULT NULL COMMENT '切片目标大小（token）',
  `chunk_method` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'general' COMMENT '切片方法（general/qa/table/book/one）',
  `chunk_overlap` int DEFAULT '100' COMMENT '切片重叠大小（token，general/book 生效）',
  `top_k` int DEFAULT NULL COMMENT '默认检索条数',
  `similarity_threshold` decimal(4,3) DEFAULT '0.500' COMMENT '相似度阈值（0~1）',
  `embedding_model_id` bigint DEFAULT NULL COMMENT '绑定向量模型（ai_model.id，model_type=embedding；空=按旧版全局配置/平台默认向量模型解析）',
  `rerank_model_id` bigint DEFAULT NULL COMMENT '绑定重排模型（ai_model.id，model_type=rerank；空=按旧版平台级配置解析，均未配置则不重排）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_base`
--

LOCK TABLES `ai_knowledge_base` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_base` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_base` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_mcp_server`
--

DROP TABLE IF EXISTS `ai_mcp_server`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_mcp_server` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共数据）',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务名（唯一，工具编码引用）',
  `endpoint` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '基础地址',
  `sse_path` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'SSE 路径（默认 /sse）',
  `transport` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'sse' COMMENT '传输方式',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部 MCP Server 注册（平台公共 tenant_id=0）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_mcp_server`
--

LOCK TABLES `ai_mcp_server` WRITE;
/*!40000 ALTER TABLE `ai_mcp_server` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_mcp_server` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model`
--

DROP TABLE IF EXISTS `ai_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共数据）',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称',
  `provider` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商标注（deepseek/zhipu/moonshot/dashscope/openai/anthropic/ollama/xfyun/openai-compat，仅展示与预设联动）',
  `protocol` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'openai-chat' COMMENT '接口类型（openai-chat=OpenAI兼容 chat/completions；anthropic-messages=Claude 原生 /v1/messages；向量模型行固定 openai-chat）',
  `model_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'chat' COMMENT '模型类型（chat=对话模型 embedding=向量模型 rerank=重排模型，一行只表达一种能力）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '默认向量模型标记（仅 model_type=embedding 有意义，全局唯一）',
  `base_url` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型端点（openai-chat 自带版本前缀如 https://api.deepseek.com；anthropic-messages 如 https://api.anthropic.com）',
  `api_key_enc` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'API Key 密文（AES-GCM）',
  `chat_model` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '对话模型名（model_type=embedding 时为空串）',
  `embedding_model` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型名（model_type=embedding 时为本行向量模型名；chat 行为旧版遗留字段，仅旧数据兼容读取）',
  `embedding_base_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型独立端点（空复用 base_url）',
  `embedding_api_key_enc` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型独立 API Key 密文（空复用 api_key_enc）',
  `temperature` double DEFAULT NULL COMMENT '默认温度',
  `top_p` double DEFAULT NULL COMMENT '默认 topP',
  `max_tokens` int DEFAULT NULL COMMENT '默认最大输出 tokens',
  `failover_model_id` bigint DEFAULT NULL COMMENT '备用对话模型（ai_model.id，model_type=chat；主模型瞬态错误重试仍失败时自动切换，空=不切换）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_test_time` datetime DEFAULT NULL COMMENT '最近连通性测试时间',
  `last_test_ok` tinyint DEFAULT NULL COMMENT '最近测试结果：0 失败 1 成功',
  `last_test_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近测试失败原因',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型配置（平台公共 tenant_id=0）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model`
--

LOCK TABLES `ai_model` WRITE;
/*!40000 ALTER TABLE `ai_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model_tenant`
--

DROP TABLE IF EXISTS `ai_model_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_tenant` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `model_id` bigint NOT NULL COMMENT '模型 ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_model` (`model_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型-租户可用范围（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model_tenant`
--

LOCK TABLES `ai_model_tenant` WRITE;
/*!40000 ALTER TABLE `ai_model_tenant` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_tenant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model_tenant_config`
--

DROP TABLE IF EXISTS `ai_model_tenant_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_tenant_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `model_id` bigint NOT NULL COMMENT '模型 ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `base_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '覆盖端点（空=沿用平台配置）',
  `api_key_enc` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '覆盖 API Key 密文 AES-GCM（空=沿用平台配置）',
  `temperature` double DEFAULT NULL COMMENT '覆盖温度（空=沿用平台配置）',
  `top_p` double DEFAULT NULL COMMENT '覆盖 topP（空=沿用平台配置）',
  `max_tokens` int DEFAULT NULL COMMENT '覆盖最大输出 tokens（空=沿用平台配置）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_model` (`model_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型租户级覆盖配置（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model_tenant_config`
--

LOCK TABLES `ai_model_tenant_config` WRITE;
/*!40000 ALTER TABLE `ai_model_tenant_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_tenant_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_tool`
--

DROP TABLE IF EXISTS `ai_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_tool` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（0=平台公共系统工具，>0=租户自有工具）',
  `tool_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具编码（全局唯一）',
  `name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工具名称',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '功能说明（面向模型）',
  `parameters_json_schema` text COLLATE utf8mb4_general_ci COMMENT '入参 JSON Schema',
  `source` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'rpc' COMMENT 'builtin / local / rpc / http / dify',
  `rpc_group` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'RPC 提供方服务名（Dubbo group）',
  `config_json` text COLLATE utf8mb4_general_ci COMMENT '外部工具连接配置（source=http/dify 时生效）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_code` (`tool_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工具登记（tenant_id=0 平台公共系统工具，>0 租户自有）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_tool`
--

LOCK TABLES `ai_tool` WRITE;
/*!40000 ALTER TABLE `ai_tool` DISABLE KEYS */;
INSERT INTO `ai_tool` VALUES (8101,0,'oa_flow','OA 流程查询与操作','运行期：查询当前用户的待办/已办/我发起/抄送列表、流程实例详情（含表单数据与审批记录）、已发布流程模板目录、审批统计，代发起人催办。设计期（流程设计供数）：查组织部门树、角色清单、部门人员、部门负责人、按姓名搜人、查看现有流程的编排 JSON。','{\"type\":\"object\",\"properties\":{\"action\":{\"type\":\"string\",\"enum\":[\"todo_query\",\"instance_detail\",\"model_catalog\",\"stats\",\"remind\",\"org_tree\",\"role_list\",\"dept_users\",\"dept_leaders\",\"search_users\",\"model_detail\"],\"description\":\"todo_query=查任务列表(需scope)；instance_detail=实例详情(需instanceId)；model_catalog=流程模板目录；stats=审批统计；remind=催办(需instanceId)；org_tree=部门树(编号|名称|上级)；role_list=角色清单；dept_users=部门人员(需deptId)；dept_leaders=部门负责人(可选deptId)；search_users=按姓名搜人(需keyword)；model_detail=查看流程编排JSON(需modelId)\"},\"scope\":{\"type\":\"string\",\"enum\":[\"todo\",\"done\",\"mine\",\"cc\"],\"description\":\"todo_query 的列表范围\"},\"instanceId\":{\"type\":\"string\",\"description\":\"流程实例编号\"},\"opinion\":{\"type\":\"string\",\"description\":\"催办留言\"},\"deptId\":{\"type\":\"integer\",\"description\":\"dept_users/dept_leaders 的部门编号\"},\"includeChildren\":{\"type\":\"boolean\",\"description\":\"dept_users 是否含子部门，默认 false\"},\"keyword\":{\"type\":\"string\",\"description\":\"search_users 的姓名关键词\"},\"modelId\":{\"type\":\"integer\",\"description\":\"model_detail 的流程模板编号\"},\"pageNum\":{\"type\":\"integer\",\"description\":\"页码，默认 1\"},\"pageSize\":{\"type\":\"integer\",\"description\":\"每页条数，默认 5，最大 20\"}},\"required\":[\"action\"]}','rpc','mse-oa',NULL,0,NULL,NULL,'2026-09-21 13:10:45',NULL,'2026-09-21 13:10:45',0),(8102,0,'data_query','数据中台智能查询','按 action 检索数据中台的表/列元数据（meta_search），执行受控只读 SQL 查询（execute：自动注入租户条件、限行数、只允许 SELECT），或列出可用数据源（datasource_list：中台库 + 本租户已启用数据源）。写 SQL 前先用 meta_search 确认表和列名。','{\"type\":\"object\",\"properties\":{\"action\":{\"type\":\"string\",\"enum\":[\"meta_search\",\"execute\",\"datasource_list\"],\"description\":\"meta_search=检索表/列元数据；execute=执行受控只读 SQL；datasource_list=列出可用数据源\"},\"keyword\":{\"type\":\"string\",\"description\":\"meta_search 的表名/注释关键字\"},\"metaTableId\":{\"type\":\"integer\",\"description\":\"meta_search 指定表编号时返回该表的列清单\"},\"datasourceId\":{\"type\":\"integer\",\"description\":\"execute 的数据源编号，0 或省略=中台库（仅 ods_ 表）\"},\"sql\":{\"type\":\"string\",\"description\":\"execute 的只读 SELECT 语句（不含分页，系统自动追加）\"},\"pageNum\":{\"type\":\"integer\",\"description\":\"页码，默认 1\"},\"pageSize\":{\"type\":\"integer\",\"description\":\"每页条数，默认 20，最大 200\"}},\"required\":[\"action\"]}','rpc','mse-data',NULL,0,NULL,NULL,'2026-09-21 13:10:45',NULL,'2026-09-21 13:10:45',0);
/*!40000 ALTER TABLE `ai_tool` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_tool_log`
--

DROP TABLE IF EXISTS `ai_tool_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_tool_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `tool_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具编码',
  `agent_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体编码',
  `session_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话 ID',
  `arguments` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '入参 JSON',
  `result` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '出参内容',
  `success` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `error_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_tool` (`tenant_id`,`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工具调用审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_tool_log`
--

LOCK TABLES `ai_tool_log` WRITE;
/*!40000 ALTER TABLE `ai_tool_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_tool_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_usage_log`
--

DROP TABLE IF EXISTS `ai_usage_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_usage_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `channel` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'gateway / rpc / openapi',
  `agent_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体编码',
  `model` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模型名',
  `conversation_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话 ID',
  `user_id` bigint DEFAULT NULL COMMENT '触发用户（openapi 通道为空）',
  `prompt_tokens` int DEFAULT NULL COMMENT '提示词 tokens 消耗',
  `completion_tokens` int DEFAULT NULL COMMENT '补全 tokens 消耗',
  `total_tokens` int DEFAULT NULL COMMENT '总 tokens 消耗',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `error_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`create_time`),
  KEY `idx_agent` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 用量流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_usage_log`
--

LOCK TABLES `ai_usage_log` WRITE;
/*!40000 ALTER TABLE `ai_usage_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_usage_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_workflow_run`
--

DROP TABLE IF EXISTS `ai_workflow_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_workflow_run` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `agent_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码',
  `agent_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体名称快照',
  `channel` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发通道：gateway 对话 / debug 试运行 / rpc 无状态 / openapi 开放接口',
  `session_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话号（debug 无会话为空）',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '运行结果：success / failed / cancelled',
  `query_text` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户输入（截断存档）',
  `answer` text COLLATE utf8mb4_general_ci COMMENT '最终回答（截断存档）',
  `error` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '总耗时毫秒',
  `node_total` int DEFAULT NULL COMMENT '节点轨迹条数',
  `traces_json` longtext COLLATE utf8mb4_general_ci COMMENT '节点轨迹快照 JSON（与 SSE node 事件同构）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（平台调试行为 0）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_agent` (`agent_code`,`create_time`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流运行历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_workflow_run`
--

LOCK TABLES `ai_workflow_run` WRITE;
/*!40000 ALTER TABLE `ai_workflow_run` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_workflow_run` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api`
--

DROP TABLE IF EXISTS `dat_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 名称',
  `path` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '开放路径 /openapi/data/{path}，租户内唯一',
  `datasource_id` bigint NOT NULL COMMENT '查询的数据源（0 = 中台库）',
  `sql_template` text COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SQL 模板（仅 SELECT，:param 占位）',
  `params_json` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数定义 JSON [{name,type,required,remark}]',
  `page_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '0 关 1 开（pageNum/pageSize 自动分页包裹）',
  `max_rows` int NOT NULL DEFAULT '1000' COMMENT '单次返回行数上限',
  `timeout_ms` int NOT NULL DEFAULT '10000' COMMENT '查询超时（毫秒）',
  `qps_limit` int NOT NULL DEFAULT '60' COMMENT '每分钟最大调用次数（0 = 跟随全局默认）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 发布 1 下线',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_path` (`tenant_id`,`path`,`deleted`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api`
--

LOCK TABLES `dat_api` WRITE;
/*!40000 ALTER TABLE `dat_api` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_app`
--

DROP TABLE IF EXISTS `dat_api_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_app` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `app_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局唯一',
  `app_secret` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密文（AES-GCM）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_app_key` (`app_key`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务调用方应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_app`
--

LOCK TABLES `dat_api_app` WRITE;
/*!40000 ALTER TABLE `dat_api_app` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_approval`
--

DROP TABLE IF EXISTS `dat_api_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_approval` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `api_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'API 名称',
  `action` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'submit/approve/reject/offline',
  `comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `operator_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人姓名',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`,`api_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 审批记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_approval`
--

LOCK TABLES `dat_api_approval` WRITE;
/*!40000 ALTER TABLE `dat_api_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_grant`
--

DROP TABLE IF EXISTS `dat_api_grant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_grant` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `app_id` bigint NOT NULL COMMENT 'dat_api_app.id',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（空=永久）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_api_app` (`api_id`,`app_id`,`deleted`),
  KEY `idx_tenant_app` (`tenant_id`,`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 授权';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_grant`
--

LOCK TABLES `dat_api_grant` WRITE;
/*!40000 ALTER TABLE `dat_api_grant` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_grant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_log`
--

DROP TABLE IF EXISTS `dat_api_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `app_id` bigint NOT NULL COMMENT '调用方应用 ID',
  `params` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '本次请求参数 JSON',
  `row_count` int DEFAULT NULL COMMENT '返回行数',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `status` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'success / failed',
  `error_msg` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `client_ip` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端 IP',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`,`api_id`,`create_time`),
  KEY `idx_tenant_app` (`tenant_id`,`app_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 调用日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_log`
--

LOCK TABLES `dat_api_log` WRITE;
/*!40000 ALTER TABLE `dat_api_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_datasource`
--

DROP TABLE IF EXISTS `dat_datasource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_datasource` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源名称',
  `type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'mysql / dm / kingbase',
  `host` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口',
  `db_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '库名/schema',
  `extra_params` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JDBC 追加参数（如 useSSL=false&…）',
  `username` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '口令密文（AES-GCM）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_test_time` datetime DEFAULT NULL COMMENT '最近连通性测试时间',
  `last_test_ok` tinyint DEFAULT NULL COMMENT '最近测试结果：0 失败 1 成功',
  `last_test_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近测试失败原因',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部数据源注册';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_datasource`
--

LOCK TABLES `dat_datasource` WRITE;
/*!40000 ALTER TABLE `dat_datasource` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_datasource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_lineage`
--

DROP TABLE IF EXISTS `dat_lineage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_lineage` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `job_id` bigint NOT NULL COMMENT '同步任务',
  `job_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称',
  `datasource_id` bigint NOT NULL COMMENT '来源数据源',
  `source_type` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'table' COMMENT 'table / sql',
  `source_table` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源表（sql 来源存查询摘要）',
  `target_table` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标 ODS 表',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_job` (`tenant_id`,`job_id`,`deleted`),
  KEY `idx_tenant_target` (`tenant_id`,`target_table`),
  KEY `idx_tenant_source` (`tenant_id`,`source_table`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务级血缘（同步任务：源表 → ODS）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_lineage`
--

LOCK TABLES `dat_lineage` WRITE;
/*!40000 ALTER TABLE `dat_lineage` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_lineage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_meta_column`
--

DROP TABLE IF EXISTS `dat_meta_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_meta_column` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `meta_table_id` bigint NOT NULL COMMENT '所属 dat_meta_table',
  `column_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '列名',
  `data_type` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据类型',
  `nullable` tinyint NOT NULL DEFAULT '1' COMMENT '是否可空（1 是 0 否）',
  `column_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列注释',
  `ordinal` int NOT NULL DEFAULT '0' COMMENT '列序号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_table` (`tenant_id`,`meta_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='元数据-列';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_meta_column`
--

LOCK TABLES `dat_meta_column` WRITE;
/*!40000 ALTER TABLE `dat_meta_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_meta_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_meta_table`
--

DROP TABLE IF EXISTS `dat_meta_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_meta_table` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `datasource_id` bigint NOT NULL COMMENT '0 = 中台内部（ODS/中台库自身）',
  `table_schema` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属 schema',
  `table_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名',
  `category` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'source' COMMENT 'source 源库表 / ods 中台贴源表',
  `table_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表注释',
  `row_count` bigint DEFAULT NULL COMMENT ' approximate 行数',
  `collected_time` datetime NOT NULL COMMENT '最近采集时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_ds_table` (`tenant_id`,`datasource_id`,`table_name`,`deleted`),
  KEY `idx_tenant_cat` (`tenant_id`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='元数据-表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_meta_table`
--

LOCK TABLES `dat_meta_table` WRITE;
/*!40000 ALTER TABLE `dat_meta_table` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_meta_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_quality_report`
--

DROP TABLE IF EXISTS `dat_quality_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_quality_report` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `rule_id` bigint NOT NULL COMMENT '质量规则 ID',
  `rule_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名冗余',
  `status` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'success / failed',
  `total_rows` bigint DEFAULT NULL COMMENT '检测总行数',
  `bad_rows` bigint DEFAULT NULL COMMENT '坏数据行数',
  `pass_rate` decimal(5,2) DEFAULT NULL COMMENT '通过率（%）',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `error_msg` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `trigger_type` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT 'manual / sched',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_rule` (`tenant_id`,`rule_id`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据质量报告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_quality_report`
--

LOCK TABLES `dat_quality_report` WRITE;
/*!40000 ALTER TABLE `dat_quality_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_quality_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_quality_rule`
--

DROP TABLE IF EXISTS `dat_quality_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_quality_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称',
  `datasource_id` bigint NOT NULL DEFAULT '0' COMMENT '0 = 中台库贴源层',
  `target_table` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标表',
  `rule_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'not_null/unique/range/enum/custom_sql/row_count_fluctuation',
  `column_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '检测列（custom_sql/fluctuation 无需）',
  `rule_config` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则配置 JSON（min/max/values/sql/thresholdPct）',
  `cron` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调度 cron（空=仅手动）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `last_pass` tinyint DEFAULT NULL COMMENT '最近结果：0 未通过 1 通过',
  `last_pass_rate` decimal(5,2) DEFAULT NULL COMMENT '最近通过率（%）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据质量规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_quality_rule`
--

LOCK TABLES `dat_quality_rule` WRITE;
/*!40000 ALTER TABLE `dat_quality_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_quality_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_query_allowlist`
--

DROP TABLE IF EXISTS `dat_query_allowlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_query_allowlist` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `table_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '放行的表名（小写归一化）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_table` (`tenant_id`,`table_name`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='中台库通道查询白名单（ods_ 之外可查业务表）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_query_allowlist`
--

LOCK TABLES `dat_query_allowlist` WRITE;
/*!40000 ALTER TABLE `dat_query_allowlist` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_query_allowlist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_sync_job`
--

DROP TABLE IF EXISTS `dat_sync_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_sync_job` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `datasource_id` bigint NOT NULL COMMENT '来源数据源',
  `source_type` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'table' COMMENT 'table 表 / sql 自定义查询',
  `source_table` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '源表名（source_type=table）',
  `query_sql` text COLLATE utf8mb4_general_ci COMMENT '自定义查询 SQL（source_type=sql，仅 SELECT）',
  `target_table` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标 ODS 表（引擎自动建，ods_ 前缀）',
  `sync_mode` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'full' COMMENT 'full 全量 / incr 增量',
  `incr_column` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '增量水位列（时间戳/自增 ID）',
  `watermark` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前水位值（引擎回写）',
  `cron` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调度 cron（Spring 表达式，空=仅手动）',
  `dep_job_id` bigint DEFAULT NULL COMMENT '前置任务（成功完成后级联触发本任务）',
  `row_count` bigint NOT NULL DEFAULT '0' COMMENT '最近一次同步行数',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_ds` (`tenant_id`,`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据集成同步任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_sync_job`
--

LOCK TABLES `dat_sync_job` WRITE;
/*!40000 ALTER TABLE `dat_sync_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_sync_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_sync_job_log`
--

DROP TABLE IF EXISTS `dat_sync_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_sync_job_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `job_id` bigint NOT NULL COMMENT '同步任务ID',
  `trigger_type` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'manual 手动 / sched 调度',
  `status` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'running / success / failed',
  `sync_mode` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '本次执行模式 full / incr',
  `row_count` bigint NOT NULL DEFAULT '0' COMMENT '同步行数',
  `watermark_used` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行前水位',
  `watermark_new` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行后水位',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `error_msg` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_job` (`tenant_id`,`job_id`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同步任务运行日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_sync_job_log`
--

LOCK TABLES `dat_sync_job_log` WRITE;
/*!40000 ALTER TABLE `dat_sync_job_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_sync_job_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_event`
--

DROP TABLE IF EXISTS `demo_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_event` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `event_type` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型',
  `payload` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '事件负载（JSON）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='已消费租户事件（共享主库）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_event`
--

LOCK TABLES `demo_event` WRITE;
/*!40000 ALTER TABLE `demo_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_order`
--

DROP TABLE IF EXISTS `demo_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `order_no` varchar(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号',
  `product_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `amount` decimal(12,2) DEFAULT NULL COMMENT '订单金额',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order_no` (`tenant_id`,`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例订单（@TenantDatasource 混合路由）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_order`
--

LOCK TABLES `demo_order` WRITE;
/*!40000 ALTER TABLE `demo_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_stock`
--

DROP TABLE IF EXISTS `demo_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_stock` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `remain` int NOT NULL DEFAULT '0' COMMENT '剩余可下单数量',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例库存（共享主库，跨库事务的另一端）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_stock`
--

LOCK TABLES `demo_stock` WRITE;
/*!40000 ALTER TABLE `demo_stock` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_file`
--

DROP TABLE IF EXISTS `fs_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_file` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花，物理对象键同名）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `file_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始文件名',
  `storage_key` varchar(512) COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储键（{tenantId}/{yyyyMM}/{fileId}.{ext}）',
  `storage_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储类型：local / rustfs',
  `content_type` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容 MIME 类型',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '字节数',
  `biz_type` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务分类（如 oa/attachment）',
  `biz_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务关联 ID',
  `access_level` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tenant' COMMENT '访问级别：tenant 登录本租户 / public 匿名可读（/fs/asset/{id}）',
  `content_md5` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容 MD5（32 位小写 hex；同租户同 MD5 秒传复用物理对象）',
  `upload_id` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分片直传会话 ID（仅 rustfs 直传进行中非空，complete/abort 后清空/删行）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_md5` (`content_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件元数据（物理对象按 storage_type 由 FileStorage 实现保管）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_file`
--

LOCK TABLES `fs_file` WRITE;
/*!40000 ALTER TABLE `fs_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `fs_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_alert`
--

DROP TABLE IF EXISTS `mon_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_alert` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `target_id` bigint NOT NULL COMMENT '监控目标',
  `target_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标名称（冗余，目标删除后仍可读）',
  `level` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'warn' COMMENT '级别：warn/error',
  `rule_key` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则键：down/latency/cpu/mem/disk/redis_mem/mysql_threads',
  `content` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警内容',
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'firing' COMMENT '状态：firing/resolved',
  `first_trigger_time` datetime DEFAULT NULL COMMENT '首次触发时间',
  `last_trigger_time` datetime DEFAULT NULL COMMENT '最近触发时间',
  `resolve_time` datetime DEFAULT NULL COMMENT '恢复时间',
  `notify_result` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '通知结果 JSON（inbox/webhook）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_alert_target` (`target_id`,`status`),
  KEY `idx_mon_alert_time` (`last_trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控告警';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_alert`
--

LOCK TABLES `mon_alert` WRITE;
/*!40000 ALTER TABLE `mon_alert` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_record`
--

DROP TABLE IF EXISTS `mon_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_record` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `target_id` bigint NOT NULL COMMENT '监控目标',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '采集状态：up/down',
  `latency_ms` int DEFAULT NULL COMMENT '响应耗时（ms）',
  `metrics` text COLLATE utf8mb4_general_ci COMMENT '指标集 JSON（按类型：cpu_usage_pct/threads_connected/qps…）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_record_target` (`target_id`,`collect_time`),
  KEY `idx_mon_record_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控采样记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_record`
--

LOCK TABLES `mon_record` WRITE;
/*!40000 ALTER TABLE `mon_record` DISABLE KEYS */;
INSERT INTO `mon_record` VALUES (2101901966140141570,0,12,'2026-09-21 13:10:54','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\"}',NULL,'2026-09-21 13:10:55',NULL,'2026-09-21 13:10:55',0),(2101901966219833345,0,13,'2026-09-21 13:10:54','up',23,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:10:55',NULL,'2026-09-21 13:10:55',0),(2101902092237697026,0,12,'2026-09-21 13:11:24','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:11:25',NULL,'2026-09-21 13:11:25',0),(2101902092309000193,0,13,'2026-09-21 13:11:24','up',24,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:11:25',NULL,'2026-09-21 13:11:25',0),(2101902097476382721,0,2101901965926232066,'2026-09-21 13:11:26','up',0,'{\"cpu_usage_pct\":0.0,\"mem_total_gb\":63.9,\"mem_used_gb\":31.4,\"mem_usage_pct\":49.3,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"uptime_hours\":74.9}',NULL,'2026-09-21 13:11:26',NULL,'2026-09-21 13:11:26',0),(2101902218343641089,0,12,'2026-09-21 13:11:54','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:11:55',NULL,'2026-09-21 13:11:55',0),(2101902218402361345,0,13,'2026-09-21 13:11:54','up',17,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:11:55',NULL,'2026-09-21 13:11:55',0),(2101902260659974145,0,2101901965926232066,'2026-09-21 13:12:05','up',0,'{\"cpu_usage_pct\":12.1,\"mem_total_gb\":63.9,\"mem_used_gb\":31.6,\"mem_usage_pct\":49.5,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":26.6,\"net_tx_kbps\":8.8,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:12:05',NULL,'2026-09-21 13:12:05',0),(2101902344512499713,0,12,'2026-09-21 13:12:25','up',7,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.3}',NULL,'2026-09-21 13:12:25',NULL,'2026-09-21 13:12:25',0),(2101902344642523137,0,13,'2026-09-21 13:12:25','up',24,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:12:25',NULL,'2026-09-21 13:12:25',0),(2101902386753335298,0,2101901965926232066,'2026-09-21 13:12:35','up',0,'{\"cpu_usage_pct\":11.0,\"mem_total_gb\":63.9,\"mem_used_gb\":31.4,\"mem_usage_pct\":49.1,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":6.2,\"net_tx_kbps\":3.9,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:12:35',NULL,'2026-09-21 13:12:35',0),(2101902470631026690,0,12,'2026-09-21 13:12:55','up',8,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:12:55',NULL,'2026-09-21 13:12:55',0),(2101902470761050114,0,13,'2026-09-21 13:12:55','up',24,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:12:55',NULL,'2026-09-21 13:12:55',0),(2101902512880250881,0,2101901965926232066,'2026-09-21 13:13:05','up',0,'{\"cpu_usage_pct\":11.3,\"mem_total_gb\":63.9,\"mem_used_gb\":31.5,\"mem_usage_pct\":49.3,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":5.0,\"net_tx_kbps\":3.3,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:13:05',NULL,'2026-09-21 13:13:05',0),(2101902596804079617,0,12,'2026-09-21 13:13:25','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.4}',NULL,'2026-09-21 13:13:25',NULL,'2026-09-21 13:13:25',0),(2101902596866994178,0,13,'2026-09-21 13:13:25','up',21,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:13:25',NULL,'2026-09-21 13:13:25',0),(2101902639032332289,0,2101901965926232066,'2026-09-21 13:13:35','up',0,'{\"cpu_usage_pct\":10.2,\"mem_total_gb\":63.9,\"mem_used_gb\":31.3,\"mem_usage_pct\":49.0,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":6.1,\"net_tx_kbps\":3.4,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:13:35',NULL,'2026-09-21 13:13:35',0),(2101902722968743938,0,12,'2026-09-21 13:13:55','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:13:55',NULL,'2026-09-21 13:13:55',0),(2101902723035852802,0,13,'2026-09-21 13:13:55','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:13:55',NULL,'2026-09-21 13:13:55',0),(2101902765356380162,0,2101901965926232066,'2026-09-21 13:14:05','up',0,'{\"cpu_usage_pct\":9.7,\"mem_total_gb\":63.9,\"mem_used_gb\":32.5,\"mem_usage_pct\":50.9,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":32.9,\"net_tx_kbps\":39.8,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:14:05',NULL,'2026-09-21 13:14:05',0),(2101902849036939266,0,12,'2026-09-21 13:14:25','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.4}',NULL,'2026-09-21 13:14:25',NULL,'2026-09-21 13:14:25',0),(2101902849108242434,0,13,'2026-09-21 13:14:25','up',22,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:14:25',NULL,'2026-09-21 13:14:25',0),(2101902891260997634,0,2101901965926232066,'2026-09-21 13:14:35','up',0,'{\"cpu_usage_pct\":8.5,\"mem_total_gb\":63.9,\"mem_used_gb\":32.9,\"mem_usage_pct\":51.6,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":26.4,\"net_tx_kbps\":6.1,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:14:35',NULL,'2026-09-21 13:14:35',0),(2101902975256129537,0,12,'2026-09-21 13:14:55','up',10,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:14:55',NULL,'2026-09-21 13:14:55',0),(2101902975386152961,0,13,'2026-09-21 13:14:55','up',34,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:14:55',NULL,'2026-09-21 13:14:55',0),(2101903017455022082,0,2101901965926232066,'2026-09-21 13:15:05','up',0,'{\"cpu_usage_pct\":12.9,\"mem_total_gb\":63.9,\"mem_used_gb\":32.9,\"mem_usage_pct\":51.5,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":14.6,\"net_tx_kbps\":9.8,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:15:05',NULL,'2026-09-21 13:15:05',0),(2101903101336907778,0,12,'2026-09-21 13:15:25','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.4}',NULL,'2026-09-21 13:15:25',NULL,'2026-09-21 13:15:25',0),(2101903101399822337,0,13,'2026-09-21 13:15:25','up',21,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:15:26',NULL,'2026-09-21 13:15:26',0),(2101903143565160450,0,2101901965926232066,'2026-09-21 13:15:35','up',0,'{\"cpu_usage_pct\":10.6,\"mem_total_gb\":63.9,\"mem_used_gb\":32.7,\"mem_usage_pct\":51.3,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":24.0,\"net_tx_kbps\":8.7,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:15:36',NULL,'2026-09-21 13:15:36',0),(2101903227476406273,0,12,'2026-09-21 13:15:55','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":6.2}',NULL,'2026-09-21 13:15:56',NULL,'2026-09-21 13:15:56',0),(2101903227543515138,0,13,'2026-09-21 13:15:55','up',22,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:15:56',NULL,'2026-09-21 13:15:56',0),(2101903269725630466,0,2101901965926232066,'2026-09-21 13:16:05','up',0,'{\"cpu_usage_pct\":9.9,\"mem_total_gb\":63.9,\"mem_used_gb\":33.0,\"mem_usage_pct\":51.7,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":26.8,\"net_tx_kbps\":41.0,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:16:06',NULL,'2026-09-21 13:16:06',0),(2101903353578156033,0,12,'2026-09-21 13:16:25','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:16:26',NULL,'2026-09-21 13:16:26',0),(2101903353653653506,0,13,'2026-09-21 13:16:25','up',18,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:16:26',NULL,'2026-09-21 13:16:26',0),(2101903395760271361,0,2101901965926232066,'2026-09-21 13:16:35','up',0,'{\"cpu_usage_pct\":9.4,\"mem_total_gb\":63.9,\"mem_used_gb\":32.8,\"mem_usage_pct\":51.3,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":17.9,\"net_tx_kbps\":9.8,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:16:36',NULL,'2026-09-21 13:16:36',0),(2101903479755403265,0,12,'2026-09-21 13:16:55','up',10,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:16:56',NULL,'2026-09-21 13:16:56',0),(2101903479881232385,0,13,'2026-09-21 13:16:55','up',29,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:16:56',NULL,'2026-09-21 13:16:56',0),(2101903521908158466,0,2101901965926232066,'2026-09-21 13:17:05','up',0,'{\"cpu_usage_pct\":9.3,\"mem_total_gb\":63.9,\"mem_used_gb\":32.8,\"mem_usage_pct\":51.4,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":11.6,\"net_tx_kbps\":6.5,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:17:06',NULL,'2026-09-21 13:17:06',0),(2101903605873930241,0,12,'2026-09-21 13:17:25','up',7,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:17:26',NULL,'2026-09-21 13:17:26',0),(2101903605936844801,0,13,'2026-09-21 13:17:25','up',19,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:17:26',NULL,'2026-09-21 13:17:26',0),(2101903648018296833,0,2101901965926232066,'2026-09-21 13:17:35','up',0,'{\"cpu_usage_pct\":9.4,\"mem_total_gb\":63.9,\"mem_used_gb\":31.4,\"mem_usage_pct\":49.2,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":7.7,\"net_tx_kbps\":3.4,\"uptime_hours\":75.0}',NULL,'2026-09-21 13:17:36',NULL,'2026-09-21 13:17:36',0),(2101903731958902785,0,12,'2026-09-21 13:17:55','up',7,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:17:56',NULL,'2026-09-21 13:17:56',0),(2101903732026011650,0,13,'2026-09-21 13:17:55','up',19,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:17:56',NULL,'2026-09-21 13:17:56',0),(2101903774191349761,0,2101901965926232066,'2026-09-21 13:18:05','up',0,'{\"cpu_usage_pct\":10.8,\"mem_total_gb\":63.9,\"mem_used_gb\":31.5,\"mem_usage_pct\":49.3,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":18.8,\"net_tx_kbps\":7.0,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:18:06',NULL,'2026-09-21 13:18:06',0),(2101903858110984193,0,12,'2026-09-21 13:18:25','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:18:26',NULL,'2026-09-21 13:18:26',0),(2101903858182287362,0,13,'2026-09-21 13:18:25','up',16,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:18:26',NULL,'2026-09-21 13:18:26',0),(2101903900255350785,0,2101901965926232066,'2026-09-21 13:18:35','up',0,'{\"cpu_usage_pct\":9.9,\"mem_total_gb\":63.9,\"mem_used_gb\":31.3,\"mem_usage_pct\":49.0,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":7.1,\"net_tx_kbps\":5.0,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:18:36',NULL,'2026-09-21 13:18:36',0),(2101903984191762434,0,12,'2026-09-21 13:18:55','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:18:56',NULL,'2026-09-21 13:18:56',0),(2101903984321785857,0,13,'2026-09-21 13:18:56','up',17,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:18:56',NULL,'2026-09-21 13:18:56',0),(2101904026340323330,0,2101901965926232066,'2026-09-21 13:19:06','up',0,'{\"cpu_usage_pct\":11.4,\"mem_total_gb\":63.9,\"mem_used_gb\":31.5,\"mem_usage_pct\":49.3,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":60.2,\"net_tx_kbps\":42.5,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:19:06',NULL,'2026-09-21 13:19:06',0),(2101904110297706497,0,12,'2026-09-21 13:19:26','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:19:26',NULL,'2026-09-21 13:19:26',0),(2101904110369009666,0,13,'2026-09-21 13:19:26','up',19,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:19:26',NULL,'2026-09-21 13:19:26',0),(2101904152458850306,0,2101901965926232066,'2026-09-21 13:19:36','up',0,'{\"cpu_usage_pct\":10.6,\"mem_total_gb\":63.9,\"mem_used_gb\":31.6,\"mem_usage_pct\":49.5,\"disk_usage_pct\":72.7,\"disk_detail\":\"C:\\\\ 72.7%，D:\\\\ 46.1%\",\"net_rx_kbps\":27.2,\"net_tx_kbps\":3.9,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:19:36',NULL,'2026-09-21 13:19:36',0),(2101904236382679041,0,12,'2026-09-21 13:19:56','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:19:56',NULL,'2026-09-21 13:19:56',0),(2101904236453982210,0,13,'2026-09-21 13:19:56','up',22,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:19:56',NULL,'2026-09-21 13:19:56',0),(2101904278514462722,0,2101901965926232066,'2026-09-21 13:20:06','up',0,'{\"cpu_usage_pct\":12.4,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.8,\"disk_detail\":\"C:\\\\ 72.8%，D:\\\\ 46.1%\",\"net_rx_kbps\":23.6,\"net_tx_kbps\":7.6,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:20:06',NULL,'2026-09-21 13:20:06',0),(2101904362488623105,0,12,'2026-09-21 13:20:26','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":3.0}',NULL,'2026-09-21 13:20:26',NULL,'2026-09-21 13:20:26',0),(2101904362555731969,0,13,'2026-09-21 13:20:26','up',25,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:20:26',NULL,'2026-09-21 13:20:26',0),(2101904404632989697,0,2101901965926232066,'2026-09-21 13:20:36','up',0,'{\"cpu_usage_pct\":7.9,\"mem_total_gb\":63.9,\"mem_used_gb\":30.2,\"mem_usage_pct\":47.4,\"disk_usage_pct\":72.9,\"disk_detail\":\"C:\\\\ 72.9%，D:\\\\ 46.1%\",\"net_rx_kbps\":7.5,\"net_tx_kbps\":4.2,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:20:36',NULL,'2026-09-21 13:20:36',0),(2101904488586178562,0,12,'2026-09-21 13:20:56','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":4.1}',NULL,'2026-09-21 13:20:56',NULL,'2026-09-21 13:20:56',0),(2101904488720396289,0,13,'2026-09-21 13:20:56','up',30,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:20:56',NULL,'2026-09-21 13:20:56',0),(2101904530814431233,0,2101901965926232066,'2026-09-21 13:21:06','up',0,'{\"cpu_usage_pct\":9.4,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.7,\"disk_usage_pct\":72.9,\"disk_detail\":\"C:\\\\ 72.9%，D:\\\\ 46.1%\",\"net_rx_kbps\":10.9,\"net_tx_kbps\":5.1,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:21:06',NULL,'2026-09-21 13:21:06',0),(2101904614780203009,0,12,'2026-09-21 13:21:26','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:21:26',NULL,'2026-09-21 13:21:26',0),(2101904615430320129,0,13,'2026-09-21 13:21:26','up',159,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:21:26',NULL,'2026-09-21 13:21:26',0),(2101904656903598082,0,2101901965926232066,'2026-09-21 13:21:36','up',0,'{\"cpu_usage_pct\":7.4,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.4,\"disk_usage_pct\":73.0,\"disk_detail\":\"C:\\\\ 73.0%，D:\\\\ 46.1%\",\"net_rx_kbps\":4.9,\"net_tx_kbps\":5.9,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:21:36',NULL,'2026-09-21 13:21:36',0),(2101904740919701506,0,12,'2026-09-21 13:21:56','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:21:56',NULL,'2026-09-21 13:21:56',0),(2101904740991004673,0,13,'2026-09-21 13:21:56','up',21,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:21:56',NULL,'2026-09-21 13:21:56',0),(2101904783076651010,0,2101901965926232066,'2026-09-21 13:22:06','up',0,'{\"cpu_usage_pct\":8.2,\"mem_total_gb\":63.9,\"mem_used_gb\":30.5,\"mem_usage_pct\":47.7,\"disk_usage_pct\":73.2,\"disk_detail\":\"C:\\\\ 73.2%，D:\\\\ 46.1%\",\"net_rx_kbps\":10.0,\"net_tx_kbps\":4.9,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:22:06',NULL,'2026-09-21 13:22:06',0),(2101904867029839874,0,12,'2026-09-21 13:22:26','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:22:26',NULL,'2026-09-21 13:22:26',0),(2101904867088560129,0,13,'2026-09-21 13:22:26','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:22:26',NULL,'2026-09-21 13:22:26',0),(2101904909149040642,0,2101901965926232066,'2026-09-21 13:22:36','up',0,'{\"cpu_usage_pct\":7.7,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.5,\"disk_usage_pct\":73.3,\"disk_detail\":\"C:\\\\ 73.3%，D:\\\\ 46.1%\",\"net_rx_kbps\":6.0,\"net_tx_kbps\":3.5,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:22:37',NULL,'2026-09-21 13:22:37',0),(2101904993093840898,0,12,'2026-09-21 13:22:56','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:22:57',NULL,'2026-09-21 13:22:57',0),(2101904993169338370,0,13,'2026-09-21 13:22:56','up',18,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:22:57',NULL,'2026-09-21 13:22:57',0),(2101905035288539137,0,2101901965926232066,'2026-09-21 13:23:06','up',0,'{\"cpu_usage_pct\":9.5,\"mem_total_gb\":63.9,\"mem_used_gb\":30.5,\"mem_usage_pct\":47.8,\"disk_usage_pct\":73.4,\"disk_detail\":\"C:\\\\ 73.4%，D:\\\\ 46.1%\",\"net_rx_kbps\":27.4,\"net_tx_kbps\":39.0,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:23:07',NULL,'2026-09-21 13:23:07',0),(2101905119313031169,0,12,'2026-09-21 13:23:26','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:23:27',NULL,'2026-09-21 13:23:27',0),(2101905119438860289,0,13,'2026-09-21 13:23:26','up',30,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:23:27',NULL,'2026-09-21 13:23:27',0),(2101905161457397762,0,2101901965926232066,'2026-09-21 13:23:36','up',0,'{\"cpu_usage_pct\":8.5,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.5,\"disk_usage_pct\":73.5,\"disk_detail\":\"C:\\\\ 73.5%，D:\\\\ 46.1%\",\"net_rx_kbps\":7.6,\"net_tx_kbps\":3.7,\"uptime_hours\":75.1}',NULL,'2026-09-21 13:23:37',NULL,'2026-09-21 13:23:37',0),(2101905245423169537,0,12,'2026-09-21 13:23:56','up',7,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:23:57',NULL,'2026-09-21 13:23:57',0),(2101905245481889794,0,13,'2026-09-21 13:23:56','up',21,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:23:57',NULL,'2026-09-21 13:23:57',0),(2101905287538176001,0,2101901965926232066,'2026-09-21 13:24:06','up',0,'{\"cpu_usage_pct\":8.1,\"mem_total_gb\":63.9,\"mem_used_gb\":30.5,\"mem_usage_pct\":47.8,\"disk_usage_pct\":73.5,\"disk_detail\":\"C:\\\\ 73.5%，D:\\\\ 46.1%\",\"net_rx_kbps\":19.0,\"net_tx_kbps\":15.1,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:24:07',NULL,'2026-09-21 13:24:07',0),(2101905371512336386,0,12,'2026-09-21 13:24:26','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:24:27',NULL,'2026-09-21 13:24:27',0),(2101905371579445250,0,13,'2026-09-21 13:24:26','up',18,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:24:27',NULL,'2026-09-21 13:24:27',0),(2101905413711228929,0,2101901965926232066,'2026-09-21 13:24:36','up',0,'{\"cpu_usage_pct\":7.0,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":24.9,\"net_tx_kbps\":4.4,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:24:37',NULL,'2026-09-21 13:24:37',0),(2101905497605697537,0,12,'2026-09-21 13:24:56','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:24:57',NULL,'2026-09-21 13:24:57',0),(2101905497718943746,0,13,'2026-09-21 13:24:56','up',31,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:24:57',NULL,'2026-09-21 13:24:57',0),(2101905539720704001,0,2101901965926232066,'2026-09-21 13:25:06','up',0,'{\"cpu_usage_pct\":10.1,\"mem_total_gb\":63.9,\"mem_used_gb\":30.5,\"mem_usage_pct\":47.8,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":5.0,\"net_tx_kbps\":2.7,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:25:07',NULL,'2026-09-21 13:25:07',0),(2101905623652921345,0,12,'2026-09-21 13:25:26','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:25:27',NULL,'2026-09-21 13:25:27',0),(2101905623715835906,0,13,'2026-09-21 13:25:26','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:25:27',NULL,'2026-09-21 13:25:27',0),(2101905665801482241,0,2101901965926232066,'2026-09-21 13:25:36','up',0,'{\"cpu_usage_pct\":8.5,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":13.0,\"net_tx_kbps\":5.3,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:25:37',NULL,'2026-09-21 13:25:37',0),(2101905749792419841,0,12,'2026-09-21 13:25:56','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":4.1}',NULL,'2026-09-21 13:25:57',NULL,'2026-09-21 13:25:57',0),(2101905749863723010,0,13,'2026-09-21 13:25:56','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:25:57',NULL,'2026-09-21 13:25:57',0),(2101905792033255425,0,2101901965926232066,'2026-09-21 13:26:06','up',0,'{\"cpu_usage_pct\":8.2,\"mem_total_gb\":63.9,\"mem_used_gb\":30.6,\"mem_usage_pct\":47.9,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":9.6,\"net_tx_kbps\":4.7,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:26:07',NULL,'2026-09-21 13:26:07',0),(2101905875931918338,0,12,'2026-09-21 13:26:27','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:26:27',NULL,'2026-09-21 13:26:27',0),(2101905876061941761,0,13,'2026-09-21 13:26:27','up',28,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:26:27',NULL,'2026-09-21 13:26:27',0),(2101905918160171010,0,2101901965926232066,'2026-09-21 13:26:37','up',0,'{\"cpu_usage_pct\":7.2,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.7,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":5.5,\"net_tx_kbps\":3.0,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:26:37',NULL,'2026-09-21 13:26:37',0),(2101906002100776962,0,12,'2026-09-21 13:26:57','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:26:57',NULL,'2026-09-21 13:26:57',0),(2101906002234994689,0,13,'2026-09-21 13:26:57','up',26,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:26:57',NULL,'2026-09-21 13:26:57',0),(2101906044236754946,0,2101901965926232066,'2026-09-21 13:27:07','up',0,'{\"cpu_usage_pct\":8.1,\"mem_total_gb\":63.9,\"mem_used_gb\":30.6,\"mem_usage_pct\":47.9,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":6.7,\"net_tx_kbps\":3.7,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:27:07',NULL,'2026-09-21 13:27:07',0),(2101906128227692545,0,12,'2026-09-21 13:27:27','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:27:27',NULL,'2026-09-21 13:27:27',0),(2101906128286412802,0,13,'2026-09-21 13:27:27','up',22,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:27:27',NULL,'2026-09-21 13:27:27',0),(2101906170338504706,0,2101901965926232066,'2026-09-21 13:27:37','up',0,'{\"cpu_usage_pct\":7.5,\"mem_total_gb\":63.9,\"mem_used_gb\":30.2,\"mem_usage_pct\":47.2,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":5.8,\"net_tx_kbps\":4.1,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:27:37',NULL,'2026-09-21 13:27:37',0),(2101906254329442305,0,12,'2026-09-21 13:27:57','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:27:57',NULL,'2026-09-21 13:27:57',0),(2101906254937616385,0,13,'2026-09-21 13:27:57','up',149,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:27:57',NULL,'2026-09-21 13:27:57',0),(2101906296461225986,0,2101901965926232066,'2026-09-21 13:28:07','up',0,'{\"cpu_usage_pct\":8.9,\"mem_total_gb\":63.9,\"mem_used_gb\":30.2,\"mem_usage_pct\":47.4,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":10.8,\"net_tx_kbps\":4.7,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:28:07',NULL,'2026-09-21 13:28:07',0),(2101906380435386370,0,12,'2026-09-21 13:28:27','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:28:27',NULL,'2026-09-21 13:28:27',0),(2101906380498300930,0,13,'2026-09-21 13:28:27','up',18,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:28:27',NULL,'2026-09-21 13:28:27',0),(2101906422625890305,0,2101901965926232066,'2026-09-21 13:28:37','up',0,'{\"cpu_usage_pct\":8.4,\"mem_total_gb\":63.9,\"mem_used_gb\":30.1,\"mem_usage_pct\":47.2,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":17.6,\"net_tx_kbps\":10.0,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:28:37',NULL,'2026-09-21 13:28:37',0),(2101906506570690562,0,12,'2026-09-21 13:28:57','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:28:57',NULL,'2026-09-21 13:28:57',0),(2101906506633605122,0,13,'2026-09-21 13:28:57','up',18,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:28:57',NULL,'2026-09-21 13:28:57',0),(2101906548706668546,0,2101901965926232066,'2026-09-21 13:29:07','up',0,'{\"cpu_usage_pct\":7.7,\"mem_total_gb\":63.9,\"mem_used_gb\":30.2,\"mem_usage_pct\":47.4,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":5.1,\"net_tx_kbps\":2.3,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:29:07',NULL,'2026-09-21 13:29:07',0),(2101906632651468802,0,12,'2026-09-21 13:29:27','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:29:27',NULL,'2026-09-21 13:29:27',0),(2101906632714383361,0,13,'2026-09-21 13:29:27','up',25,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:29:27',NULL,'2026-09-21 13:29:27',0),(2101906674829389826,0,2101901965926232066,'2026-09-21 13:29:37','up',0,'{\"cpu_usage_pct\":8.6,\"mem_total_gb\":63.9,\"mem_used_gb\":30.1,\"mem_usage_pct\":47.1,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":25.0,\"net_tx_kbps\":3.4,\"uptime_hours\":75.2}',NULL,'2026-09-21 13:29:37',NULL,'2026-09-21 13:29:37',0),(2101906758774190081,0,12,'2026-09-21 13:29:57','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:29:57',NULL,'2026-09-21 13:29:57',0),(2101906758853881857,0,13,'2026-09-21 13:29:57','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:29:58',NULL,'2026-09-21 13:29:58',0),(2101906800943722498,0,2101901965926232066,'2026-09-21 13:30:07','up',0,'{\"cpu_usage_pct\":8.4,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.4,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":10.2,\"net_tx_kbps\":4.5,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:30:08',NULL,'2026-09-21 13:30:08',0),(2101906884896911362,0,12,'2026-09-21 13:30:27','up',7,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:30:28',NULL,'2026-09-21 13:30:28',0),(2101906885035323394,0,13,'2026-09-21 13:30:27','up',34,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:30:28',NULL,'2026-09-21 13:30:28',0),(2101906927062249473,0,2101901965926232066,'2026-09-21 13:30:37','up',0,'{\"cpu_usage_pct\":8.4,\"mem_total_gb\":63.9,\"mem_used_gb\":30.1,\"mem_usage_pct\":47.2,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":37.4,\"net_tx_kbps\":32.3,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:30:38',NULL,'2026-09-21 13:30:38',0),(2101907011032215554,0,12,'2026-09-21 13:30:57','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":4.1}',NULL,'2026-09-21 13:30:58',NULL,'2026-09-21 13:30:58',0),(2101907011095130113,0,13,'2026-09-21 13:30:57','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:30:58',NULL,'2026-09-21 13:30:58',0),(2101907053151416321,0,2101901965926232066,'2026-09-21 13:31:07','up',0,'{\"cpu_usage_pct\":9.1,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.4,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":58.4,\"net_tx_kbps\":60.1,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:31:08',NULL,'2026-09-21 13:31:08',0),(2101907137112993793,0,12,'2026-09-21 13:31:27','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:31:28',NULL,'2026-09-21 13:31:28',0),(2101907137171714050,0,13,'2026-09-21 13:31:27','up',21,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:31:28',NULL,'2026-09-21 13:31:28',0),(2101907179278331906,0,2101901965926232066,'2026-09-21 13:31:37','up',0,'{\"cpu_usage_pct\":7.3,\"mem_total_gb\":63.9,\"mem_used_gb\":30.1,\"mem_usage_pct\":47.1,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":8.9,\"net_tx_kbps\":8.3,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:31:38',NULL,'2026-09-21 13:31:38',0),(2101907263231520769,0,12,'2026-09-21 13:31:57','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:31:58',NULL,'2026-09-21 13:31:58',0),(2101907263361544194,0,13,'2026-09-21 13:31:57','up',25,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:31:58',NULL,'2026-09-21 13:31:58',0),(2101907305455579137,0,2101901965926232066,'2026-09-21 13:32:07','up',0,'{\"cpu_usage_pct\":9.1,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.5,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":15.1,\"net_tx_kbps\":8.1,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:32:08',NULL,'2026-09-21 13:32:08',0),(2101907389333270529,0,12,'2026-09-21 13:32:27','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:32:28',NULL,'2026-09-21 13:32:28',0),(2101907389400379394,0,13,'2026-09-21 13:32:27','up',21,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:32:28',NULL,'2026-09-21 13:32:28',0),(2101907431456665602,0,2101901965926232066,'2026-09-21 13:32:37','up',0,'{\"cpu_usage_pct\":7.9,\"mem_total_gb\":63.9,\"mem_used_gb\":30.1,\"mem_usage_pct\":47.2,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":5.4,\"net_tx_kbps\":2.7,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:32:38',NULL,'2026-09-21 13:32:38',0),(2101907515376300033,0,12,'2026-09-21 13:32:57','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.8}',NULL,'2026-09-21 13:32:58',NULL,'2026-09-21 13:32:58',0),(2101907515506323458,0,13,'2026-09-21 13:32:57','up',31,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:32:58',NULL,'2026-09-21 13:32:58',0),(2101907557596164098,0,2101901965926232066,'2026-09-21 13:33:07','up',0,'{\"cpu_usage_pct\":8.3,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":5.4,\"net_tx_kbps\":2.5,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:33:08',NULL,'2026-09-21 13:33:08',0),(2101907641486438402,0,12,'2026-09-21 13:33:27','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:33:28',NULL,'2026-09-21 13:33:28',0),(2101907641557741569,0,13,'2026-09-21 13:33:27','up',21,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:33:28',NULL,'2026-09-21 13:33:28',0),(2101907683614027778,0,2101901965926232066,'2026-09-21 13:33:37','up',0,'{\"cpu_usage_pct\":8.0,\"mem_total_gb\":63.9,\"mem_used_gb\":30.2,\"mem_usage_pct\":47.4,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":6.6,\"net_tx_kbps\":5.0,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:33:38',NULL,'2026-09-21 13:33:38',0),(2101907767571410945,0,12,'2026-09-21 13:33:58','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:33:58',NULL,'2026-09-21 13:33:58',0),(2101907767642714114,0,13,'2026-09-21 13:33:58','up',29,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:33:58',NULL,'2026-09-21 13:33:58',0),(2101907809686417410,0,2101901965926232066,'2026-09-21 13:34:08','up',0,'{\"cpu_usage_pct\":8.6,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.7,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":9.2,\"net_tx_kbps\":7.1,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:34:08',NULL,'2026-09-21 13:34:08',0),(2101907893639606273,0,12,'2026-09-21 13:34:28','up',4,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:34:28',NULL,'2026-09-21 13:34:28',0),(2101907893694132225,0,13,'2026-09-21 13:34:28','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:34:28',NULL,'2026-09-21 13:34:28',0),(2101907935792361474,0,2101901965926232066,'2026-09-21 13:34:38','up',0,'{\"cpu_usage_pct\":7.8,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.4,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":36.3,\"net_tx_kbps\":7.4,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:34:38',NULL,'2026-09-21 13:34:38',0),(2101908019716190210,0,12,'2026-09-21 13:34:58','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.6}',NULL,'2026-09-21 13:34:58',NULL,'2026-09-21 13:34:58',0),(2101908019783299074,0,13,'2026-09-21 13:34:58','up',19,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:34:58',NULL,'2026-09-21 13:34:58',0),(2101908061940248578,0,2101901965926232066,'2026-09-21 13:35:08','up',0,'{\"cpu_usage_pct\":7.5,\"mem_total_gb\":63.9,\"mem_used_gb\":30.5,\"mem_usage_pct\":47.7,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":5.1,\"net_tx_kbps\":3.0,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:35:08',NULL,'2026-09-21 13:35:08',0),(2101908145876660225,0,12,'2026-09-21 13:35:28','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":2.5}',NULL,'2026-09-21 13:35:28',NULL,'2026-09-21 13:35:28',0),(2101908146006683649,0,13,'2026-09-21 13:35:28','up',37,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:35:28',NULL,'2026-09-21 13:35:28',0),(2101908188079747073,0,2101901965926232066,'2026-09-21 13:35:38','up',0,'{\"cpu_usage_pct\":8.7,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.5,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":8.4,\"net_tx_kbps\":7.7,\"uptime_hours\":75.3}',NULL,'2026-09-21 13:35:38',NULL,'2026-09-21 13:35:38',0),(2101908271957438466,0,12,'2026-09-21 13:35:58','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":4.2}',NULL,'2026-09-21 13:35:58',NULL,'2026-09-21 13:35:58',0),(2101908272020353025,0,13,'2026-09-21 13:35:58','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:35:58',NULL,'2026-09-21 13:35:58',0),(2101908314198274049,0,2101901965926232066,'2026-09-21 13:36:08','up',0,'{\"cpu_usage_pct\":10.6,\"mem_total_gb\":63.9,\"mem_used_gb\":30.5,\"mem_usage_pct\":47.8,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":11.7,\"net_tx_kbps\":5.3,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:36:08',NULL,'2026-09-21 13:36:08',0),(2101908398138880002,0,12,'2026-09-21 13:36:28','up',6,'{\"threads_connected\":\"14\",\"max_connections\":\"151\",\"qps\":6.5}',NULL,'2026-09-21 13:36:28',NULL,'2026-09-21 13:36:28',0),(2101908398264709122,0,13,'2026-09-21 13:36:28','up',27,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:36:28',NULL,'2026-09-21 13:36:28',0),(2101908440350355457,0,2101901965926232066,'2026-09-21 13:36:38','up',0,'{\"cpu_usage_pct\":32.1,\"mem_total_gb\":63.9,\"mem_used_gb\":30.5,\"mem_usage_pct\":47.7,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":20.7,\"net_tx_kbps\":19.7,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:36:38',NULL,'2026-09-21 13:36:38',0),(2101908524274184194,0,12,'2026-09-21 13:36:58','up',4,'{\"threads_connected\":\"23\",\"max_connections\":\"151\",\"qps\":27.6}',NULL,'2026-09-21 13:36:58',NULL,'2026-09-21 13:36:58',0),(2101908524337098753,0,13,'2026-09-21 13:36:58','up',20,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:36:58',NULL,'2026-09-21 13:36:58',0),(2101908566519214081,0,2101901965926232066,'2026-09-21 13:37:08','up',0,'{\"cpu_usage_pct\":23.7,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":8.3,\"net_tx_kbps\":12.8,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:37:08',NULL,'2026-09-21 13:37:08',0),(2101908650484985858,0,12,'2026-09-21 13:37:28','up',5,'{\"threads_connected\":\"23\",\"max_connections\":\"151\",\"qps\":26.6}',NULL,'2026-09-21 13:37:29',NULL,'2026-09-21 13:37:29',0),(2101908650552094721,0,13,'2026-09-21 13:37:28','up',22,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.6,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:37:29',NULL,'2026-09-21 13:37:29',0),(2101908692675489794,0,2101901965926232066,'2026-09-21 13:37:38','up',0,'{\"cpu_usage_pct\":13.5,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":21.3,\"net_tx_kbps\":15.2,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:37:39',NULL,'2026-09-21 13:37:39',0),(2101908776561569794,0,12,'2026-09-21 13:37:58','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":8.8}',NULL,'2026-09-21 13:37:59',NULL,'2026-09-21 13:37:59',0),(2101908776624484354,0,13,'2026-09-21 13:37:58','up',23,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"5\",\"hit_rate_pct\":92.5,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:37:59',NULL,'2026-09-21 13:37:59',0),(2101908818794016770,0,2101901965926232066,'2026-09-21 13:38:08','up',0,'{\"cpu_usage_pct\":11.7,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":20.9,\"net_tx_kbps\":22.0,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:38:09',NULL,'2026-09-21 13:38:09',0),(2101908902793342977,0,12,'2026-09-21 13:38:28','up',6,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":16.5}',NULL,'2026-09-21 13:38:29',NULL,'2026-09-21 13:38:29',0),(2101908902860451841,0,13,'2026-09-21 13:38:28','up',23,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.5,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:38:29',NULL,'2026-09-21 13:38:29',0),(2101908945101287426,0,2101901965926232066,'2026-09-21 13:38:38','up',0,'{\"cpu_usage_pct\":12.0,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.5,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":9.0,\"net_tx_kbps\":16.3,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:38:39',NULL,'2026-09-21 13:38:39',0),(2101909028953812994,0,12,'2026-09-21 13:38:58','up',5,'{\"threads_connected\":\"13\",\"max_connections\":\"151\",\"qps\":58.9}',NULL,'2026-09-21 13:38:59',NULL,'2026-09-21 13:38:59',0),(2101909029016727553,0,13,'2026-09-21 13:38:58','up',19,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.5,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:38:59',NULL,'2026-09-21 13:38:59',0),(2101909071140122626,0,2101901965926232066,'2026-09-21 13:39:08','up',0,'{\"cpu_usage_pct\":14.7,\"mem_total_gb\":63.9,\"mem_used_gb\":30.3,\"mem_usage_pct\":47.5,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":17.9,\"net_tx_kbps\":21.0,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:39:09',NULL,'2026-09-21 13:39:09',0),(2101909155034591234,0,12,'2026-09-21 13:39:28','up',6,'{\"threads_connected\":\"23\",\"max_connections\":\"151\",\"qps\":7.1}',NULL,'2026-09-21 13:39:29',NULL,'2026-09-21 13:39:29',0),(2101909155160420353,0,13,'2026-09-21 13:39:28','up',23,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.5,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:39:29',NULL,'2026-09-21 13:39:29',0),(2101909197279621122,0,2101901965926232066,'2026-09-21 13:39:38','up',0,'{\"cpu_usage_pct\":13.2,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":41.6,\"net_tx_kbps\":23.1,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:39:39',NULL,'2026-09-21 13:39:39',0),(2101909281216032769,0,12,'2026-09-21 13:39:58','up',4,'{\"threads_connected\":\"23\",\"max_connections\":\"151\",\"qps\":3.0}',NULL,'2026-09-21 13:39:59',NULL,'2026-09-21 13:39:59',0),(2101909281283141634,0,13,'2026-09-21 13:39:58','up',16,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.5,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:39:59',NULL,'2026-09-21 13:39:59',0),(2101909323645612033,0,2101901965926232066,'2026-09-21 13:40:09','up',0,'{\"cpu_usage_pct\":13.8,\"mem_total_gb\":63.9,\"mem_used_gb\":30.4,\"mem_usage_pct\":47.6,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":71.3,\"net_tx_kbps\":368.3,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:40:09',NULL,'2026-09-21 13:40:09',0),(2101909407288422401,0,12,'2026-09-21 13:40:28','up',5,'{\"threads_connected\":\"23\",\"max_connections\":\"151\",\"qps\":2.7}',NULL,'2026-09-21 13:40:29',NULL,'2026-09-21 13:40:29',0),(2101909407418445826,0,13,'2026-09-21 13:40:28','up',25,'{\"used_memory_mb\":1.5,\"connected_clients\":\"29\",\"ops_per_sec\":\"0\",\"hit_rate_pct\":92.5,\"redis_version\":\"7.4.11\"}',NULL,'2026-09-21 13:40:29',NULL,'2026-09-21 13:40:29',0),(2101909491686207490,0,2101901965926232066,'2026-09-21 13:40:49','up',0,'{\"cpu_usage_pct\":19.1,\"mem_total_gb\":63.9,\"mem_used_gb\":30.6,\"mem_usage_pct\":47.9,\"disk_usage_pct\":72.6,\"disk_detail\":\"C:\\\\ 72.6%，D:\\\\ 46.1%\",\"net_rx_kbps\":119.1,\"net_tx_kbps\":420.8,\"uptime_hours\":75.4}',NULL,'2026-09-21 13:40:49',NULL,'2026-09-21 13:40:49',0);
/*!40000 ALTER TABLE `mon_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_setting`
--

DROP TABLE IF EXISTS `mon_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_setting` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `config_key` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键（notify）',
  `config_value` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置值 JSON（NotifySetting）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mon_setting_key` (`tenant_id`,`config_key`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控服务配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_setting`
--

LOCK TABLES `mon_setting` WRITE;
/*!40000 ALTER TABLE `mon_setting` DISABLE KEYS */;
INSERT INTO `mon_setting` VALUES (1,0,'notify','{\"inboxEnabled\":true,\"inboxUsers\":[{\"id\":1,\"name\":\"admin\"}],\"webhookEnabled\":false,\"webhookUrl\":\"\",\"webhookFormat\":\"dingtalk\",\"repeatNotifyMinutes\":0}',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0);
/*!40000 ALTER TABLE `mon_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_target`
--

DROP TABLE IF EXISTS `mon_target`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_target` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标名称',
  `target_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：service/http/mysql/redis/rocketmq/os',
  `service_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务/HTTP 地址（http://host:port）',
  `host` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主机（mysql/redis/rocketmq；os 固定本机）',
  `port` int DEFAULT NULL COMMENT '端口',
  `username` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认证用户名',
  `password` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认证密码（界面脱敏）',
  `collect_interval` int DEFAULT '30' COMMENT '采集间隔（秒）',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0 启用 1 停用',
  `alert_config` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警规则 JSON：{down,latencyMs,cpuPercent,memPercent,diskPercent,redisMemPercent,mysqlMaxThreads}',
  `status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'unknown' COMMENT '最近状态：up/down/unknown',
  `status_time` datetime DEFAULT NULL COMMENT '最近状态时间',
  `last_latency_ms` int DEFAULT NULL COMMENT '最近响应耗时（ms）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_target_type` (`target_type`),
  KEY `idx_mon_target_status` (`enabled`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控目标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_target`
--

LOCK TABLES `mon_target` WRITE;
/*!40000 ALTER TABLE `mon_target` DISABLE KEYS */;
INSERT INTO `mon_target` VALUES (1,0,'mse-gateway','service','http://localhost:48080',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'网关',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(2,0,'mse-auth','service','http://localhost:48083',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'认证服务',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(3,0,'mse-system','service','http://localhost:48081',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'系统管理服务',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(4,0,'mse-tenant','service','http://localhost:48082',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'租户管理服务',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(5,0,'mse-oa','service','http://localhost:8090',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'协同办公',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(6,0,'mse-mp','service','http://localhost:8091',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'微信公众号',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(7,0,'mse-open','service','http://localhost:8092',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'开放平台',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(8,0,'mse-data','service','http://localhost:8093',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'数据中台',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(9,0,'mse-ai','service','http://localhost:8094',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'AI 智能体平台',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(10,0,'mse-fs','service','http://localhost:8095',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'文件存储',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(11,0,'mse-search','service','http://localhost:8096',NULL,NULL,NULL,NULL,30,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'搜索引擎',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(12,0,'MySQL 主库','mysql',NULL,'localhost',3306,'root','123456',30,0,'{\"down\":true,\"mysqlMaxThreads\":500}','up','2026-09-21 13:40:28',5,'业务/基座库（默认启用）',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:40:29',0),(13,0,'Redis 缓存','redis',NULL,'localhost',6379,NULL,NULL,30,0,'{\"down\":true,\"redisMemPercent\":90}','up','2026-09-21 13:40:28',25,'缓存/会话（默认启用）',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:40:29',0),(14,0,'Nacos','http','http://localhost:18080',NULL,NULL,NULL,NULL,60,1,'{\"down\":true,\"latencyMs\":3000}','unknown',NULL,NULL,'注册/配置中心（控制台端口，按需启用）',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(15,0,'Elasticsearch','http','http://localhost:9200/_cluster/health',NULL,NULL,NULL,NULL,60,1,'{\"down\":true}','unknown',NULL,NULL,'搜索引擎中间件（按需启用）',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(16,0,'RocketMQ','rocketmq',NULL,'localhost',9876,NULL,NULL,60,1,'{\"down\":true}','unknown',NULL,NULL,'消息队列 namesrv（按需启用）',NULL,'2026-09-21 13:10:47',NULL,'2026-09-21 13:10:47',0),(2101901965926232066,0,'本机操作系统','os',NULL,'127.0.0.1',NULL,NULL,NULL,30,0,NULL,'up','2026-09-21 13:40:49',0,'monitor 所在主机（OSHI 自动创建）',NULL,'2026-09-21 13:10:55',NULL,'2026-09-21 13:40:49',0);
/*!40000 ALTER TABLE `mon_target` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_account`
--

DROP TABLE IF EXISTS `mp_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_account` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `app_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公众号 AppID（全局唯一：回调按此路由）',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公众号名称',
  `type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'subscription' COMMENT 'subscription 订阅号 / service 服务号',
  `verified` tinyint NOT NULL DEFAULT '0' COMMENT '是否已认证：0 否 1 是',
  `app_secret` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'AppSecret 密文（AES-GCM）',
  `callback_token` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器配置 Token 密文',
  `aes_key` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'EncodingAESKey 密文',
  `encrypt_mode` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'safe' COMMENT 'plain/compatible/safe',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_app_id` (`app_id`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公众号账号';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_account`
--

LOCK TABLES `mp_account` WRITE;
/*!40000 ALTER TABLE `mp_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_fan`
--

DROP TABLE IF EXISTS `mp_fan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_fan` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `open_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '粉丝 OpenID',
  `subscribed` tinyint NOT NULL DEFAULT '1' COMMENT '1 关注中 0 已取关',
  `subscribe_time` datetime DEFAULT NULL COMMENT '关注时间',
  `unsubscribe_time` datetime DEFAULT NULL COMMENT '取关时间',
  `subscribe_scene` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关注场景（ADD_SCENE_QR_CODE 等）',
  `nickname` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '粉丝昵称',
  `head_img_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像地址',
  `sex` tinyint NOT NULL DEFAULT '0' COMMENT '1 男 2 女 0 未知',
  `province` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省份',
  `city` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市',
  `union_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开放平台 UnionID',
  `profile_incomplete` tinyint NOT NULL DEFAULT '0' COMMENT '1 = 资料拉取失败（未认证号常见）',
  `last_active_time` datetime DEFAULT NULL COMMENT '最近互动时间',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account_openid` (`account_id`,`open_id`),
  KEY `idx_tenant_sub` (`tenant_id`,`subscribed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='粉丝（回调事件自动维护，管理端只读）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_fan`
--

LOCK TABLES `mp_fan` WRITE;
/*!40000 ALTER TABLE `mp_fan` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_fan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_menu`
--

DROP TABLE IF EXISTS `mp_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_menu` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `menu_json` longtext COLLATE utf8mb4_general_ci COMMENT '与微信 menu/create 同构 JSON',
  `synced_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿有改动 1 已同步',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account` (`account_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义菜单草稿（每号一份）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_menu`
--

LOCK TABLES `mp_menu` WRITE;
/*!40000 ALTER TABLE `mp_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_message`
--

DROP TABLE IF EXISTS `mp_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_message` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `open_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '粉丝 OpenID',
  `direction` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'in' COMMENT 'in 收 / out 发（预留）',
  `kind` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'event / message',
  `type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'subscribe/click/text/image/…（归一化小写）',
  `msg_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信消息 id（事件类为空）',
  `dedup_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等锚点 MD5（挡微信重试）',
  `payload` longtext COLLATE utf8mb4_general_ci COMMENT '归一化载荷 JSON（snake_case）',
  `raw_xml` longtext COLLATE utf8mb4_general_ci COMMENT '解密后明文报文（超长截断）',
  `reply_content` longtext COLLATE utf8mb4_general_ci COMMENT '被动回复缓存（明文 XML，重试复用）',
  `trace_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链路追踪ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_dedup` (`dedup_key`),
  KEY `idx_tenant_account` (`tenant_id`,`account_id`,`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息/事件流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_message`
--

LOCK TABLES `mp_message` WRITE;
/*!40000 ALTER TABLE `mp_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_reply_rule`
--

DROP TABLE IF EXISTS `mp_reply_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_reply_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名',
  `match_type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'subscribe/unsubscribe/click/msg_any/keyword_exact/keyword_contains/default',
  `match_value` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单 Key / 关键词（, 空格 | 分隔多值）',
  `reply_type` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'text' COMMENT 'text / news',
  `reply_content` longtext COLLATE utf8mb4_general_ci COMMENT '{"content":…} 或 {"items":[…≤8]}',
  `priority` int NOT NULL DEFAULT '0' COMMENT '同类型内大者先',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用（0 停用 1 启用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_account` (`tenant_id`,`account_id`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动回复规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_reply_rule`
--

LOCK TABLES `mp_reply_rule` WRITE;
/*!40000 ALTER TABLE `mp_reply_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_reply_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_token`
--

DROP TABLE IF EXISTS `mp_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_token` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `token` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'token 密文（运行期内存缓存为主）',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  `err_code` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败错误码（成功时清空）',
  `err_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败错误信息（成功时清空）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account` (`account_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='access_token 落库状态（失败原因供管理端排查）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_token`
--

LOCK TABLES `mp_token` WRITE;
/*!40000 ALTER TABLE `mp_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_ai_agent_map`
--

DROP TABLE IF EXISTS `oa_ai_agent_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_ai_agent_map` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `scene` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '场景（assist 审批助手 / process 流程生成 / fill 智能填单）',
  `agent_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '映射的智能体编码（ai_agent.agent_code；空串=使用内置默认）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_scene` (`tenant_id`,`scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 场景 → 智能体映射（租户隔离；保存即生效，不删行）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_ai_agent_map`
--

LOCK TABLES `oa_ai_agent_map` WRITE;
/*!40000 ALTER TABLE `oa_ai_agent_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_ai_agent_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_cc`
--

DROP TABLE IF EXISTS `oa_cc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_cc` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `node_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '抄送节点编号',
  `node_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '抄送节点名称',
  `user_id` bigint NOT NULL COMMENT '被抄送人',
  `title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程标题快照',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='抄送记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_cc`
--

LOCK TABLES `oa_cc` WRITE;
/*!40000 ALTER TABLE `oa_cc` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_cc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_delegate_proxy`
--

DROP TABLE IF EXISTS `oa_delegate_proxy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_delegate_proxy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '委托人',
  `proxy_user_id` bigint NOT NULL COMMENT '受托人',
  `start_time` datetime NOT NULL COMMENT '生效开始时间',
  `end_time` datetime NOT NULL COMMENT '生效结束时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 0 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '委托说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_proxy_user` (`tenant_id`,`proxy_user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批委托代理（v1 全流程代理，不区分流程模板）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_delegate_proxy`
--

LOCK TABLES `oa_delegate_proxy` WRITE;
/*!40000 ALTER TABLE `oa_delegate_proxy` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_delegate_proxy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_dept_leader`
--

DROP TABLE IF EXISTS `oa_dept_leader`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_dept_leader` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `user_id` bigint NOT NULL COMMENT '负责人用户编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_dept_user` (`tenant_id`,`dept_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门负责人（基座 sys_dept 无负责人字段，OA 侧扩展）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_dept_leader`
--

LOCK TABLES `oa_dept_leader` WRITE;
/*!40000 ALTER TABLE `oa_dept_leader` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_dept_leader` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_file`
--

DROP TABLE IF EXISTS `oa_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_file` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小（字节）',
  `ext` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件扩展名',
  `path` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储路径（local 模式；fs 模式为占位符 -）',
  `fs_file_id` bigint DEFAULT NULL COMMENT 'mse-fs 文件编号（storage_type=fs 时物理寻址键）',
  `storage_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'local' COMMENT '存储模式：local 本地磁盘 / fs mse-fs',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='附件元数据（local 落 mse.oa.file-dir / fs 存 mse-fs）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_file`
--

LOCK TABLES `oa_file` WRITE;
/*!40000 ALTER TABLE `oa_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form`
--

DROP TABLE IF EXISTS `oa_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `category_id` bigint DEFAULT NULL COMMENT '分类编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '表单名称',
  `icon` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `config` longtext COLLATE utf8mb4_general_ci COMMENT '设计器文档 JSON（草稿态）',
  `version` int NOT NULL DEFAULT '0' COMMENT '已发布版本号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿 1 已发布 2 停用',
  `source` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CUSTOM' COMMENT '来源：SYSTEM 内置模板 CUSTOM 自定义',
  `template_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内置模板标识（SYSTEM 行，租户内唯一）',
  `template_version` int DEFAULT NULL COMMENT '安装时的模板版本号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_template` (`tenant_id`,`template_code`),
  KEY `idx_tenant` (`tenant_id`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form`
--

LOCK TABLES `oa_form` WRITE;
/*!40000 ALTER TABLE `oa_form` DISABLE KEYS */;
INSERT INTO `oa_form` VALUES (9001,1,9000,'请假申请','Calendar','内置示例：小时级请假，调休类型联动余额校验','{\"fields\":[{\"id\":\"field_type\",\"type\":\"radio\",\"label\":\"请假类型\",\"required\":true,\"options\":[\"事假\",\"病假\",\"年假\",\"调休\",\"婚假\",\"产假\",\"陪产假\",\"丧假\",\"其他\"]},{\"id\":\"field_start\",\"type\":\"datetime\",\"label\":\"开始时间\",\"required\":true},{\"id\":\"field_end\",\"type\":\"datetime\",\"label\":\"结束时间\",\"required\":true},{\"id\":\"field_hours\",\"type\":\"number\",\"label\":\"请假时长(小时)\",\"required\":true,\"compute\":{\"type\":\"durationHours\",\"startField\":\"field_start\",\"endField\":\"field_end\"}},{\"id\":\"field_days\",\"type\":\"number\",\"label\":\"请假天数(天)\",\"compute\":{\"type\":\"durationDays\",\"startField\":\"field_start\",\"endField\":\"field_end\"}},{\"id\":\"field_balance\",\"type\":\"balance\",\"label\":\"可调休余额\"},{\"id\":\"field_reason\",\"type\":\"textarea\",\"label\":\"请假事由\",\"required\":true},{\"id\":\"field_file\",\"type\":\"attachment\",\"label\":\"证明附件\",\"required\":false}],\"rules\":[{\"id\":\"rule_comp_balance\",\"conditions\":[{\"field\":\"field_type\",\"op\":\"EQ\",\"value\":\"调休\"}],\"actions\":[{\"target\":\"field_balance\",\"effect\":\"show\"}]}]}',1,1,'SYSTEM','leave',3,NULL,'2026-09-21 13:10:49',NULL,'2026-09-21 13:10:49',0);
/*!40000 ALTER TABLE `oa_form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_category`
--

DROP TABLE IF EXISTS `oa_form_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_category` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单/流程分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_category`
--

LOCK TABLES `oa_form_category` WRITE;
/*!40000 ALTER TABLE `oa_form_category` DISABLE KEYS */;
INSERT INTO `oa_form_category` VALUES (9000,1,'人事行政',1,NULL,'2026-09-21 13:10:49',NULL,'2026-09-21 13:10:49',0);
/*!40000 ALTER TABLE `oa_form_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_data`
--

DROP TABLE IF EXISTS `oa_form_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_data` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `form_id` bigint NOT NULL COMMENT '表单编号',
  `form_version` int DEFAULT NULL COMMENT '提交时的表单版本号',
  `instance_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联流程实例（独立提交为空）',
  `summary` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据摘要',
  `data_values` longtext COLLATE utf8mb4_general_ci COMMENT '控件数据 JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_form` (`tenant_id`,`form_id`),
  KEY `idx_instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单提交数据（JSON 存储）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_data`
--

LOCK TABLES `oa_form_data` WRITE;
/*!40000 ALTER TABLE `oa_form_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_version`
--

DROP TABLE IF EXISTS `oa_form_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `form_id` bigint NOT NULL COMMENT '表单编号',
  `version` int NOT NULL COMMENT '版本号',
  `config` longtext COLLATE utf8mb4_general_ci COMMENT '设计器文档 JSON 快照',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_form_version` (`tenant_id`,`form_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单发布版本快照（在途流程渲染不受后续编辑影响）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_version`
--

LOCK TABLES `oa_form_version` WRITE;
/*!40000 ALTER TABLE `oa_form_version` DISABLE KEYS */;
INSERT INTO `oa_form_version` VALUES (9002,1,9001,1,'{\"fields\":[{\"id\":\"field_type\",\"type\":\"radio\",\"label\":\"请假类型\",\"required\":true,\"options\":[\"事假\",\"病假\",\"年假\",\"调休\",\"婚假\",\"产假\",\"陪产假\",\"丧假\",\"其他\"]},{\"id\":\"field_start\",\"type\":\"datetime\",\"label\":\"开始时间\",\"required\":true},{\"id\":\"field_end\",\"type\":\"datetime\",\"label\":\"结束时间\",\"required\":true},{\"id\":\"field_hours\",\"type\":\"number\",\"label\":\"请假时长(小时)\",\"required\":true,\"compute\":{\"type\":\"durationHours\",\"startField\":\"field_start\",\"endField\":\"field_end\"}},{\"id\":\"field_days\",\"type\":\"number\",\"label\":\"请假天数(天)\",\"compute\":{\"type\":\"durationDays\",\"startField\":\"field_start\",\"endField\":\"field_end\"}},{\"id\":\"field_balance\",\"type\":\"balance\",\"label\":\"可调休余额\"},{\"id\":\"field_reason\",\"type\":\"textarea\",\"label\":\"请假事由\",\"required\":true},{\"id\":\"field_file\",\"type\":\"attachment\",\"label\":\"证明附件\",\"required\":false}],\"rules\":[{\"id\":\"rule_comp_balance\",\"conditions\":[{\"field\":\"field_type\",\"op\":\"EQ\",\"value\":\"调休\"}],\"actions\":[{\"target\":\"field_balance\",\"effect\":\"show\"}]}]}',NULL,'2026-09-21 13:10:49',NULL,'2026-09-21 13:10:49',0);
/*!40000 ALTER TABLE `oa_form_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_model`
--

DROP TABLE IF EXISTS `oa_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `category_id` bigint DEFAULT NULL COMMENT '分类编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程名称',
  `icon` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `form_id` bigint DEFAULT NULL COMMENT '绑定表单',
  `design_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SIMPLE' COMMENT '设计方式：SIMPLE 简易设计器 / BPMN 自由设计',
  `simple_config` longtext COLLATE utf8mb4_general_ci COMMENT '仿钉钉设计器节点树 JSON',
  `bpmn_config` longtext COLLATE utf8mb4_general_ci COMMENT 'BPMN 设计 XML（design_type=BPMN 时使用）',
  `proc_def_key` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程 key（oa_+id）',
  `deploy_version` int DEFAULT NULL COMMENT '最新部署版本（展示用）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿 1 已发布 2 停用',
  `source` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CUSTOM' COMMENT '来源：SYSTEM 内置模板 CUSTOM 自定义',
  `template_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内置模板标识（SYSTEM 行，租户内唯一）',
  `template_version` int DEFAULT NULL COMMENT '安装时的模板版本号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_template` (`tenant_id`,`template_code`),
  KEY `idx_tenant` (`tenant_id`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程模型（部署后版本存 ACT_RE_PROCDEF）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_model`
--

LOCK TABLES `oa_model` WRITE;
/*!40000 ALTER TABLE `oa_model` DISABLE KEYS */;
INSERT INTO `oa_model` VALUES (9101,1,9000,'请假审批','Clock','内置示例：直属主管审批，超3天加签发起人自选的总经理，结果抄送',9001,'SIMPLE','{\"children\":[{\"id\":\"node_9101_1\",\"type\":\"APPROVER\",\"name\":\"直属主管审批\",\"approveMode\":\"ANY\",\"emptyStrategy\":\"ADMIN\",\"opinionRequired\":false,\"assignee\":{\"type\":\"MANAGER_CHAIN\",\"levels\":1},\"formPerms\":{},\"children\":[]},{\"id\":\"node_9101_2\",\"type\":\"BRANCH\",\"name\":\"请假时长\",\"branchType\":\"EXCLUSIVE\",\"branches\":[{\"id\":\"branch_9101_a\",\"name\":\"超过3天\",\"conditions\":[{\"field\":\"field_days\",\"op\":\"GT\",\"value\":\"3\"}],\"children\":[{\"id\":\"node_9101_3\",\"type\":\"APPROVER\",\"name\":\"总经理审批\",\"approveMode\":\"ANY\",\"emptyStrategy\":\"ADMIN\",\"opinionRequired\":true,\"assignee\":{\"type\":\"STARTER_CHOSEN\"},\"formPerms\":{},\"children\":[]}]},{\"id\":\"branch_9101_b\",\"name\":\"其他\",\"isDefault\":true,\"children\":[{\"id\":\"node_9101_4\",\"type\":\"CC\",\"name\":\"抄送行政部\",\"assignee\":{\"type\":\"USER\",\"userIds\":[]},\"children\":[]}]}],\"children\":[]}]}',NULL,'oa_9101',NULL,0,'SYSTEM','leave',3,NULL,'2026-09-21 13:10:49',NULL,'2026-09-21 13:10:49',0);
/*!40000 ALTER TABLE `oa_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_overtime_record`
--

DROP TABLE IF EXISTS `oa_overtime_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_overtime_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '归属用户',
  `instance_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联流程实例（EARN=加班单 CONSUME=请假/调休单；ADJUST 为空）',
  `record_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'EARN 加班入账 / CONSUME 调休扣减 / ADJUST 手工调整（带符号）',
  `hours` decimal(10,2) NOT NULL COMMENT '小时数（统一计量单位；ADJUST 带符号）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注（单据标题/调整原因）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_instance_type` (`tenant_id`,`instance_id`,`record_type`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='加班/调休余额流水（余额=SUM(hours)，唯一键保证流程回调幂等）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_overtime_record`
--

LOCK TABLES `oa_overtime_record` WRITE;
/*!40000 ALTER TABLE `oa_overtime_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_overtime_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_process_instance_ext`
--

DROP TABLE IF EXISTS `oa_process_instance_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_process_instance_ext` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `model_id` bigint NOT NULL COMMENT '流程模型编号',
  `proc_def_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程定义编号',
  `proc_def_key` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程 key',
  `form_id` bigint DEFAULT NULL COMMENT '关联表单编号',
  `form_version` int DEFAULT NULL COMMENT '发起时的表单版本号',
  `form_data_id` bigint DEFAULT NULL COMMENT '表单提交数据编号',
  `biz_key` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务键（开放接口发起携带的外部业务单据号）',
  `title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实例标题',
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/APPROVED/REJECTED/WITHDRAWN',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `result` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结束说明',
  `create_by` bigint DEFAULT NULL COMMENT '发起人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_instance` (`instance_id`),
  KEY `idx_tenant_start` (`tenant_id`,`create_by`),
  KEY `idx_tenant_biz_key` (`tenant_id`,`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程实例扩展（标题/状态/发起人视图）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_process_instance_ext`
--

LOCK TABLES `oa_process_instance_ext` WRITE;
/*!40000 ALTER TABLE `oa_process_instance_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_process_instance_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_setting`
--

DROP TABLE IF EXISTS `oa_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_setting` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `setting_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '设置键（如 work_hours_per_day 每日工时）',
  `setting_value` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设置值',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_key` (`tenant_id`,`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户级 OA 设置（键值对）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_setting`
--

LOCK TABLES `oa_setting` WRITE;
/*!40000 ALTER TABLE `oa_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_task_log`
--

DROP TABLE IF EXISTS `oa_task_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_task_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `task_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎任务编号',
  `node_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批节点编号',
  `node_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批节点名称',
  `user_id` bigint DEFAULT NULL COMMENT '操作人',
  `user_name` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人昵称快照',
  `action` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'START/APPROVE/REJECT/RETURN/TRANSFER/CC/WITHDRAW/RESUBMIT',
  `opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `target` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作目标（转办人/退回节点等）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_instance` (`tenant_id`,`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批时间线';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_task_log`
--

LOCK TABLES `oa_task_log` WRITE;
/*!40000 ALTER TABLE `oa_task_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_task_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_webhook_config`
--

DROP TABLE IF EXISTS `oa_webhook_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_webhook_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称（如 ERP 单据回调）',
  `url` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '回调地址（POST JSON）',
  `secret` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签名密钥（HMAC-SHA256，空=不签名；不回显）',
  `model_id` bigint DEFAULT NULL COMMENT '限定流程模型编号（空=全部流程）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 / 1 停用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_model` (`tenant_id`,`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Webhook 回调配置（流程结束推送业务系统）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_webhook_config`
--

LOCK TABLES `oa_webhook_config` WRITE;
/*!40000 ALTER TABLE `oa_webhook_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_webhook_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_webhook_log`
--

DROP TABLE IF EXISTS `oa_webhook_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_webhook_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `config_id` bigint NOT NULL COMMENT '回调配置编号',
  `instance_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例编号',
  `event` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'process.finished / test',
  `payload` longtext COLLATE utf8mb4_general_ci COMMENT '推送载荷 JSON',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 待推送(重试中) / 1 成功 / 2 死信',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已重试次数（不含首次）',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间（空=待首次推送）',
  `last_response` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近响应摘要（状态码+响应片段/异常）',
  `finish_time` datetime DEFAULT NULL COMMENT '推送完成时间（成功或转死信）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_config_status` (`tenant_id`,`config_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Webhook 推送日志（重试队列：超限转死信）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_webhook_log`
--

LOCK TABLES `oa_webhook_log` WRITE;
/*!40000 ALTER TABLE `oa_webhook_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_webhook_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_api`
--

DROP TABLE IF EXISTS `open_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共目录）',
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 标识（open:resource:action）',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 名称',
  `path` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '路径模板（Ant 风格，如 /open/api/user/{id}）',
  `http_method` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GET' COMMENT 'HTTP 方法（大写；ALL=不限）',
  `target` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分发目标：契约接口全名#方法名（空=open-service 内置端点）',
  `source` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'seed' COMMENT '目录来源：seed=SQL 种子；rpc=业务积木自注册',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_api`
--

LOCK TABLES `open_api` WRITE;
/*!40000 ALTER TABLE `open_api` DISABLE KEYS */;
INSERT INTO `open_api` VALUES (8001,0,'open:user:page','分页查询用户','/open/api/user/page','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8002,0,'open:user:get','查询用户','/open/api/user/{id}','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8003,0,'open:user:get-by-username','按用户名查询','/open/api/user/by-username','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8004,0,'open:user:create','创建用户','/open/api/user','POST',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8005,0,'open:user:update','更新用户','/open/api/user','PUT',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8006,0,'open:user:status','启停用户','/open/api/user/{id}/status','PUT',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8007,0,'open:user:reset-password','重置用户密码','/open/api/user/{id}/password','PUT',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8008,0,'open:user:delete','删除用户','/open/api/user/{id}','DELETE',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8009,0,'open:user:role-ids','查询用户角色','/open/api/user/{id}/role-ids','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8010,0,'open:user:assign-roles','分配用户角色','/open/api/user/{id}/role-ids','PUT',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8011,0,'open:user:extend-get','读用户扩展属性','/open/api/user/{id}/extend','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8012,0,'open:user:extend-put','写用户扩展属性','/open/api/user/{id}/extend','PUT',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8013,0,'open:dept:tree','部门树查询','/open/api/dept/tree','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8014,0,'open:dept:create','创建部门','/open/api/dept','POST',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8015,0,'open:dept:update','更新部门','/open/api/dept','PUT',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8016,0,'open:dept:delete','删除部门','/open/api/dept/{id}','DELETE',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8017,0,'open:role:list','角色列表查询','/open/api/role/list','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8018,0,'open:role:create','创建角色','/open/api/role','POST',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8019,0,'open:bind:create','创建身份绑定','/open/api/bind','POST',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8020,0,'open:bind:resolve','外部标识解析','/open/api/bind/resolve','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8021,0,'open:bind:by-user','查询用户全部绑定','/open/api/bind/by-user/{userId}','GET',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(8022,0,'open:bind:delete','解除身份绑定','/open/api/bind/{bindId}','DELETE',NULL,'seed',0,NULL,NULL,'2026-09-21 13:10:50',NULL,'2026-09-21 13:10:50',0),(2101903120152555521,0,'open:fs:file-upload','上传文件','/open/api/fs/file/upload','POST','vip.mseapp.fs.api.RemoteFileService#uploadFile','rpc',0,'body: fileName/contentType/contentBase64(必填)、bizType/bizId/accessLevel(可选)；返回文件元数据（fileId 等），同租户同 MD5 秒传复用对象',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120215470082,0,'open:fs:file-get','查询文件元数据','/open/api/fs/file/{fileId}','GET','vip.mseapp.fs.api.RemoteFileService#getFileMeta','rpc',0,'返回文件元数据（跨租户 fileId 不可见，404 语义）',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120286773249,0,'open:fs:file-download','下载文件内容','/open/api/fs/file/{fileId}/download','POST','vip.mseapp.fs.api.RemoteFileService#downloadFile','rpc',0,'返回 contentBase64（小文件场景；大文件走服务代理下载端点）',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120345493505,0,'open:fs:file-delete','删除文件','/open/api/fs/file/{fileId}','DELETE','vip.mseapp.fs.api.RemoteFileService#deleteFile','rpc',0,'逻辑删元数据并尽力删除物理对象（秒传共享对象仍有引用时保留）',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120345493506,0,'open:ai:chat','AI 对话','/open/api/ai/chat','POST','vip.mseapp.ai.api.RemoteAiChatService#chat','rpc',0,'body: agentCode/message/conversationId/variables；调用已发布智能体（按应用绑定租户校验可见范围并记录用量）',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120408408065,0,'open:data:api-invoke','调用数据 API','/open/api/data/api/invoke','POST','vip.mseapp.data.api.RemoteDataApiService#invoke','rpc',0,'body: apiPath/params/pageNum/pageSize；调用管理端已审批发布的查询 API（受控通道：SQL 防护+租户谓词+行数上限）',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120492294146,0,'open:mp:fans-page','查询粉丝','/open/api/mp/fans','GET','vip.mseapp.mp.api.RemoteMpFanService#pageFans','rpc',0,'query: accountId/subscribed/keyword/pageNum/pageSize；粉丝由回调事件自动维护',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120538431490,0,'open:mp:messages-page','查询消息流水','/open/api/mp/messages','GET','vip.mseapp.mp.api.RemoteMpMessageService#pageMessages','rpc',0,'query: accountId/direction/kind/type/keyword/pageNum/pageSize',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120601346049,0,'open:oa:process-start','发起流程','/open/api/oa/process','POST','vip.mseapp.oa.api.RemoteOaProcessService#startProcess','rpc',0,'body: modelId/businessKey(业务单据号)/dataValues/summary/choices/operatorUserId；返回 processInstanceId',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120668454913,0,'open:oa:process-detail','查询流程详情','/open/api/oa/process/{processInstanceId}','GET','vip.mseapp.oa.api.RemoteOaProcessService#getProcessDetail','rpc',0,'按流程实例编号查状态与当前节点（无登录态，机器语义）',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120668454914,0,'open:oa:process-query','按业务键查流程','/open/api/oa/process/query','POST','vip.mseapp.oa.api.RemoteOaProcessService#getProcessDetail','rpc',0,'body: bizKey（或 processInstanceId）；bizKey 取该业务单据最新实例',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120731369474,0,'open:oa:todo-page','查询待办任务','/open/api/oa/todo','GET','vip.mseapp.oa.api.RemoteOaProcessService#pageTodo','rpc',0,'query: operatorUserId/pageNum/pageSize；外部系统拉待办做提醒',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120794284034,0,'open:oa:process-timeline','查询审批历史','/open/api/oa/process/timeline','GET','vip.mseapp.oa.api.RemoteOaProcessService#getTimeline','rpc',0,'query: processInstanceId（或 bizKey）；每步节点/审批人/动作/意见/时间',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120857198594,0,'open:oa:process-approve','审批同意','/open/api/oa/process/approve','POST','vip.mseapp.oa.api.RemoteOaProcessService#approve','rpc',0,'body: taskId/operatorUserId/opinion',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120869781506,0,'open:oa:process-reject','审批拒绝','/open/api/oa/process/reject','POST','vip.mseapp.oa.api.RemoteOaProcessService#reject','rpc',0,'body: taskId/operatorUserId/opinion(必填)；按节点拒绝策略分派',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120924307458,0,'open:oa:process-return','退回指定节点','/open/api/oa/process/return','POST','vip.mseapp.oa.api.RemoteOaProcessService#returnTask','rpc',0,'body: taskId/targetNodeId/operatorUserId/opinion；发起人节点为 oa_starter',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120987222018,0,'open:oa:process-transfer','转办','/open/api/oa/process/transfer','POST','vip.mseapp.oa.api.RemoteOaProcessService#transferTask','rpc',0,'body: taskId/targetUserId/operatorUserId/opinion',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121054330882,0,'open:oa:process-withdraw','发起人撤回','/open/api/oa/process/withdraw','POST','vip.mseapp.oa.api.RemoteOaProcessService#withdraw','rpc',0,'body: processInstanceId/operatorUserId/opinion；仅发起人且运行中',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121054330883,0,'open:oa:process-resubmit','改单重提','/open/api/oa/process/resubmit','POST','vip.mseapp.oa.api.RemoteOaProcessService#resubmit','rpc',0,'body: taskId(发起人节点)/dataValues/summary/operatorUserId；退回发起人后重新提交',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121117245442,0,'open:oa:model-save','保存流程模型','/open/api/oa/model','POST','vip.mseapp.oa.api.RemoteOaModelService#saveModel','rpc',0,'body: id(空=创建)/name/formId/designType(SIMPLE/BPMN)/simpleConfig(节点树 JSON)/operatorUserId',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121180160001,0,'open:oa:model-deploy','部署流程模型','/open/api/oa/model/deploy','POST','vip.mseapp.oa.api.RemoteOaModelService#deployModel','rpc',0,'body: modelId/operatorUserId；绑定表单须已发布',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121255657473,0,'open:oa:model-get','查询流程模型详情','/open/api/oa/model/{modelId}','GET','vip.mseapp.oa.api.RemoteOaModelService#getModel','rpc',0,'返回 simpleConfig/bpmnConfig 设计态原文',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121305989122,0,'open:oa:model-page','分页查询流程模型','/open/api/oa/model','GET','vip.mseapp.oa.api.RemoteOaModelService#pageModel','rpc',0,'query: keyword/status/pageNum/pageSize',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121305989123,0,'open:oa:model-choice-nodes','查询自选审批人节点','/open/api/oa/model/{modelId}/choice-nodes','GET','vip.mseapp.oa.api.RemoteOaModelService#choiceNodes','rpc',0,'返回 STARTER_CHOSEN 节点清单（发起流程 choices 参数依据）',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121373097985,0,'open:oa:webhook-save','保存回调配置','/open/api/oa/webhook','POST','vip.mseapp.oa.api.RemoteOaWebhookService#saveWebhook','rpc',0,'body: id(空=创建)/name/url/secret(HMAC 签名密钥)/modelId(空=全部流程)/status',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121440206850,0,'open:oa:webhook-list','查询回调配置','/open/api/oa/webhook','GET','vip.mseapp.oa.api.RemoteOaWebhookService#listWebhook','rpc',0,'query: modelId（可空）；secret 不回显',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121440206851,0,'open:oa:webhook-delete','删除回调配置','/open/api/oa/webhook/{webhookId}','DELETE','vip.mseapp.oa.api.RemoteOaWebhookService#deleteWebhook','rpc',0,'删除后该配置不再接收推送',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121503121410,0,'open:oa:webhook-test','测试推送','/open/api/oa/webhook/test','POST','vip.mseapp.oa.api.RemoteOaWebhookService#testWebhook','rpc',0,'body: webhookId；同步发一条 test 事件并返回响应摘要',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121566035969,0,'open:oa:form-save','保存表单','/open/api/oa/form','POST','vip.mseapp.oa.api.RemoteOaFormService#saveForm','rpc',0,'body: id(空=创建)/name/categoryId/icon/remark/config(设计器控件树 JSON)/operatorUserId',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121633144834,0,'open:oa:form-publish','发布表单','/open/api/oa/form/publish','POST','vip.mseapp.oa.api.RemoteOaFormService#publishForm','rpc',0,'body: formId/operatorUserId；发布产生版本快照，流程部署前置条件',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121633144835,0,'open:oa:form-get','查询表单详情','/open/api/oa/form/{formId}','GET','vip.mseapp.oa.api.RemoteOaFormService#getForm','rpc',0,'返回 config 与 fieldDefs（字段清单，即 dataValues 的 key）',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903121696059394,0,'open:oa:form-page','分页查询表单','/open/api/oa/form','GET','vip.mseapp.oa.api.RemoteOaFormService#pageForm','rpc',0,'query: keyword/status/pageNum/pageSize',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0);
/*!40000 ALTER TABLE `open_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_api_log`
--

DROP TABLE IF EXISTS `open_api_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_api_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台域/未过鉴权）',
  `app_id` bigint DEFAULT NULL COMMENT '应用编号（未过 token 校验为空）',
  `api_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '命中的 API 标识（未匹配清单为空）',
  `method` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求 HTTP 方法',
  `path` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求路径',
  `ip` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源 IP',
  `status_code` int DEFAULT NULL COMMENT 'HTTP 状态码',
  `cost_ms` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  `error_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因（鉴权/限流等）',
  `trace_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链路追踪ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`,`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 调用审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_api_log`
--

LOCK TABLES `open_api_log` WRITE;
/*!40000 ALTER TABLE `open_api_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_api_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_app`
--

DROP TABLE IF EXISTS `open_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_app` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `app_key` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用标识（自动生成，全局唯一）',
  `app_secret` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'AppSecret 密文（AES-GCM，明文仅创建/重置时展示一次）',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '业务绑定租户（0=平台域；开放 API 数据隔离锚点）',
  `rate_limit_qps` bigint DEFAULT NULL COMMENT '限流 QPS（NULL=用 mse.open.default-rate-limit）',
  `expire_time` datetime DEFAULT NULL COMMENT '凭证过期时间（NULL=长期）',
  `sign_required` tinyint NOT NULL DEFAULT '0' COMMENT '强制请求签名验签（0 关 1 开；HMAC-SHA256 + 时间戳防重放 + nonce）',
  `ip_whitelist` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP 白名单（逗号/分号/换行分隔，支持 IPv4 CIDR；空=不限制）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_key` (`app_key`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放平台接入应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_app`
--

LOCK TABLES `open_app` WRITE;
/*!40000 ALTER TABLE `open_app` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_app_api`
--

DROP TABLE IF EXISTS `open_app_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_app_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号（随应用绑定租户）',
  `app_id` bigint NOT NULL COMMENT '应用编号',
  `api_id` bigint NOT NULL COMMENT 'API 编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_api` (`app_id`,`api_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用-API 授权关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_app_api`
--

LOCK TABLES `open_app_api` WRITE;
/*!40000 ALTER TABLE `open_app_api` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_app_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_user_bind`
--

DROP TABLE IF EXISTS `open_user_bind`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_user_bind` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '中台用户编号（sys_user.id）',
  `source_system` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源系统标识（接入方自定）',
  `external_user_id` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '外部系统用户标识',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind` (`tenant_id`,`source_system`,`external_user_id`,`deleted`),
  KEY `idx_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一身份绑定（外部标识 ↔ 中台账号）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_user_bind`
--

LOCK TABLES `open_user_bind` WRITE;
/*!40000 ALTER TABLE `open_user_bind` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_user_bind` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_user_extend`
--

DROP TABLE IF EXISTS `open_user_extend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_user_extend` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '中台用户编号（租户内唯一）',
  `extend_json` text COLLATE utf8mb4_general_ci COMMENT '扩展属性 JSON 对象（整体读写，≤4KB）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`tenant_id`,`user_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户扩展属性（接入系统差异化画像）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_user_extend`
--

LOCK TABLES `open_user_extend` WRITE;
/*!40000 ALTER TABLE `open_user_extend` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_user_extend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_hot_keyword`
--

DROP TABLE IF EXISTS `search_hot_keyword`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_hot_keyword` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `keyword` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索关键词',
  `hit_count` bigint DEFAULT '1' COMMENT '命中次数',
  `last_search_time` datetime DEFAULT NULL COMMENT '最近搜索时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_hot_keyword` (`tenant_id`,`keyword`,`deleted`),
  KEY `idx_search_hot_hit` (`tenant_id`,`hit_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搜索热词';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_hot_keyword`
--

LOCK TABLES `search_hot_keyword` WRITE;
/*!40000 ALTER TABLE `search_hot_keyword` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_hot_keyword` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_scan_cursor`
--

DROP TABLE IF EXISTS `search_scan_cursor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_scan_cursor` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `source_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '源标识',
  `shard_index` int NOT NULL DEFAULT '0' COMMENT '分片号（0 起）',
  `watermark` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '增量水位下界（首次全量=0）',
  `last_scan_time` datetime DEFAULT NULL COMMENT '最近扫描时间（调度 cron 锚点）',
  `fail_count` int DEFAULT '0' COMMENT '连续失败次数',
  `last_error` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败原因',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_cursor` (`source_code`,`shard_index`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='扫描游标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_scan_cursor`
--

LOCK TABLES `search_scan_cursor` WRITE;
/*!40000 ALTER TABLE `search_scan_cursor` DISABLE KEYS */;
INSERT INTO `search_scan_cursor` VALUES (2101904254548209666,'search:oa:instance',0,'0','2026-09-21 13:20:01',0,NULL,NULL,'2026-09-21 13:20:00',NULL,'2026-09-21 13:20:00',0),(2101904254548209667,'search:oa:form-data',0,'0','2026-09-21 13:20:01',0,NULL,NULL,'2026-09-21 13:20:00',NULL,'2026-09-21 13:20:00',0);
/*!40000 ALTER TABLE `search_scan_cursor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_source`
--

DROP TABLE IF EXISTS `search_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_source` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `source_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '源标识（全局唯一）',
  `source_name` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '源名称',
  `domain` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务域显示名（如 OA域；空=不分域，仅参与全部域搜索）',
  `provider_group` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据提供者分组（业务服务名）',
  `jump_url_template` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '跳转模板（/sub/{app}/...，支持 {bizId}/{extra.字段}）',
  `permission_code` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结果可见权限码（空=登录即可见）',
  `data_scope_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '个人数据范围过滤：0 关闭 1 开启',
  `scan_cron` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '增量扫描 cron（秒 分 时 日 月 周）',
  `incremental_column` varchar(64) COLLATE utf8mb4_general_ci DEFAULT 'update_time' COMMENT '增量水位列名',
  `batch_size` int DEFAULT '500' COMMENT '每批拉取条数',
  `shard_count` int DEFAULT '1' COMMENT '分片数',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0 启用 1 停用',
  `fields_json` text COLLATE utf8mb4_general_ci COMMENT '平台级字段配置（JSON 数组；空=无配置，查询退回内置 title/content）',
  `fields_customized` tinyint NOT NULL DEFAULT '0' COMMENT '字段配置定制标记：0 跟随推送刷新 1 管理端已定制',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_source_code` (`source_code`,`deleted`),
  KEY `idx_search_source_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='可搜索源';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_source`
--

LOCK TABLES `search_source` WRITE;
/*!40000 ALTER TABLE `search_source` DISABLE KEYS */;
INSERT INTO `search_source` VALUES (2101903120601346050,'search:oa:instance','OA 审批单','OA域','mse-oa','/sub/oa/detail/instance/{extra.instanceId}','oa:process:mine',1,'0 */10 * * * ?','update_time',500,1,0,'[{\"field\":\"instanceId\",\"label\":\"引擎实例编号\",\"type\":\"keyword\",\"searchable\":false,\"weight\":null,\"highlightable\":false,\"returned\":true},{\"field\":\"status\",\"label\":\"审批状态\",\"type\":\"keyword\",\"searchable\":false,\"weight\":null,\"highlightable\":false,\"returned\":true},{\"field\":\"modelId\",\"label\":\"流程模型\",\"type\":\"keyword\",\"searchable\":false,\"weight\":null,\"highlightable\":false,\"returned\":true},{\"field\":\"formDataId\",\"label\":\"表单数据\",\"type\":\"keyword\",\"searchable\":false,\"weight\":null,\"highlightable\":false,\"returned\":true}]',0,'审批实例：标题、状态与审批结果，按发起人数据范围过滤',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0),(2101903120668454915,'search:oa:form-data','OA 表单数据','OA域','mse-oa','/sub/oa/form/data/{bizId}','oa:form:data:list',1,'0 */10 * * * ?','update_time',500,1,0,'[{\"field\":\"formId\",\"label\":\"表单\",\"type\":\"keyword\",\"searchable\":false,\"weight\":null,\"highlightable\":false,\"returned\":true},{\"field\":\"formVersion\",\"label\":\"表单版本\",\"type\":\"keyword\",\"searchable\":false,\"weight\":null,\"highlightable\":false,\"returned\":true},{\"field\":\"instanceId\",\"label\":\"引擎实例编号\",\"type\":\"keyword\",\"searchable\":false,\"weight\":null,\"highlightable\":false,\"returned\":true}]',0,'自定义表单提交数据：摘要与控件填写内容全文，按发起人数据范围过滤',NULL,'2026-09-21 13:15:30',NULL,'2026-09-21 13:15:30',0);
/*!40000 ALTER TABLE `search_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_task`
--

DROP TABLE IF EXISTS `search_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_task` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `task_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务类型：FULL_REBUILD/INDEX_OPTIMIZE/SCAN_NOW',
  `source_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标源（SCAN_NOW/FULL_REBUILD 必填）',
  `idle_only` tinyint NOT NULL DEFAULT '1' COMMENT '闲时专用：0 立即 1 仅闲时窗口内执行',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0 待执行 1 执行中 2 成功 3 失败',
  `progress` int DEFAULT '0' COMMENT '进度百分比（0-100）',
  `processed_rows` bigint DEFAULT NULL COMMENT '处理文档数',
  `payload` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务参数（JSON，预留）',
  `error_msg` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_search_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搜索引擎任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_task`
--

LOCK TABLES `search_task` WRITE;
/*!40000 ALTER TABLE `search_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_tenant_field`
--

DROP TABLE IF EXISTS `search_tenant_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_tenant_field` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `source_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '源标识（须为平台已注册源）',
  `fields_json` text COLLATE utf8mb4_general_ci COMMENT '覆盖字段配置（JSON 数组；仅 searchable/weight/highlightable/returned/label 生效）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_tenant_field` (`tenant_id`,`source_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户搜索字段覆盖';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_tenant_field`
--

LOCK TABLES `search_tenant_field` WRITE;
/*!40000 ALTER TABLE `search_tenant_field` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_tenant_field` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `config_name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名称',
  `config_key` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数键名（租户内唯一，代码层校验）',
  `config_value` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数键值',
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '0 系统内置 1 自定义',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_key` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (4,0,'管理平台界面名称','sys.platform.name','ai-mse 管理平台',0,'管理平台首页左上角显示名称（系统内置，可改值不可删除）',NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(5,0,'登录策略-允许多端登录','sys.login.multiDevice','true',0,'false=单点登录（登录顶掉该账号全部旧会话）；租户可在自己参数里覆盖平台默认',NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(6,0,'登录策略-同端互斥','sys.login.sameDeviceExclusive','false',0,'true=同一设备类型仅保留最新会话（允许多端并存时生效）',NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(53100,0,'OA AI 能力模式','oa.ai.mode','off',0,'OA 智能（AI 生成流程/审批助手/AI 填单）模式：off 关闭；auto 平台优先失败回退直连；platform 仅 mse-ai 平台；direct 直连模型（依赖 spring.ai.* 配置）',NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0);
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门编号（0=根）',
  `ancestors` varchar(500) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '祖级列表（逗号分隔，如 0,1000；用于本部门及以下展开）',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
INSERT INTO `sys_dept` VALUES (900,0,0,'0','平台总部',1,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dict_type` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典类型',
  `dict_label` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据标签',
  `dict_value` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据键值',
  `dict_sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `list_class` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签样式（default/primary/success/info/warning/danger）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认 0 否 1 是',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` VALUES (21,0,'sys_normal_disable','正常','0',1,'success',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(22,0,'sys_normal_disable','停用','1',2,'danger',0,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dict_name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典类型（租户内唯一，代码层校验）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` VALUES (11,0,'系统状态','sys_normal_disable',0,'通用状态（正常/停用）',NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_domain_config`
--

DROP TABLE IF EXISTS `sys_domain_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_domain_config` (
  `id` bigint NOT NULL COMMENT '编号',
  `domain` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '域名（完整主机名，小写，全局唯一）',
  `site_title` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录页标题（NULL=前端默认标题）',
  `site_logo` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Logo 图片地址（http(s):// 或 / 开头路径或 data:image）',
  `login_background` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录背景图地址（NULL=背景色/默认渐变）',
  `background_color` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '背景色或渐变（CSS background 值；背景图存在时垫底）',
  `copyright` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版权文案（登录页底部）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用（停用后登录页回退默认样式）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='域名个性化配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_domain_config`
--

LOCK TABLES `sys_domain_config` WRITE;
/*!40000 ALTER TABLE `sys_domain_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_domain_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_home_template`
--

DROP TABLE IF EXISTS `sys_home_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_home_template` (
  `id` bigint NOT NULL COMMENT '编号',
  `scope_type` tinyint NOT NULL DEFAULT '1' COMMENT '范围 1 平台全局 2 租户覆盖',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（scope=2 为本租户；scope=1 恒为 0）',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名',
  `layout_json` text COLLATE utf8mb4_general_ci COMMENT '布局 JSON：{"layout":[...],"catalog":[可选块...],"themes":[...],"defaultThemeId":"..."}',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用（停用视为无配置，级联回退）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_home_scope` (`scope_type`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户首页模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_home_template`
--

LOCK TABLES `sys_home_template` WRITE;
/*!40000 ALTER TABLE `sys_home_template` DISABLE KEYS */;
INSERT INTO `sys_home_template` VALUES (1,1,0,'平台默认首页','{\"layout\":[{\"id\":\"w-welcome\",\"type\":\"welcome\",\"title\":\"欢迎\",\"span\":24}],\"catalog\":[{\"id\":\"c-oa-todo\",\"type\":\"list\",\"title\":\"OA 待办\",\"span\":12,\"permission\":\"oa:process:todo\",\"api\":\"/oa/process/todo\",\"params\":{\"pageNum\":1,\"pageSize\":6},\"fieldMap\":{\"title\":\"title\",\"sub\":\"modelName\",\"time\":\"startTime\"},\"link\":\"/sub/oa/detail/task/{taskId}?businessKey={businessKey}\",\"refreshSeconds\":60},{\"id\":\"c-mp-stats\",\"type\":\"stat\",\"title\":\"公众号概览\",\"span\":12,\"permission\":\"mp:stats:list\",\"api\":\"/mp/stats/summary\",\"metrics\":[{\"field\":\"accountTotal\",\"label\":\"公众号\"},{\"field\":\"fanSubscribed\",\"label\":\"订阅粉丝\"},{\"field\":\"messageToday\",\"label\":\"今日消息\"}]},{\"id\":\"c-data-overview\",\"type\":\"stat\",\"title\":\"数据中台概览\",\"span\":12,\"permission\":\"data:overview:list\",\"api\":\"/data/overview\",\"metrics\":[{\"field\":\"datasourceCount\",\"label\":\"数据源\"},{\"field\":\"odsTableCount\",\"label\":\"ODS 表\"},{\"field\":\"publishedApiCount\",\"label\":\"已发布 API\"},{\"field\":\"todayApiCalls\",\"label\":\"今日调用\"}]}],\"themes\":[{\"id\":\"t-light\",\"name\":\"简约浅灰\",\"bg\":{\"type\":\"color\",\"value\":\"#f5f7fa\"}},{\"id\":\"t-blue\",\"name\":\"晨曦蓝渐变\",\"bg\":{\"type\":\"gradient\",\"value\":\"linear-gradient(135deg,#dceeff 0%,#f5f7fa 55%,#e3f4e9 100%)\"}},{\"id\":\"t-dark\",\"name\":\"静谧深蓝\",\"bg\":{\"type\":\"color\",\"value\":\"#1c2b3a\"}}],\"defaultThemeId\":\"t-light\"}',0,'内置默认模板，租户可在首页配置中覆盖',NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_home_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_home_user_setting`
--

DROP TABLE IF EXISTS `sys_home_user_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_home_user_setting` (
  `id` bigint NOT NULL COMMENT '编号',
  `owner_type` tinyint NOT NULL DEFAULT '1' COMMENT '归属 1 租户用户 2 平台用户',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（owner=1 为本租户；owner=2 恒为 0）',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `layout_json` text COLLATE utf8mb4_general_ci COMMENT '个性化 JSON：{"layout":[...],"themeId":"..."}',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_home_owner` (`owner_type`,`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户门户首页个性化设置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_home_user_setting`
--

LOCK TABLES `sys_home_user_setting` WRITE;
/*!40000 ALTER TABLE `sys_home_user_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_home_user_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台；租户不存在时的失败记录也落 0）',
  `username` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号',
  `ip` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录 IP',
  `browser` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '浏览器',
  `os` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作系统',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `msg` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '登录时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
INSERT INTO `sys_logininfor` VALUES (2101896732722507777,0,'admin','0:0:0:0:0:0:0:1','Edge','Windows',1,'租户不存在','2026-09-21 12:50:07',0,'2026-09-21 12:50:07'),(2101896743157936130,0,'platform','0:0:0:0:0:0:0:1','Edge','Windows',0,'登录成功','2026-09-21 12:50:10',0,'2026-09-21 12:50:10');
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL COMMENT '编号',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单编号（0=根）',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `type` tinyint NOT NULL COMMENT '1 目录 2 菜单 3 子应用页面',
  `menu_scope` tinyint NOT NULL DEFAULT '2' COMMENT '可见域 1 平台端 2 租户端 3 双端',
  `path` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由地址（type=3 为子应用内部路由，支持 :param 动态段）',
  `micro_name` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属子应用标识（type=3 专用，关联 sys_micro_app.name）',
  `component` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径（相对前端 views，不带扩展名）',
  `icon` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `visible` tinyint NOT NULL DEFAULT '0' COMMENT '0 显示 1 隐藏',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='导航菜单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,0,'系统管理',1,2,'/system',NULL,NULL,'Setting',10,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(100,1,'用户管理',2,2,'user',NULL,'User','User',1,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(110,1,'角色管理',2,2,'role',NULL,'system/Role','Avatar',2,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(120,1,'部门管理',2,2,'dept',NULL,'system/Dept','Grid',3,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(130,1,'岗位管理',2,2,'post',NULL,'system/Post','Postcard',4,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(140,1,'字典管理',2,2,'dict',NULL,'system/Dict','Collection',5,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(150,1,'参数设置',2,2,'config',NULL,'system/Config','Tools',6,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(160,1,'日志管理',1,2,'log',NULL,NULL,'Document',7,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(161,160,'操作日志',2,2,'operlog',NULL,'system/log/OperLog','Memo',1,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(162,160,'登录日志',2,2,'logininfor',NULL,'system/log/Logininfor','Tickets',2,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(170,1,'首页配置',2,2,'home-layout',NULL,'system/HomeLayout','Operation',8,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(180,1,'在线用户',2,2,'online',NULL,'system/OnlineUser','Connection',9,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(200,0,'平台管理',1,1,'/platform',NULL,NULL,'Monitor',20,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(210,200,'租户管理',2,1,'tenant',NULL,'Tenant','OfficeBuilding',1,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(220,200,'套餐管理',2,1,'tenant-package',NULL,'platform/Package','Goods',2,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(230,200,'菜单管理',2,1,'menu',NULL,'platform/Menu','Menu',3,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(240,200,'权限点管理',2,1,'permission',NULL,'platform/Permission','Key',4,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(250,200,'平台用户',2,1,'user',NULL,'User','User',5,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(260,200,'平台角色',2,1,'role',NULL,'system/Role','Avatar',6,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(270,200,'平台部门',2,1,'dept',NULL,'system/Dept','Grid',7,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(280,200,'域名个性化',2,1,'domain-config',NULL,'platform/DomainConfig','Picture',8,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(290,200,'子应用管理',2,1,'micro-app',NULL,'platform/MicroApp','Menu',9,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(300,200,'首页配置',2,1,'home-layout',NULL,'system/HomeLayout','Operation',10,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(310,200,'参数设置',2,1,'config',NULL,'system/Config','Tools',11,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(320,200,'在线用户',2,1,'online',NULL,'system/OnlineUser','Connection',12,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(330,200,'租户数据源',2,1,'tenant-datasource',NULL,'platform/TenantDatasource','Coin',13,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(331,200,'隔离策略',2,1,'tenant-isolation',NULL,'platform/TenantIsolation','SetUp',14,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(400,0,'演示中心',1,2,'/sub/demo',NULL,NULL,'Box',20,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(401,400,'订单演示',3,2,'/order','demo',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(420,0,'协同办公',1,2,'/sub/oa',NULL,NULL,'Box',22,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(421,420,'发起审批',3,2,'/start','oa',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(422,420,'待办中心',3,2,'/task','oa',NULL,NULL,2,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(423,420,'表单管理',3,2,'/form','oa',NULL,NULL,3,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(424,420,'流程管理',3,2,'/model','oa',NULL,NULL,4,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(425,420,'组织设置',3,2,'/org','oa',NULL,NULL,5,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(426,420,'委托代理',3,2,'/proxy','oa',NULL,NULL,6,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(427,420,'报表统计',3,2,'/stats','oa',NULL,NULL,7,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(428,420,'调休余额',3,2,'/balance','oa',NULL,NULL,8,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(429,420,'Webhook 回调',3,2,'/webhook','oa',NULL,NULL,9,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(430,0,'微信公众号',1,2,'/sub/mp',NULL,NULL,'Box',23,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(431,430,'仪表盘',3,2,'/dashboard','mp',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(432,430,'公众号管理',3,2,'/account','mp',NULL,NULL,2,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(433,430,'菜单配置',3,2,'/menu','mp',NULL,NULL,3,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(434,430,'粉丝管理',3,2,'/fan','mp',NULL,NULL,4,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(435,430,'消息事件',3,2,'/message','mp',NULL,NULL,5,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(436,430,'回复规则',3,2,'/rule','mp',NULL,NULL,6,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(440,0,'数据中台',1,2,'/sub/data',NULL,NULL,'Box',24,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(441,440,'数据概览',3,2,'/overview','data',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(442,440,'数据源管理',3,2,'/datasource','data',NULL,NULL,2,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(443,440,'数据集成',3,2,'/sync','data',NULL,NULL,3,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(444,440,'数据地图',3,2,'/datamap','data',NULL,NULL,4,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(445,440,'数据服务',3,2,'/service','data',NULL,NULL,5,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(446,440,'质量监控',3,2,'/quality','data',NULL,NULL,6,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(447,440,'查询白名单',3,2,'/allowlist','data',NULL,NULL,7,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(450,0,'开放平台',1,1,'/sub/open',NULL,NULL,'Box',30,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(451,450,'应用管理',3,1,'/apps','open',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(452,450,'调用日志',3,1,'/logs','open',NULL,NULL,2,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(453,450,'身份绑定',3,1,'/binds','open',NULL,NULL,3,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(454,450,'接入指南',3,1,'/guide','open',NULL,NULL,4,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(455,450,'API 目录',3,1,'/apis','open',NULL,NULL,5,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(460,0,'AI 平台',1,3,'/sub/ai',NULL,NULL,'MagicStick',25,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(461,460,'对话工作台',3,2,'/chat','ai',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(462,460,'智能体管理',3,2,'/agent','ai',NULL,NULL,2,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(463,460,'知识库',3,2,'/kb','ai',NULL,NULL,3,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(464,460,'工具中心',3,3,'/tool','ai',NULL,NULL,7,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(465,460,'用量统计',3,2,'/usage','ai',NULL,NULL,5,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(466,460,'模型与智能体',3,1,'/platform/model','ai',NULL,NULL,6,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(467,460,'开放能力',3,1,'/platform/open','ai',NULL,NULL,8,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(468,460,'用量统计',3,1,'/platform/usage','ai',NULL,NULL,9,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(469,460,'模型配置',3,2,'/model-config','ai',NULL,NULL,9,0,0,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(470,0,'文件存储',1,2,'/sub/fs',NULL,NULL,'FolderOpened',26,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(471,470,'文件管理',3,2,'/file','fs',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(480,0,'搜索引擎',1,3,'/sub/search',NULL,NULL,'Search',27,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(481,480,'源管理',3,1,'/source','search',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(482,480,'任务中心',3,1,'/task','search',NULL,NULL,2,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(483,480,'热词统计',3,1,'/hot','search',NULL,NULL,3,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(484,480,'搜索测试',3,1,'/playground','search',NULL,NULL,4,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(485,420,'AI 智能体设置',3,2,'/ai-setting','oa',NULL,NULL,10,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(486,480,'索引管理',3,1,'/index','search',NULL,NULL,5,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(487,480,'搜索配置',3,2,'/tenant-field','search',NULL,NULL,6,0,0,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(490,0,'监控中心',1,1,'/sub/monitor',NULL,NULL,'Monitor',28,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(491,490,'监控总览',3,1,'/dashboard','monitor',NULL,NULL,1,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(492,490,'监控目标',3,1,'/targets','monitor',NULL,NULL,2,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(493,490,'告警中心',3,1,'/alerts','monitor',NULL,NULL,3,0,0,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0);
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_message`
--

DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL COMMENT '编号',
  `type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型（业务自定义，如 TODO/CC/RESULT/REMIND）',
  `receiver_id` bigint NOT NULL COMMENT '接收人',
  `title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息标题',
  `content` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息内容',
  `biz_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务关联标识（如流程实例编号）',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '0 未读 1 已读',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_receiver` (`tenant_id`,`receiver_id`,`read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_message`
--

LOCK TABLES `sys_message` WRITE;
/*!40000 ALTER TABLE `sys_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_micro_app`
--

DROP TABLE IF EXISTS `sys_micro_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_micro_app` (
  `id` bigint NOT NULL COMMENT '编号',
  `name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '子应用唯一标识（决定主应用路由前缀 /sub/{name}，须与子应用构建 --base=/sub-apps/{name}/ 一致）',
  `title` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '显示名（未配置页面菜单时作为侧边栏单项标题）',
  `dev_entry` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开发环境入口（子应用 dev server，如 //localhost:3400）',
  `prod_entry` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '生产环境入口（子应用静态资源部署路径，如 /sub-apps/oa/）',
  `side` tinyint NOT NULL DEFAULT '1' COMMENT '可见端 1 租户端 2 平台端 3 双端',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='微前端子应用注册表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_micro_app`
--

LOCK TABLES `sys_micro_app` WRITE;
/*!40000 ALTER TABLE `sys_micro_app` DISABLE KEYS */;
INSERT INTO `sys_micro_app` VALUES (1,'demo','演示中心','//localhost:3300','/sub-apps/demo/',1,1,0,'独立工程 mse-demo（demo-ui + demo-boot）',NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(3,'oa','协同办公','//localhost:3400','/sub-apps/oa/',1,3,0,'独立工程 mse-biz/mse-oa（oa-ui + oa-boot）',NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4,'mp','微信公众号','//localhost:3500','/sub-apps/mp/',1,4,0,'独立工程 mse-biz/mse-mp（mp-ui + mp-boot）',NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(5,'open','开放平台','//localhost:3600','/sub-apps/open/',2,5,0,'独立工程 mse-biz/mse-open（open-ui + open-boot）',NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(6,'data','数据中台','//localhost:3700','/sub-apps/data/',1,6,0,'独立工程 mse-biz/mse-data（data-ui + data-boot）',NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(7,'ai','AI 智能体平台','//localhost:3800','/sub-apps/ai/',3,7,0,'独立工程 mse-biz/mse-ai（ai-ui + ai-boot，双端）',NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8,'fs','文件存储','//localhost:3900','/sub-apps/fs/',1,8,0,'独立工程 mse-biz/mse-fs（fs-ui + fs-boot）',NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(9,'search','搜索引擎平台','//localhost:4000','/sub-apps/search/',3,9,0,'独立工程 mse-biz/mse-search（search-ui + search-boot，双端）',NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(10,'monitor','监控中心','//localhost:4010','/sub-apps/monitor/',2,10,0,'独立工程 mse-biz/mse-monitor（monitor-ui + monitor-boot，平台端）',NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0);
/*!40000 ALTER TABLE `sys_micro_app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `module` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务模块（如 system:user）',
  `oper_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作类型（新增/修改/删除/导入/导出等）',
  `method` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法名（类.方法）',
  `request_method` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
  `oper_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求 URL',
  `oper_param` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求参数（JSON，超长截断）',
  `oper_user_id` bigint DEFAULT NULL COMMENT '操作人员编号',
  `oper_username` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人员账号',
  `oper_ip` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作 IP',
  `cost_ms` int DEFAULT NULL COMMENT '耗时（毫秒）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `error_msg` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误消息（超长截断）',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL COMMENT '编号',
  `menu_id` bigint DEFAULT NULL COMMENT '挂靠菜单（授权界面分组 + 套餐裁剪推导依据；NULL=通用权限，不受套餐裁剪）',
  `module` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属模块（system/tenant/demo）',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限名',
  `code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限标识（module:resource:action）',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='权限点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_permission`
--

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` VALUES (1001,100,'system','用户查询','system:user:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1002,100,'system','用户详情','system:user:query',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1003,100,'system','用户新增','system:user:create',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1004,100,'system','用户修改','system:user:update',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1005,100,'system','用户删除','system:user:delete',5,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1006,100,'system','重置密码','system:user:reset-password',6,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1007,100,'system','分配角色','system:user:assign-role',7,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1008,100,'system','用户导入','system:user:import',8,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1009,100,'system','用户导出','system:user:export',9,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1101,110,'system','角色查询','system:role:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1102,110,'system','角色详情','system:role:query',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1103,110,'system','角色新增','system:role:create',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1104,110,'system','角色修改','system:role:update',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1105,110,'system','角色删除','system:role:delete',5,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1106,110,'system','角色授权','system:role:assign',6,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1201,120,'system','部门查询','system:dept:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1202,120,'system','部门新增','system:dept:create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1203,120,'system','部门修改','system:dept:update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1204,120,'system','部门删除','system:dept:delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1301,130,'system','岗位查询','system:post:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1302,130,'system','岗位详情','system:post:query',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1303,130,'system','岗位新增','system:post:create',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1304,130,'system','岗位修改','system:post:update',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1305,130,'system','岗位删除','system:post:delete',5,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1401,140,'system','字典查询','system:dict:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1402,140,'system','字典新增','system:dict:create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1403,140,'system','字典修改','system:dict:update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1404,140,'system','字典删除','system:dict:delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1501,150,'system','参数查询','system:config:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1502,150,'system','参数新增','system:config:create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1503,150,'system','参数修改','system:config:update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1504,150,'system','参数删除','system:config:delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1601,161,'system','操作日志查询','system:operlog:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1602,161,'system','操作日志删除','system:operlog:delete',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1611,162,'system','登录日志查询','system:logininfor:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1612,162,'system','登录日志删除','system:logininfor:delete',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1701,170,'system','首页模板查询','system:home-template:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1702,170,'system','首页模板保存','system:home-template:save',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1703,170,'system','首页模板恢复默认','system:home-template:reset',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1801,180,'system','在线用户查询','system:online:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(1802,180,'system','强制下线','system:online:kick',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2101,210,'tenant','租户查询','tenant:tenant:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2102,210,'tenant','租户详情','tenant:tenant:query',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2103,210,'tenant','租户新增','tenant:tenant:create',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2104,210,'tenant','租户修改','tenant:tenant:update',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2105,210,'tenant','租户删除','tenant:tenant:delete',5,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2106,210,'tenant','启停租户','tenant:tenant:change-status',6,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2201,220,'tenant','套餐查询','tenant:package:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2202,220,'tenant','套餐新增','tenant:package:create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2203,220,'tenant','套餐修改','tenant:package:update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2204,220,'tenant','套餐删除','tenant:package:delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2301,230,'system','菜单查询','system:menu:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2302,230,'system','菜单新增','system:menu:create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2303,230,'system','菜单修改','system:menu:update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2304,230,'system','菜单删除','system:menu:delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2401,240,'system','权限点查询','system:permission:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2402,240,'system','权限点新增','system:permission:create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2403,240,'system','权限点修改','system:permission:update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2404,240,'system','权限点删除','system:permission:delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2501,250,'system','平台用户查询','system:user:platform-list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2502,250,'system','平台用户新增','system:user:platform-create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2503,250,'system','平台用户修改','system:user:platform-update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2504,250,'system','平台用户删除','system:user:platform-delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2601,260,'system','平台角色查询','system:role:platform-list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2602,260,'system','平台角色详情','system:role:platform-query',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2603,260,'system','平台角色新增','system:role:platform-create',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2604,260,'system','平台角色修改','system:role:platform-update',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2605,260,'system','平台角色授权','system:role:platform-assign',5,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2701,270,'system','平台部门查询','system:dept:platform-list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2702,270,'system','平台部门新增','system:dept:platform-create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2703,270,'system','平台部门修改','system:dept:platform-update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2704,270,'system','平台部门删除','system:dept:platform-delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2801,280,'tenant','个性化查询','tenant:domain:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2802,280,'tenant','个性化详情','tenant:domain:query',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2803,280,'tenant','个性化新增','tenant:domain:create',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2804,280,'tenant','个性化修改','tenant:domain:update',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2805,280,'tenant','个性化删除','tenant:domain:delete',5,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2901,NULL,'system','个人信息查询','system:profile:query',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2902,NULL,'system','个人信息修改','system:profile:update',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2911,290,'system','子应用查询','system:micro-app:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2912,290,'system','子应用新增','system:micro-app:create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2913,290,'system','子应用修改','system:micro-app:update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(2914,290,'system','子应用删除','system:micro-app:delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3001,300,'system','首页模板查询','system:home-template:platform-list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3002,300,'system','首页模板保存','system:home-template:platform-save',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3003,300,'system','首页模板恢复默认','system:home-template:platform-reset',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3004,310,'system','参数查询','system:config:platform-list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3005,310,'system','参数新增','system:config:platform-create',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3006,310,'system','参数修改','system:config:platform-update',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3007,310,'system','参数删除','system:config:platform-delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3101,401,'demo','订单查询','demo:order:list',1,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(3102,401,'demo','订单创建','demo:order:create',2,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(3201,320,'system','在线用户查询','system:online:platform-list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3202,320,'system','强制下线','system:online:platform-kick',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3301,330,'tenant','数据源查询','tenant:ds:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3302,330,'tenant','数据源详情','tenant:ds:query',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3303,330,'tenant','数据源新增','tenant:ds:create',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3304,330,'tenant','数据源修改','tenant:ds:update',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3305,330,'tenant','数据源删除','tenant:ds:delete',5,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3311,331,'tenant','策略查询','tenant:route:list',1,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3312,331,'tenant','策略详情','tenant:route:query',2,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3313,331,'tenant','策略保存','tenant:route:save',3,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3314,331,'tenant','策略删除','tenant:route:delete',4,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3315,331,'tenant','生成分表','tenant:route:generate',5,0,NULL,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(4101,471,'fs','文件查询','fs:file:list',1,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4102,471,'fs','文件上传','fs:file:upload',2,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4103,471,'fs','文件下载','fs:file:download',3,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4104,471,'fs','文件删除','fs:file:delete',4,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4105,471,'fs','存储迁移','fs:storage:migrate',5,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4201,481,'search','源查询','search:source:list',1,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4202,481,'search','源编辑（调参/启停/手工登记）','search:source:save',2,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4203,482,'search','任务查询','search:task:list',3,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4204,482,'search','任务触发（重建/优化/扫描）','search:task:save',4,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4205,483,'search','热词统计查询','search:stats:list',5,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4206,484,'search','搜索调试（推文档/过滤对比）','search:debug:use',6,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4207,486,'search','索引查询/数据浏览','search:index:list',7,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4208,486,'search','索引删除/清空','search:index:delete',8,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4209,481,'search','源字段配置（平台）','search:source:field',9,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4210,487,'search','搜索配置查询（租户）','search:tenantconfig:list',10,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4211,487,'search','搜索配置保存（租户）','search:tenantconfig:save',11,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(4301,491,'monitor','总览查询','monitor:dashboard:query',1,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4302,492,'monitor','目标查询','monitor:target:query',2,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4303,492,'monitor','目标编辑','monitor:target:save',3,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4304,492,'monitor','目标删除','monitor:target:delete',4,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4305,492,'monitor','立即检测','monitor:target:probe',5,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4306,493,'monitor','告警查询','monitor:alert:query',6,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4307,493,'monitor','告警处理','monitor:alert:handle',7,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4308,493,'monitor','通知设置查看','monitor:notify:query',8,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(4309,493,'monitor','通知设置维护','monitor:notify:save',9,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(5001,423,'oa','表单查询','oa:form:list',1,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5002,423,'oa','表单编辑','oa:form:save',2,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5003,423,'oa','表单发布/停用','oa:form:publish',3,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5004,423,'oa','表单删除','oa:form:delete',4,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5005,423,'oa','表单数据查看','oa:form:data:list',5,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5006,423,'oa','表单数据提交','oa:form:data:save',6,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5007,423,'oa','表单数据导出','oa:form:data:export',7,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5008,424,'oa','流程查询','oa:model:list',8,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5009,424,'oa','流程编辑','oa:model:save',9,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5010,424,'oa','流程部署/停用','oa:model:deploy',10,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5011,424,'oa','流程删除','oa:model:delete',11,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5012,421,'oa','发起审批','oa:process:start',12,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5013,422,'oa','待办任务','oa:process:todo',13,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5014,422,'oa','已办任务','oa:process:done',14,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5015,422,'oa','我发起的','oa:process:mine',15,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5016,422,'oa','抄送我的','oa:process:cc',16,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5017,422,'oa','审批处理','oa:process:approve',17,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5018,425,'oa','组织查询','oa:org:list',18,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5019,425,'oa','部门负责人维护','oa:org:leader',19,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5020,426,'oa','委托代理查看','oa:proxy:list',20,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5021,426,'oa','代理配置维护','oa:proxy:save',21,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5022,427,'oa','报表统计查看','oa:stats:view',22,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5023,422,'oa','AI 助手使用','oa:ai:use',23,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5024,NULL,'oa','我的调休余额','oa:balance:my',24,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5025,428,'oa','调休余额管理','oa:balance:list',25,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5026,428,'oa','调休余额调整','oa:balance:adjust',26,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5027,428,'oa','考勤参数设置','oa:setting:save',27,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5028,NULL,'oa','OA 参数查看','oa:setting:list',28,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5029,429,'oa','回调配置查看','oa:webhook:list',29,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5030,429,'oa','回调配置维护','oa:webhook:save',30,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5031,429,'oa','回调配置删除','oa:webhook:delete',31,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(5032,485,'oa','AI 智能体设置','oa:ai:config',32,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(6001,432,'mp','公众号查询','mp:account:list',1,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6002,432,'mp','公众号编辑','mp:account:save',2,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6003,432,'mp','公众号删除','mp:account:delete',3,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6004,433,'mp','菜单查询','mp:menu:list',4,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6005,433,'mp','菜单编辑','mp:menu:save',5,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6006,434,'mp','粉丝查询','mp:fan:list',6,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6007,435,'mp','消息查询','mp:message:list',7,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6008,436,'mp','规则查询','mp:rule:list',8,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6009,436,'mp','规则编辑','mp:rule:save',9,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6010,436,'mp','规则删除','mp:rule:delete',10,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(6011,431,'mp','统计查询','mp:stats:list',11,0,NULL,NULL,'2026-09-21 13:10:31',NULL,'2026-09-21 13:10:31',0),(7001,451,'open','应用查询','open:app:list',1,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7002,451,'open','应用详情','open:app:query',2,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7003,451,'open','应用新增','open:app:create',3,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7004,451,'open','应用修改','open:app:update',4,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7005,451,'open','应用启停','open:app:change-status',5,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7006,451,'open','重置密钥','open:app:reset-secret',6,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7007,451,'open','应用删除','open:app:delete',7,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7008,451,'open','API 授权','open:app:grant',8,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7009,451,'open','API 清单查询','open:api:list',9,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7010,452,'open','调用日志查询','open:log:list',10,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7011,453,'open','绑定关系查询','open:bind:list',11,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(7012,455,'open','API 目录启停','open:api:status',12,0,NULL,NULL,'2026-09-21 13:10:32',NULL,'2026-09-21 13:10:32',0),(8001,466,'ai','模型查询','ai:model:list',1,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8002,466,'ai','模型编辑','ai:model:save',2,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8003,466,'ai','模型删除','ai:model:delete',3,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8004,466,'ai','模型连通性测试','ai:model:test',4,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8005,466,'ai','公共智能体管理','ai:agent:platform',5,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8006,462,'ai','智能体查询','ai:agent:list',6,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8007,462,'ai','智能体编辑','ai:agent:save',7,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8008,462,'ai','智能体删除','ai:agent:delete',8,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8009,461,'ai','AI 对话','ai:chat:use',9,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8010,463,'ai','知识库查询','ai:kb:list',10,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8011,463,'ai','知识库编辑','ai:kb:save',11,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8012,463,'ai','文档上传','ai:kb:upload',12,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8013,463,'ai','知识库检索','ai:kb:search',13,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8014,464,'ai','工具查询','ai:tool:list',14,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8015,465,'ai','用量查询','ai:usage:list',15,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8016,467,'ai','MCP 查询','ai:mcp:list',16,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8017,467,'ai','MCP 编辑','ai:mcp:save',17,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8018,467,'ai','API Key 查询','ai:apikey:list',18,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8019,467,'ai','API Key 签发','ai:apikey:create',19,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8020,468,'ai','全局用量查询','ai:usage:platform',20,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8021,464,'ai','工具管理','ai:tool:manage',21,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8022,469,'ai','租户模型配置查询','ai:model:tenant:list',22,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(8023,469,'ai','租户模型配置保存','ai:model:tenant:save',23,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9501,442,'data','数据源查询','data:source:list',1,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9502,442,'data','数据源编辑','data:source:save',2,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9503,442,'data','数据源删除','data:source:delete',3,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9504,443,'data','同步任务查询','data:sync:list',4,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9505,443,'data','同步任务编辑','data:sync:save',5,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9506,443,'data','同步任务删除','data:sync:delete',6,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9507,443,'data','同步任务执行','data:sync:run',7,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9508,444,'data','元数据查询','data:meta:list',8,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9509,444,'data','元数据采集','data:meta:collect',9,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9510,445,'data','API 查询','data:service:list',10,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9511,445,'data','API 编辑','data:service:save',11,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9512,445,'data','API 删除','data:service:delete',12,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9513,445,'data','调用方应用管理','data:service:app',13,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9514,441,'data','概览查询','data:overview:list',14,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9515,446,'data','质量规则查询','data:quality:list',15,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9516,446,'data','质量规则编辑','data:quality:save',16,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9517,446,'data','质量规则删除','data:quality:delete',17,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9518,446,'data','质量规则执行','data:quality:run',18,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9519,445,'data','API 审批','data:service:approve',19,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9520,444,'data','数据导出','data:meta:export',20,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9521,447,'data','查询白名单查询','data:allow:list',21,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9522,447,'data','查询白名单编辑','data:allow:save',22,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0),(9523,447,'data','查询白名单删除','data:allow:delete',23,0,NULL,NULL,'2026-09-21 13:10:30',NULL,'2026-09-21 13:10:30',0);
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `post_code` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码（租户内唯一，代码层校验）',
  `post_name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_id`,`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `code` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '数据范围 1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (10,0,'平台运营','platform_op',1,1,0,'租户/套餐管理，不含菜单与权限点维护',NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色自定义数据部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_dept`
--

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `menu_id` bigint NOT NULL COMMENT '菜单编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (101,0,10,200,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(102,0,10,210,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(103,0,10,220,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `permission_id` bigint NOT NULL COMMENT '权限点编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`),
  KEY `idx_tenant_permission` (`tenant_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限点关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` VALUES (101,0,10,2101,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(102,0,10,2102,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(103,0,10,2103,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(104,0,10,2104,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(105,0,10,2105,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(106,0,10,2106,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(107,0,10,2201,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(108,0,10,2202,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(109,0,10,2203,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(110,0,10,2204,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(111,0,10,2301,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
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
  `domain` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '绑定域名（完整主机名，小写，全局唯一；NULL=未绑定）',
  `contact_user_name` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  UNIQUE KEY `uk_domain` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_tenant`
--

LOCK TABLES `sys_tenant` WRITE;
/*!40000 ALTER TABLE `sys_tenant` DISABLE KEYS */;
INSERT INTO `sys_tenant` VALUES (2101908385836986369,'demo2','租户2',NULL,'mse_tenant2',NULL,NULL,NULL,0,NULL,NULL,1,'2026-09-21 13:36:25',1,'2026-09-21 13:36:25',0);
/*!40000 ALTER TABLE `sys_tenant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_tenant_datasource`
--

DROP TABLE IF EXISTS `sys_tenant_datasource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant_datasource` (
  `id` bigint NOT NULL COMMENT '编号',
  `ds_key` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源 key（隔离配置绑定引用；启动/变更时注册进 dynamic-datasource）',
  `ds_mode` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'dedicated' COMMENT '库模式 shared 共享库（多租户绑定，tenant_id 行级隔离）/ dedicated 独立库（单租户独占）',
  `service_name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '*' COMMENT '所属服务（spring.application.name，如 mse-oa；*=通用）',
  `db_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mysql' COMMENT '数据库类型 mysql/dm/kingbase',
  `host` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主机地址（IP 或域名）',
  `port` int NOT NULL COMMENT '端口（MySQL 3306 / 达梦 5236 / 金仓 54321）',
  `db_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '库名（MySQL/金仓为 database；达梦为 schema，习惯大写）',
  `params` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '额外 JDBC 参数（追加到连接串，如 useSSL=false）',
  `username` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '连接账号',
  `password` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '连接密码（AES 加密存储，密钥 mse.tenant.dynamic-ds.encrypt-key）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_key` (`ds_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户数据源';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_tenant_datasource`
--

LOCK TABLES `sys_tenant_datasource` WRITE;
/*!40000 ALTER TABLE `sys_tenant_datasource` DISABLE KEYS */;
INSERT INTO `sys_tenant_datasource` VALUES (2101898208446427138,'mse_tenant1','dedicated','*','mysql','127.0.0.1',3306,'mse_tenant1',NULL,'root','aGwQiAKYn6Y/hqx6pI1ILOigO4hSr3GxinSi5r2E0IdUWw==',0,NULL,1,'2026-09-21 12:55:59',1,'2026-09-21 12:55:59',0),(2101898285504180226,'mse_tenant2','shared','*','mysql','127.0.0.1',3306,'mse_tenant2',NULL,'root','9EEYRjeXqeJIt9Fsx6cJXlq6u3iMO0mymq25IxyNGP/Djw==',0,NULL,1,'2026-09-21 12:56:17',1,'2026-09-21 13:38:35',0);
/*!40000 ALTER TABLE `sys_tenant_datasource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_tenant_package`
--

DROP TABLE IF EXISTS `sys_tenant_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant_package` (
  `id` bigint NOT NULL COMMENT '编号',
  `name` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐名',
  `menu_ids` varchar(2048) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '可用菜单编号集合（逗号分隔；同时圈定挂靠菜单的权限点，并推导子应用可用性：含任一 type=3 页面即可用）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户套餐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_tenant_package`
--

LOCK TABLES `sys_tenant_package` WRITE;
/*!40000 ALTER TABLE `sys_tenant_package` DISABLE KEYS */;
INSERT INTO `sys_tenant_package` VALUES (1,'基础套餐','1,100,110,120,130,140,150,160,161,162,170,180','系统管理（含岗位/字典/参数/日志/首页配置/在线用户）',0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_tenant_package` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_tenant_service_config`
--

DROP TABLE IF EXISTS `sys_tenant_service_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant_service_config` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `service_name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务名（spring.application.name，如 mse-oa）',
  `isolation_mode` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '隔离模式 INDEPENDENT_DB 独立库 / SHARED_TABLE_SPLIT 共享库分表 / SHARED_FIELD 共享库字段隔离',
  `ds_key` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由数据源 key（空=不路由，落本服务业务主库）',
  `table_suffix` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务级分表后缀（SHARED_TABLE_SPLIT 用，如 _0001）',
  `tenant_column` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段隔离列名（空=tenant_id）',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_service` (`tenant_id`,`service_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户×服务隔离配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_tenant_service_config`
--

LOCK TABLES `sys_tenant_service_config` WRITE;
/*!40000 ALTER TABLE `sys_tenant_service_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_tenant_service_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_tenant_service_table_policy`
--

DROP TABLE IF EXISTS `sys_tenant_service_table_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant_service_table_policy` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `service_name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务名',
  `table_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '逻辑表名（小写）',
  `table_mode` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '表级模式 SPLIT 分表 / FIELD 字段隔离 / GLOBAL 全局共享',
  `table_suffix` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分表后缀（空=服务级 table_suffix）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_service_table` (`tenant_id`,`service_name`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户×服务表级隔离策略';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_tenant_service_table_policy`
--

LOCK TABLES `sys_tenant_service_table_policy` WRITE;
/*!40000 ALTER TABLE `sys_tenant_service_table_policy` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_tenant_service_table_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dept_id` bigint DEFAULT NULL COMMENT '部门编号（数据权限锚点）',
  `username` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'BCrypt',
  `nickname` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户昵称',
  `mobile` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号码',
  `email` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱地址',
  `avatar` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像地址',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `super_admin` tinyint NOT NULL DEFAULT '0' COMMENT '租户管理员标志（权限=套餐域内全量；平台端=全量 *）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_username` (`tenant_id`,`username`),
  KEY `idx_tenant_dept` (`tenant_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,0,900,'platform','$2a$10$hvzCJfttG/KRR4rGXdYQOuQsx2jeFjI3uUX34jx3dhylud93VrHwG','平台管理员',NULL,NULL,NULL,0,1,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0),(3,0,900,'platform_op','$2a$10$Vv30Sfciur6s/Mw/sD8uF.zjDxAevdl0jhx6Tm5EC/eN1TCFuDGtG','平台运营',NULL,NULL,NULL,0,0,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `post_id` bigint NOT NULL COMMENT '岗位编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_tenant_post` (`tenant_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户岗位关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_post`
--

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
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
INSERT INTO `sys_user_role` VALUES (1,0,3,10,NULL,'2026-09-21 12:48:24',NULL,'2026-09-21 12:48:24',0);
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
-- Current Database: `mse_tenant1`
--

/*!40000 DROP DATABASE IF EXISTS `mse_tenant1`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `mse_tenant1` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `mse_tenant1`;

--
-- Table structure for table `ACT_EVT_LOG`
--

DROP TABLE IF EXISTS `ACT_EVT_LOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_EVT_LOG` (
  `LOG_NR_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` longblob,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IS_PROCESSED_` tinyint DEFAULT '0',
  PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_EVT_LOG`
--

LOCK TABLES `ACT_EVT_LOG` WRITE;
/*!40000 ALTER TABLE `ACT_EVT_LOG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_EVT_LOG` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_GE_BYTEARRAY`
--

DROP TABLE IF EXISTS `ACT_GE_BYTEARRAY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_GE_BYTEARRAY` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_BYTEAR_DEPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_GE_BYTEARRAY`
--

LOCK TABLES `ACT_GE_BYTEARRAY` WRITE;
/*!40000 ALTER TABLE `ACT_GE_BYTEARRAY` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_GE_BYTEARRAY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_GE_PROPERTY`
--

DROP TABLE IF EXISTS `ACT_GE_PROPERTY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_GE_PROPERTY` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_GE_PROPERTY`
--

LOCK TABLES `ACT_GE_PROPERTY` WRITE;
/*!40000 ALTER TABLE `ACT_GE_PROPERTY` DISABLE KEYS */;
INSERT INTO `ACT_GE_PROPERTY` VALUES ('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('common.schema.version','8.0.0.0',1),('eventregistry.schema.version','8.0.0.0',1),('next.dbid','1',1),('schema.history','create(8.0.0.0)',1),('schema.version','8.0.0.0',1);
/*!40000 ALTER TABLE `ACT_GE_PROPERTY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ACTINST`
--

DROP TABLE IF EXISTS `ACT_HI_ACTINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ACTINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ACTINST`
--

LOCK TABLES `ACT_HI_ACTINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ACTINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ACTINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ATTACHMENT`
--

DROP TABLE IF EXISTS `ACT_HI_ATTACHMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ATTACHMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `URL_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ATTACHMENT`
--

LOCK TABLES `ACT_HI_ATTACHMENT` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ATTACHMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ATTACHMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_COMMENT`
--

DROP TABLE IF EXISTS `ACT_HI_COMMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_COMMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_COMMENT`
--

LOCK TABLES `ACT_HI_COMMENT` WRITE;
/*!40000 ALTER TABLE `ACT_HI_COMMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_COMMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_DETAIL`
--

DROP TABLE IF EXISTS `ACT_HI_DETAIL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_DETAIL` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_DETAIL`
--

LOCK TABLES `ACT_HI_DETAIL` WRITE;
/*!40000 ALTER TABLE `ACT_HI_DETAIL` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_DETAIL` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_ENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_HI_ENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_ENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_ENTITYLINK`
--

LOCK TABLES `ACT_HI_ENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_HI_ENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_ENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_IDENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_HI_IDENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_IDENTITYLINK`
--

LOCK TABLES `ACT_HI_IDENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_HI_IDENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_IDENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_PROCINST`
--

DROP TABLE IF EXISTS `ACT_HI_PROCINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_PROCINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_PROCINST`
--

LOCK TABLES `ACT_HI_PROCINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_PROCINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_PROCINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_TASKINST`
--

DROP TABLE IF EXISTS `ACT_HI_TASKINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_TASKINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_TASKINST`
--

LOCK TABLES `ACT_HI_TASKINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_TASKINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_TASKINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_TSK_LOG`
--

DROP TABLE IF EXISTS `ACT_HI_TSK_LOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_TSK_LOG` (
  `ID_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ACT_HI_TSK_LOG_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_TSK_LOG`
--

LOCK TABLES `ACT_HI_TSK_LOG` WRITE;
/*!40000 ALTER TABLE `ACT_HI_TSK_LOG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_TSK_LOG` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_HI_VARINST`
--

DROP TABLE IF EXISTS `ACT_HI_VARINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_HI_VARINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(100) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_HI_VARINST`
--

LOCK TABLES `ACT_HI_VARINST` WRITE;
/*!40000 ALTER TABLE `ACT_HI_VARINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_HI_VARINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_BYTEARRAY`
--

DROP TABLE IF EXISTS `ACT_ID_BYTEARRAY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_BYTEARRAY` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_BYTEARRAY`
--

LOCK TABLES `ACT_ID_BYTEARRAY` WRITE;
/*!40000 ALTER TABLE `ACT_ID_BYTEARRAY` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_BYTEARRAY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_GROUP`
--

DROP TABLE IF EXISTS `ACT_ID_GROUP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_GROUP` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_GROUP`
--

LOCK TABLES `ACT_ID_GROUP` WRITE;
/*!40000 ALTER TABLE `ACT_ID_GROUP` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_GROUP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_INFO`
--

DROP TABLE IF EXISTS `ACT_ID_INFO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_INFO` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PASSWORD_` longblob,
  `PARENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_INFO`
--

LOCK TABLES `ACT_ID_INFO` WRITE;
/*!40000 ALTER TABLE `ACT_ID_INFO` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_INFO` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_MEMBERSHIP`
--

DROP TABLE IF EXISTS `ACT_ID_MEMBERSHIP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_MEMBERSHIP` (
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
  KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `ACT_ID_GROUP` (`ID_`),
  CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `ACT_ID_USER` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_MEMBERSHIP`
--

LOCK TABLES `ACT_ID_MEMBERSHIP` WRITE;
/*!40000 ALTER TABLE `ACT_ID_MEMBERSHIP` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_MEMBERSHIP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PRIV`
--

DROP TABLE IF EXISTS `ACT_ID_PRIV`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PRIV` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PRIV`
--

LOCK TABLES `ACT_ID_PRIV` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PRIV` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_PRIV` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PRIV_MAPPING`
--

DROP TABLE IF EXISTS `ACT_ID_PRIV_MAPPING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PRIV_MAPPING` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PRIV_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`),
  KEY `ACT_IDX_PRIV_USER` (`USER_ID_`),
  KEY `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `ACT_ID_PRIV` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PRIV_MAPPING`
--

LOCK TABLES `ACT_ID_PRIV_MAPPING` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PRIV_MAPPING` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_PRIV_MAPPING` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_PROPERTY`
--

DROP TABLE IF EXISTS `ACT_ID_PROPERTY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_PROPERTY` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_PROPERTY`
--

LOCK TABLES `ACT_ID_PROPERTY` WRITE;
/*!40000 ALTER TABLE `ACT_ID_PROPERTY` DISABLE KEYS */;
INSERT INTO `ACT_ID_PROPERTY` VALUES ('schema.version','8.0.0.0',1);
/*!40000 ALTER TABLE `ACT_ID_PROPERTY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_TOKEN`
--

DROP TABLE IF EXISTS `ACT_ID_TOKEN`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_TOKEN` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TOKEN_VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATE_` timestamp(3) NULL DEFAULT NULL,
  `IP_ADDRESS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_AGENT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATA_` varchar(2000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_TOKEN`
--

LOCK TABLES `ACT_ID_TOKEN` WRITE;
/*!40000 ALTER TABLE `ACT_ID_TOKEN` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_TOKEN` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_ID_USER`
--

DROP TABLE IF EXISTS `ACT_ID_USER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_ID_USER` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `FIRST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LAST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DISPLAY_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EMAIL_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PWD_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PICTURE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_ID_USER`
--

LOCK TABLES `ACT_ID_USER` WRITE;
/*!40000 ALTER TABLE `ACT_ID_USER` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_ID_USER` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_PROCDEF_INFO`
--

DROP TABLE IF EXISTS `ACT_PROCDEF_INFO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_PROCDEF_INFO` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
  CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_PROCDEF_INFO`
--

LOCK TABLES `ACT_PROCDEF_INFO` WRITE;
/*!40000 ALTER TABLE `ACT_PROCDEF_INFO` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_PROCDEF_INFO` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_DEPLOYMENT`
--

DROP TABLE IF EXISTS `ACT_RE_DEPLOYMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_DEPLOYMENT` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_DEPLOYMENT`
--

LOCK TABLES `ACT_RE_DEPLOYMENT` WRITE;
/*!40000 ALTER TABLE `ACT_RE_DEPLOYMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_DEPLOYMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_MODEL`
--

DROP TABLE IF EXISTS `ACT_RE_MODEL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_MODEL` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_MODEL`
--

LOCK TABLES `ACT_RE_MODEL` WRITE;
/*!40000 ALTER TABLE `ACT_RE_MODEL` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_MODEL` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RE_PROCDEF`
--

DROP TABLE IF EXISTS `ACT_RE_PROCDEF`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RE_PROCDEF` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RE_PROCDEF`
--

LOCK TABLES `ACT_RE_PROCDEF` WRITE;
/*!40000 ALTER TABLE `ACT_RE_PROCDEF` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RE_PROCDEF` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_ACTINST`
--

DROP TABLE IF EXISTS `ACT_RU_ACTINST`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_ACTINST` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_ACTINST`
--

LOCK TABLES `ACT_RU_ACTINST` WRITE;
/*!40000 ALTER TABLE `ACT_RU_ACTINST` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_ACTINST` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_DEADLETTER_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_DEADLETTER_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_DEADLETTER_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_DEADLETTER_JOB`
--

LOCK TABLES `ACT_RU_DEADLETTER_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_DEADLETTER_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_DEADLETTER_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_ENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_RU_ENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_ENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_ENTITYLINK`
--

LOCK TABLES `ACT_RU_ENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_ENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_ENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EVENT_SUBSCR`
--

DROP TABLE IF EXISTS `ACT_RU_EVENT_SUBSCR`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EVENT_SUBSCR` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EVENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
  KEY `ACT_IDX_EVENT_SUBSCR_EXEC_ID` (`EXECUTION_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_PROC_ID` (`PROC_INST_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EVENT_SUBSCR`
--

LOCK TABLES `ACT_RU_EVENT_SUBSCR` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EVENT_SUBSCR` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EVENT_SUBSCR` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EXECUTION`
--

DROP TABLE IF EXISTS `ACT_RU_EXECUTION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EXECUTION` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint DEFAULT NULL,
  `IS_CONCURRENT_` tinyint DEFAULT NULL,
  `IS_SCOPE_` tinyint DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint DEFAULT NULL,
  `IS_MI_ROOT_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `CACHED_ENT_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `EVT_SUBSCR_COUNT_` int DEFAULT NULL,
  `TASK_COUNT_` int DEFAULT NULL,
  `JOB_COUNT_` int DEFAULT NULL,
  `TIMER_JOB_COUNT_` int DEFAULT NULL,
  `SUSP_JOB_COUNT_` int DEFAULT NULL,
  `DEADLETTER_JOB_COUNT_` int DEFAULT NULL,
  `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
  KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE,
  CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EXECUTION`
--

LOCK TABLES `ACT_RU_EXECUTION` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EXECUTION` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EXECUTION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_EXTERNAL_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_EXTERNAL_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_EXTERNAL_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_EXTERNAL_JOB`
--

LOCK TABLES `ACT_RU_EXTERNAL_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_EXTERNAL_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_EXTERNAL_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_HISTORY_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_HISTORY_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_HISTORY_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_HISTORY_JOB`
--

LOCK TABLES `ACT_RU_HISTORY_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_HISTORY_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_HISTORY_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_IDENTITYLINK`
--

DROP TABLE IF EXISTS `ACT_RU_IDENTITYLINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `ACT_RU_TASK` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_IDENTITYLINK`
--

LOCK TABLES `ACT_RU_IDENTITYLINK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_IDENTITYLINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_IDENTITYLINK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_JOB`
--

LOCK TABLES `ACT_RU_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_SUSPENDED_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_SUSPENDED_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_SUSPENDED_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_SUSPENDED_JOB`
--

LOCK TABLES `ACT_RU_SUSPENDED_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_SUSPENDED_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_SUSPENDED_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_TASK`
--

DROP TABLE IF EXISTS `ACT_RU_TASK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_TASK` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `SUB_TASK_COUNT_` int DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
  KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_TASK`
--

LOCK TABLES `ACT_RU_TASK` WRITE;
/*!40000 ALTER TABLE `ACT_RU_TASK` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_TASK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_TIMER_JOB`
--

DROP TABLE IF EXISTS `ACT_RU_TIMER_JOB`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_TIMER_JOB` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
  KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_TIMER_JOB`
--

LOCK TABLES `ACT_RU_TIMER_JOB` WRITE;
/*!40000 ALTER TABLE `ACT_RU_TIMER_JOB` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_TIMER_JOB` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ACT_RU_VARIABLE`
--

DROP TABLE IF EXISTS `ACT_RU_VARIABLE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACT_RU_VARIABLE` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ACT_RU_VARIABLE`
--

LOCK TABLES `ACT_RU_VARIABLE` WRITE;
/*!40000 ALTER TABLE `ACT_RU_VARIABLE` DISABLE KEYS */;
/*!40000 ALTER TABLE `ACT_RU_VARIABLE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_CHANNEL_DEFINITION`
--

DROP TABLE IF EXISTS `FLW_CHANNEL_DEFINITION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_CHANNEL_DEFINITION` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `IMPLEMENTATION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CHANNEL_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_CHANNEL_DEFINITION`
--

LOCK TABLES `FLW_CHANNEL_DEFINITION` WRITE;
/*!40000 ALTER TABLE `FLW_CHANNEL_DEFINITION` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_CHANNEL_DEFINITION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_DEFINITION`
--

DROP TABLE IF EXISTS `FLW_EVENT_DEFINITION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_DEFINITION` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_EVENT_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_DEFINITION`
--

LOCK TABLES `FLW_EVENT_DEFINITION` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_DEFINITION` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_DEFINITION` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_DEPLOYMENT`
--

DROP TABLE IF EXISTS `FLW_EVENT_DEPLOYMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_DEPLOYMENT` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_DEPLOYMENT`
--

LOCK TABLES `FLW_EVENT_DEPLOYMENT` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_DEPLOYMENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_DEPLOYMENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_EVENT_RESOURCE`
--

DROP TABLE IF EXISTS `FLW_EVENT_RESOURCE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_EVENT_RESOURCE` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_EVENT_RSRC_DPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `FLW_FK_EVENT_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `FLW_EVENT_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_EVENT_RESOURCE`
--

LOCK TABLES `FLW_EVENT_RESOURCE` WRITE;
/*!40000 ALTER TABLE `FLW_EVENT_RESOURCE` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_EVENT_RESOURCE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_RU_BATCH`
--

DROP TABLE IF EXISTS `FLW_RU_BATCH`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_RU_BATCH` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BATCH_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_RU_BATCH`
--

LOCK TABLES `FLW_RU_BATCH` WRITE;
/*!40000 ALTER TABLE `FLW_RU_BATCH` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_RU_BATCH` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FLW_RU_BATCH_PART`
--

DROP TABLE IF EXISTS `FLW_RU_BATCH_PART`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FLW_RU_BATCH_PART` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `BATCH_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESULT_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_BATCH_PART` (`BATCH_ID_`),
  CONSTRAINT `FLW_FK_BATCH_PART_PARENT` FOREIGN KEY (`BATCH_ID_`) REFERENCES `FLW_RU_BATCH` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FLW_RU_BATCH_PART`
--

LOCK TABLES `FLW_RU_BATCH_PART` WRITE;
/*!40000 ALTER TABLE `FLW_RU_BATCH_PART` DISABLE KEYS */;
/*!40000 ALTER TABLE `FLW_RU_BATCH_PART` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_agent`
--

DROP TABLE IF EXISTS `ai_agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码（全局唯一）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像（emoji 或 URL）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '系统提示词（支持 {{var}} 占位）',
  `model_id` bigint DEFAULT NULL COMMENT '绑定模型（ai_model.id，空=平台默认模型）',
  `temperature` double DEFAULT NULL COMMENT '温度覆盖',
  `welcome_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开场白',
  `tool_codes` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工具编码列表（逗号分隔）',
  `kb_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '知识库 ID 列表（逗号分隔）',
  `orchestration_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'simple' COMMENT '编排模式：simple 简单配置 / flow 画布编排',
  `graph_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '画布编排 DSL（mse-flow/v1 JSON，flow 模式生效）',
  `draft_graph_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '画布编排草稿（编辑器保存目标，发布时校验并拷贝到 graph_json）',
  `scope` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tenant' COMMENT 'platform 平台公共 / tenant 租户自有',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（platform 时为 0）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 已发布 1 下线',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_code` (`agent_code`,`deleted`),
  KEY `idx_scope` (`scope`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能体（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_agent`
--

LOCK TABLES `ai_agent` WRITE;
/*!40000 ALTER TABLE `ai_agent` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_api_key`
--

DROP TABLE IF EXISTS `ai_api_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_api_key` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用 ID',
  `key_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API Key 哈希（SHA-256）',
  `key_prefix` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Key 前缀（识别用）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '业务归属租户',
  `agent_codes` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体白名单（空=全部已发布）',
  `rate_per_minute` int DEFAULT NULL COMMENT '每分钟调用上限（0/空=不限）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 密钥（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_api_key`
--

LOCK TABLES `ai_api_key` WRITE;
/*!40000 ALTER TABLE `ai_api_key` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_api_key` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_message`
--

DROP TABLE IF EXISTS `ai_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_message` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `session_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话 ID（ai_chat_session.id 字符串）',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'user / assistant',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `ext` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展信息 JSON（assistant 消息：datasets 工具数据结果 / sources 引用来源，历史回放恢复用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对话消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_message`
--

LOCK TABLES `ai_chat_message` WRITE;
/*!40000 ALTER TABLE `ai_chat_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_session`
--

DROP TABLE IF EXISTS `ai_chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_session` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话标题',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码',
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `last_active_time` datetime DEFAULT NULL COMMENT '最近活跃时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_agent` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对话会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_session`
--

LOCK TABLES `ai_chat_session` WRITE;
/*!40000 ALTER TABLE `ai_chat_session` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_kb_chunk`
--

DROP TABLE IF EXISTS `ai_kb_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_kb_chunk` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `kb_id` bigint NOT NULL COMMENT '知识库 ID',
  `document_id` bigint NOT NULL COMMENT '文档 ID',
  `seq` int NOT NULL DEFAULT '0' COMMENT '切片序号',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '切片正文',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_kb_doc` (`kb_id`,`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库切片';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_kb_chunk`
--

LOCK TABLES `ai_kb_chunk` WRITE;
/*!40000 ALTER TABLE `ai_kb_chunk` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_kb_chunk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_kb_document`
--

DROP TABLE IF EXISTS `ai_kb_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_kb_document` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `kb_id` bigint NOT NULL COMMENT '知识库 ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件类型',
  `size_bytes` bigint DEFAULT NULL COMMENT '文件大小',
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原文件归档路径（支撑重新解析/重建索引）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 待处理 1 处理中 2 完成 3 失败',
  `chunk_count` int DEFAULT NULL COMMENT '切片数量',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_kb_document`
--

LOCK TABLES `ai_kb_document` WRITE;
/*!40000 ALTER TABLE `ai_kb_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_kb_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_base`
--

DROP TABLE IF EXISTS `ai_knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_base` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `chunk_size` int DEFAULT NULL COMMENT '切片目标大小（token）',
  `chunk_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'general' COMMENT '切片方法（general/qa/table/book/one）',
  `chunk_overlap` int DEFAULT '100' COMMENT '切片重叠大小（token，general/book 生效）',
  `top_k` int DEFAULT NULL COMMENT '默认检索条数',
  `similarity_threshold` decimal(4,3) DEFAULT '0.500' COMMENT '相似度阈值（0~1）',
  `embedding_model_id` bigint DEFAULT NULL COMMENT '绑定向量模型（ai_model.id，model_type=embedding；空=按旧版全局配置/平台默认向量模型解析）',
  `rerank_model_id` bigint DEFAULT NULL COMMENT '绑定重排模型（ai_model.id，model_type=rerank；空=按旧版平台级配置解析，均未配置则不重排）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_base`
--

LOCK TABLES `ai_knowledge_base` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_base` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_base` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_mcp_server`
--

DROP TABLE IF EXISTS `ai_mcp_server`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_mcp_server` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共数据）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务名（唯一，工具编码引用）',
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '基础地址',
  `sse_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'SSE 路径（默认 /sse）',
  `transport` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'sse' COMMENT '传输方式',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部 MCP Server 注册（平台公共 tenant_id=0）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_mcp_server`
--

LOCK TABLES `ai_mcp_server` WRITE;
/*!40000 ALTER TABLE `ai_mcp_server` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_mcp_server` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model`
--

DROP TABLE IF EXISTS `ai_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共数据）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称',
  `provider` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商标注（deepseek/zhipu/moonshot/dashscope/openai/anthropic/ollama/xfyun/openai-compat，仅展示与预设联动）',
  `protocol` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'openai-chat' COMMENT '接口类型（openai-chat=OpenAI兼容 chat/completions；anthropic-messages=Claude 原生 /v1/messages；向量模型行固定 openai-chat）',
  `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'chat' COMMENT '模型类型（chat=对话模型 embedding=向量模型 rerank=重排模型，一行只表达一种能力）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '默认向量模型标记（仅 model_type=embedding 有意义，全局唯一）',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型端点（openai-chat 自带版本前缀如 https://api.deepseek.com；anthropic-messages 如 https://api.anthropic.com）',
  `api_key_enc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'API Key 密文（AES-GCM）',
  `chat_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对话模型名（model_type=embedding 时为空串）',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型名（model_type=embedding 时为本行向量模型名；chat 行为旧版遗留字段，仅旧数据兼容读取）',
  `embedding_base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型独立端点（空复用 base_url）',
  `embedding_api_key_enc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型独立 API Key 密文（空复用 api_key_enc）',
  `temperature` double DEFAULT NULL COMMENT '默认温度',
  `top_p` double DEFAULT NULL COMMENT '默认 topP',
  `max_tokens` int DEFAULT NULL COMMENT '默认最大输出 tokens',
  `failover_model_id` bigint DEFAULT NULL COMMENT '备用对话模型（ai_model.id，model_type=chat；主模型瞬态错误重试仍失败时自动切换，空=不切换）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_test_time` datetime DEFAULT NULL COMMENT '最近连通性测试时间',
  `last_test_ok` tinyint DEFAULT NULL COMMENT '最近测试结果：0 失败 1 成功',
  `last_test_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近测试失败原因',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型配置（平台公共 tenant_id=0）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model`
--

LOCK TABLES `ai_model` WRITE;
/*!40000 ALTER TABLE `ai_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model_tenant`
--

DROP TABLE IF EXISTS `ai_model_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_tenant` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `model_id` bigint NOT NULL COMMENT '模型 ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_model` (`model_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型-租户可用范围（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model_tenant`
--

LOCK TABLES `ai_model_tenant` WRITE;
/*!40000 ALTER TABLE `ai_model_tenant` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_tenant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model_tenant_config`
--

DROP TABLE IF EXISTS `ai_model_tenant_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_tenant_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `model_id` bigint NOT NULL COMMENT '模型 ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '覆盖端点（空=沿用平台配置）',
  `api_key_enc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '覆盖 API Key 密文 AES-GCM（空=沿用平台配置）',
  `temperature` double DEFAULT NULL COMMENT '覆盖温度（空=沿用平台配置）',
  `top_p` double DEFAULT NULL COMMENT '覆盖 topP（空=沿用平台配置）',
  `max_tokens` int DEFAULT NULL COMMENT '覆盖最大输出 tokens（空=沿用平台配置）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_model` (`model_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型租户级覆盖配置（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model_tenant_config`
--

LOCK TABLES `ai_model_tenant_config` WRITE;
/*!40000 ALTER TABLE `ai_model_tenant_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_tenant_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_tool`
--

DROP TABLE IF EXISTS `ai_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_tool` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（0=平台公共系统工具，>0=租户自有工具）',
  `tool_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具编码（全局唯一）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工具名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '功能说明（面向模型）',
  `parameters_json_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '入参 JSON Schema',
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'rpc' COMMENT 'builtin / local / rpc / http / dify',
  `rpc_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'RPC 提供方服务名（Dubbo group）',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '外部工具连接配置（source=http/dify 时生效）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_code` (`tool_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工具登记（tenant_id=0 平台公共系统工具，>0 租户自有）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_tool`
--

LOCK TABLES `ai_tool` WRITE;
/*!40000 ALTER TABLE `ai_tool` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_tool` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_tool_log`
--

DROP TABLE IF EXISTS `ai_tool_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_tool_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `tool_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具编码',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体编码',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话 ID',
  `arguments` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '入参 JSON',
  `result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '出参内容',
  `success` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_tool` (`tenant_id`,`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工具调用审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_tool_log`
--

LOCK TABLES `ai_tool_log` WRITE;
/*!40000 ALTER TABLE `ai_tool_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_tool_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_usage_log`
--

DROP TABLE IF EXISTS `ai_usage_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_usage_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'gateway / rpc / openapi',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体编码',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模型名',
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话 ID',
  `user_id` bigint DEFAULT NULL COMMENT '触发用户（openapi 通道为空）',
  `prompt_tokens` int DEFAULT NULL COMMENT '提示词 tokens 消耗',
  `completion_tokens` int DEFAULT NULL COMMENT '补全 tokens 消耗',
  `total_tokens` int DEFAULT NULL COMMENT '总 tokens 消耗',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`create_time`),
  KEY `idx_agent` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 用量流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_usage_log`
--

LOCK TABLES `ai_usage_log` WRITE;
/*!40000 ALTER TABLE `ai_usage_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_usage_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_workflow_run`
--

DROP TABLE IF EXISTS `ai_workflow_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_workflow_run` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码',
  `agent_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体名称快照',
  `channel` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发通道：gateway 对话 / debug 试运行 / rpc 无状态 / openapi 开放接口',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话号（debug 无会话为空）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '运行结果：success / failed / cancelled',
  `query_text` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户输入（截断存档）',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '最终回答（截断存档）',
  `error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '总耗时毫秒',
  `node_total` int DEFAULT NULL COMMENT '节点轨迹条数',
  `traces_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '节点轨迹快照 JSON（与 SSE node 事件同构）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（平台调试行为 0）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_agent` (`agent_code`,`create_time`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流运行历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_workflow_run`
--

LOCK TABLES `ai_workflow_run` WRITE;
/*!40000 ALTER TABLE `ai_workflow_run` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_workflow_run` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api`
--

DROP TABLE IF EXISTS `dat_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 名称',
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '开放路径 /openapi/data/{path}，租户内唯一',
  `datasource_id` bigint NOT NULL COMMENT '查询的数据源（0 = 中台库）',
  `sql_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SQL 模板（仅 SELECT，:param 占位）',
  `params_json` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数定义 JSON [{name,type,required,remark}]',
  `page_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '0 关 1 开（pageNum/pageSize 自动分页包裹）',
  `max_rows` int NOT NULL DEFAULT '1000' COMMENT '单次返回行数上限',
  `timeout_ms` int NOT NULL DEFAULT '10000' COMMENT '查询超时（毫秒）',
  `qps_limit` int NOT NULL DEFAULT '60' COMMENT '每分钟最大调用次数（0 = 跟随全局默认）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 发布 1 下线',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_path` (`tenant_id`,`path`,`deleted`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api`
--

LOCK TABLES `dat_api` WRITE;
/*!40000 ALTER TABLE `dat_api` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_app`
--

DROP TABLE IF EXISTS `dat_api_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_app` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `app_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局唯一',
  `app_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密文（AES-GCM）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_app_key` (`app_key`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务调用方应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_app`
--

LOCK TABLES `dat_api_app` WRITE;
/*!40000 ALTER TABLE `dat_api_app` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_approval`
--

DROP TABLE IF EXISTS `dat_api_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_approval` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `api_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'API 名称',
  `action` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'submit/approve/reject/offline',
  `comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人姓名',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`,`api_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 审批记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_approval`
--

LOCK TABLES `dat_api_approval` WRITE;
/*!40000 ALTER TABLE `dat_api_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_grant`
--

DROP TABLE IF EXISTS `dat_api_grant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_grant` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `app_id` bigint NOT NULL COMMENT 'dat_api_app.id',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（空=永久）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_api_app` (`api_id`,`app_id`,`deleted`),
  KEY `idx_tenant_app` (`tenant_id`,`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 授权';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_grant`
--

LOCK TABLES `dat_api_grant` WRITE;
/*!40000 ALTER TABLE `dat_api_grant` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_grant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_log`
--

DROP TABLE IF EXISTS `dat_api_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `app_id` bigint NOT NULL COMMENT '调用方应用 ID',
  `params` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '本次请求参数 JSON',
  `row_count` int DEFAULT NULL COMMENT '返回行数',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'success / failed',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端 IP',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`,`api_id`,`create_time`),
  KEY `idx_tenant_app` (`tenant_id`,`app_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 调用日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_log`
--

LOCK TABLES `dat_api_log` WRITE;
/*!40000 ALTER TABLE `dat_api_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_datasource`
--

DROP TABLE IF EXISTS `dat_datasource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_datasource` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'mysql / dm / kingbase',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口',
  `db_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '库名/schema',
  `extra_params` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JDBC 追加参数（如 useSSL=false&…）',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '口令密文（AES-GCM）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_test_time` datetime DEFAULT NULL COMMENT '最近连通性测试时间',
  `last_test_ok` tinyint DEFAULT NULL COMMENT '最近测试结果：0 失败 1 成功',
  `last_test_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近测试失败原因',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部数据源注册';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_datasource`
--

LOCK TABLES `dat_datasource` WRITE;
/*!40000 ALTER TABLE `dat_datasource` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_datasource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_lineage`
--

DROP TABLE IF EXISTS `dat_lineage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_lineage` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `job_id` bigint NOT NULL COMMENT '同步任务',
  `job_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称',
  `datasource_id` bigint NOT NULL COMMENT '来源数据源',
  `source_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'table' COMMENT 'table / sql',
  `source_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源表（sql 来源存查询摘要）',
  `target_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标 ODS 表',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_job` (`tenant_id`,`job_id`,`deleted`),
  KEY `idx_tenant_target` (`tenant_id`,`target_table`),
  KEY `idx_tenant_source` (`tenant_id`,`source_table`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务级血缘（同步任务：源表 → ODS）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_lineage`
--

LOCK TABLES `dat_lineage` WRITE;
/*!40000 ALTER TABLE `dat_lineage` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_lineage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_meta_column`
--

DROP TABLE IF EXISTS `dat_meta_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_meta_column` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `meta_table_id` bigint NOT NULL COMMENT '所属 dat_meta_table',
  `column_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '列名',
  `data_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据类型',
  `nullable` tinyint NOT NULL DEFAULT '1' COMMENT '是否可空（1 是 0 否）',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列注释',
  `ordinal` int NOT NULL DEFAULT '0' COMMENT '列序号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_table` (`tenant_id`,`meta_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='元数据-列';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_meta_column`
--

LOCK TABLES `dat_meta_column` WRITE;
/*!40000 ALTER TABLE `dat_meta_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_meta_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_meta_table`
--

DROP TABLE IF EXISTS `dat_meta_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_meta_table` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `datasource_id` bigint NOT NULL COMMENT '0 = 中台内部（ODS/中台库自身）',
  `table_schema` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属 schema',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名',
  `category` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'source' COMMENT 'source 源库表 / ods 中台贴源表',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表注释',
  `row_count` bigint DEFAULT NULL COMMENT ' approximate 行数',
  `collected_time` datetime NOT NULL COMMENT '最近采集时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_ds_table` (`tenant_id`,`datasource_id`,`table_name`,`deleted`),
  KEY `idx_tenant_cat` (`tenant_id`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='元数据-表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_meta_table`
--

LOCK TABLES `dat_meta_table` WRITE;
/*!40000 ALTER TABLE `dat_meta_table` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_meta_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_quality_report`
--

DROP TABLE IF EXISTS `dat_quality_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_quality_report` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `rule_id` bigint NOT NULL COMMENT '质量规则 ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名冗余',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'success / failed',
  `total_rows` bigint DEFAULT NULL COMMENT '检测总行数',
  `bad_rows` bigint DEFAULT NULL COMMENT '坏数据行数',
  `pass_rate` decimal(5,2) DEFAULT NULL COMMENT '通过率（%）',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `trigger_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT 'manual / sched',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_rule` (`tenant_id`,`rule_id`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据质量报告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_quality_report`
--

LOCK TABLES `dat_quality_report` WRITE;
/*!40000 ALTER TABLE `dat_quality_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_quality_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_quality_rule`
--

DROP TABLE IF EXISTS `dat_quality_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_quality_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称',
  `datasource_id` bigint NOT NULL DEFAULT '0' COMMENT '0 = 中台库贴源层',
  `target_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标表',
  `rule_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'not_null/unique/range/enum/custom_sql/row_count_fluctuation',
  `column_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '检测列（custom_sql/fluctuation 无需）',
  `rule_config` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则配置 JSON（min/max/values/sql/thresholdPct）',
  `cron` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调度 cron（空=仅手动）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `last_pass` tinyint DEFAULT NULL COMMENT '最近结果：0 未通过 1 通过',
  `last_pass_rate` decimal(5,2) DEFAULT NULL COMMENT '最近通过率（%）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据质量规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_quality_rule`
--

LOCK TABLES `dat_quality_rule` WRITE;
/*!40000 ALTER TABLE `dat_quality_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_quality_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_query_allowlist`
--

DROP TABLE IF EXISTS `dat_query_allowlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_query_allowlist` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '放行的表名（小写归一化）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_table` (`tenant_id`,`table_name`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='中台库通道查询白名单（ods_ 之外可查业务表）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_query_allowlist`
--

LOCK TABLES `dat_query_allowlist` WRITE;
/*!40000 ALTER TABLE `dat_query_allowlist` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_query_allowlist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_sync_job`
--

DROP TABLE IF EXISTS `dat_sync_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_sync_job` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `datasource_id` bigint NOT NULL COMMENT '来源数据源',
  `source_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'table' COMMENT 'table 表 / sql 自定义查询',
  `source_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '源表名（source_type=table）',
  `query_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '自定义查询 SQL（source_type=sql，仅 SELECT）',
  `target_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标 ODS 表（引擎自动建，ods_ 前缀）',
  `sync_mode` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'full' COMMENT 'full 全量 / incr 增量',
  `incr_column` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '增量水位列（时间戳/自增 ID）',
  `watermark` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前水位值（引擎回写）',
  `cron` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调度 cron（Spring 表达式，空=仅手动）',
  `dep_job_id` bigint DEFAULT NULL COMMENT '前置任务（成功完成后级联触发本任务）',
  `row_count` bigint NOT NULL DEFAULT '0' COMMENT '最近一次同步行数',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_ds` (`tenant_id`,`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据集成同步任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_sync_job`
--

LOCK TABLES `dat_sync_job` WRITE;
/*!40000 ALTER TABLE `dat_sync_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_sync_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_sync_job_log`
--

DROP TABLE IF EXISTS `dat_sync_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_sync_job_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `job_id` bigint NOT NULL COMMENT '同步任务ID',
  `trigger_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'manual 手动 / sched 调度',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'running / success / failed',
  `sync_mode` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '本次执行模式 full / incr',
  `row_count` bigint NOT NULL DEFAULT '0' COMMENT '同步行数',
  `watermark_used` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行前水位',
  `watermark_new` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行后水位',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_job` (`tenant_id`,`job_id`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同步任务运行日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_sync_job_log`
--

LOCK TABLES `dat_sync_job_log` WRITE;
/*!40000 ALTER TABLE `dat_sync_job_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_sync_job_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_event`
--

DROP TABLE IF EXISTS `demo_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_event` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `event_type` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型',
  `payload` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '事件负载（JSON）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='已消费租户事件（共享主库）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_event`
--

LOCK TABLES `demo_event` WRITE;
/*!40000 ALTER TABLE `demo_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_order`
--

DROP TABLE IF EXISTS `demo_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `order_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `amount` decimal(12,2) DEFAULT NULL COMMENT '订单金额',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order_no` (`tenant_id`,`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例订单（@TenantDatasource 混合路由）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_order`
--

LOCK TABLES `demo_order` WRITE;
/*!40000 ALTER TABLE `demo_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_stock`
--

DROP TABLE IF EXISTS `demo_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_stock` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `remain` int NOT NULL DEFAULT '0' COMMENT '剩余可下单数量',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例库存（共享主库，跨库事务的另一端）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_stock`
--

LOCK TABLES `demo_stock` WRITE;
/*!40000 ALTER TABLE `demo_stock` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_file`
--

DROP TABLE IF EXISTS `fs_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_file` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花，物理对象键同名）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始文件名',
  `storage_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储键（{tenantId}/{yyyyMM}/{fileId}.{ext}）',
  `storage_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储类型：local / rustfs',
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容 MIME 类型',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '字节数',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务分类（如 oa/attachment）',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务关联 ID',
  `access_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tenant' COMMENT '访问级别：tenant 登录本租户 / public 匿名可读（/fs/asset/{id}）',
  `content_md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容 MD5（32 位小写 hex；同租户同 MD5 秒传复用物理对象）',
  `upload_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分片直传会话 ID（仅 rustfs 直传进行中非空，complete/abort 后清空/删行）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_md5` (`content_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件元数据（物理对象按 storage_type 由 FileStorage 实现保管）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_file`
--

LOCK TABLES `fs_file` WRITE;
/*!40000 ALTER TABLE `fs_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `fs_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_alert`
--

DROP TABLE IF EXISTS `mon_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_alert` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `target_id` bigint NOT NULL COMMENT '监控目标',
  `target_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标名称（冗余，目标删除后仍可读）',
  `level` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'warn' COMMENT '级别：warn/error',
  `rule_key` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则键：down/latency/cpu/mem/disk/redis_mem/mysql_threads',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警内容',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'firing' COMMENT '状态：firing/resolved',
  `first_trigger_time` datetime DEFAULT NULL COMMENT '首次触发时间',
  `last_trigger_time` datetime DEFAULT NULL COMMENT '最近触发时间',
  `resolve_time` datetime DEFAULT NULL COMMENT '恢复时间',
  `notify_result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '通知结果 JSON（inbox/webhook）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_alert_target` (`target_id`,`status`),
  KEY `idx_mon_alert_time` (`last_trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控告警';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_alert`
--

LOCK TABLES `mon_alert` WRITE;
/*!40000 ALTER TABLE `mon_alert` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_record`
--

DROP TABLE IF EXISTS `mon_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_record` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `target_id` bigint NOT NULL COMMENT '监控目标',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '采集状态：up/down',
  `latency_ms` int DEFAULT NULL COMMENT '响应耗时（ms）',
  `metrics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '指标集 JSON（按类型：cpu_usage_pct/threads_connected/qps…）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_record_target` (`target_id`,`collect_time`),
  KEY `idx_mon_record_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控采样记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_record`
--

LOCK TABLES `mon_record` WRITE;
/*!40000 ALTER TABLE `mon_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_setting`
--

DROP TABLE IF EXISTS `mon_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_setting` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `config_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键（notify）',
  `config_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置值 JSON（NotifySetting）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mon_setting_key` (`tenant_id`,`config_key`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控服务配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_setting`
--

LOCK TABLES `mon_setting` WRITE;
/*!40000 ALTER TABLE `mon_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_target`
--

DROP TABLE IF EXISTS `mon_target`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_target` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标名称',
  `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：service/http/mysql/redis/rocketmq/os',
  `service_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务/HTTP 地址（http://host:port）',
  `host` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主机（mysql/redis/rocketmq；os 固定本机）',
  `port` int DEFAULT NULL COMMENT '端口',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认证用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认证密码（界面脱敏）',
  `collect_interval` int DEFAULT '30' COMMENT '采集间隔（秒）',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0 启用 1 停用',
  `alert_config` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警规则 JSON：{down,latencyMs,cpuPercent,memPercent,diskPercent,redisMemPercent,mysqlMaxThreads}',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'unknown' COMMENT '最近状态：up/down/unknown',
  `status_time` datetime DEFAULT NULL COMMENT '最近状态时间',
  `last_latency_ms` int DEFAULT NULL COMMENT '最近响应耗时（ms）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_target_type` (`target_type`),
  KEY `idx_mon_target_status` (`enabled`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控目标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_target`
--

LOCK TABLES `mon_target` WRITE;
/*!40000 ALTER TABLE `mon_target` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_target` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_account`
--

DROP TABLE IF EXISTS `mp_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_account` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公众号 AppID（全局唯一：回调按此路由）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公众号名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'subscription' COMMENT 'subscription 订阅号 / service 服务号',
  `verified` tinyint NOT NULL DEFAULT '0' COMMENT '是否已认证：0 否 1 是',
  `app_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'AppSecret 密文（AES-GCM）',
  `callback_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器配置 Token 密文',
  `aes_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'EncodingAESKey 密文',
  `encrypt_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'safe' COMMENT 'plain/compatible/safe',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_app_id` (`app_id`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公众号账号';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_account`
--

LOCK TABLES `mp_account` WRITE;
/*!40000 ALTER TABLE `mp_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_fan`
--

DROP TABLE IF EXISTS `mp_fan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_fan` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '粉丝 OpenID',
  `subscribed` tinyint NOT NULL DEFAULT '1' COMMENT '1 关注中 0 已取关',
  `subscribe_time` datetime DEFAULT NULL COMMENT '关注时间',
  `unsubscribe_time` datetime DEFAULT NULL COMMENT '取关时间',
  `subscribe_scene` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关注场景（ADD_SCENE_QR_CODE 等）',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '粉丝昵称',
  `head_img_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像地址',
  `sex` tinyint NOT NULL DEFAULT '0' COMMENT '1 男 2 女 0 未知',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省份',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市',
  `union_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开放平台 UnionID',
  `profile_incomplete` tinyint NOT NULL DEFAULT '0' COMMENT '1 = 资料拉取失败（未认证号常见）',
  `last_active_time` datetime DEFAULT NULL COMMENT '最近互动时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account_openid` (`account_id`,`open_id`),
  KEY `idx_tenant_sub` (`tenant_id`,`subscribed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='粉丝（回调事件自动维护，管理端只读）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_fan`
--

LOCK TABLES `mp_fan` WRITE;
/*!40000 ALTER TABLE `mp_fan` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_fan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_menu`
--

DROP TABLE IF EXISTS `mp_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_menu` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `menu_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '与微信 menu/create 同构 JSON',
  `synced_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿有改动 1 已同步',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account` (`account_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义菜单草稿（每号一份）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_menu`
--

LOCK TABLES `mp_menu` WRITE;
/*!40000 ALTER TABLE `mp_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_message`
--

DROP TABLE IF EXISTS `mp_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_message` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '粉丝 OpenID',
  `direction` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'in' COMMENT 'in 收 / out 发（预留）',
  `kind` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'event / message',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'subscribe/click/text/image/…（归一化小写）',
  `msg_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信消息 id（事件类为空）',
  `dedup_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等锚点 MD5（挡微信重试）',
  `payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '归一化载荷 JSON（snake_case）',
  `raw_xml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '解密后明文报文（超长截断）',
  `reply_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '被动回复缓存（明文 XML，重试复用）',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链路追踪ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_dedup` (`dedup_key`),
  KEY `idx_tenant_account` (`tenant_id`,`account_id`,`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息/事件流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_message`
--

LOCK TABLES `mp_message` WRITE;
/*!40000 ALTER TABLE `mp_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_reply_rule`
--

DROP TABLE IF EXISTS `mp_reply_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_reply_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名',
  `match_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'subscribe/unsubscribe/click/msg_any/keyword_exact/keyword_contains/default',
  `match_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单 Key / 关键词（, 空格 | 分隔多值）',
  `reply_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'text' COMMENT 'text / news',
  `reply_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '{"content":…} 或 {"items":[…≤8]}',
  `priority` int NOT NULL DEFAULT '0' COMMENT '同类型内大者先',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用（0 停用 1 启用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_account` (`tenant_id`,`account_id`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动回复规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_reply_rule`
--

LOCK TABLES `mp_reply_rule` WRITE;
/*!40000 ALTER TABLE `mp_reply_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_reply_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_token`
--

DROP TABLE IF EXISTS `mp_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_token` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'token 密文（运行期内存缓存为主）',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  `err_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败错误码（成功时清空）',
  `err_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败错误信息（成功时清空）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account` (`account_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='access_token 落库状态（失败原因供管理端排查）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_token`
--

LOCK TABLES `mp_token` WRITE;
/*!40000 ALTER TABLE `mp_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_ai_agent_map`
--

DROP TABLE IF EXISTS `oa_ai_agent_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_ai_agent_map` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `scene` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '场景（assist 审批助手 / process 流程生成 / fill 智能填单）',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '映射的智能体编码（ai_agent.agent_code；空串=使用内置默认）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_scene` (`tenant_id`,`scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 场景 → 智能体映射（租户隔离；保存即生效，不删行）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_ai_agent_map`
--

LOCK TABLES `oa_ai_agent_map` WRITE;
/*!40000 ALTER TABLE `oa_ai_agent_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_ai_agent_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_cc`
--

DROP TABLE IF EXISTS `oa_cc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_cc` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `node_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '抄送节点编号',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '抄送节点名称',
  `user_id` bigint NOT NULL COMMENT '被抄送人',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程标题快照',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='抄送记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_cc`
--

LOCK TABLES `oa_cc` WRITE;
/*!40000 ALTER TABLE `oa_cc` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_cc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_delegate_proxy`
--

DROP TABLE IF EXISTS `oa_delegate_proxy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_delegate_proxy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '委托人',
  `proxy_user_id` bigint NOT NULL COMMENT '受托人',
  `start_time` datetime NOT NULL COMMENT '生效开始时间',
  `end_time` datetime NOT NULL COMMENT '生效结束时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 0 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '委托说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_proxy_user` (`tenant_id`,`proxy_user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批委托代理（v1 全流程代理，不区分流程模板）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_delegate_proxy`
--

LOCK TABLES `oa_delegate_proxy` WRITE;
/*!40000 ALTER TABLE `oa_delegate_proxy` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_delegate_proxy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_dept_leader`
--

DROP TABLE IF EXISTS `oa_dept_leader`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_dept_leader` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `user_id` bigint NOT NULL COMMENT '负责人用户编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_dept_user` (`tenant_id`,`dept_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门负责人（基座 sys_dept 无负责人字段，OA 侧扩展）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_dept_leader`
--

LOCK TABLES `oa_dept_leader` WRITE;
/*!40000 ALTER TABLE `oa_dept_leader` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_dept_leader` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_file`
--

DROP TABLE IF EXISTS `oa_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_file` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小（字节）',
  `ext` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件扩展名',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储路径（local 模式；fs 模式为占位符 -）',
  `fs_file_id` bigint DEFAULT NULL COMMENT 'mse-fs 文件编号（storage_type=fs 时物理寻址键）',
  `storage_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'local' COMMENT '存储模式：local 本地磁盘 / fs mse-fs',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='附件元数据（local 落 mse.oa.file-dir / fs 存 mse-fs）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_file`
--

LOCK TABLES `oa_file` WRITE;
/*!40000 ALTER TABLE `oa_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form`
--

DROP TABLE IF EXISTS `oa_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `category_id` bigint DEFAULT NULL COMMENT '分类编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表单名称',
  `icon` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '设计器文档 JSON（草稿态）',
  `version` int NOT NULL DEFAULT '0' COMMENT '已发布版本号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿 1 已发布 2 停用',
  `source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CUSTOM' COMMENT '来源：SYSTEM 内置模板 CUSTOM 自定义',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内置模板标识（SYSTEM 行，租户内唯一）',
  `template_version` int DEFAULT NULL COMMENT '安装时的模板版本号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_template` (`tenant_id`,`template_code`),
  KEY `idx_tenant` (`tenant_id`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form`
--

LOCK TABLES `oa_form` WRITE;
/*!40000 ALTER TABLE `oa_form` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_category`
--

DROP TABLE IF EXISTS `oa_form_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_category` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单/流程分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_category`
--

LOCK TABLES `oa_form_category` WRITE;
/*!40000 ALTER TABLE `oa_form_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_data`
--

DROP TABLE IF EXISTS `oa_form_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_data` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `form_id` bigint NOT NULL COMMENT '表单编号',
  `form_version` int DEFAULT NULL COMMENT '提交时的表单版本号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联流程实例（独立提交为空）',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据摘要',
  `data_values` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '控件数据 JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_form` (`tenant_id`,`form_id`),
  KEY `idx_instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单提交数据（JSON 存储）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_data`
--

LOCK TABLES `oa_form_data` WRITE;
/*!40000 ALTER TABLE `oa_form_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_version`
--

DROP TABLE IF EXISTS `oa_form_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `form_id` bigint NOT NULL COMMENT '表单编号',
  `version` int NOT NULL COMMENT '版本号',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '设计器文档 JSON 快照',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_form_version` (`tenant_id`,`form_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单发布版本快照（在途流程渲染不受后续编辑影响）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_version`
--

LOCK TABLES `oa_form_version` WRITE;
/*!40000 ALTER TABLE `oa_form_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_model`
--

DROP TABLE IF EXISTS `oa_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `category_id` bigint DEFAULT NULL COMMENT '分类编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程名称',
  `icon` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `form_id` bigint DEFAULT NULL COMMENT '绑定表单',
  `design_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SIMPLE' COMMENT '设计方式：SIMPLE 简易设计器 / BPMN 自由设计',
  `simple_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '仿钉钉设计器节点树 JSON',
  `bpmn_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'BPMN 设计 XML（design_type=BPMN 时使用）',
  `proc_def_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程 key（oa_+id）',
  `deploy_version` int DEFAULT NULL COMMENT '最新部署版本（展示用）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿 1 已发布 2 停用',
  `source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CUSTOM' COMMENT '来源：SYSTEM 内置模板 CUSTOM 自定义',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内置模板标识（SYSTEM 行，租户内唯一）',
  `template_version` int DEFAULT NULL COMMENT '安装时的模板版本号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_template` (`tenant_id`,`template_code`),
  KEY `idx_tenant` (`tenant_id`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程模型（部署后版本存 ACT_RE_PROCDEF）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_model`
--

LOCK TABLES `oa_model` WRITE;
/*!40000 ALTER TABLE `oa_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_overtime_record`
--

DROP TABLE IF EXISTS `oa_overtime_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_overtime_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '归属用户',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联流程实例（EARN=加班单 CONSUME=请假/调休单；ADJUST 为空）',
  `record_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'EARN 加班入账 / CONSUME 调休扣减 / ADJUST 手工调整（带符号）',
  `hours` decimal(10,2) NOT NULL COMMENT '小时数（统一计量单位；ADJUST 带符号）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注（单据标题/调整原因）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_instance_type` (`tenant_id`,`instance_id`,`record_type`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='加班/调休余额流水（余额=SUM(hours)，唯一键保证流程回调幂等）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_overtime_record`
--

LOCK TABLES `oa_overtime_record` WRITE;
/*!40000 ALTER TABLE `oa_overtime_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_overtime_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_process_instance_ext`
--

DROP TABLE IF EXISTS `oa_process_instance_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_process_instance_ext` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `model_id` bigint NOT NULL COMMENT '流程模型编号',
  `proc_def_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程定义编号',
  `proc_def_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程 key',
  `form_id` bigint DEFAULT NULL COMMENT '关联表单编号',
  `form_version` int DEFAULT NULL COMMENT '发起时的表单版本号',
  `form_data_id` bigint DEFAULT NULL COMMENT '表单提交数据编号',
  `biz_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务键（开放接口发起携带的外部业务单据号）',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实例标题',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/APPROVED/REJECTED/WITHDRAWN',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `result` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结束说明',
  `create_by` bigint DEFAULT NULL COMMENT '发起人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_instance` (`instance_id`),
  KEY `idx_tenant_start` (`tenant_id`,`create_by`),
  KEY `idx_tenant_biz_key` (`tenant_id`,`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程实例扩展（标题/状态/发起人视图）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_process_instance_ext`
--

LOCK TABLES `oa_process_instance_ext` WRITE;
/*!40000 ALTER TABLE `oa_process_instance_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_process_instance_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_setting`
--

DROP TABLE IF EXISTS `oa_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_setting` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `setting_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设置键（如 work_hours_per_day 每日工时）',
  `setting_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设置值',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_key` (`tenant_id`,`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户级 OA 设置（键值对）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_setting`
--

LOCK TABLES `oa_setting` WRITE;
/*!40000 ALTER TABLE `oa_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_task_log`
--

DROP TABLE IF EXISTS `oa_task_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_task_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎任务编号',
  `node_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批节点编号',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批节点名称',
  `user_id` bigint DEFAULT NULL COMMENT '操作人',
  `user_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人昵称快照',
  `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'START/APPROVE/REJECT/RETURN/TRANSFER/CC/WITHDRAW/RESUBMIT',
  `opinion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `target` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作目标（转办人/退回节点等）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_instance` (`tenant_id`,`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批时间线';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_task_log`
--

LOCK TABLES `oa_task_log` WRITE;
/*!40000 ALTER TABLE `oa_task_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_task_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_webhook_config`
--

DROP TABLE IF EXISTS `oa_webhook_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_webhook_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称（如 ERP 单据回调）',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回调地址（POST JSON）',
  `secret` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签名密钥（HMAC-SHA256，空=不签名；不回显）',
  `model_id` bigint DEFAULT NULL COMMENT '限定流程模型编号（空=全部流程）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 / 1 停用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_model` (`tenant_id`,`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Webhook 回调配置（流程结束推送业务系统）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_webhook_config`
--

LOCK TABLES `oa_webhook_config` WRITE;
/*!40000 ALTER TABLE `oa_webhook_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_webhook_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_webhook_log`
--

DROP TABLE IF EXISTS `oa_webhook_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_webhook_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `config_id` bigint NOT NULL COMMENT '回调配置编号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例编号',
  `event` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'process.finished / test',
  `payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '推送载荷 JSON',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 待推送(重试中) / 1 成功 / 2 死信',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已重试次数（不含首次）',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间（空=待首次推送）',
  `last_response` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近响应摘要（状态码+响应片段/异常）',
  `finish_time` datetime DEFAULT NULL COMMENT '推送完成时间（成功或转死信）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_config_status` (`tenant_id`,`config_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Webhook 推送日志（重试队列：超限转死信）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_webhook_log`
--

LOCK TABLES `oa_webhook_log` WRITE;
/*!40000 ALTER TABLE `oa_webhook_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_webhook_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_api`
--

DROP TABLE IF EXISTS `open_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共目录）',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 标识（open:resource:action）',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 名称',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '路径模板（Ant 风格，如 /open/api/user/{id}）',
  `http_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GET' COMMENT 'HTTP 方法（大写；ALL=不限）',
  `target` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分发目标：契约接口全名#方法名（空=open-service 内置端点）',
  `source` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'seed' COMMENT '目录来源：seed=SQL 种子；rpc=业务积木自注册',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_api`
--

LOCK TABLES `open_api` WRITE;
/*!40000 ALTER TABLE `open_api` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_api_log`
--

DROP TABLE IF EXISTS `open_api_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_api_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台域/未过鉴权）',
  `app_id` bigint DEFAULT NULL COMMENT '应用编号（未过 token 校验为空）',
  `api_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '命中的 API 标识（未匹配清单为空）',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求 HTTP 方法',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求路径',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源 IP',
  `status_code` int DEFAULT NULL COMMENT 'HTTP 状态码',
  `cost_ms` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因（鉴权/限流等）',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链路追踪ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`,`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 调用审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_api_log`
--

LOCK TABLES `open_api_log` WRITE;
/*!40000 ALTER TABLE `open_api_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_api_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_app`
--

DROP TABLE IF EXISTS `open_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_app` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `app_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用标识（自动生成，全局唯一）',
  `app_secret` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'AppSecret 密文（AES-GCM，明文仅创建/重置时展示一次）',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '业务绑定租户（0=平台域；开放 API 数据隔离锚点）',
  `rate_limit_qps` bigint DEFAULT NULL COMMENT '限流 QPS（NULL=用 mse.open.default-rate-limit）',
  `expire_time` datetime DEFAULT NULL COMMENT '凭证过期时间（NULL=长期）',
  `sign_required` tinyint NOT NULL DEFAULT '0' COMMENT '强制请求签名验签（0 关 1 开；HMAC-SHA256 + 时间戳防重放 + nonce）',
  `ip_whitelist` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP 白名单（逗号/分号/换行分隔，支持 IPv4 CIDR；空=不限制）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_key` (`app_key`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放平台接入应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_app`
--

LOCK TABLES `open_app` WRITE;
/*!40000 ALTER TABLE `open_app` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_app_api`
--

DROP TABLE IF EXISTS `open_app_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_app_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号（随应用绑定租户）',
  `app_id` bigint NOT NULL COMMENT '应用编号',
  `api_id` bigint NOT NULL COMMENT 'API 编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_api` (`app_id`,`api_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用-API 授权关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_app_api`
--

LOCK TABLES `open_app_api` WRITE;
/*!40000 ALTER TABLE `open_app_api` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_app_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_user_bind`
--

DROP TABLE IF EXISTS `open_user_bind`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_user_bind` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '中台用户编号（sys_user.id）',
  `source_system` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源系统标识（接入方自定）',
  `external_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '外部系统用户标识',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind` (`tenant_id`,`source_system`,`external_user_id`,`deleted`),
  KEY `idx_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一身份绑定（外部标识 ↔ 中台账号）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_user_bind`
--

LOCK TABLES `open_user_bind` WRITE;
/*!40000 ALTER TABLE `open_user_bind` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_user_bind` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_user_extend`
--

DROP TABLE IF EXISTS `open_user_extend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_user_extend` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '中台用户编号（租户内唯一）',
  `extend_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展属性 JSON 对象（整体读写，≤4KB）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`tenant_id`,`user_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户扩展属性（接入系统差异化画像）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_user_extend`
--

LOCK TABLES `open_user_extend` WRITE;
/*!40000 ALTER TABLE `open_user_extend` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_user_extend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_hot_keyword`
--

DROP TABLE IF EXISTS `search_hot_keyword`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_hot_keyword` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `keyword` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索关键词',
  `hit_count` bigint DEFAULT '1' COMMENT '命中次数',
  `last_search_time` datetime DEFAULT NULL COMMENT '最近搜索时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_hot_keyword` (`tenant_id`,`keyword`,`deleted`),
  KEY `idx_search_hot_hit` (`tenant_id`,`hit_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搜索热词';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_hot_keyword`
--

LOCK TABLES `search_hot_keyword` WRITE;
/*!40000 ALTER TABLE `search_hot_keyword` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_hot_keyword` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_tenant_field`
--

DROP TABLE IF EXISTS `search_tenant_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_tenant_field` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '源标识（须为平台已注册源）',
  `fields_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '覆盖字段配置（JSON 数组；仅 searchable/weight/highlightable/returned/label 生效）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_tenant_field` (`tenant_id`,`source_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户搜索字段覆盖';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_tenant_field`
--

LOCK TABLES `search_tenant_field` WRITE;
/*!40000 ALTER TABLE `search_tenant_field` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_tenant_field` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `config_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数键名（租户内唯一，代码层校验）',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数键值',
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '0 系统内置 1 自定义',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_key` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门编号（0=根）',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '祖级列表（逗号分隔，如 0,1000；用于本部门及以下展开）',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典类型',
  `dict_label` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据键值',
  `dict_sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `list_class` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签样式（default/primary/success/info/warning/danger）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认 0 否 1 是',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dict_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典类型（租户内唯一，代码层校验）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台；租户不存在时的失败记录也落 0）',
  `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号',
  `ip` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录 IP',
  `browser` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '浏览器',
  `os` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作系统',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '登录时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_message`
--

DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL COMMENT '编号',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型（业务自定义，如 TODO/CC/RESULT/REMIND）',
  `receiver_id` bigint NOT NULL COMMENT '接收人',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息内容',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务关联标识（如流程实例编号）',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '0 未读 1 已读',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_receiver` (`tenant_id`,`receiver_id`,`read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_message`
--

LOCK TABLES `sys_message` WRITE;
/*!40000 ALTER TABLE `sys_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `module` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务模块（如 system:user）',
  `oper_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作类型（新增/修改/删除/导入/导出等）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法名（类.方法）',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求 URL',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求参数（JSON，超长截断）',
  `oper_user_id` bigint DEFAULT NULL COMMENT '操作人员编号',
  `oper_username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人员账号',
  `oper_ip` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作 IP',
  `cost_ms` int DEFAULT NULL COMMENT '耗时（毫秒）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误消息（超长截断）',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `post_code` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码（租户内唯一，代码层校验）',
  `post_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_id`,`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `code` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '数据范围 1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色自定义数据部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_dept`
--

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `menu_id` bigint NOT NULL COMMENT '菜单编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `permission_id` bigint NOT NULL COMMENT '权限点编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`),
  KEY `idx_tenant_permission` (`tenant_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限点关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dept_id` bigint DEFAULT NULL COMMENT '部门编号（数据权限锚点）',
  `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'BCrypt',
  `nickname` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户昵称',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号码',
  `email` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱地址',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像地址',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `super_admin` tinyint NOT NULL DEFAULT '0' COMMENT '租户管理员标志（权限=套餐域内全量；平台端=全量 *）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_username` (`tenant_id`,`username`),
  KEY `idx_tenant_dept` (`tenant_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (2101908494482042881,2101908385836986369,NULL,'admin','$2a$10$Wc/ygIAy1YTb/mSXI17HK.2YAsdt3elOG2TPHR9aLpVxC4qbzsA1G','admin',NULL,NULL,NULL,0,1,1,'2026-09-21 13:36:51',1,'2026-09-21 13:36:51',0);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `post_id` bigint NOT NULL COMMENT '岗位编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_tenant_post` (`tenant_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户岗位关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_post`
--

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
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
  `xid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局事务ID',
  `context` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '上下文',
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
-- Current Database: `mse_tenant2`
--

/*!40000 DROP DATABASE IF EXISTS `mse_tenant2`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `mse_tenant2` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `mse_tenant2`;

--
-- Table structure for table `ai_agent`
--

DROP TABLE IF EXISTS `ai_agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码（全局唯一）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像（emoji 或 URL）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '系统提示词（支持 {{var}} 占位）',
  `model_id` bigint DEFAULT NULL COMMENT '绑定模型（ai_model.id，空=平台默认模型）',
  `temperature` double DEFAULT NULL COMMENT '温度覆盖',
  `welcome_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开场白',
  `tool_codes` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工具编码列表（逗号分隔）',
  `kb_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '知识库 ID 列表（逗号分隔）',
  `orchestration_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'simple' COMMENT '编排模式：simple 简单配置 / flow 画布编排',
  `graph_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '画布编排 DSL（mse-flow/v1 JSON，flow 模式生效）',
  `draft_graph_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '画布编排草稿（编辑器保存目标，发布时校验并拷贝到 graph_json）',
  `scope` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tenant' COMMENT 'platform 平台公共 / tenant 租户自有',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（platform 时为 0）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 已发布 1 下线',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_code` (`agent_code`,`deleted`),
  KEY `idx_scope` (`scope`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能体（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_agent`
--

LOCK TABLES `ai_agent` WRITE;
/*!40000 ALTER TABLE `ai_agent` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_api_key`
--

DROP TABLE IF EXISTS `ai_api_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_api_key` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用 ID',
  `key_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API Key 哈希（SHA-256）',
  `key_prefix` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Key 前缀（识别用）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '业务归属租户',
  `agent_codes` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体白名单（空=全部已发布）',
  `rate_per_minute` int DEFAULT NULL COMMENT '每分钟调用上限（0/空=不限）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 密钥（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_api_key`
--

LOCK TABLES `ai_api_key` WRITE;
/*!40000 ALTER TABLE `ai_api_key` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_api_key` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_message`
--

DROP TABLE IF EXISTS `ai_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_message` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `session_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话 ID（ai_chat_session.id 字符串）',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'user / assistant',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `ext` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展信息 JSON（assistant 消息：datasets 工具数据结果 / sources 引用来源，历史回放恢复用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对话消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_message`
--

LOCK TABLES `ai_chat_message` WRITE;
/*!40000 ALTER TABLE `ai_chat_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_session`
--

DROP TABLE IF EXISTS `ai_chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_session` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话标题',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码',
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `last_active_time` datetime DEFAULT NULL COMMENT '最近活跃时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_agent` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对话会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_session`
--

LOCK TABLES `ai_chat_session` WRITE;
/*!40000 ALTER TABLE `ai_chat_session` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_kb_chunk`
--

DROP TABLE IF EXISTS `ai_kb_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_kb_chunk` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `kb_id` bigint NOT NULL COMMENT '知识库 ID',
  `document_id` bigint NOT NULL COMMENT '文档 ID',
  `seq` int NOT NULL DEFAULT '0' COMMENT '切片序号',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '切片正文',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_kb_doc` (`kb_id`,`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库切片';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_kb_chunk`
--

LOCK TABLES `ai_kb_chunk` WRITE;
/*!40000 ALTER TABLE `ai_kb_chunk` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_kb_chunk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_kb_document`
--

DROP TABLE IF EXISTS `ai_kb_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_kb_document` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `kb_id` bigint NOT NULL COMMENT '知识库 ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件类型',
  `size_bytes` bigint DEFAULT NULL COMMENT '文件大小',
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原文件归档路径（支撑重新解析/重建索引）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 待处理 1 处理中 2 完成 3 失败',
  `chunk_count` int DEFAULT NULL COMMENT '切片数量',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_kb_document`
--

LOCK TABLES `ai_kb_document` WRITE;
/*!40000 ALTER TABLE `ai_kb_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_kb_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_base`
--

DROP TABLE IF EXISTS `ai_knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_base` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `chunk_size` int DEFAULT NULL COMMENT '切片目标大小（token）',
  `chunk_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'general' COMMENT '切片方法（general/qa/table/book/one）',
  `chunk_overlap` int DEFAULT '100' COMMENT '切片重叠大小（token，general/book 生效）',
  `top_k` int DEFAULT NULL COMMENT '默认检索条数',
  `similarity_threshold` decimal(4,3) DEFAULT '0.500' COMMENT '相似度阈值（0~1）',
  `embedding_model_id` bigint DEFAULT NULL COMMENT '绑定向量模型（ai_model.id，model_type=embedding；空=按旧版全局配置/平台默认向量模型解析）',
  `rerank_model_id` bigint DEFAULT NULL COMMENT '绑定重排模型（ai_model.id，model_type=rerank；空=按旧版平台级配置解析，均未配置则不重排）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_base`
--

LOCK TABLES `ai_knowledge_base` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_base` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_base` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_mcp_server`
--

DROP TABLE IF EXISTS `ai_mcp_server`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_mcp_server` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共数据）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务名（唯一，工具编码引用）',
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '基础地址',
  `sse_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'SSE 路径（默认 /sse）',
  `transport` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'sse' COMMENT '传输方式',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部 MCP Server 注册（平台公共 tenant_id=0）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_mcp_server`
--

LOCK TABLES `ai_mcp_server` WRITE;
/*!40000 ALTER TABLE `ai_mcp_server` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_mcp_server` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model`
--

DROP TABLE IF EXISTS `ai_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共数据）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称',
  `provider` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商标注（deepseek/zhipu/moonshot/dashscope/openai/anthropic/ollama/xfyun/openai-compat，仅展示与预设联动）',
  `protocol` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'openai-chat' COMMENT '接口类型（openai-chat=OpenAI兼容 chat/completions；anthropic-messages=Claude 原生 /v1/messages；向量模型行固定 openai-chat）',
  `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'chat' COMMENT '模型类型（chat=对话模型 embedding=向量模型 rerank=重排模型，一行只表达一种能力）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '默认向量模型标记（仅 model_type=embedding 有意义，全局唯一）',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型端点（openai-chat 自带版本前缀如 https://api.deepseek.com；anthropic-messages 如 https://api.anthropic.com）',
  `api_key_enc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'API Key 密文（AES-GCM）',
  `chat_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对话模型名（model_type=embedding 时为空串）',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型名（model_type=embedding 时为本行向量模型名；chat 行为旧版遗留字段，仅旧数据兼容读取）',
  `embedding_base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型独立端点（空复用 base_url）',
  `embedding_api_key_enc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '向量模型独立 API Key 密文（空复用 api_key_enc）',
  `temperature` double DEFAULT NULL COMMENT '默认温度',
  `top_p` double DEFAULT NULL COMMENT '默认 topP',
  `max_tokens` int DEFAULT NULL COMMENT '默认最大输出 tokens',
  `failover_model_id` bigint DEFAULT NULL COMMENT '备用对话模型（ai_model.id，model_type=chat；主模型瞬态错误重试仍失败时自动切换，空=不切换）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_test_time` datetime DEFAULT NULL COMMENT '最近连通性测试时间',
  `last_test_ok` tinyint DEFAULT NULL COMMENT '最近测试结果：0 失败 1 成功',
  `last_test_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近测试失败原因',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型配置（平台公共 tenant_id=0）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model`
--

LOCK TABLES `ai_model` WRITE;
/*!40000 ALTER TABLE `ai_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model_tenant`
--

DROP TABLE IF EXISTS `ai_model_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_tenant` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `model_id` bigint NOT NULL COMMENT '模型 ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_model` (`model_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型-租户可用范围（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model_tenant`
--

LOCK TABLES `ai_model_tenant` WRITE;
/*!40000 ALTER TABLE `ai_model_tenant` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_tenant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model_tenant_config`
--

DROP TABLE IF EXISTS `ai_model_tenant_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_tenant_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `model_id` bigint NOT NULL COMMENT '模型 ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '覆盖端点（空=沿用平台配置）',
  `api_key_enc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '覆盖 API Key 密文 AES-GCM（空=沿用平台配置）',
  `temperature` double DEFAULT NULL COMMENT '覆盖温度（空=沿用平台配置）',
  `top_p` double DEFAULT NULL COMMENT '覆盖 topP（空=沿用平台配置）',
  `max_tokens` int DEFAULT NULL COMMENT '覆盖最大输出 tokens（空=沿用平台配置）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_model` (`model_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型租户级覆盖配置（平台级）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model_tenant_config`
--

LOCK TABLES `ai_model_tenant_config` WRITE;
/*!40000 ALTER TABLE `ai_model_tenant_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_model_tenant_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_tool`
--

DROP TABLE IF EXISTS `ai_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_tool` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（0=平台公共系统工具，>0=租户自有工具）',
  `tool_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具编码（全局唯一）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工具名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '功能说明（面向模型）',
  `parameters_json_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '入参 JSON Schema',
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'rpc' COMMENT 'builtin / local / rpc / http / dify',
  `rpc_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'RPC 提供方服务名（Dubbo group）',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '外部工具连接配置（source=http/dify 时生效）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_code` (`tool_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工具登记（tenant_id=0 平台公共系统工具，>0 租户自有）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_tool`
--

LOCK TABLES `ai_tool` WRITE;
/*!40000 ALTER TABLE `ai_tool` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_tool` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_tool_log`
--

DROP TABLE IF EXISTS `ai_tool_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_tool_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `tool_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具编码',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体编码',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话 ID',
  `arguments` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '入参 JSON',
  `result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '出参内容',
  `success` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_tool` (`tenant_id`,`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工具调用审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_tool_log`
--

LOCK TABLES `ai_tool_log` WRITE;
/*!40000 ALTER TABLE `ai_tool_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_tool_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_usage_log`
--

DROP TABLE IF EXISTS `ai_usage_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_usage_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'gateway / rpc / openapi',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体编码',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模型名',
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话 ID',
  `user_id` bigint DEFAULT NULL COMMENT '触发用户（openapi 通道为空）',
  `prompt_tokens` int DEFAULT NULL COMMENT '提示词 tokens 消耗',
  `completion_tokens` int DEFAULT NULL COMMENT '补全 tokens 消耗',
  `total_tokens` int DEFAULT NULL COMMENT '总 tokens 消耗',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`create_time`),
  KEY `idx_agent` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 用量流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_usage_log`
--

LOCK TABLES `ai_usage_log` WRITE;
/*!40000 ALTER TABLE `ai_usage_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_usage_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_workflow_run`
--

DROP TABLE IF EXISTS `ai_workflow_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_workflow_run` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '智能体编码',
  `agent_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '智能体名称快照',
  `channel` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发通道：gateway 对话 / debug 试运行 / rpc 无状态 / openapi 开放接口',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话号（debug 无会话为空）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '运行结果：success / failed / cancelled',
  `query_text` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户输入（截断存档）',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '最终回答（截断存档）',
  `error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '总耗时毫秒',
  `node_total` int DEFAULT NULL COMMENT '节点轨迹条数',
  `traces_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '节点轨迹快照 JSON（与 SSE node 事件同构）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '归属租户（平台调试行为 0）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_agent` (`agent_code`,`create_time`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流运行历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_workflow_run`
--

LOCK TABLES `ai_workflow_run` WRITE;
/*!40000 ALTER TABLE `ai_workflow_run` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_workflow_run` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api`
--

DROP TABLE IF EXISTS `dat_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 名称',
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '开放路径 /openapi/data/{path}，租户内唯一',
  `datasource_id` bigint NOT NULL COMMENT '查询的数据源（0 = 中台库）',
  `sql_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SQL 模板（仅 SELECT，:param 占位）',
  `params_json` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数定义 JSON [{name,type,required,remark}]',
  `page_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '0 关 1 开（pageNum/pageSize 自动分页包裹）',
  `max_rows` int NOT NULL DEFAULT '1000' COMMENT '单次返回行数上限',
  `timeout_ms` int NOT NULL DEFAULT '10000' COMMENT '查询超时（毫秒）',
  `qps_limit` int NOT NULL DEFAULT '60' COMMENT '每分钟最大调用次数（0 = 跟随全局默认）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 发布 1 下线',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_path` (`tenant_id`,`path`,`deleted`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api`
--

LOCK TABLES `dat_api` WRITE;
/*!40000 ALTER TABLE `dat_api` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_app`
--

DROP TABLE IF EXISTS `dat_api_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_app` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `app_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局唯一',
  `app_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密文（AES-GCM）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_app_key` (`app_key`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务调用方应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_app`
--

LOCK TABLES `dat_api_app` WRITE;
/*!40000 ALTER TABLE `dat_api_app` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_approval`
--

DROP TABLE IF EXISTS `dat_api_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_approval` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `api_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'API 名称',
  `action` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'submit/approve/reject/offline',
  `comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人姓名',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`,`api_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 审批记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_approval`
--

LOCK TABLES `dat_api_approval` WRITE;
/*!40000 ALTER TABLE `dat_api_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_grant`
--

DROP TABLE IF EXISTS `dat_api_grant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_grant` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `app_id` bigint NOT NULL COMMENT 'dat_api_app.id',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（空=永久）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_api_app` (`api_id`,`app_id`,`deleted`),
  KEY `idx_tenant_app` (`tenant_id`,`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 授权';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_grant`
--

LOCK TABLES `dat_api_grant` WRITE;
/*!40000 ALTER TABLE `dat_api_grant` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_grant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_api_log`
--

DROP TABLE IF EXISTS `dat_api_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_api_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `api_id` bigint NOT NULL COMMENT '数据服务 API ID',
  `app_id` bigint NOT NULL COMMENT '调用方应用 ID',
  `params` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '本次请求参数 JSON',
  `row_count` int DEFAULT NULL COMMENT '返回行数',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'success / failed',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端 IP',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_api` (`tenant_id`,`api_id`,`create_time`),
  KEY `idx_tenant_app` (`tenant_id`,`app_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据服务 API 调用日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_api_log`
--

LOCK TABLES `dat_api_log` WRITE;
/*!40000 ALTER TABLE `dat_api_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_api_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_datasource`
--

DROP TABLE IF EXISTS `dat_datasource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_datasource` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'mysql / dm / kingbase',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口',
  `db_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '库名/schema',
  `extra_params` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JDBC 追加参数（如 useSSL=false&…）',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '口令密文（AES-GCM）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_test_time` datetime DEFAULT NULL COMMENT '最近连通性测试时间',
  `last_test_ok` tinyint DEFAULT NULL COMMENT '最近测试结果：0 失败 1 成功',
  `last_test_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近测试失败原因',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部数据源注册';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_datasource`
--

LOCK TABLES `dat_datasource` WRITE;
/*!40000 ALTER TABLE `dat_datasource` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_datasource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_lineage`
--

DROP TABLE IF EXISTS `dat_lineage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_lineage` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `job_id` bigint NOT NULL COMMENT '同步任务',
  `job_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称',
  `datasource_id` bigint NOT NULL COMMENT '来源数据源',
  `source_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'table' COMMENT 'table / sql',
  `source_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源表（sql 来源存查询摘要）',
  `target_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标 ODS 表',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_job` (`tenant_id`,`job_id`,`deleted`),
  KEY `idx_tenant_target` (`tenant_id`,`target_table`),
  KEY `idx_tenant_source` (`tenant_id`,`source_table`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务级血缘（同步任务：源表 → ODS）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_lineage`
--

LOCK TABLES `dat_lineage` WRITE;
/*!40000 ALTER TABLE `dat_lineage` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_lineage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_meta_column`
--

DROP TABLE IF EXISTS `dat_meta_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_meta_column` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `meta_table_id` bigint NOT NULL COMMENT '所属 dat_meta_table',
  `column_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '列名',
  `data_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据类型',
  `nullable` tinyint NOT NULL DEFAULT '1' COMMENT '是否可空（1 是 0 否）',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列注释',
  `ordinal` int NOT NULL DEFAULT '0' COMMENT '列序号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_table` (`tenant_id`,`meta_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='元数据-列';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_meta_column`
--

LOCK TABLES `dat_meta_column` WRITE;
/*!40000 ALTER TABLE `dat_meta_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_meta_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_meta_table`
--

DROP TABLE IF EXISTS `dat_meta_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_meta_table` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `datasource_id` bigint NOT NULL COMMENT '0 = 中台内部（ODS/中台库自身）',
  `table_schema` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属 schema',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名',
  `category` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'source' COMMENT 'source 源库表 / ods 中台贴源表',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表注释',
  `row_count` bigint DEFAULT NULL COMMENT ' approximate 行数',
  `collected_time` datetime NOT NULL COMMENT '最近采集时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_ds_table` (`tenant_id`,`datasource_id`,`table_name`,`deleted`),
  KEY `idx_tenant_cat` (`tenant_id`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='元数据-表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_meta_table`
--

LOCK TABLES `dat_meta_table` WRITE;
/*!40000 ALTER TABLE `dat_meta_table` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_meta_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_quality_report`
--

DROP TABLE IF EXISTS `dat_quality_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_quality_report` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `rule_id` bigint NOT NULL COMMENT '质量规则 ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名冗余',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'success / failed',
  `total_rows` bigint DEFAULT NULL COMMENT '检测总行数',
  `bad_rows` bigint DEFAULT NULL COMMENT '坏数据行数',
  `pass_rate` decimal(5,2) DEFAULT NULL COMMENT '通过率（%）',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `trigger_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT 'manual / sched',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_rule` (`tenant_id`,`rule_id`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据质量报告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_quality_report`
--

LOCK TABLES `dat_quality_report` WRITE;
/*!40000 ALTER TABLE `dat_quality_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_quality_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_quality_rule`
--

DROP TABLE IF EXISTS `dat_quality_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_quality_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则名称',
  `datasource_id` bigint NOT NULL DEFAULT '0' COMMENT '0 = 中台库贴源层',
  `target_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标表',
  `rule_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'not_null/unique/range/enum/custom_sql/row_count_fluctuation',
  `column_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '检测列（custom_sql/fluctuation 无需）',
  `rule_config` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则配置 JSON（min/max/values/sql/thresholdPct）',
  `cron` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调度 cron（空=仅手动）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `last_pass` tinyint DEFAULT NULL COMMENT '最近结果：0 未通过 1 通过',
  `last_pass_rate` decimal(5,2) DEFAULT NULL COMMENT '最近通过率（%）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据质量规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_quality_rule`
--

LOCK TABLES `dat_quality_rule` WRITE;
/*!40000 ALTER TABLE `dat_quality_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_quality_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_query_allowlist`
--

DROP TABLE IF EXISTS `dat_query_allowlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_query_allowlist` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '放行的表名（小写归一化）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_table` (`tenant_id`,`table_name`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='中台库通道查询白名单（ods_ 之外可查业务表）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_query_allowlist`
--

LOCK TABLES `dat_query_allowlist` WRITE;
/*!40000 ALTER TABLE `dat_query_allowlist` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_query_allowlist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_sync_job`
--

DROP TABLE IF EXISTS `dat_sync_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_sync_job` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `datasource_id` bigint NOT NULL COMMENT '来源数据源',
  `source_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'table' COMMENT 'table 表 / sql 自定义查询',
  `source_table` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '源表名（source_type=table）',
  `query_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '自定义查询 SQL（source_type=sql，仅 SELECT）',
  `target_table` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标 ODS 表（引擎自动建，ods_ 前缀）',
  `sync_mode` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'full' COMMENT 'full 全量 / incr 增量',
  `incr_column` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '增量水位列（时间戳/自增 ID）',
  `watermark` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前水位值（引擎回写）',
  `cron` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调度 cron（Spring 表达式，空=仅手动）',
  `dep_job_id` bigint DEFAULT NULL COMMENT '前置任务（成功完成后级联触发本任务）',
  `row_count` bigint NOT NULL DEFAULT '0' COMMENT '最近一次同步行数',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_ds` (`tenant_id`,`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据集成同步任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_sync_job`
--

LOCK TABLES `dat_sync_job` WRITE;
/*!40000 ALTER TABLE `dat_sync_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_sync_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dat_sync_job_log`
--

DROP TABLE IF EXISTS `dat_sync_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dat_sync_job_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `job_id` bigint NOT NULL COMMENT '同步任务ID',
  `trigger_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'manual 手动 / sched 调度',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'running / success / failed',
  `sync_mode` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '本次执行模式 full / incr',
  `row_count` bigint NOT NULL DEFAULT '0' COMMENT '同步行数',
  `watermark_used` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行前水位',
  `watermark_new` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行后水位',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_job` (`tenant_id`,`job_id`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同步任务运行日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dat_sync_job_log`
--

LOCK TABLES `dat_sync_job_log` WRITE;
/*!40000 ALTER TABLE `dat_sync_job_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `dat_sync_job_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_event`
--

DROP TABLE IF EXISTS `demo_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_event` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `event_type` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型',
  `payload` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '事件负载（JSON）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='已消费租户事件（共享主库）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_event`
--

LOCK TABLES `demo_event` WRITE;
/*!40000 ALTER TABLE `demo_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_order`
--

DROP TABLE IF EXISTS `demo_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `order_no` varchar(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号',
  `product_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `amount` decimal(12,2) DEFAULT NULL COMMENT '订单金额',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order_no` (`tenant_id`,`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例订单（独立库租户）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_order`
--

LOCK TABLES `demo_order` WRITE;
/*!40000 ALTER TABLE `demo_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `demo_stock`
--

DROP TABLE IF EXISTS `demo_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `demo_stock` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `remain` int NOT NULL DEFAULT '0' COMMENT '剩余可下单数量',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='示例库存（共享主库，跨库事务的另一端）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `demo_stock`
--

LOCK TABLES `demo_stock` WRITE;
/*!40000 ALTER TABLE `demo_stock` DISABLE KEYS */;
/*!40000 ALTER TABLE `demo_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_file`
--

DROP TABLE IF EXISTS `fs_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_file` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花，物理对象键同名）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始文件名',
  `storage_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储键（{tenantId}/{yyyyMM}/{fileId}.{ext}）',
  `storage_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储类型：local / rustfs',
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容 MIME 类型',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '字节数',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务分类（如 oa/attachment）',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务关联 ID',
  `access_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'tenant' COMMENT '访问级别：tenant 登录本租户 / public 匿名可读（/fs/asset/{id}）',
  `content_md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容 MD5（32 位小写 hex；同租户同 MD5 秒传复用物理对象）',
  `upload_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分片直传会话 ID（仅 rustfs 直传进行中非空，complete/abort 后清空/删行）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_md5` (`content_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件元数据（物理对象按 storage_type 由 FileStorage 实现保管）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_file`
--

LOCK TABLES `fs_file` WRITE;
/*!40000 ALTER TABLE `fs_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `fs_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_alert`
--

DROP TABLE IF EXISTS `mon_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_alert` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `target_id` bigint NOT NULL COMMENT '监控目标',
  `target_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标名称（冗余，目标删除后仍可读）',
  `level` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'warn' COMMENT '级别：warn/error',
  `rule_key` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规则键：down/latency/cpu/mem/disk/redis_mem/mysql_threads',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警内容',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'firing' COMMENT '状态：firing/resolved',
  `first_trigger_time` datetime DEFAULT NULL COMMENT '首次触发时间',
  `last_trigger_time` datetime DEFAULT NULL COMMENT '最近触发时间',
  `resolve_time` datetime DEFAULT NULL COMMENT '恢复时间',
  `notify_result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '通知结果 JSON（inbox/webhook）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_alert_target` (`target_id`,`status`),
  KEY `idx_mon_alert_time` (`last_trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控告警';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_alert`
--

LOCK TABLES `mon_alert` WRITE;
/*!40000 ALTER TABLE `mon_alert` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_record`
--

DROP TABLE IF EXISTS `mon_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_record` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `target_id` bigint NOT NULL COMMENT '监控目标',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '采集状态：up/down',
  `latency_ms` int DEFAULT NULL COMMENT '响应耗时（ms）',
  `metrics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '指标集 JSON（按类型：cpu_usage_pct/threads_connected/qps…）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_record_target` (`target_id`,`collect_time`),
  KEY `idx_mon_record_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控采样记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_record`
--

LOCK TABLES `mon_record` WRITE;
/*!40000 ALTER TABLE `mon_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_setting`
--

DROP TABLE IF EXISTS `mon_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_setting` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `config_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键（notify）',
  `config_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置值 JSON（NotifySetting）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mon_setting_key` (`tenant_id`,`config_key`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控服务配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_setting`
--

LOCK TABLES `mon_setting` WRITE;
/*!40000 ALTER TABLE `mon_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mon_target`
--

DROP TABLE IF EXISTS `mon_target`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mon_target` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标名称',
  `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：service/http/mysql/redis/rocketmq/os',
  `service_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务/HTTP 地址（http://host:port）',
  `host` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主机（mysql/redis/rocketmq；os 固定本机）',
  `port` int DEFAULT NULL COMMENT '端口',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认证用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认证密码（界面脱敏）',
  `collect_interval` int DEFAULT '30' COMMENT '采集间隔（秒）',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0 启用 1 停用',
  `alert_config` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警规则 JSON：{down,latencyMs,cpuPercent,memPercent,diskPercent,redisMemPercent,mysqlMaxThreads}',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'unknown' COMMENT '最近状态：up/down/unknown',
  `status_time` datetime DEFAULT NULL COMMENT '最近状态时间',
  `last_latency_ms` int DEFAULT NULL COMMENT '最近响应耗时（ms）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mon_target_type` (`target_type`),
  KEY `idx_mon_target_status` (`enabled`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监控目标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mon_target`
--

LOCK TABLES `mon_target` WRITE;
/*!40000 ALTER TABLE `mon_target` DISABLE KEYS */;
/*!40000 ALTER TABLE `mon_target` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_account`
--

DROP TABLE IF EXISTS `mp_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_account` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公众号 AppID（全局唯一：回调按此路由）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公众号名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'subscription' COMMENT 'subscription 订阅号 / service 服务号',
  `verified` tinyint NOT NULL DEFAULT '0' COMMENT '是否已认证：0 否 1 是',
  `app_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'AppSecret 密文（AES-GCM）',
  `callback_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器配置 Token 密文',
  `aes_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'EncodingAESKey 密文',
  `encrypt_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'safe' COMMENT 'plain/compatible/safe',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_app_id` (`app_id`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公众号账号';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_account`
--

LOCK TABLES `mp_account` WRITE;
/*!40000 ALTER TABLE `mp_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_fan`
--

DROP TABLE IF EXISTS `mp_fan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_fan` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '粉丝 OpenID',
  `subscribed` tinyint NOT NULL DEFAULT '1' COMMENT '1 关注中 0 已取关',
  `subscribe_time` datetime DEFAULT NULL COMMENT '关注时间',
  `unsubscribe_time` datetime DEFAULT NULL COMMENT '取关时间',
  `subscribe_scene` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关注场景（ADD_SCENE_QR_CODE 等）',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '粉丝昵称',
  `head_img_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像地址',
  `sex` tinyint NOT NULL DEFAULT '0' COMMENT '1 男 2 女 0 未知',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省份',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市',
  `union_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开放平台 UnionID',
  `profile_incomplete` tinyint NOT NULL DEFAULT '0' COMMENT '1 = 资料拉取失败（未认证号常见）',
  `last_active_time` datetime DEFAULT NULL COMMENT '最近互动时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account_openid` (`account_id`,`open_id`),
  KEY `idx_tenant_sub` (`tenant_id`,`subscribed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='粉丝（回调事件自动维护，管理端只读）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_fan`
--

LOCK TABLES `mp_fan` WRITE;
/*!40000 ALTER TABLE `mp_fan` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_fan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_menu`
--

DROP TABLE IF EXISTS `mp_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_menu` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `menu_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '与微信 menu/create 同构 JSON',
  `synced_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿有改动 1 已同步',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account` (`account_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义菜单草稿（每号一份）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_menu`
--

LOCK TABLES `mp_menu` WRITE;
/*!40000 ALTER TABLE `mp_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_message`
--

DROP TABLE IF EXISTS `mp_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_message` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '粉丝 OpenID',
  `direction` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'in' COMMENT 'in 收 / out 发（预留）',
  `kind` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'event / message',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'subscribe/click/text/image/…（归一化小写）',
  `msg_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信消息 id（事件类为空）',
  `dedup_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等锚点 MD5（挡微信重试）',
  `payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '归一化载荷 JSON（snake_case）',
  `raw_xml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '解密后明文报文（超长截断）',
  `reply_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '被动回复缓存（明文 XML，重试复用）',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链路追踪ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_dedup` (`dedup_key`),
  KEY `idx_tenant_account` (`tenant_id`,`account_id`,`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息/事件流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_message`
--

LOCK TABLES `mp_message` WRITE;
/*!40000 ALTER TABLE `mp_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_reply_rule`
--

DROP TABLE IF EXISTS `mp_reply_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_reply_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名',
  `match_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'subscribe/unsubscribe/click/msg_any/keyword_exact/keyword_contains/default',
  `match_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单 Key / 关键词（, 空格 | 分隔多值）',
  `reply_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'text' COMMENT 'text / news',
  `reply_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '{"content":…} 或 {"items":[…≤8]}',
  `priority` int NOT NULL DEFAULT '0' COMMENT '同类型内大者先',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用（0 停用 1 启用）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_account` (`tenant_id`,`account_id`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动回复规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_reply_rule`
--

LOCK TABLES `mp_reply_rule` WRITE;
/*!40000 ALTER TABLE `mp_reply_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_reply_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mp_token`
--

DROP TABLE IF EXISTS `mp_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mp_token` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'token 密文（运行期内存缓存为主）',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  `err_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败错误码（成功时清空）',
  `err_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近失败错误信息（成功时清空）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_account` (`account_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='access_token 落库状态（失败原因供管理端排查）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mp_token`
--

LOCK TABLES `mp_token` WRITE;
/*!40000 ALTER TABLE `mp_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `mp_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_ai_agent_map`
--

DROP TABLE IF EXISTS `oa_ai_agent_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_ai_agent_map` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `scene` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '场景（assist 审批助手 / process 流程生成 / fill 智能填单）',
  `agent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '映射的智能体编码（ai_agent.agent_code；空串=使用内置默认）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_scene` (`tenant_id`,`scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 场景 → 智能体映射（租户隔离；保存即生效，不删行）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_ai_agent_map`
--

LOCK TABLES `oa_ai_agent_map` WRITE;
/*!40000 ALTER TABLE `oa_ai_agent_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_ai_agent_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_cc`
--

DROP TABLE IF EXISTS `oa_cc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_cc` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `node_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '抄送节点编号',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '抄送节点名称',
  `user_id` bigint NOT NULL COMMENT '被抄送人',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程标题快照',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='抄送记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_cc`
--

LOCK TABLES `oa_cc` WRITE;
/*!40000 ALTER TABLE `oa_cc` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_cc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_delegate_proxy`
--

DROP TABLE IF EXISTS `oa_delegate_proxy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_delegate_proxy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '委托人',
  `proxy_user_id` bigint NOT NULL COMMENT '受托人',
  `start_time` datetime NOT NULL COMMENT '生效开始时间',
  `end_time` datetime NOT NULL COMMENT '生效结束时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 0 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '委托说明',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_proxy_user` (`tenant_id`,`proxy_user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批委托代理（v1 全流程代理，不区分流程模板）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_delegate_proxy`
--

LOCK TABLES `oa_delegate_proxy` WRITE;
/*!40000 ALTER TABLE `oa_delegate_proxy` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_delegate_proxy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_dept_leader`
--

DROP TABLE IF EXISTS `oa_dept_leader`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_dept_leader` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `user_id` bigint NOT NULL COMMENT '负责人用户编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_dept_user` (`tenant_id`,`dept_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门负责人（基座 sys_dept 无负责人字段，OA 侧扩展）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_dept_leader`
--

LOCK TABLES `oa_dept_leader` WRITE;
/*!40000 ALTER TABLE `oa_dept_leader` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_dept_leader` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_file`
--

DROP TABLE IF EXISTS `oa_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_file` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小（字节）',
  `ext` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件扩展名',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储路径（local 模式；fs 模式为占位符 -）',
  `fs_file_id` bigint DEFAULT NULL COMMENT 'mse-fs 文件编号（storage_type=fs 时物理寻址键）',
  `storage_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'local' COMMENT '存储模式：local 本地磁盘 / fs mse-fs',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='附件元数据（local 落 mse.oa.file-dir / fs 存 mse-fs）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_file`
--

LOCK TABLES `oa_file` WRITE;
/*!40000 ALTER TABLE `oa_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form`
--

DROP TABLE IF EXISTS `oa_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `category_id` bigint DEFAULT NULL COMMENT '分类编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表单名称',
  `icon` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '设计器文档 JSON（草稿态）',
  `version` int NOT NULL DEFAULT '0' COMMENT '已发布版本号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿 1 已发布 2 停用',
  `source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CUSTOM' COMMENT '来源：SYSTEM 内置模板 CUSTOM 自定义',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内置模板标识（SYSTEM 行，租户内唯一）',
  `template_version` int DEFAULT NULL COMMENT '安装时的模板版本号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_template` (`tenant_id`,`template_code`),
  KEY `idx_tenant` (`tenant_id`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form`
--

LOCK TABLES `oa_form` WRITE;
/*!40000 ALTER TABLE `oa_form` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_category`
--

DROP TABLE IF EXISTS `oa_form_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_category` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单/流程分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_category`
--

LOCK TABLES `oa_form_category` WRITE;
/*!40000 ALTER TABLE `oa_form_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_data`
--

DROP TABLE IF EXISTS `oa_form_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_data` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `form_id` bigint NOT NULL COMMENT '表单编号',
  `form_version` int DEFAULT NULL COMMENT '提交时的表单版本号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联流程实例（独立提交为空）',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据摘要',
  `data_values` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '控件数据 JSON',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_form` (`tenant_id`,`form_id`),
  KEY `idx_instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单提交数据（JSON 存储）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_data`
--

LOCK TABLES `oa_form_data` WRITE;
/*!40000 ALTER TABLE `oa_form_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_form_version`
--

DROP TABLE IF EXISTS `oa_form_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_form_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `form_id` bigint NOT NULL COMMENT '表单编号',
  `version` int NOT NULL COMMENT '版本号',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '设计器文档 JSON 快照',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_form_version` (`tenant_id`,`form_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单发布版本快照（在途流程渲染不受后续编辑影响）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_form_version`
--

LOCK TABLES `oa_form_version` WRITE;
/*!40000 ALTER TABLE `oa_form_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_form_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_model`
--

DROP TABLE IF EXISTS `oa_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_model` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `category_id` bigint DEFAULT NULL COMMENT '分类编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程名称',
  `icon` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `form_id` bigint DEFAULT NULL COMMENT '绑定表单',
  `design_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SIMPLE' COMMENT '设计方式：SIMPLE 简易设计器 / BPMN 自由设计',
  `simple_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '仿钉钉设计器节点树 JSON',
  `bpmn_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'BPMN 设计 XML（design_type=BPMN 时使用）',
  `proc_def_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程 key（oa_+id）',
  `deploy_version` int DEFAULT NULL COMMENT '最新部署版本（展示用）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 草稿 1 已发布 2 停用',
  `source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CUSTOM' COMMENT '来源：SYSTEM 内置模板 CUSTOM 自定义',
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内置模板标识（SYSTEM 行，租户内唯一）',
  `template_version` int DEFAULT NULL COMMENT '安装时的模板版本号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_template` (`tenant_id`,`template_code`),
  KEY `idx_tenant` (`tenant_id`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程模型（部署后版本存 ACT_RE_PROCDEF）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_model`
--

LOCK TABLES `oa_model` WRITE;
/*!40000 ALTER TABLE `oa_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_overtime_record`
--

DROP TABLE IF EXISTS `oa_overtime_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_overtime_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '归属用户',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联流程实例（EARN=加班单 CONSUME=请假/调休单；ADJUST 为空）',
  `record_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'EARN 加班入账 / CONSUME 调休扣减 / ADJUST 手工调整（带符号）',
  `hours` decimal(10,2) NOT NULL COMMENT '小时数（统一计量单位；ADJUST 带符号）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注（单据标题/调整原因）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_instance_type` (`tenant_id`,`instance_id`,`record_type`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='加班/调休余额流水（余额=SUM(hours)，唯一键保证流程回调幂等）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_overtime_record`
--

LOCK TABLES `oa_overtime_record` WRITE;
/*!40000 ALTER TABLE `oa_overtime_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_overtime_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_process_instance_ext`
--

DROP TABLE IF EXISTS `oa_process_instance_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_process_instance_ext` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `model_id` bigint NOT NULL COMMENT '流程模型编号',
  `proc_def_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程定义编号',
  `proc_def_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎流程 key',
  `form_id` bigint DEFAULT NULL COMMENT '关联表单编号',
  `form_version` int DEFAULT NULL COMMENT '发起时的表单版本号',
  `form_data_id` bigint DEFAULT NULL COMMENT '表单提交数据编号',
  `biz_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务键（开放接口发起携带的外部业务单据号）',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实例标题',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/APPROVED/REJECTED/WITHDRAWN',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `result` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结束说明',
  `create_by` bigint DEFAULT NULL COMMENT '发起人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_instance` (`instance_id`),
  KEY `idx_tenant_start` (`tenant_id`,`create_by`),
  KEY `idx_tenant_biz_key` (`tenant_id`,`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程实例扩展（标题/状态/发起人视图）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_process_instance_ext`
--

LOCK TABLES `oa_process_instance_ext` WRITE;
/*!40000 ALTER TABLE `oa_process_instance_ext` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_process_instance_ext` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_setting`
--

DROP TABLE IF EXISTS `oa_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_setting` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `setting_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设置键（如 work_hours_per_day 每日工时）',
  `setting_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设置值',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_tenant_key` (`tenant_id`,`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户级 OA 设置（键值对）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_setting`
--

LOCK TABLES `oa_setting` WRITE;
/*!40000 ALTER TABLE `oa_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_task_log`
--

DROP TABLE IF EXISTS `oa_task_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_task_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '引擎流程实例编号',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '引擎任务编号',
  `node_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批节点编号',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批节点名称',
  `user_id` bigint DEFAULT NULL COMMENT '操作人',
  `user_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人昵称快照',
  `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'START/APPROVE/REJECT/RETURN/TRANSFER/CC/WITHDRAW/RESUBMIT',
  `opinion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `target` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作目标（转办人/退回节点等）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_instance` (`tenant_id`,`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批时间线';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_task_log`
--

LOCK TABLES `oa_task_log` WRITE;
/*!40000 ALTER TABLE `oa_task_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_task_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_webhook_config`
--

DROP TABLE IF EXISTS `oa_webhook_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_webhook_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称（如 ERP 单据回调）',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回调地址（POST JSON）',
  `secret` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签名密钥（HMAC-SHA256，空=不签名；不回显）',
  `model_id` bigint DEFAULT NULL COMMENT '限定流程模型编号（空=全部流程）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 / 1 停用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_model` (`tenant_id`,`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Webhook 回调配置（流程结束推送业务系统）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_webhook_config`
--

LOCK TABLES `oa_webhook_config` WRITE;
/*!40000 ALTER TABLE `oa_webhook_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_webhook_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oa_webhook_log`
--

DROP TABLE IF EXISTS `oa_webhook_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_webhook_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `config_id` bigint NOT NULL COMMENT '回调配置编号',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例编号',
  `event` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'process.finished / test',
  `payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '推送载荷 JSON',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 待推送(重试中) / 1 成功 / 2 死信',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已重试次数（不含首次）',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间（空=待首次推送）',
  `last_response` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近响应摘要（状态码+响应片段/异常）',
  `finish_time` datetime DEFAULT NULL COMMENT '推送完成时间（成功或转死信）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_config_status` (`tenant_id`,`config_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Webhook 推送日志（重试队列：超限转死信）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oa_webhook_log`
--

LOCK TABLES `oa_webhook_log` WRITE;
/*!40000 ALTER TABLE `oa_webhook_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `oa_webhook_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_api`
--

DROP TABLE IF EXISTS `open_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台公共目录）',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 标识（open:resource:action）',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API 名称',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '路径模板（Ant 风格，如 /open/api/user/{id}）',
  `http_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GET' COMMENT 'HTTP 方法（大写；ALL=不限）',
  `target` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分发目标：契约接口全名#方法名（空=open-service 内置端点）',
  `source` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'seed' COMMENT '目录来源：seed=SQL 种子；rpc=业务积木自注册',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_api`
--

LOCK TABLES `open_api` WRITE;
/*!40000 ALTER TABLE `open_api` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_api_log`
--

DROP TABLE IF EXISTS `open_api_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_api_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台域/未过鉴权）',
  `app_id` bigint DEFAULT NULL COMMENT '应用编号（未过 token 校验为空）',
  `api_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '命中的 API 标识（未匹配清单为空）',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求 HTTP 方法',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求路径',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源 IP',
  `status_code` int DEFAULT NULL COMMENT 'HTTP 状态码',
  `cost_ms` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败原因（鉴权/限流等）',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链路追踪ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`,`id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放 API 调用审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_api_log`
--

LOCK TABLES `open_api_log` WRITE;
/*!40000 ALTER TABLE `open_api_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_api_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_app`
--

DROP TABLE IF EXISTS `open_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_app` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `app_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用标识（自动生成，全局唯一）',
  `app_secret` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'AppSecret 密文（AES-GCM，明文仅创建/重置时展示一次）',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '业务绑定租户（0=平台域；开放 API 数据隔离锚点）',
  `rate_limit_qps` bigint DEFAULT NULL COMMENT '限流 QPS（NULL=用 mse.open.default-rate-limit）',
  `expire_time` datetime DEFAULT NULL COMMENT '凭证过期时间（NULL=长期）',
  `sign_required` tinyint NOT NULL DEFAULT '0' COMMENT '强制请求签名验签（0 关 1 开；HMAC-SHA256 + 时间戳防重放 + nonce）',
  `ip_whitelist` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP 白名单（逗号/分号/换行分隔，支持 IPv4 CIDR；空=不限制）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_key` (`app_key`,`deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放平台接入应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_app`
--

LOCK TABLES `open_app` WRITE;
/*!40000 ALTER TABLE `open_app` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_app` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_app_api`
--

DROP TABLE IF EXISTS `open_app_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_app_api` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户编号（随应用绑定租户）',
  `app_id` bigint NOT NULL COMMENT '应用编号',
  `api_id` bigint NOT NULL COMMENT 'API 编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_api` (`app_id`,`api_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用-API 授权关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_app_api`
--

LOCK TABLES `open_app_api` WRITE;
/*!40000 ALTER TABLE `open_app_api` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_app_api` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_user_bind`
--

DROP TABLE IF EXISTS `open_user_bind`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_user_bind` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '中台用户编号（sys_user.id）',
  `source_system` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源系统标识（接入方自定）',
  `external_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '外部系统用户标识',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind` (`tenant_id`,`source_system`,`external_user_id`,`deleted`),
  KEY `idx_user` (`tenant_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一身份绑定（外部标识 ↔ 中台账号）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_user_bind`
--

LOCK TABLES `open_user_bind` WRITE;
/*!40000 ALTER TABLE `open_user_bind` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_user_bind` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `open_user_extend`
--

DROP TABLE IF EXISTS `open_user_extend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_user_extend` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '中台用户编号（租户内唯一）',
  `extend_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展属性 JSON 对象（整体读写，≤4KB）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0 未删除 1 已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`tenant_id`,`user_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户扩展属性（接入系统差异化画像）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `open_user_extend`
--

LOCK TABLES `open_user_extend` WRITE;
/*!40000 ALTER TABLE `open_user_extend` DISABLE KEYS */;
/*!40000 ALTER TABLE `open_user_extend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_hot_keyword`
--

DROP TABLE IF EXISTS `search_hot_keyword`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_hot_keyword` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `keyword` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索关键词',
  `hit_count` bigint DEFAULT '1' COMMENT '命中次数',
  `last_search_time` datetime DEFAULT NULL COMMENT '最近搜索时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_hot_keyword` (`tenant_id`,`keyword`,`deleted`),
  KEY `idx_search_hot_hit` (`tenant_id`,`hit_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搜索热词';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_hot_keyword`
--

LOCK TABLES `search_hot_keyword` WRITE;
/*!40000 ALTER TABLE `search_hot_keyword` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_hot_keyword` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_tenant_field`
--

DROP TABLE IF EXISTS `search_tenant_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_tenant_field` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `source_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '源标识（须为平台已注册源）',
  `fields_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '覆盖字段配置（JSON 数组；仅 searchable/weight/highlightable/returned/label 生效）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_tenant_field` (`tenant_id`,`source_code`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户搜索字段覆盖';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_tenant_field`
--

LOCK TABLES `search_tenant_field` WRITE;
/*!40000 ALTER TABLE `search_tenant_field` DISABLE KEYS */;
/*!40000 ALTER TABLE `search_tenant_field` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `config_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数键名（租户内唯一，代码层校验）',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数键值',
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '0 系统内置 1 自定义',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_key` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门编号（0=根）',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '祖级列表（逗号分隔，如 0,1000；用于本部门及以下展开）',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典类型',
  `dict_label` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据键值',
  `dict_sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `list_class` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签样式（default/primary/success/info/warning/danger）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认 0 否 1 是',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dict_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典类型（租户内唯一，代码层校验）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台；租户不存在时的失败记录也落 0）',
  `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号',
  `ip` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录 IP',
  `browser` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '浏览器',
  `os` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作系统',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '登录时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_message`
--

DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL COMMENT '编号',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型（业务自定义，如 TODO/CC/RESULT/REMIND）',
  `receiver_id` bigint NOT NULL COMMENT '接收人',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息内容',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务关联标识（如流程实例编号）',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '0 未读 1 已读',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_receiver` (`tenant_id`,`receiver_id`,`read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_message`
--

LOCK TABLES `sys_message` WRITE;
/*!40000 ALTER TABLE `sys_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `module` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务模块（如 system:user）',
  `oper_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作类型（新增/修改/删除/导入/导出等）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法名（类.方法）',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求 URL',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求参数（JSON，超长截断）',
  `oper_user_id` bigint DEFAULT NULL COMMENT '操作人员编号',
  `oper_username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人员账号',
  `oper_ip` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作 IP',
  `cost_ms` int DEFAULT NULL COMMENT '耗时（毫秒）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 成功 1 失败',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误消息（超长截断）',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `post_code` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码（租户内唯一，代码层校验）',
  `post_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 正常 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_id`,`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `code` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '数据范围 1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色自定义数据部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_dept`
--

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `menu_id` bigint NOT NULL COMMENT '菜单编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `permission_id` bigint NOT NULL COMMENT '权限点编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`),
  KEY `idx_tenant_permission` (`tenant_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限点关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `dept_id` bigint DEFAULT NULL COMMENT '部门编号（数据权限锚点）',
  `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'BCrypt',
  `nickname` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户昵称',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号码',
  `email` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱地址',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像地址',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 启用 1 停用',
  `super_admin` tinyint NOT NULL DEFAULT '0' COMMENT '租户管理员标志（权限=套餐域内全量；平台端=全量 *）',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_username` (`tenant_id`,`username`),
  KEY `idx_tenant_dept` (`tenant_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `post_id` bigint NOT NULL COMMENT '岗位编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_tenant_post` (`tenant_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户岗位关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_post`
--

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号（0=平台）',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `role_id` bigint NOT NULL COMMENT '角色编号',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
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
  `branch_id` bigint NOT NULL COMMENT '分支事务 ID',
  `xid` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局事务 ID',
  `context` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '回滚上下文（序列化方式等）',
  `rollback_info` longblob NOT NULL COMMENT '回滚数据快照（前后镜像）',
  `log_status` int NOT NULL COMMENT '日志状态（0 正常 1 全局已完成）',
  `log_created` datetime(6) NOT NULL COMMENT '日志创建时间',
  `log_modified` datetime(6) NOT NULL COMMENT '日志修改时间',
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
-- Current Database: `mse_tenant3`
--

/*!40000 DROP DATABASE IF EXISTS `mse_tenant3`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `mse_tenant3` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `mse_tenant3`;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-21 13:41:00
