# Mitigating Broken Access Control using type system


## What is it about?

In 2021 OWASP moved Broken Access Control type of vulnerabilities to the [first place of their TOP10 list](https://owasp.org/Top10/). 
One of the reasons it can happen, is the fact that developers forget.
  * Forget to check if the user is authenticated when writing a new method in a controller
  * Forget to check that the data they are presenting, belongs to a different user
  * Forget to check that if the user has admin rights 

And it is understandable. Maybe project is being delivered under great pressure before the Black Friday, or product owner does 
not consider security as a priority and authorization was not even discussed, or the service was inherited from another team
and no one in the team does fully understand how it works. 
Or possibly it was you who wrote it but didn't work on it in last year, and completely forgot everything!

Here I would like to present some ideas of how a type system of your language can save future you or others from making
mistakes that can cost your company a huge lawsuit and can cost you a job.


## But first, let's look at some beautiful design...

Rust. I like Rust very much. Mainly for its brilliant API. Let's look at one example:

```rust
let data = vec![1,2,3,4,5];
let mutex = Mutex::new(data);

mutex.lock().unwrap().push(6); // OK
data.push(7); // Compile time error
println!("{:?}", data);  // Compile time error
```

I will not explain here how this works in details, just would like to mention the properties of this API:

  * Once you've decided the data must be locked, there is (almost) no way to bypass this restriction
  * Anyone who needs to read this data or change this data later in the code, has to acquire the lock first

You need to put quite some extra effort to misuse it.

I'm not advocating to use Rust everywhere, but let's try to get inspired and see where we can apply something like this.

## Example 1: Some Financial application in Java

**DISCLAIMER: Examples are often oversimplified! Please, look at the intent, not implementation.** 

Let's assume we have some Java application which does something with money. 
There we have Accounts, and money can be withdrawn from it.
Our current code, might be looking similar to this:

```java
import java.util.Optional;

public class Account {
    public final String id;
    private long deposit;

    // Some constructor here

    public void withdraw(long amount) {
        if (deposit < amount) {
            throw new RuntimeException("Not enough money!");
        }
        deposit -= amount;
    }
}

public interface AccountRepository {
    Optional<Account> get(String accountId);
    void save(Account account);
}
```
We would really like to avoid Race conditions with accounts which can lead to monet being withdrawn twice,
and we won't be able to track it. In essence, we do not want an `Account` to be changed without acquiring lock. 
To lock an account, we might have some `AccountLockService` looking like this:
```java
public interface AccountLockService {
    void acquireLock(String accountId);
    void releaseLock(String accountId);
}
```

When a developer wants to make a change in account, they have to **ALWAYS** follow these steps in this **EXACT** order:
```java
public class SomeService {
    // ...
   void doSomething(String accountId) {
       lockService.acquire(accountId);
       try {
           var maybeAccount = repository.get(accountId);
           var account = maybeAccount.get();

           // do things with the account

           repository.save(account);
       } finally {
           lockService.release(accountId);
       }
   }
}
```

In this case there are multiple problems with this approach:

  * It's easy to forget to acquire and release the lock
  * Developer can acquire the lock after getting the order and the lock won't be that useful anymore
  * Forgetting try-finally block, means that in case of exception we might have hanging lock and other (correct) 
    code won't be able to work with the account
  * There is no problem of saving account after the lock was released

We can mitigate some of these problems, by adding `isLocked(accountId)` method on `LockService`, have a reference to
it in `AccountRepository` and check if the lock was acquired before saving the account. 
This will prevent inconsistencies in our database, but if new feature is developed under time pressure, there 
is a high chance that integration tests are not added (if exist at all) and we will find out about the issue in production.
And depending on the organization deployment culture, it can potentially be weeks from the day the code was written.

You can have code like this if, for example, you use some NoSQL database that does not have transactions. 
If you use an SQL database, you will probably have no `LockService`, but will be doing locks on DB level with SQL,
potentially like this:
```java
public interface AccountRepository {
    Optional<Account> get(String accountId);
    Optional<Account> getForModification(String accountId); // Acquires the lock
    void save(Account account);  // Releases the lock
}
```
We have 2 "get" methods, because sometimes we want just display the data without changing and there is no point
in acquiring the lock, and sometimes we want the account to be locked. 

```java
public class SomeService {
    // ...
   void doSomething(String accountId) {
       var maybeAccount = repository.getForModification(accountId);
       var account = maybeAccount.get();

       // do things with the account

       repository.save(account);
   }
}
```

Here we have slightly different problems:

  * If an exception is thrown, it's not clear how do we release the lock. 
    Should we have a separate method on the repository to release a lock and call it in `finally`?
  * It's still easy to forget acquiring the lock. The only potential safety net here is that our ORM or DB driver might 
    throw an exception if we try to release the lock we didn't acquire. I personally have never seen this in real life. 
    But even in case if it throws, we will probably find out about it only after deployment.

What can we do here to mitigate all these problems? What if we remember that we got inspired 
by the Rust `Mutex` example and try to do the same?

Let's try to make the compiler help us (and everyone). 
We want to allow saving only the accounts that have been locked:

```java
public interface AccountRepository {
    Optional<Account> get(String accountId);
    /**
     *  If the account exists, will acquire the lock and return LockedAccount
     */
    Optional<LockedAccount> getForModification(String accountId);
    void save(LockedAccount account);
}
```

OK! We have a different type now, and it's impossible to save the account without acquiring the lock first.

What about releasing? And in general, how can we model locked account? And make it easy to use...

In Java, there is an idiomatic Java pattern [try-with-resources](https://docs.oracle.com/javase/7/docs/technotes/guides/language/try-with-resources.html) 
which is often used to read files (or access some global resources), so developer won't forget to close the file handle. 
Lock is a global resource! 
Can we use this pattern? Let's try to put everything together:

```java

import java.util.logging.Logger;

public class SomeService {
    private AccountRepository repository;
    // ...
    public void withdraw(String accountId, long sum) {
        try (var lockedAccount = repository.getLocked(accountId).orElseThrow(() -> new RuntimeException("O_o"))) {
            lockedAccount.account().withdraw(sum);
            repository.save(lockedAccount);
        }
    }
}

public interface AccountRepository {
    Optional<Account> get(String id);
    Optional<LockedAccount> getLocked(String id);
    void save(LockedAccount account);
}

public interface LockedAccount extends AutoCloseable {
    Account account();
    @Override
    void close();
}

public class SqlAccountRepository implements AccountRepository {
    @Override
    public Optional<Account> get(String id) {
        //...
    }

    @Override
    public Optional<LockedAccount> getLocked(String id) {
        // Open transaction...
        // Acquire the lock...
        
        return this.get(id).map(SqlLockedAccount::new);
    }

    @Override
    public void save(LockedAccount lockedAccount) {
        saveAccountForReal(lockedAccount.account());
    }

    private void saveAccountForReal(Account account) {
        //...
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
        public void close() { // Will be called by JVM on leaving `try` block in `try-with-resources` pattern
            releaseLock(account.id);
            isLocked = false;
            account = null;  // So that LockedAccount will not be used once again after the lock was released
        }

        /**
         * Called by Garbage Collector before removing the object from memory.
         * We want to notify ourselves, if someone forgets to release the lock.
         */
        @Override
        protected void finalize() {
            if (isLocked) {
                Logger.getGlobal().severe("Someone forgot to release lock for account: " + account.id);
                this.close();
            }
        }
    }
}
```

So what features does this design have:

  * We cannot save the account, unless we lock it - compiler will not allow it
  * `try-with-resources` usage is rather easy to use and lock will be released in time
  * No possibility of accidentally saving it after we released the lock. 
    We reset the `Account` reference in `LockedAccount` after releasing lock
  * Even if we forget to use `try-with-resources`, our IDE will remind us. Or our code linter. 
    Or, in impossible case when we do not have a code linter in our deployment pipeline, 
    we will see some messages in logs coming from `finalize()` method
  * `SqlLockedAccount` is private in `SqlAccountRepository`, so the only way to get it is by calling `getLocked`. **REMEMBER THIS!** We will need it later


Can someone circumvent this protection? - Ofcourse! There are at least 2 ways from the top of my head:

  1. Reflection
  2. One can create their own class implementing `LockedAccount` interface

But let's not forget why we were doing it. Main problems are, people do not know or forgot that 
the account has to be locked and how to do the locking.
Using reflection and implementing `LockedAccount` is harder to do than use it correctly, 
and in worst case will be very visible as soon as someone else will look at the code. 

What is missing in this example is ability to create an account. There are different ways to do it. In case if 
account ID gets assigned by the database (i.e. autoincrement, which you should consider not using due to the chance
of enumeration attack), then repository can have `create(Account)` method, returning either `Account` or `LockedAccount`
depending on your use-case.

#### To summarize

We've designed a set of classes that is easy to use and hard to misuse. 
If we misuse them, the compiler will instantly tell us.


But what about Access Control and security? We're getting there, don't worry.


TBC...
