package com.chillflix.indexer.mapper;

import com.chillflix.indexer.dto.SeriesEpisodeDTO;
import com.chillflix.indexer.entities.SeriesEpisode;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SeriesEpisodeMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SeriesEpisode toEntity(SeriesEpisodeDTO dto);

    SeriesEpisodeDTO toDto(SeriesEpisode entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(SeriesEpisodeDTO dto, @MappingTarget SeriesEpisode entity);
}