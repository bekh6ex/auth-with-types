package auth.with.types;

import auth.with.types.domain.CustomerRepository;
import auth.with.types.domain.MyDataRepository;
import auth.with.types.domain.auth.AuthService;
import auth.with.types.domain.auth.AuthenticatedUser;
import auth.with.types.domain.auth.ProjectPermission;

import java.util.Optional;

public class App {
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        AuthService authService = new AuthService();

        AuthenticatedUser authenticatedUser = authService.authenticate("some jwt");
        MyDataRepository myDataRepository = new MyDataRepository();

        CustomerRepository customerRepository = new CustomerRepository();
        Optional<ProjectPermission> projectPermission = authenticatedUser.projectPermission(myDataRepository);

        if (projectPermission.isPresent()) {
            customerRepository.getCustomers(projectPermission.get());
        } else {
            System.out.println("You are not allowed to access customers");
        }



        System.out.println(new App().getGreeting());
    }
}
