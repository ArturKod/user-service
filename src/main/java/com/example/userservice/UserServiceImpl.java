package com.example.userservice;

import com.example.userservice.dao.UserDao;
import com.example.userservice.dao.UserDaoImpl;
import com.example.userservice.exception.UserServiceException;
import com.example.userservice.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");

    private final UserDao userDao;

    public UserServiceImpl() {
        this(new UserDaoImpl());
    }

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public User createUser(String name, String email, Integer age) {
        validateName(name);
        validateEmail(email);
        validateAge(age);

        User user = new User(name.trim(), email.trim().toLowerCase(), age);
        User saved = userDao.save(user);
        log.info("Создан пользователь: {}", saved);
        return saved;
    }

    @Override
    public Optional<User> getUserById(Long id) {
        validateId(id);
        return userDao.findById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    public User updateUser(Long id, String name, String email, Integer age) {
        validateId(id);

        User existing = userDao.findById(id)
                .orElseThrow(() -> new UserServiceException("Пользователь с id=" + id + " не найден"));

        if (name != null && !name.isBlank()) {
            existing.setName(name.trim());
        }
        if (email != null && !email.isBlank()) {
            validateEmail(email);
            existing.setEmail(email.trim().toLowerCase());
        }
        if (age != null) {
            validateAge(age);
            existing.setAge(age);
        }

        User updated = userDao.update(existing);
        log.info("Обновлён пользователь: {}", updated);
        return updated;
    }

    @Override
    public void deleteUser(Long id) {
        validateId(id);
        userDao.findById(id)
                .orElseThrow(() -> new UserServiceException("Пользователь с id=" + id + " не найден"));
        userDao.deleteById(id);
        log.info("Удалён пользователь с id={}", id);
    }


    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new UserServiceException("ID должен быть положительным числом");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new UserServiceException("Имя не может быть пустым");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new UserServiceException("Некорректный email: " + email);
        }
    }

    private void validateAge(Integer age) {
        if (age == null || age < 0 || age > 150) {
            throw new UserServiceException("Возраст должен быть в диапазоне 0..150");
        }
    }
}