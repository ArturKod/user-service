package com.example.userservice;

import com.example.userservice.dao.UserDao;
import com.example.userservice.dao.UserDaoImpl;
import com.example.userservice.exception.UserServiceException;
import com.example.userservice.model.User;
import com.example.userservice.util.HibernateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final UserDao userDao = new UserDaoImpl();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        log.info("Запуск user-service");
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readInt("Выберите пункт: ");

            try {
                switch (choice) {
                    case 1 -> createUser();
                    case 2 -> findUserById();
                    case 3 -> findAllUsers();
                    case 4 -> updateUser();
                    case 5 -> deleteUser();
                    case 0 -> running = false;
                    default -> System.out.println("Неверный пункт меню.");
                }
            } catch (UserServiceException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }

        HibernateUtil.shutdown();
        System.out.println("До свидания!");
    }

    private static void printMenu() {
        System.out.println("\n=== User Service ===");
        System.out.println("1. Создать пользователя");
        System.out.println("2. Найти по ID");
        System.out.println("3. Показать всех");
        System.out.println("4. Обновить пользователя");
        System.out.println("5. Удалить пользователя");
        System.out.println("0. Выход");
    }

    private static void createUser() {
        String name = readString("Имя: ");
        String email = readString("Email: ");
        int age = readInt("Возраст: ");

        User user = new User(name, email, age);
        User saved = userDao.save(user);
        System.out.println("Создан: " + saved);
    }

    private static void findUserById() {
        long id = readInt("ID: ");
        Optional<User> user = userDao.findById(id);
        user.ifPresentOrElse(
                System.out::println,
                () -> System.out.println("Пользователь не найден.")
        );
    }

    private static void findAllUsers() {
        List<User> users = userDao.findAll();
        if (users.isEmpty()) {
            System.out.println("Список пуст.");
        } else {
            users.forEach(System.out::println);
        }
    }

    private static void updateUser() {
        long id = readInt("ID пользователя для обновления: ");
        Optional<User> existing = userDao.findById(id);
        if (existing.isEmpty()) {
            System.out.println("Пользователь не найден.");
            return;
        }

        User user = existing.get();
        String name = readString("Новое имя (" + user.getName() + "): ");
        if (!name.isBlank()) user.setName(name);

        String email = readString("Новый email (" + user.getEmail() + "): ");
        if (!email.isBlank()) user.setEmail(email);

        String ageStr = readString("Новый возраст (" + user.getAge() + "): ");
        if (!ageStr.isBlank()) user.setAge(Integer.parseInt(ageStr));

        User updated = userDao.update(user);
        System.out.println("Обновлён: " + updated);
    }

    private static void deleteUser() {
        long id = readInt("ID пользователя для удаления: ");
        userDao.deleteById(id);
        System.out.println("Операция удаления выполнена.");
    }


    private static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число.");
            }
        }
    }
}