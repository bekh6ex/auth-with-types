package financial.domain;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> get(String id);
    Optional<LockedAccount> getLocked(String id);
    void save(LockedAccount account);
}
