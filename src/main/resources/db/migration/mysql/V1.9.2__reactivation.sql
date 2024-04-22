CREATE TABLE `reactivation` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `code` char(40) NOT NULL,
  `email` varchar(150) NOT NULL,
  `reactivation_status` varchar(20) NOT NULL,
  `requested_at` datetime NOT NULL,
  `reactivated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
