package com.example.userservice.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

public class HibernateUtil {

    private static volatile SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            synchronized (HibernateUtil.class) {
                if (sessionFactory == null || sessionFactory.isClosed()) {
                    sessionFactory = buildSessionFactory();
                }
            }
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Ошибка инициализации SessionFactory: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }

    public static void rebuildWith(Properties overrides) {
        shutdown();
        sessionFactory = null;
        Configuration cfg = new Configuration().configure();
        overrides.forEach((k, v) -> cfg.setProperty(k.toString(), v.toString()));
        sessionFactory = cfg.buildSessionFactory();
    }
}