package com.twitter.app.repository;

import com.twitter.app.model.TwitterDetails;
import com.twitter.app.model.UserDetails;
import com.twitter.app.utils.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class TwitterRepository {

    Logger logger = LoggerFactory.getLogger(TwitterRepository.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    public int saveSecrets(TwitterDetails twitterDetails) {
        int savedUserCount = Integer.MIN_VALUE;
        try {
            savedUserCount = jdbcTemplate.update("INSERT INTO TWEETER_SECRETS (userid, consumerkey, consumersecret, token, secret) VALUES (?, ?, ?, ?, ?)",
                    getUserId(twitterDetails.getEmail()), // "userid from user table"
                    twitterDetails.getConsumerKey(),
                    twitterDetails.getConsumerSecret(),
                    twitterDetails.getToken(),
                    twitterDetails.getSecret());
        } catch (Exception e) {
            logger.error("Error updating twitter secrets. " + e);
            e.printStackTrace();
        }
        return savedUserCount;
    }

    public int saveNewUser(UserDetails userDetails) {
        int saveNewUser = Integer.MIN_VALUE;
        try {
            saveNewUser = jdbcTemplate.update("INSERT INTO USERS (full_name, email, mobile, password) VALUES (?, ?, ?, ?)",
                    userDetails.getName(),
                    userDetails.getEmail(),
                    userDetails.getMobile(),
                    userDetails.getPassword());
        } catch (Exception e) {
            logger.error("Error registering new user.");
            e.printStackTrace();
        }
        return saveNewUser;
    }

    public int deleteSecrets(TwitterDetails twitterDetails) {
        int deleteSecrets = Integer.MIN_VALUE;
        try {
            int userId = getUserId(twitterDetails.getEmail());
            deleteSecrets = jdbcTemplate.update("DELETE FROM TWEETER_SECRETS WHERE USERID=?", userId);
        } catch (Exception e) {
            logger.error("Error updating twitter secrets. " + e);
            e.printStackTrace();
        }
        return deleteSecrets;
    }

    public int deleteSecretsUsingUserId(int userId) {
        int deleteSecrets = Integer.MIN_VALUE;
        try {
            deleteSecrets = jdbcTemplate.update("DELETE FROM TWEETER_SECRETS WHERE USERID=?", userId);
        } catch (Exception e) {
            logger.error("Error updating twitter secrets. " + e);
            e.printStackTrace();
        }
        return deleteSecrets;
    }

    public int getUserId(String email) throws Exception {
        int userId = userRepository.getUserId(email);
        if (userId > 0) {
            return userId;
        }
        throw new Exception("user not exists!");
    }

    public Map<String, Object> findUserSecrets(String email) {
        try {
            Map<String, Object> userSecrets = jdbcTemplate.queryForMap("select * from TWEETER_SECRETS where USERID=?", getUserId(email));
            return userSecrets;
        } catch (EmptyResultDataAccessException noSecretsAvailable) {
            logger.info("No secrets available.");
            return null;
        } catch (IncorrectResultSizeDataAccessException duplicateData) {
            logger.error("Duplicate record found." + duplicateData);
            return null;
        } catch (Exception e) {
            logger.error("Error getting user secrets for - " + email);
            logger.error("Error message: " + e.getMessage());
            return null;
        }
    }

    public boolean userSecretsAvailable(String email) {
        return findUserSecrets(email) != null;
    }

}
