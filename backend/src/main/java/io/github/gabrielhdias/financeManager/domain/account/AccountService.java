package io.github.gabrielhdias.financeManager.domain.account;

import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserRepository;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.DuplicateResourceException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(
        AccountRepository accountRepository,
        UserRepository userRepository,
        TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Account create(Long userId, String name) {
        User user = findUserById(userId);

        if (accountRepository.existsByUserIdAndName(userId, name.trim())) {
            throw new DuplicateResourceException(
                "Já existe uma conta com esse nome para o usuário"
            );
        }

        Account account = new Account(name, user);

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> listByUser(Long userId) {
        return accountRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Account findById(Long userId, Long accountId) {
        return findAccountByIdAndUserId(accountId, userId);
    }

    @Transactional
    public Account rename(Long userId, Long accountId, String newName) {
        Account account = findAccountByIdAndUserId(accountId, userId);

        String normalizedName = newName.trim();

        if (!account.getName().equals(normalizedName)
            && accountRepository.existsByUserIdAndName(userId, normalizedName)) {
            throw new DuplicateResourceException(
                "Já existe uma conta com esse nome para o usuário"
            );
        }

        account.rename(newName);

        return account;
    }

    @Transactional
    public void delete(Long userId, Long accountId) {
        Account account = findAccountByIdAndUserId(accountId, userId);

        if (transactionRepository.existsByAccountId(accountId)) {
            throw new BusinessRuleException(
                "Não é possível excluir uma conta que possui transações"
            );
        }

        accountRepository.delete(account);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuário não encontrado"
            ));
    }

    private Account findAccountByIdAndUserId(
        Long accountId,
        Long userId
    ) {
        return accountRepository.findByIdAndUserId(
                accountId,
                userId
            )
            .orElseThrow(() -> new ResourceNotFoundException(
                "Conta não encontrada"
            ));
    }
}
