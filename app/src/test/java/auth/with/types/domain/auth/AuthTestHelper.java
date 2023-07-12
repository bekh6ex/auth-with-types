package auth.with.types.domain.auth;

import java.util.UUID;

public class AuthTestHelper {

    public static AuthenticatedUserId authUserId(UUID id) {
        return new AuthenticatedUserId(id);
    }
}
