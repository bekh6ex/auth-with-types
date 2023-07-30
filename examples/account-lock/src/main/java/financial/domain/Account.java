package financial.domain;

public class Account {
    public final String id;
    private long deposit;

    public Account(String id, long deposit) {
        this.id = id;
        this.deposit = deposit;
    }

    // Some constructor here

    public void withdraw(long amount) {
        if (deposit < amount) {
            throw new RuntimeException("Not enough money!");
        }
        deposit -= amount;
    }
}
