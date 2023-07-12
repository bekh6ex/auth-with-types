package auth.with.types.domain.auth;

public class AuthService {

    public AuthService() {}

    public AuthenticatedUser authenticate(String token) {
        throw new RuntimeException("Not implemented");
    }
}


abstract class Permission {
    Permission() {
    }
}

