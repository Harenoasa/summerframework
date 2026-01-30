package org.harenoasa.summerframework.jdbc;

import org.harenoasa.summerframework.entity.exception.DataAccessException;

import javax.sql.DataSource;

public class JdbcTemplate {

    final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Number queryForNumber(String sql, Object... args) throws DataAccessException {
        return queryForObject(sql, NumberRowMapper.instance, args);
    }

    public <T>  T queryForObject(String sql, Class<T> clazz, Object... args)  throws DataAccessException {


        if(clazz == String.class) return (T) queryForObject(sql ,StringRowMapper.instance, args);
        if(clazz == Boolean.class || clazz == boolean.class)return queryForObject(sql,BooleanRowMapper.instance, args);
        if(Number.class.isAssignableFrom(clazz) || clazz.isPrimitive())return (T) queryForObject(sql, new BooleanRowMapper<>(clazz),args);
    }
}
