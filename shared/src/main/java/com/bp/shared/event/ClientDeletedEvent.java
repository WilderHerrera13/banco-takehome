package com.bp.shared.event;

import java.time.Instant;

public record ClientDeletedEvent(
        String clientId,
        Instant fechaEvento) {
}
