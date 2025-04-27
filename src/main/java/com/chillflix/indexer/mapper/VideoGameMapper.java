package com.chillflix.indexer.mapper;

import com.chillflix.indexer.dto.VideoGameDTO;
import com.chillflix.indexer.entities.VideoGame;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VideoGameMapper {

    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    VideoGame toEntity(VideoGameDTO dto);

    VideoGameDTO toDto(VideoGame entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(VideoGameDTO dto, @MappingTarget VideoGame entity);
}