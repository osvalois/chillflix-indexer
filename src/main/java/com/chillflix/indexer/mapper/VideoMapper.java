package com.chillflix.indexer.mapper;

import com.chillflix.indexer.dto.VideoDTO;
import com.chillflix.indexer.entities.Video;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VideoMapper {

    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Video toEntity(VideoDTO dto);

    VideoDTO toDto(Video entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(VideoDTO dto, @MappingTarget Video entity);
}