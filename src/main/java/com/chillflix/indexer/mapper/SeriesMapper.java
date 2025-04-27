package com.chillflix.indexer.mapper;

import com.chillflix.indexer.dto.SeriesDTO;
import com.chillflix.indexer.entities.Series;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SeriesMapper {

    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Series toEntity(SeriesDTO dto);

    SeriesDTO toDto(Series entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(SeriesDTO dto, @MappingTarget Series entity);
}