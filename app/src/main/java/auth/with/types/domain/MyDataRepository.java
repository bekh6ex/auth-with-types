package auth.with.types.domain;


import auth.with.types.domain.auth.AuthenticatedUserId;

public class MyDataRepository {

    public MyUserData getMyUserData(AuthenticatedUserId userId){
        throw new RuntimeException("Not implemented");
    }

    public Project getMyProject(AuthenticatedUserId userId){
        throw new RuntimeException("Not implemented");
    }
}

record MyUserData(String username, String email){}
