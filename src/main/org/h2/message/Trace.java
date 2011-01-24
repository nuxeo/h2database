/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.message;

import java.text.MessageFormat;
import java.util.ArrayList;
import org.h2.constant.SysProperties;
import org.h2.expression.ParameterInterface;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;

/**
 * This class represents a trace module.
 */
public class Trace {

    /**
     * The trace module name for commands.
     */
    public static final String COMMAND = "command";

    /**
     * The trace module name for constraints.
     */
    public static final String CONSTRAINT = "constraint";

    /**
     * The trace module name for databases.
     */
    public static final String DATABASE = "database";

    /**
     * The trace module name for functions.
     */
    public static final String FUNCTION = "function";

    /**
     * The trace module name for file locks.
     */
    public static final String FILE_LOCK = "fileLock";

    /**
     * The trace module name for indexes.
     */
    public static final String INDEX = "index";

    /**
     * The trace module name for the JDBC API.
     */
    public static final String JDBC = "jdbc";

    /**
     * The trace module name for locks.
     */
    public static final String LOCK = "lock";

    /**
     * The trace module name for schemas.
     */
    public static final String SCHEMA = "schema";

    /**
     * The trace module name for sessions.
     */
    public static final String SESSION = "session";

    /**
     * The trace module name for sequences.
     */
    public static final String SEQUENCE = "sequence";

    /**
     * The trace module name for settings.
     */
    public static final String SETTING = "setting";

    /**
     * The trace module name for tables.
     */
    public static final String TABLE = "table";

    /**
     * The trace module name for triggers.
     */
    public static final String TRIGGER = "trigger";

    /**
     * The trace module name for users.
     */
    public static final String USER = "user";

    /**
     * The trace module name for the page store.
     */
    public static final String PAGE_STORE = "pageStore";

    private TraceWriter traceWriter;
    private String module;
    private String lineSeparator;
    private int traceLevel = TraceSystem.PARENT;

    Trace(TraceWriter traceWriter, String module) {
        this.traceWriter = traceWriter;
        this.module = module;
        this.lineSeparator = SysProperties.LINE_SEPARATOR;
    }

    /**
     * Set the trace level of this component. This setting overrides the parent
     * trace level.
     *
     * @param level the new level
     */
    public void setLevel(int level) {
        this.traceLevel = level;
    }

    private boolean isEnabled(int level) {
        if (this.traceLevel == TraceSystem.PARENT) {
            return traceWriter.isEnabled(level);
        }
        return level <= this.traceLevel;
    }

    /**
     * Check if the trace level is equal or higher than INFO.
     *
     * @return true if it is
     */
    public boolean isInfoEnabled() {
        return isEnabled(TraceSystem.INFO);
    }

    /**
     * Check if the trace level is equal or higher than DEBUG.
     *
     * @return true if it is
     */
    public boolean isDebugEnabled() {
        return isEnabled(TraceSystem.DEBUG);
    }

    /**
     * Write a message with trace level ERROR to the trace system.
     *
     * @param t the exception
     * @param s the message
     */
    public void error(Throwable t, String s) {
        if (isEnabled(TraceSystem.ERROR)) {
            traceWriter.write(TraceSystem.ERROR, module, s, t);
        }
    }

