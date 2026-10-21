package com.bp.account.mappers;

import com.bp.account.models.Movement;
import com.bp.shared.dto.MovementDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MovementMapper {

    MovementDto toDto(Movement movement);
}
