package com.oltpbenchmark.api;

import java.io.File;
import java.util.Collection;

import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.benchmarks.epinions.EpinionsBenchmark;
import com.oltpbenchmark.benchmarks.epinions.procedures.GetItemAverageRating;
import com.oltpbenchmark.types.DatabaseType;
import com.oltpbenchmark.util.ClassUtil;
import com.oltpbenchmark.util.FileUtil;

import junit.framework.TestCase;

public class TestStatementDialects extends TestCase {
    
    static {
      org.apache.log4j.PropertyConfigurator.configure("/home/pavlo/Documents/OLTPBenchmark/OLTPBenchmark/log4j.properties");
    }
    
    private EpinionsBenchmark benchmark;
    private WorkloadConfiguration workConf;
    private File xmlFile;
    
    private static final DatabaseType TARGET_DATABASE = DatabaseType.SQLITE;
    private static final Class<? extends Procedure> TARGET_PROCEDURE = GetItemAverageRating.class;
    private static final String TARGET_STMT = "getAverageRating";
    private static final String TARGET_STMT_SQL = "SELECT * FROM review";
    
    private static final String dialectXML = 
            "<dialects>\n" +
            "<dialect type=\"" + TARGET_DATABASE.name() + "\">\n" +
            "<procedure name=\"" + TARGET_PROCEDURE.getSimpleName() + "\">\n" +
            "<statement name=\""+ TARGET_STMT + "\">" + TARGET_STMT_SQL + "</statement>\n" +
            "</procedure>\n" +
            "</dialect>\n" +
            "</dialects>";
                
    
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        this.workConf = new WorkloadConfiguration();
        this.benchmark = new EpinionsBenchmark(this.workConf);
        this.xmlFile = FileUtil.writeStringToTempFile(dialectXML, "xml");
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
        if (this.xmlFile.exists()) {
            this.xmlFile.delete();
        }
    }
    
    /**
     * testLoadXMLFile
     */
    public void testLoadXMLFile() throws Exception {
        for (DatabaseType dbType : DatabaseType.values()) {
            this.workConf.setDBType(dbType);
            File xmlFile = this.benchmark.getSQLDialect();
            assertNotNull(dbType.toString(), xmlFile);
            
            StatementDialects dialects = new StatementDialects(dbType, xmlFile);
            boolean ret = dialects.load();
            if (ret == false) continue;
            
            Collection<String> procNames = dialects.getProcedureNames();
            assertNotNull(dbType.toString(), procNames);
            assertFalse(dbType.toString(), procNames.isEmpty());
            
            for (String procName : procNames) {
                assertFalse(procName.isEmpty());
                Collection<String> stmtNames = dialects.getStatementNames(procName);
                assertNotNull(procName, stmtNames);
                assertFalse(procName, stmtNames.isEmpty());
                
                for (String stmtName : stmtNames) {
                    assertFalse(stmtName.isEmpty());
                    String stmtSQL = dialects.getSQL(procName, stmtName);
                    assertNotNull(stmtSQL);
                    assertFalse(stmtSQL.isEmpty());
                } // FOR (stmt)
            } // FOR (proc)
        } // FOR (dbtype)
    }
    
    /**
     * testSetDialect
     */
    public void testSetDialect() throws Exception {
        // Load in our fabricated dialects
        StatementDialects dialects = new StatementDialects(TARGET_DATABASE, this.xmlFile);
        boolean ret = dialects.load();
        assertTrue(ret);
        Procedure proc = ClassUtil.newInstance(TARGET_PROCEDURE, new Object[0], new Class<?>[0]);
        assertNotNull(proc);
        proc.initialize();
        proc.loadSQLDialect(dialects);
        
        // And then check to see that our target SQLStmt got its SQL changed
        SQLStmt stmt = proc.getStatment(TARGET_STMT);
        assertNotNull(stmt);
        assertEquals(TARGET_STMT_SQL, stmt.getSQL());
    }
}