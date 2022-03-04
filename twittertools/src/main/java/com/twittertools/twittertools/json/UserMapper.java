package com.twittertools.twittertools.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.twittertools.twittertools.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserMapper {
    private String id;
    private String name;
    private String username;
    private String description;
    @JsonProperty("profile_image_url")
    private String profileImageUrl;
    private Object withheld;

    public User mapToUser(){
        return new User(id, name, username, description, profileImageUrl, withheld, null);
    }
}
