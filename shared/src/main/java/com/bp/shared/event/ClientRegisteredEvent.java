package com.bp.shared.event;

import java.time.Instant;

public record ClientRegisteredEvent(
        String clientId,
        String name,
        String identification,
        Instant fechaEvento) {
}
