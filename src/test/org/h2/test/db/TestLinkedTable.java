/*
 * Copyright 2004-2006 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.db;

import java.math.BigDecimal;
import java.sql.*;

import org.h2.test.TestBase;

public class TestLinkedTable extends TestBase {

    public void test() throws Exception {
        deleteDb("linked1");
        deleteDb("linked2");
        Class.forName("org.h2.Driver");
        
        Connection conn = DriverManager.getConnection("jdbc:h2:"+BASE_DIR+"/linked1", "sa1", "abc");
        Statement stat = conn.createStatement();
        stat.execute("CREATE TEMP TABLE TEST_TEMP(ID INT PRIMARY KEY)");
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR(255), XT TINYINT, XD DECIMAL(10,2), XTS TIMESTAMP, XBY BINARY(255), XBO BIT, XSM SMALLINT, XBI BIGINT, XBL BLOB, XDA DATE, XTI TIME, XCL CLOB, XDO DOUBLE)");
        stat.execute("CREATE INDEX IDXNAME ON TEST(NAME)");
        stat.execute("INSERT INTO TEST VALUES(0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)");
        stat.execute("INSERT INTO TEST VALUES(1, 'Hello', -1, 10.30, '2001-02-03 11:22:33.4455', X'FF0102', TRUE, 3000, 1234567890123456789, X'1122AA', DATE '0002-01-01', TIME '00:00:00', 'J\u00fcrg', 2.25)"); 
        testRow(stat, "TEST");
        stat.execute("INSERT INTO TEST VALUES(2, 'World', 30, 100.05, '2005-12-31 12:34:56.789', X'FFEECC33', FALSE, 1, -1234567890123456789, X'4455FF', DATE '9999-12-31', TIME '23:59:59', 'George', -2.5)");
        testRow(stat, "TEST");
        stat.execute("SELECT * FROM TEST_TEMP");
        conn.close();
        
        conn = DriverManager.getConnection("jdbc:h2:"+BASE_DIR+"/linked1", "sa1", "abc");
        stat = conn.createStatement();        
        testRow(stat, "TEST");
        try {
            stat.execute("SELECT * FROM TEST_TEMP");
            error("temp table must not be persistent");
        } catch(SQLException e) {
            checkNotGeneralException(e);
        }
        conn.close();
        
        conn = DriverManager.getConnection("jdbc:h2:"+BASE_DIR+"/linked2", "sa2", "def");
        stat = conn.createStatement();
        stat.execute("CREATE LINKED TABLE IF NOT EXISTS LINK_TEST('org.h2.Driver', 'jdbc:h2:"+BASE_DIR+"/linked1', 'sa1', 'abc', 'TEST')");
        stat.execute("CREATE LINKED TABLE IF NOT EXISTS LINK_TEST('org.h2.Driver', 'jdbc:h2:"+BASE_DIR+"/linked1', 'sa1', 'abc', 'TEST')");
        testRow(stat, "LINK_TEST");
        
        conn.close();
        conn = DriverManager.getConnection("jdbc:h2:"+BASE_DIR+"/linked2", "sa2", "def");
        stat = conn.createStatement();
        
        stat.execute("INSERT INTO LINK_TEST VALUES(3, 'Link Test', 30, 100.05, '2005-12-31 12:34:56.789', X'FFEECC33', FALSE, 1, -1234567890123456789, X'4455FF', DATE '9999-12-31', TIME '23:59:59', 'George', -2.5)");
        
        ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM LINK_TEST");
        rs.next();
        check(rs.getInt(1), 4);

        rs = stat.executeQuery("SELECT COUNT(*) FROM LINK_TEST WHERE NAME='Link Test'");
        rs.next();
        check(rs.getInt(1), 1);

        int uc = stat.executeUpdate("DELETE FROM LINK_TEST WHERE ID=3");
        check(uc, 1);

        rs = stat.executeQuery("SELECT COUNT(*) FROM LINK_TEST");
        rs.next();
        check(rs.getInt(1), 3);

        rs = stat.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='LINK_TEST'");
        rs.next();
        check(rs.getString("TABLE_TYPE"), "TABLE LINK");
        
        rs.next();
        rs = stat.executeQuery("SELECT * FROM LINK_TEST WHERE ID=0");
        rs.next();
        check(rs.getString("NAME")==null && rs.wasNull());
        check(rs.getString("XT")==null && rs.wasNull());
        check(rs.getInt("ID")==0 && !rs.wasNull());
        check(rs.getBigDecimal("XD")==null && rs.wasNull());
        check(rs.getTimestamp("XTS")==null && rs.wasNull());
        check(rs.getBytes("XBY")==null && rs.wasNull());
        check(rs.getBoolean("XBO")==false && rs.wasNull());
        check(rs.getShort("XSM")==0 && rs.wasNull());
        check(rs.getLong("XBI")==0 && rs.wasNull());
        check(rs.getString("XBL")==null && rs.wasNull());
        check(rs.getString("XDA")==null && rs.wasNull());
        check(rs.getString("XTI")==null && rs.wasNull());
        check(rs.getString("XCL")==null && rs.wasNull());
        check(rs.getString("XDO")==null && rs.wasNull());
        checkFalse(rs.next());
        
        stat.execute("DROP TABLE LINK_TEST");
        
        stat.execute("CREATE LINKED TABLE LINK_TEST('org.h2.Driver', 'jdbc:h2:"+BASE_DIR+"/linked1', 'sa1', 'abc', '(SELECT COUNT(*) FROM TEST)')");
        rs = stat.executeQuery("SELECT * FROM LINK_TEST");
        rs.next();
        check(rs.getInt(1), 3);
        checkFalse(rs.next());
        
        conn.close();
        
        deleteDb("linked1");
        deleteDb("linked2");
    }
    
    void testRow(Statement stat, String name) throws Exception {
        ResultSet rs = stat.executeQuery("SELECT * FROM "+name+" WHERE ID=1");
        rs.next();
        check(rs.getString("NAME"), "Hello");
        check(rs.getByte("XT"), -1);
        BigDecimal bd = rs.getBigDecimal("XD");
        check(bd.equals(new BigDecimal("10.30")));
        Timestamp ts = rs.getTimestamp("XTS");
        String s = ts.toString();
        check(s, "2001-02-03 11:22:33.4455");
        check(ts.equals(Timestamp.valueOf("2001-02-03 11:22:33.4455")));
        check(rs.getBytes("XBY"), new byte[]{(byte)255, (byte)1, (byte)2});
        check(rs.getBoolean("XBO"));
        check(rs.getShort("XSM"), 3000);
        check(rs.getLong("XBI"), 1234567890123456789L);
        check(rs.getString("XBL"), "1122aa");
        check(rs.getString("XDA"), "0002-01-01");
        check(rs.getString("XTI"), "00:00:00");
        check(rs.getString("XCL"), "J\u00fcrg");
        check(rs.getString("XDO"), "2.25");
        
    }

}
