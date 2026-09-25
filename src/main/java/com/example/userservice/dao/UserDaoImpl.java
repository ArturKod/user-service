package com.example.userservice.dao;

import com.example.userservice.exception.UserServiceException;
import com.example.userservice.model.User;
import com.example.userservice.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    @Override
    public User save(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(user);
            tx.commit();
            log.info("Создан пользователь: {}", user);
            return user;
        } catch (Exception e) {
            rollbackQuietly(tx);
            log.error("Ошибка при создании пользователя", e);
            throw new UserServiceException("Не удалось создать пользователя", e);
        } finally {
            session.close();
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.get(User.class, id);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователя по id={}", id, e);
            throw new UserServiceException("Не удалось найти пользователя", e);
        }
    }

    @Override
    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User ORDER BY id", User.class).list();
        } catch (Exception e) {
            log.error("Ошибка при получении списка пользователей", e);
            throw new UserServiceException("Не удалось получить список пользователей", e);
        }
    }

    @Override
    public User update(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            User merged = session.merge(user);
            tx.commit();
            log.info("Обновлён пользователь: {}", merged);
            return merged;
        } catch (Exception e) {
            rollbackQuietly(tx);
            log.error("Ошибка при обновлении пользователя", e);
            throw new UserServiceException("Не удалось обновить пользователя", e);
        } finally {
            session.close();
        }
    }

    @Override
    public void deleteById(Long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            User user = session.get(User.class, id);
            if (user != null) {
                session.remove(user);
                log.info("Удалён пользователь с id={}", id);
            } else {
                log.warn("Пользователь с id={} не найден для удаления", id);
            }
            tx.commit();
        } catch (Exception e) {
            rollbackQuietly(tx);
            log.error("Ошибка при удалении пользователя с id={}", id, e);
            throw new UserServiceException("Не удалось удалить пользователя", e);
        } finally {
            session.close();
        }
    }

    private void rollbackQuietly(Transaction tx) {
        if (tx != null && tx.isActive()) {
            try {
                tx.rollback();
            } catch (Exception rollbackEx) {
                log.error("Ошибка отката транзакции", rollbackEx);
            }
        }
    }
}