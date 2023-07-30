package financial.infrastructure;

import financial.domain.Account;
import financial.domain.AccountRepository;
import financial.domain.LockedAccount;
import java.util.logging.Logger;

import java.util.Optional;

public class SqlAccountRepository implements AccountRepository {
    @Override
    public Optional<Account> get(String id) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Optional<LockedAccount> getLocked(String id) {
        // TODO: Open transaction
        // TODO: Acquire the lock

        return this.get(id).map(SqlLockedAccount::new);
    }

    @Override
    public void save(LockedAccount lockedAccount) {
        saveAccountForReal(lockedAccount.account());
    }

    private void saveAccountForReal(Account account) {
        throw new RuntimeException("Not implemented");
    }

    private void releaseLock(String accountId) {
        throw new RuntimeException("Not implemented");
    }

    private class SqlLockedAccount implements LockedAccount {
        private Account account;
        private boolean isLocked = true;

        SqlLockedAccount(Account account) {
            this.account = account;
        }

        @Override
        public Account account() {
            return account;
        }

        @Override
        public void close() {
            releaseLock(account.id);
            isLocked = false;
            account = null;  // So that LockedAccount will not be used once again after the lock was released
        }

        /**
         * Called by Garbage Collector before removing the object from memory.
         * We want to notify ourselves, if someone forgets to release the lock.
         *
         * PS: `finalize()` is deprecated, so you should not use implement.
         * It's here like this just to display the purpose, modern approach with java.lang.ref.Cleaner requires a bit more code.
         */
        @Override
        protected void finalize() { // Call by Garbage Collector before removing the object from memory
            if (isLocked) {
                Logger.getGlobal().severe("Someone forgot to release lock for account: " + account.id);
                this.close();
            }
        }
    }
}
