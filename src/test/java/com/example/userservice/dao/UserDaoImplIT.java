package com.example.userservice.dao;

import com.example.userservice.exception.UserServiceException;
import com.example.userservice.model.User;
import com.example.userservice.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("UserDaoImpl — интеграционные тесты (Testcontainers + PostgreSQL)")
class UserDaoImplIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("userdb")
                    .withUsername("test")
                    .withPassword("test");

    private static UserDaoImpl dao;

    @BeforeAll
    static void init() {
        // Переопределяем подключение Hibernate на контейнер
        Properties overrides = new Properties();
        overrides.put("hibernate.connection.url", POSTGRES.getJdbcUrl());
        overrides.put("hibernate.connection.username", POSTGRES.getUsername());
        overrides.put("hibernate.connection.password", POSTGRES.getPassword());
        overrides.put("hibernate.hbm2ddl.auto", "create-drop"); // чистая схема на каждый запуск

        HibernateUtil.rebuildWith(overrides);
        dao = new UserDaoImpl();
    }

    @AfterAll
    static void tearDown() {
        HibernateUtil.shutdown();
    }

    @BeforeEach
    void cleanDatabase() {
        // Изоляция: чистим таблицу перед каждым тестом
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.createMutationQuery("DELETE FROM User").executeUpdate();
            tx.commit();
        }
    }

    @Test
    @DisplayName("save: сохраняет и присваивает id и createdAt")
    void save_persistsUser() {
        User saved = dao.save(new User("Alice", "alice@example.com", 30));

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findById: находит сохранённого")
    void findById_found() {
        User saved = dao.save(new User("Bob", "bob@example.com", 25));

        Optional<User> found = dao.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("findById: пустой Optional для несуществующего id")
    void findById_notFound() {
        assertThat(dao.findById(999L)).isEmpty();
    }

    @Test
    @DisplayName("findAll: возвращает всех, отсортированных по id")
    void findAll_sorted() {
        dao.save(new User("A", "a@x.com", 20));
        dao.save(new User("B", "b@x.com", 21));
        dao.save(new User("C", "c@x.com", 22));

        List<User> all = dao.findAll();

        assertThat(all).hasSize(3)
                .extracting(User::getName)
                .containsExactly("A", "B", "C");
    }

    @Test
    @DisplayName("update: изменения сохраняются в БД")
    void update_persistsChanges() {
        User saved = dao.save(new User("Old", "old@x.com", 30));
        saved.setName("New");
        saved.setAge(35);

        dao.update(saved);

        User reloaded = dao.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("New");
        assertThat(reloaded.getAge()).isEqualTo(35);
        assertThat(reloaded.getEmail()).isEqualTo("old@x.com");
    }

    @Test
    @DisplayName("deleteById: пользователь исчезает")
    void deleteById_removes() {
        User saved = dao.save(new User("Temp", "temp@x.com", 40));

        dao.deleteById(saved.getId());

        assertThat(dao.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("save: дубликат email → UserServiceException (UNIQUE constraint)")
    void save_duplicateEmail_throws() {
        dao.save(new User("First", "dup@x.com", 20));

        assertThatThrownBy(() -> dao.save(new User("Second", "dup@x.com", 21)))
                .isInstanceOf(UserServiceException.class);
    }
}