package org.harenoasa.summerframework.io.with.tx;

import org.harenoasa.summerframework.context.annotation.Autowired;
import org.harenoasa.summerframework.context.annotation.Component;
import org.harenoasa.summerframework.context.annotation.Transactional;
import org.harenoasa.summerframework.io.without.JdbcTestBase;
import org.harenoasa.summerframework.jdbc.JdbcTemplate;

@Component
@Transactional
public class UserService {

    @Autowired
    AddressService addressService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    public User createUser(String name, int age) {
        Number id = jdbcTemplate.updateAndReturnGeneratedKey(JdbcTestBase.INSERT_USER, name, age);
        User user = new User();
        user.id = id.intValue();
        user.name = name;
        user.theAge = age;
        return user;
    }

    public User getUser(int userId) {
        return jdbcTemplate.queryForObject(JdbcTestBase.SELECT_USER, User.class, userId);
    }

    public void updateUser(User user) {
        jdbcTemplate.update(JdbcTestBase.UPDATE_USER, user.name, user.theAge, user.id);
    }

    public void deleteUser(User user) {
        jdbcTemplate.update(JdbcTestBase.DELETE_USER, user.id);
        addressService.deleteAddress(user.id);
    }
}
