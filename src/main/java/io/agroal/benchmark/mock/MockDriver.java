// Copyright (C) 2017 Red Hat, Inc. and individual contributors as indicated by the @author tags.
// You may not use this file except in compliance with the Apache License, Version 2.0.

package io.agroal.benchmark.mock;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDriver;
import static java.sql.DriverManager.registerDriver;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;

/**
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class MockDriver implements Driver {

    public static void registerMockDriver() {
        try {
            registerDriver(new MockDriver());
        } catch ( SQLException e ) {
            getLogger( MockDriver.class.getName() ).log( WARNING, "Unable to register MockDriver into Driver Manager", e );
        }
    }

    @Override
    public Connection connect(String url, Properties info) {
        return new MockConnection.Empty();
    }

    public static void deregisterMockDriver() {
        try {
            deregisterDriver( getDriver( "" ) );
        } catch ( SQLException e ) {
            getLogger( MockDriver.class.getName() ).log( WARNING, "Unable to deregister MockDriver from Driver Manager", e );
        }
    }

    // --- //

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
