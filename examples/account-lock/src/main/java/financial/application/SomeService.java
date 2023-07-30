package financial.application;

import financial.domain.AccountRepository;
import financial.domain.LockedAccount;

public class SomeService {
    private AccountRepository repository;

    public SomeService(AccountRepository repository) {
        this.repository = repository;
    }

    public void withdraw(String accountId, long sum) {
        try (var lockedAccount = repository.getLocked(accountId).orElseThrow(() -> new RuntimeException("O_o"))) {
            lockedAccount.account().withdraw(sum);
            repository.save(lockedAccount);
        }
    }
}
