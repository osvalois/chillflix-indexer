package com.chillflix.indexer.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TmdbSearchRequest {
    private Integer tmdbId;
    private String language;
}