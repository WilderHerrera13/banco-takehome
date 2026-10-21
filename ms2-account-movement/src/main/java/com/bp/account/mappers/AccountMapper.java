package com.bp.account.mappers;

import com.bp.account.models.Account;
import com.bp.account.models.AccountType;
import com.bp.shared.dto.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountDto toDto(Account account);

    @Mapping(target = "currentBalance", source = "initialBalance")
    Account toEntity(AccountDto dto);

    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "initialBalance", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "active", defaultValue = "true")
    void update(@MappingTarget Account account, AccountDto dto);

    default AccountType toAccountType(String value) {
        return AccountType.valueOf(value.toUpperCase());
    }
}
