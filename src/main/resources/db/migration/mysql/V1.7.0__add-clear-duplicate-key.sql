CREATE TABLE IF NOT EXISTS duplicates_history (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    authentication_ID varchar(255) NOT NULL,
    client_ID varchar(255) NOT NULL,
    time_resolved TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=INNODB; 

DELIMITER $$
CREATE PROCEDURE clearDuplicates()
BEGIN 
    DECLARE is_done INT DEFAULT FALSE;
    DECLARE this_id INT DEFAULT "";
    DECLARE this_auth varchar(255) DEFAULT "";
    DECLARE this_client varchar(255) DEFAULT "";
    
    DECLARE this_token CURSOR FOR
        SELECT authentication_id, client_id
        FROM token
        WHERE status COLLATE utf8_unicode_ci = 0
        GROUP BY authentication_id
        HAVING COUNT(*) > 1;    
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET is_done = TRUE;
    
    OPEN this_token;
    read_loop: LOOP
    
    FETCH this_token INTO this_auth, this_client; 
    IF is_done THEN
        LEAVE read_loop;  
    END IF;
    
    SELECT "Duplicate details: ", this_auth as 'AuthID', this_client as 'ClientID';    

    UPDATE token
    SET status = 1
    WHERE authentication_id COLLATE utf8_unicode_ci = this_auth AND status COLLATE utf8_unicode_ci = 0;
    
    INSERT INTO duplicates_history (authentication_ID, client_ID)
    VALUES (this_auth, this_client);

    END LOOP;
    CLOSE this_token;
END$$
DELIMITER ;

