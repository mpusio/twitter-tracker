package com.twittertools.twittertools.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MetaMapper {
    @JsonProperty("result_count")
    private int resultCount;
    @JsonProperty("next_token")
    private String nextToken;
    @JsonProperty("previous_token")
    private String previousToken;
}
