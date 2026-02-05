package org.harenoasa.summerframework.jdbc.tx;

import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.entity.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class DataSourceTransactionManager implements PlatformTransactionManager, InvocationHandler {

    static final ThreadLocal<TransactionStatus> transactionStatus = new ThreadLocal<>();

    final Logger logger = LoggerFactory.getLogger(getClass());

    final DataSource dataSource;

    public DataSourceTransactionManager(DataSource datasource) {
        this.dataSource = datasource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TransactionStatus ts = transactionStatus.get();
        if(ts == null) {
            try(Connection connection = dataSource.getConnection()){
                final boolean autoCommit = connection.getAutoCommit();
                if (autoCommit) {
                    connection.setAutoCommit(false);
                }
                try{
                    transactionStatus.set(new TransactionStatus(connection));
                    Object r = method.invoke(proxy, args);
                    connection.commit();
                    return r;
                } catch (InvocationTargetException e) {
                    log.warn("will rollback transaction for caused exception: {}", e.getCause() == null ?  "null" : e.getCause().getClass().getName());
                    TransactionException te = new TransactionException(e.getCause());
                    try {
                        connection.rollback();
                    } catch (SQLException sqle) {
                        te.addSuppressed(sqle);
                    }
                    throw te;
                } finally {
                    transactionStatus.remove();
                    if (autoCommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }
        }
        return null;
    }
}
