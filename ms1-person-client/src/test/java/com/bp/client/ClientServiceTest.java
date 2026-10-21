package com.bp.client;

import com.bp.client.mappers.ClientMapperImpl;
import com.bp.client.models.Client;
import com.bp.client.models.Gender;
import com.bp.client.exception.ClientNotFoundException;
import com.bp.client.exception.DuplicateIdentificationException;
import com.bp.client.repository.ClientRepository;
import com.bp.client.service.ClientEventPublisher;
import com.bp.client.service.ClientService;
import com.bp.shared.dto.ClientDto;
import com.bp.shared.event.ClientRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("F5 - Unit test of the client service")
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientEventPublisher eventPublisher;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository, eventPublisher, new ClientMapperImpl());
    }

    private ClientDto newClient() {
        return new ClientDto(null, "Jose Lema", "MALE", 35, "1712345678", "Sn y principal", "098254785", "1234", true);
    }

    @Test
    @DisplayName("creates the client, persists it and publishes the domain event")
    void shouldCreateClientAndPublishEvent() {
        ClientDto request = newClient();
        when(clientRepository.existsByIdentification("1712345678")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            return new Client("cli-1", client.getName(), client.getGender(), client.getAge(),
                    client.getIdentification(), client.getAddress(), client.getPhone(), client.getPassword(),
                    client.isActive());
        });

        ClientDto result = clientService.create(request);

        assertThat(result.clientId()).isEqualTo("cli-1");
        assertThat(result.name()).isEqualTo("Jose Lema");
        assertThat(result.active()).isTrue();

        ArgumentCaptor<ClientRegisteredEvent> event = ArgumentCaptor.forClass(ClientRegisteredEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().clientId()).isEqualTo("cli-1");
        assertThat(event.getValue().name()).isEqualTo("Jose Lema");
    }

    @Test
    @DisplayName("rejects an identification that is already registered")
    void shouldRejectDuplicateIdentification() {
        when(clientRepository.existsByIdentification("1712345678")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(newClient()))
                .isInstanceOf(DuplicateIdentificationException.class)
                .hasMessageContaining("1712345678");

        verify(clientRepository, never()).save(any(Client.class));
        verify(eventPublisher, never()).publish(any(ClientRegisteredEvent.class));
    }

    @Test
    @DisplayName("returns the requested client")
    void shouldReturnExistingClient() {
        when(clientRepository.findById("cli-1")).thenReturn(Optional.of(new Client("cli-1", "Jose Lema",
                Gender.MALE, 35, "1712345678", "Sn y principal", "098254785", "1234", true)));

        ClientDto result = clientService.get("cli-1");

        assertThat(result.clientId()).isEqualTo("cli-1");
        assertThat(result.name()).isEqualTo("Jose Lema");
    }

    @Test
    @DisplayName("fails when the client does not exist")
    void shouldFailWhenClientNotFound() {
        when(clientRepository.findById("cli-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.get("cli-404"))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("cli-404");
    }
}