    /**
     * Write a message with trace level ERROR to the trace system.
     *
     * @param t the exception
     * @param s the message
     * @param params the parameters
     */
    public void error(Throwable t, String s, Object... params) {
        if (isEnabled(TraceSystem.ERROR)) {
            s = MessageFormat.format(s, params);
            traceWriter.write(TraceSystem.ERROR, module, s, t);
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     *
     * @param s the message
     */
    public void info(String s) {
        if (isEnabled(TraceSystem.INFO)) {
            traceWriter.write(TraceSystem.INFO, module, s, null);
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     *
     * @param s the message
     * @param params the parameters
     */
    public void info(String s, Object... params) {
        if (isEnabled(TraceSystem.INFO)) {
            s = MessageFormat.format(s, params);
            traceWriter.write(TraceSystem.INFO, module, s, null);
        }
    }

    /**
     * Write a message with trace level INFO to the trace system.
     *
     * @param t the exception
     * @param s the message
     */
    void info(Throwable t, String s) {
        if (isEnabled(TraceSystem.INFO)) {
            traceWriter.write(TraceSystem.INFO, module, s, t);
        }
    }

    /**
     * Format the parameter list.
     *
     * @param parameters the parameter list
     * @return the formatted text
     */
    public static String formatParams(ArrayList<? extends ParameterInterface> parameters) {
        if (parameters.size() == 0) {
            return "";
        }
        StatementBuilder buff = new StatementBuilder(" {");
        int i = 0;
        for (ParameterInterface p : parameters) {
            buff.appendExceptFirst(", ");
            buff.append(++i).append(": ").append(p.getParamValue().getTraceSQL());
        }
        return buff.append('}').toString();
    }

    /**
     * Write a SQL statement with trace level INFO to the trace system.
     *
     * @param sql the SQL statement
     * @param params the parameters used, in the for {1:...}
     * @param count the update count
     * @param time the time it took to run the statement in ms
     */
    public void infoSQL(String sql, String params, int count, long time) {
        if (!isEnabled(TraceSystem.INFO)) {
            return;
        }
        StringBuilder buff = new StringBuilder(sql.length() + params.length() + 20);
        buff.append(lineSeparator).append("/*SQL");
        boolean space = false;
        if (params.length() > 0) {
            // This looks like a bug, but it is intentional:
            // If there are no parameters, the SQL statement is
            // the rest of the line. If there are parameters, they
            // are appended at the end of the line. Knowing the size
            // of the statement simplifies separating the SQL statement
            // from the parameters (no need to parse).
            space = true;
            buff.append(" l:").append(sql.length());
        }
        if (count > 0) {
            space = true;
            buff.append(" #:").append(count);
        }
        if (time > 0) {
            space = true;
            buff.append(" t:").append(time);
        }
        if (!space) {
            buff.append(' ');
        }
        buff.append("*/").
            append(StringUtils.javaEncode(sql)).
            append(StringUtils.javaEncode(params)).
            append(';');
        sql = buff.toString();
        traceWriter.write(TraceSystem.INFO, module, sql, null);
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     *
     * @param s the message
     * @param params the parameters
     */
    public void debug(String s, Object... params) {
        if (isEnabled(TraceSystem.DEBUG)) {
            s = MessageFormat.format(s, params);
            traceWriter.write(TraceSystem.DEBUG, module, s, null);
        }
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     *
     * @param s the message
     */
    public void debug(String s) {
        if (isEnabled(TraceSystem.DEBUG)) {
            traceWriter.write(TraceSystem.DEBUG, module, s, null);
        }
    }

    /**
     * Write a message with trace level DEBUG to the trace system.
     * @param t the exception
     * @param s the message
     */
    public void debug(Throwable t, String s) {
        if (isEnabled(TraceSystem.DEBUG)) {
            traceWriter.write(TraceSystem.DEBUG, module, s, t);
        }
    }


    /**
     * Write Java source code with trace level INFO to the trace system.
     *
     * @param java the source code
     */
    public void infoCode(String java) {
        if (isEnabled(TraceSystem.INFO)) {
            traceWriter.write(TraceSystem.INFO, module, lineSeparator + "/**/" + java, null);
        }
    }

    /**
     * Write Java source code with trace level DEBUG to the trace system.
     *
     * @param java the source code
     */
    void debugCode(String java) {
        if (isEnabled(TraceSystem.DEBUG)) {
            traceWriter.write(TraceSystem.DEBUG, module, lineSeparator + "/**/" + java, null);
        }
    }

}
