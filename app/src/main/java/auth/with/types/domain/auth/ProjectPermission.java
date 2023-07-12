package auth.with.types.domain.auth;

import auth.with.types.domain.Project;

import java.util.Objects;
import java.util.Optional;

public final class ProjectPermission extends Permission {
    private final Optional<String> projectId;


    static ProjectPermission allowAll() {
        return new ProjectPermission(Optional.empty());
    }

    static ProjectPermission specificProject(String projectId) {
        Objects.requireNonNull(projectId);

        return new ProjectPermission(Optional.of(projectId));
    }


    private ProjectPermission(Optional<String> projectId) {
        super();
        this.projectId = projectId;
    }

    public Optional<String> getProjectId() {
        return this.projectId;
    }

    public boolean isAllowedToAccessProject(Project project) {
        if (this.projectId.isPresent()) {
            String allowedProject = this.projectId.get();
            return project.id().equals(allowedProject);
        } else {
            return true;
        }
    }
}
