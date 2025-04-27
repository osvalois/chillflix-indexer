package com.chillflix.indexer.mapper;

import com.chillflix.indexer.dto.MusicTrackDTO;
import com.chillflix.indexer.entities.MusicTrack;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MusicTrackMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MusicTrack toEntity(MusicTrackDTO dto);

    MusicTrackDTO toDto(MusicTrack entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(MusicTrackDTO dto, @MappingTarget MusicTrack entity);
}