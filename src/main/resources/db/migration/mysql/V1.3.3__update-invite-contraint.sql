SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `invite` MODIFY `inviter_id` mediumint(8) unsigned NULL;

SET FOREIGN_KEY_CHECKS = 1;
