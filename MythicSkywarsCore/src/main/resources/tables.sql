SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";

CREATE TABLE IF NOT EXISTS `sw_player` (
  `player_id`    INT(6) UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid`  		VARCHAR(255)    NOT NULL UNIQUE,
  `player_name`	VARCHAR(255)    NOT NULL,
  `wins`        INT(6)          NOT NULL DEFAULT 0,
  `losses`      INT(6)          NOT NULL DEFAULT 0,
  `kills`       INT(6)          NOT NULL DEFAULT 0,
  `deaths`      INT(6)          NOT NULL DEFAULT 0,
  `xp`      	INT(6)          NOT NULL DEFAULT 0,
  `pareffect`	VARCHAR(255)    NOT NULL,
  `proeffect`	VARCHAR(255)    NOT NULL,
  `glasscolor`	VARCHAR(255)    NOT NULL,
  `killsound`	VARCHAR(255)    NOT NULL,
  `winsound`	VARCHAR(255)    NOT NULL,
  `taunt`		VARCHAR(255)    NOT NULL,
  `prestige_icon` VARCHAR(64)   NOT NULL DEFAULT 'icon1',
  `souls`       INT(6)          NOT NULL DEFAULT 0,
  `soulwell_usages` INT(6)      NOT NULL DEFAULT 0,
  `soulwell_legendaries` INT(6) NOT NULL DEFAULT 0,
  `soulwell_rares` INT(6)       NOT NULL DEFAULT 0,
  `soulwell_souls_gathered` INT(6) NOT NULL DEFAULT 0,
  `soulwell_souls_purchased` INT(6) NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`),
  KEY (`uuid`)
)

  ENGINE =InnoDB
  DEFAULT CHARSET =latin1;

CREATE TABLE IF NOT EXISTS `sw_permissions` (
  `uuid`  		VARCHAR(255)    NOT NULL,
  `playername`  VARCHAR(60)     NOT NULL,
  `permissions`	VARCHAR(60)     NOT NULL,
  CONSTRAINT `id` PRIMARY KEY (`uuid`, `permissions`),
  KEY (`uuid`)
)

  ENGINE =InnoDB
  DEFAULT CHARSET =latin1;