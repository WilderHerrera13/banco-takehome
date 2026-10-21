package com.bp.client.mappers;

import com.bp.client.models.Client;
import com.bp.client.models.Gender;
import com.bp.shared.dto.ClientDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientDto toDto(Client client);

    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "active", defaultValue = "true")
    Client toEntity(ClientDto dto);

    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "identification", ignore = true)
    @Mapping(target = "active", defaultValue = "true")
    void update(@MappingTarget Client client, ClientDto dto);

    default Gender toGender(String value) {
        return value == null || value.isBlank() ? null : Gender.valueOf(value.toUpperCase());
    }
}
