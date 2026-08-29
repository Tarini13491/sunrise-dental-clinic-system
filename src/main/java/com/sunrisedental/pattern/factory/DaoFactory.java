package com.sunrisedental.pattern.factory;

import com.sunrisedental.dao.AppointmentDao;
import com.sunrisedental.dao.BillDao;
import com.sunrisedental.dao.ReportDao;
import com.sunrisedental.dao.UserDao;

/**
 * Factory Method / Simple Factory for the data-access tier.
 *
 * Why: servlets and services should not construct DAOs with `new` scattered
 * across the codebase. A factory makes it obvious which objects belong to
 * the data tier and lets us swap implementations (e.g. mock DAOs in tests)
 * without touching presentation code.
 *
 * Evaluation: a simple factory is enough for this clinic system. An Abstract
 * Factory would be over-engineering unless we supported more than one
 * database product.
 */
public final class DaoFactory {

    private static final DaoFactory INSTANCE = new DaoFactory();

    private final UserDao userDao = new UserDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final BillDao billDao = new BillDao();
    private final ReportDao reportDao = new ReportDao();

    private DaoFactory() {
    }

    public static DaoFactory get() {
        return INSTANCE;
    }

    public UserDao users() {
        return userDao;
    }

    public AppointmentDao appointments() {
        return appointmentDao;
    }

    public BillDao bills() {
        return billDao;
    }

    public ReportDao reports() {
        return reportDao;
    }
}
