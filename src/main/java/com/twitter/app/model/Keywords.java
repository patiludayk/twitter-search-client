package com.twitter.app.model;

import lombok.Data;

@Data
public class Keywords {
    //comma separated keywords to search
    private String keywords;
    private String user;
}
