package com.twitter.app.serviceimpl;

import com.twitter.app.model.TwitterDetails;
import com.twitter.app.model.UserDetails;
import com.twitter.app.repository.TwitterRepository;
import com.twitter.app.utils.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TwitterClientServiceImpl {

    Logger logger = LoggerFactory.getLogger(TwitterClientServiceImpl.class);

    @Autowired
    private TwitterRepository twitterRepository;

    @Autowired
    private UserRepository userRepository;

    public int saveTwitterSecrets(TwitterDetails twitterDetails) {
        boolean userExists = userRepository.findByEmail(twitterDetails.getEmail());
        boolean secretExists = userExists ? twitterRepository.userSecretsAvailable(twitterDetails.getEmail()) : true;
        if (userExists && !secretExists) {
            int saveSecrets = twitterRepository.saveSecrets(twitterDetails);
            return saveSecrets;
        }
        logger.error(userExists ? secretExists ? "Secrets already exists for - " + twitterDetails.getEmail() : "Secrets does not exists"
                : "User does not exists");

        return -1; // means error or user already exists
    }

    public int saveNewUser(UserDetails userDetails) {
        boolean userExists = userRepository.findByEmail(userDetails.getEmail());
        if (!userExists) {
            int saveNewUser = twitterRepository.saveNewUser(userDetails);
            return saveNewUser;
        }
        logger.error("user already exists with email - {}", userDetails.getEmail());
        return -1;
    }
}
