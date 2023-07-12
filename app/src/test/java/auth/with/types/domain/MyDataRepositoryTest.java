package auth.with.types.domain;

import auth.with.types.domain.auth.AuthTestHelper;
import auth.with.types.domain.auth.AuthenticatedUserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MyDataRepositoryTest {

    @Test
    void getMyUserData() {
        MyDataRepository myDataRepository = new MyDataRepository();
        AuthenticatedUserId authenticatedUserId = AuthTestHelper.authUserId(UUID.randomUUID());

        MyUserData myUserData = myDataRepository.getMyUserData(authenticatedUserId);

        assertNotEquals(null, myUserData);
    }
}
