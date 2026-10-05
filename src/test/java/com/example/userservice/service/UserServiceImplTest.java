package com.example.userservice.service;

import com.example.userservice.UserServiceImpl;
import com.example.userservice.dao.UserDao;
import com.example.userservice.exception.UserServiceException;
import com.example.userservice.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — юнит-тесты (Mockito)")
class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        // MockitoExtension сам сбрасывает моки перед каждым тестом → изоляция
        sampleUser = new User("Alice", "alice@example.com", 30);
        sampleUser.setId(1L);
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("сохраняет валидного пользователя и нормализует email")
        void success() {
            when(userDao.save(any(User.class))).thenReturn(sampleUser);

            User result = userService.createUser("  Alice ", "Alice@Example.com", 30);

            assertThat(result).isSameAs(sampleUser);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userDao).save(captor.capture());
            User passed = captor.getValue();
            assertThat(passed.getName()).isEqualTo("Alice");           // trim
            assertThat(passed.getEmail()).isEqualTo("alice@example.com"); // lower + trim
        }

        @Test
        @DisplayName("пустое имя → UserServiceException, DAO не вызывается")
        void blankName() {
            assertThatThrownBy(() -> userService.createUser("   ", "a@b.com", 20))
                    .isInstanceOf(UserServiceException.class)
                    .hasMessageContaining("Имя");
            verify(userDao, never()).save(any());
        }

        @Test
        @DisplayName("некорректный email → UserServiceException")
        void invalidEmail() {
            assertThatThrownBy(() -> userService.createUser("Bob", "not-an-email", 20))
                    .isInstanceOf(UserServiceException.class)
                    .hasMessageContaining("email");
            verify(userDao, never()).save(any());
        }

        @Test
        @DisplayName("возраст 200 → UserServiceException")
        void invalidAge() {
            assertThatThrownBy(() -> userService.createUser("Bob", "bob@x.com", 200))
                    .isInstanceOf(UserServiceException.class)
                    .hasMessageContaining("Возраст");
            verify(userDao, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("находит пользователя")
        void found() {
            when(userDao.findById(1L)).thenReturn(Optional.of(sampleUser));
            assertThat(userService.getUserById(1L)).contains(sampleUser);
        }

        @Test
        @DisplayName("не найден → пустой Optional")
        void notFound() {
            when(userDao.findById(99L)).thenReturn(Optional.empty());
            assertThat(userService.getUserById(99L)).isEmpty();
        }

        @Test
        @DisplayName("null id → UserServiceException, DAO не вызывается")
        void nullId() {
            assertThatThrownBy(() -> userService.getUserById(null))
                    .isInstanceOf(UserServiceException.class);
            verifyNoInteractions(userDao);
        }

        @Test
        @DisplayName("отрицательный id → UserServiceException")
        void negativeId() {
            assertThatThrownBy(() -> userService.getUserById(-5L))
                    .isInstanceOf(UserServiceException.class);
            verifyNoInteractions(userDao);
        }
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("возвращает список из DAO")
        void returnsAll() {
            when(userDao.findAll()).thenReturn(List.of(sampleUser));
            assertThat(userService.getAllUsers()).containsExactly(sampleUser);
        }

        @Test
        @DisplayName("пустой список — возвращает пустой список")
        void emptyList() {
            when(userDao.findAll()).thenReturn(List.of());
            assertThat(userService.getAllUsers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("обновляет переданные поля, остальные не трогает")
        void success() {
            when(userDao.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userDao.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User updated = userService.updateUser(1L, "Bob", null, 25);

            assertThat(updated.getName()).isEqualTo("Bob");
            assertThat(updated.getEmail()).isEqualTo("alice@example.com"); // не тронут
            assertThat(updated.getAge()).isEqualTo(25);
            verify(userDao).update(sampleUser);
        }

        @Test
        @DisplayName("несуществующий пользователь → UserServiceException")
        void notFound() {
            when(userDao.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(42L, "Bob", "bob@x.com", 25))
                    .isInstanceOf(UserServiceException.class)
                    .hasMessageContaining("не найден");
            verify(userDao, never()).update(any());
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("удаляет существующего")
        void success() {
            when(userDao.findById(1L)).thenReturn(Optional.of(sampleUser));

            userService.deleteUser(1L);

            verify(userDao).deleteById(1L);
        }

        @Test
        @DisplayName("несуществующий → UserServiceException, DAO.deleteById не вызывается")
        void notFound() {
            when(userDao.findById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(7L))
                    .isInstanceOf(UserServiceException.class);
            verify(userDao, never()).deleteById(any());
        }
    }
}