package com.twitter.app.utils;

import com.twitter.app.model.LoginDetails;
import com.twitter.app.model.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class UserRepository {

    @Autowired
    JdbcTemplate jdbcTemplate;

    Logger logger = LoggerFactory.getLogger(UserRepository.class.getName());

    public int save (UserDetails userDetails) {
        int updatedRows = jdbcTemplate.update("INSERT INTO USERS (full_name, email, mobile, password) VALUES (?, ?, ?, ?)",
                                              userDetails.getName(),
                                              userDetails.getEmail(),
                                              userDetails.getMobile(),
                                              userDetails.getPassword());
        return updatedRows;
    }

    public int delete (UserDetails userDetails){
        int deletedUser = Integer.MIN_VALUE;
        try {
            deletedUser = jdbcTemplate.update("DELETE FROM USERS WHERE EMAIL=?", userDetails.getEmail());
        }catch (Exception e){
            logger.error("Error deleting user: " + e);
        }
        return deletedUser;
    }

    public int deleteByEmail (String email){
        int deletedUser = Integer.MIN_VALUE;
        try {
            deletedUser = jdbcTemplate.update("DELETE FROM USERS WHERE EMAIL=?", email);
        }catch (Exception e){
            logger.error("Error deleting user: " + e);
        }
        return deletedUser;
    }

    public boolean findByEmail (String email) {
        Map<String, Object> user = getUser(email);
        return user != null;
    }

    private Map<String, Object> getUser (String email) {
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap("select * from USERS where email=?", email);
            return result;
        }
        catch (EmptyResultDataAccessException noresult) {
            return null;
        }
        catch (IncorrectResultSizeDataAccessException duplicateUser){
            logger.error("Duplicate user, deleting all the records.");
            deleteByEmail(email);
            return null;
        }
        catch (Exception e){
            logger.error("Error getting user: " + email);
            return null;
        }
    }

    public int getUserId (String email) {
        int userId = Integer.MIN_VALUE;
        try {
            String query = "select id from USERS where email='" + email + "'";
            userId = jdbcTemplate.queryForObject(query, Integer.class);
        }
        catch (EmptyResultDataAccessException noresult) {
            logger.error("Not registered user: " + email);
            noresult.printStackTrace();
        }
        return userId;
    }

    public boolean findTwitterDetails (String email) {
        try {
            int userId = getUserId(email);
            Map<String, Object> secrets = jdbcTemplate.queryForMap("select * from TWEETER_SECRETS where USERID=?", userId);
            return !secrets.containsValue(null);
        }
        catch (EmptyResultDataAccessException noresult) {
            return false;
        }
    }

    public boolean loginUser (LoginDetails loginDetails) {
        try {
            Map<String, Object> user = getUser(loginDetails.getEmail());
            if(null != user){
                String password = (String) user.get("password");
                return password.equals(loginDetails.getPassword());
            }
            return false;
        } catch (Exception e) {
            logger.error("Unable to login. " + e);
            return false;
        }
    }
}
