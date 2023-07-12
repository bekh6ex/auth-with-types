package auth.with.types.domain.auth;

import java.util.UUID;

public final class AuthenticatedUserId {
    public final UUID id;

    AuthenticatedUserId(UUID id) {
        this.id = id;
    }

}
