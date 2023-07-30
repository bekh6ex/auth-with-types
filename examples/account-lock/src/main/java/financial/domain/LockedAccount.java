package financial.domain;

public interface LockedAccount extends AutoCloseable {
    Account account();

    @Override
    void close();
}
