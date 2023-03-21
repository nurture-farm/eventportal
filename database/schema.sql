CREATE TABLE events (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `namespace` varchar(16) NOT NULL,
  `name` varchar(256) NOT NULL,
  `index` int NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL,
  PRIMARY KEY(`id`),
  CONSTRAINT `name_namespace_index` UNIQUE (`name`,`namespace`),
  CONSTRAINT `namespace_index` UNIQUE (`index`,`namespace`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE event_properties (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `type` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `value` varchar(512) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL,
  PRIMARY KEY(`id`),
  FOREIGN KEY (event_id) REFERENCES events(id),
  CONSTRAINT `event_property_index` UNIQUE (`event_id`,`name`,`value`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;