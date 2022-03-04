package com.twittertools.twittertools.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TwitterResponseMapper {
    @JsonProperty("data")
    List<UserMapper> data;
    @JsonProperty("meta")
    MetaMapper meta;
    @JsonProperty("errors")
    List<Object> errors;
}
