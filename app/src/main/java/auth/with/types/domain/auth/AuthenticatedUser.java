package auth.with.types.domain.auth;

import auth.with.types.domain.MyDataRepository;

import java.util.Optional;
import java.util.Set;

public class AuthenticatedUser {

    public final AuthenticatedUserId id;
    private final Set<String> roles;

    AuthenticatedUser(AuthenticatedUserId id, Set<String> roles) {
        this.id = id;
        this.roles = roles;
    }

    public Optional<ProjectPermission> projectPermission(MyDataRepository repo) {

        if (this.roles.contains("admin")) {
            return Optional.of(ProjectPermission.allowAll());
        }
        if (this.roles.contains("projectManager")) {
            var myProject = repo.getMyProject(this.id);

            return Optional.of(ProjectPermission.specificProject(myProject.id()));
        }

        return Optional.empty();
    }
}
