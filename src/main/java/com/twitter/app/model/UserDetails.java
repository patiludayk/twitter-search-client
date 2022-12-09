package com.twitter.app.model;

import lombok.Data;

@Data
public class UserDetails {
    private String name;
    private String email;
    private String mobile;
    private String password;
}
