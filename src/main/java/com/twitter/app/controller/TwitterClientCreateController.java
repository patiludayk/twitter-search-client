package com.twitter.app.controller;

import com.twitter.app.model.TwitterDetails;
import com.twitter.app.model.UserDetails;
import com.twitter.app.service.TwitterClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.StringJoiner;

@Slf4j
@Controller
@RequestMapping("user")
public class TwitterClientCreateController {

    @Autowired
    private TwitterClientService twitterClientService;

    @PostMapping("/twitter/addUser")
    public ResponseEntity addNewUser(@RequestBody UserDetails userDetails) {
        // register new user to DB
        int newUser = twitterClientService.saveNewUser(userDetails);
        if (newUser < 0) {
            log.error("unable to register user - {}", userDetails.getEmail());
            return ResponseEntity.badRequest().body("User already exists with email");
        }
        return ResponseEntity.ok("user added " + userDetails.getEmail());
    }

    @PostMapping("/twitter/secrets")
    public ResponseEntity registerTwitterClientForUser(@RequestBody TwitterDetails twitterDetails) {

        // store user specific twitter secrets to DB here
        int userSecrets = twitterClientService.saveTwitterSecrets(twitterDetails);
        if (userSecrets < 0) {
            log.error("Unable to save secrets! - " + userSecrets);
            return ResponseEntity.badRequest().body("Either User not exists or secrets already exists!");
        }
        return ResponseEntity.ok("user secrets saved!");
    }

    private void validateForNullAndEmptyValue(StringJoiner errorMsg, String field, String fieldLabel) {
        if (null == field || field.isEmpty()) {
            errorMsg.add(fieldLabel);
            log.error("Error message. Input " + fieldLabel + " has wrong value - " + field);
        }
    }

}
