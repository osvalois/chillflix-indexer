package com.chillflix.indexer.mapper;

import com.chillflix.indexer.dto.MusicDTO;
import com.chillflix.indexer.entities.Music;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MusicMapper {

    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Music toEntity(MusicDTO dto);

    MusicDTO toDto(Music entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(MusicDTO dto, @MappingTarget Music entity);
}