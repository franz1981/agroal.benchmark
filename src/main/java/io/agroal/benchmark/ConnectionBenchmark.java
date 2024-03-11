// Copyright (C) 2017 Red Hat, Inc. and individual contributors as indicated by the @author tags.
// You may not use this file except in compliance with the Apache License, Version 2.0.

package io.agroal.benchmark;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.benchmark.mock.MockDriver;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static io.agroal.api.configuration.AgroalDataSourceConfiguration.DataSourceImplementation.AGROAL;
import static io.agroal.api.configuration.AgroalDataSourceConfiguration.DataSourceImplementation.HIKARI;

/**
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
@Warmup( iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement( iterations = 10, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork( value = 2 )
@BenchmarkMode( Mode.Throughput )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@State( Scope.Benchmark )
public class ConnectionBenchmark {

    private static final Random RANDOM = new Random();

    private static DataSource dataSource;

    public enum PoolType {
        agroal, hikari
    }

    @Param
    public PoolType poolType;

    @Param( {"50", "20", "8"} )
    public int poolSize;

    @Param( {"jdbc:stub"} )
    public String jdbcUrl;

    @Param( {"10"} )
    public int preWork;

    @Param( {"10"} )
    public int postWork;

    @Param( {"false"} )
    public boolean yield;

    @Param( {"500"} )
    public int sleepUs;

    @Benchmark
    @CompilerControl( CompilerControl.Mode.DONT_INLINE )
    public Connection cycleConnection() throws SQLException {
        Connection connection = dataSource.getConnection();

        // Do some work
        doWork(preWork);

        // Yield!
        doYield( yield );

        // Wait some time
        doSleep(sleepUs);

        // Do some work
        doWork(postWork);

        connection.close();
        return connection;
    }

    public static void doWork(long amount) {
        if ( amount > 0 ) {
            Blackhole.consumeCPU( amount );
        }
    }

    public static void doYield(boolean b) {
        if ( b ) {
            Thread.yield();
        }
    }

    public static void doSleep(long us) {
        if (us > 0) {
            long nsToSleep = us * 1000;
            long started = System.nanoTime();
            long remaining;
            while ((remaining = nsToSleep - (System.nanoTime() - started)) > 0) {
                if (remaining > 50_000) {
                    LockSupport.parkNanos(remaining);
                } else {
                    Thread.yield();
                }
            }
        }
    }

    // --- //

    @Setup( Level.Trial )
    public void setup() throws SQLException {
        MockDriver.registerMockDriver();

        AgroalDataSourceConfigurationSupplier supplier = new AgroalDataSourceConfigurationSupplier()
                .metricsEnabled( false )
                .connectionPoolConfiguration( cp -> cp
                        .initialSize(poolSize)
                        .maxSize( poolSize )
                        .validationTimeout( Duration.ofMinutes( 15 ))
                        .connectionFactoryConfiguration( cf -> cf
                                .jdbcUrl( jdbcUrl )
                                .connectionProviderClassName( MockDriver.class.getName() )
                        )
                );

        switch ( poolType ) {
            case hikari:
                supplier.dataSourceImplementation( HIKARI );
                break;
            case agroal:
                supplier.dataSourceImplementation( AGROAL );
                break;
        }
        dataSource = AgroalDataSource.from( supplier );
    }

    @TearDown( Level.Trial )
    public void teardown() throws SQLException {
        ( (AgroalDataSource) dataSource ).close();

        MockDriver.deregisterMockDriver();
    }

    public static void main(String[] args) throws SQLException {
        ConnectionBenchmark benchmark = new ConnectionBenchmark();
        benchmark.poolSize = 1;
        benchmark.poolType = PoolType.hikari;
        benchmark.jdbcUrl = "jdbc:stub";
        benchmark.setup();
        benchmark.cycleConnection();
    }
}
