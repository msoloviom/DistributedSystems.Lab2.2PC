package com.lab2.twopc;

import com.ibm.db2.jcc.DB2Xid;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static javax.transaction.xa.XAResource.TMNOFLAGS;
import static javax.transaction.xa.XAResource.TMSUCCESS;
import static javax.transaction.xa.XAResource.XA_OK;

@Component
public class JtaTransactionManager {

    public static final String INSERT_INTO_FLY_BOOKING = "Insert into fly_booking values (1, 'Nick', 'DGH 678', 'DSF', 'LDM', '05/05/2018')";
    public static final String INSERT_INTO_HOTEL_BOOKING = "Insert into hotel_booking values (1, 'Nick', 'Hilton', '05/05/2018', '15/05/2018')";
    public static final String UPDATE_ACCOUNT = "Update account set value=value-1 where id = 1";

    private static PGXADataSource dataSource1;
    private static PGXADataSource dataSource2;
    private static PGXADataSource dataSource3;

    @Autowired
    private ApplicationContext appContext;

    @PostConstruct
    private void init() {
        dataSource1 = (PGXADataSource) appContext.getBean("dataSource");
        dataSource2 = (PGXADataSource) appContext.getBean("dataSource2");
        dataSource3 = (PGXADataSource) appContext.getBean("dataSource3");
    }

    /**
     * Handle the previous cleanup runGlobalTransaction so that this test can recommence.
     */
    public void setup() throws SQLException {
        Connection c1 = null;
        Connection c2 = null;
        Statement s1;
        Statement s2;
        try {

            c1 = dataSource1.getConnection();
            c2 = dataSource2.getConnection();
            s1 = c1.createStatement();
            s2 = c2.createStatement();

            try {
                s1.executeUpdate("Delete from fly_booking");
                s2.executeUpdate("Delete from hotel_booking");
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            s1.close();
            s2.close();

        } finally {
            if (c1 != null) {
                c1.close();
            }
            if (c2 != null) {
                c2.close();
            }
        }
    }

    public void runGlobalTransaction(String firstQuery, String secondQuery) {
        byte[] gtrid = new byte[]{0x44, 0x11, 0x55, 0x66};
        byte[] bqual = new byte[]{0x00, 0x22, 0x00};
        int rc1;
        int rc2;
        int rc3;

        try {
            XAConnection xaConn1 = dataSource1.getXAConnection();
            XAConnection xaConn2 = dataSource2.getXAConnection();
            XAConnection xaConn3 = dataSource3.getXAConnection();
            XAResource xaRes1 = xaConn1.getXAResource();
            XAResource xaRes2 = xaConn2.getXAResource();
            XAResource xaRes3 = xaConn3.getXAResource();
            Connection conn1 = xaConn1.getConnection();
            Connection conn2 = xaConn2.getConnection();
            Connection conn3 = xaConn3.getConnection();

            Xid xid1 = new DB2Xid(1, gtrid, bqual);
            Xid xid2 = new DB2Xid(2, gtrid, bqual);
            Xid xid3 = new DB2Xid(3, gtrid, bqual);

            xaRes1.start(xid1, TMNOFLAGS);
            xaRes2.start(xid2, TMNOFLAGS);
            xaRes3.start(xid3, TMNOFLAGS);

            Statement stmt1 = conn1.createStatement();
            Statement stmt2 = conn2.createStatement();
            Statement stmt3 = conn3.createStatement();

            stmt1.executeUpdate(firstQuery);
            stmt2.executeUpdate(secondQuery);
            stmt3.executeUpdate(UPDATE_ACCOUNT);

            xaRes1.end(xid1, TMSUCCESS);
            xaRes2.end(xid2, TMSUCCESS);
            xaRes3.end(xid3, TMSUCCESS);

            try {
                // Now prepare both branches of the distributed transaction.
                // Both branches must prepare successfully before changes
                // can be committed.
                // If the distributed transaction fails, an XAException is thrown.
                rc1 = xaRes1.prepare(xid1);
                if (rc1 == XA_OK) {
                    // Prepare was successful. Prepare the second connection.
                    rc2 = xaRes2.prepare(xid2);
                    if (rc2 == XA_OK) {
                        rc3 = xaRes3.prepare(xid3);
                        if (rc3 == XA_OK) {
                            xaRes1.commit(xid1, false);
                            xaRes2.commit(xid2, false);
                            xaRes3.commit(xid3, false);
                        }
                    }
                }
            } catch (XAException xae) {
                System.out.println("\nDistributed transaction prepare/commit failed. " +
                        "Rolling it back.");
                System.out.println("XAException error code = " + xae.errorCode);
                System.out.println("XAException message = " + xae.getMessage());
                xae.printStackTrace();
                try {
                    xaRes1.rollback(xid1);
                } catch (XAException xae1) {
                    System.out.println("Distributed Transaction rollback xaRes1 failed");
                    System.out.println("XAException error code = " + xae1.errorCode);
                    System.out.println("XAException message = " + xae1.getMessage());
                }
                try {
                    xaRes2.rollback(xid2);

                } catch (XAException xae2) {
                    System.out.println("Distributed Transaction rollback xaRes2 failed");
                    System.out.println("XAException error code = " + xae2.errorCode);
                    System.out.println("XAException message = " + xae2.getMessage());
                }try {
                    xaRes2.rollback(xid3);

                } catch (XAException xae3) {
                    System.out.println("Distributed Transaction rollback xaRes3 failed");
                    System.out.println("XAException error code = " + xae3.errorCode);
                    System.out.println("XAException message = " + xae3.getMessage());
                }
            }
            try {
                conn1.close();
                xaConn1.close();
            } catch (Exception e) {
                System.out.println("Failed to close connection 1: " + e.toString());
                e.printStackTrace();
            }
            try {
                conn2.close();
                xaConn2.close();
            } catch (Exception e) {
                System.out.println("Failed to close connection 2: " + e.toString());
                e.printStackTrace();
            }try {
                conn3.close();
                xaConn3.close();
            } catch (Exception e) {
                System.out.println("Failed to close connection 3: " + e.toString());
                e.printStackTrace();
            }
        } catch (SQLException sqlex) {
            System.out.println("SQLException caught: " + sqlex.getMessage());
            sqlex.printStackTrace();
        } catch (XAException xae) {
            System.out.println("XA error is " + xae.getMessage());
            xae.printStackTrace();
        }
    }
}