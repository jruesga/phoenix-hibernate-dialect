/*
 * Copyright (C) 2017 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ruesga.phoenix.dialect;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.commons.math3.util.Pair;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ruesga.phoenix.jpa.JpaEntityManager;
import com.ruesga.phoenix.jpa.entities.Department;
import com.ruesga.phoenix.jpa.entities.DepartmentEmployee;
import com.ruesga.phoenix.jpa.entities.Employee;
import com.ruesga.phoenix.jpa.entities.Gender;
import com.ruesga.phoenix.jpa.entities.Parameter;
import com.ruesga.phoenix.jpa.entities.Salary;
import com.ruesga.phoenix.jpa.entities.TimeRange;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PhoenixDialectTest {

    @ClassRule
    public static HBaseClusterTestRule cluster = new HBaseClusterTestRule("hbase-site.xml");

    private static EntityManager em;
    private static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Properties dbProps = new Properties();
        try {
            dbProps.load(PhoenixDialectTest.class.getResourceAsStream("/database.properties"));
        } catch (Exception e) {
        }
        if (!dbProps.containsKey("test.phoenix.dfs.nodenames") ||
                !dbProps.containsKey("test.phoenix.dfs.db.path")) {
            Assert.fail("Test are not configured. Add a file named 'database.properties' to src/test/resources " +
                "and add the following properties\n\n" +
                "    test.phoenix.dfs.nodenames: HBase zookeepers nodenames in the form <server>:<port>\n" +
                "    test.phoenix.dfs.db.path: HBase path in HDFS\\n\\n");
            return;
        }

        em = JpaEntityManager.getInstance().createEntityManager();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (em != null) {
            em.close();
        }
        JpaEntityManager.getInstance().close();
    }

    @Test
    public void test001_SelectAll() {
        TypedQuery<Employee> q = em.createQuery("select e from employee e", Employee.class);
        List<Employee> e = q.getResultList();
        Assert.assertEquals(31, e.size());
    }

    @Test
    public void test002_SelectRowKey() {
        TypedQuery<Employee> q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 10002);
        Employee e = q.getSingleResult();
        Assert.assertNotNull(e);
        Assert.assertEquals("Bezalel", e.getFirstName());
    }

    @Test
    public void test003_SelectLazyLoad() {
        TypedQuery<Employee> q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 10010);
        Employee e = q.getSingleResult();

        Set<DepartmentEmployee> departments = e.getDepartments();
        Assert.assertNotNull(departments);
        Assert.assertEquals(2, departments.size());
        Department department = findActiveEntity(departments).getDepartment();
        Assert.assertNotNull(department);
        Assert.assertEquals("Finance", department.getDeptName());
    }

    @Test
    public void test004_SelectJoin() {
        TypedQuery<Salary> q = em.createQuery("select s from salary s inner join s.employee as e " +
                "where e.empNo = :empNo", Salary.class);
        q.setParameter("empNo", 10001);
        List<Salary> salaries = q.getResultList();
        Assert.assertEquals(17, salaries.size());
        Salary salary = findActiveEntity(salaries);
        Assert.assertNotNull(salary);
        Assert.assertEquals(Double.valueOf(88958d), salary.getSalary());
    }

    @Test
    public void test005_SelectCount() {
        TypedQuery<Long> q = em.createQuery("select count(d.deptNo) from department d", Long.class);
        long count = q.getSingleResult();
        Assert.assertEquals(3L, count);
    }

    @Test
    public void test006_SelectOrderBy() {
        TypedQuery<Department> q = em.createQuery(
                "select d from department d order by d.deptName desc", Department.class);
        List<Department> departments = q.getResultList();
        Assert.assertEquals(3L, departments.size());
        Assert.assertEquals("Finance", departments.get(2).getDeptName());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void test007_SelectGroupBy() {
        TypedQuery<Pair> q = em.createQuery(
                "select new org.apache.commons.math3.util.Pair(s.employee.empNo, sum(s.salary)) from salary s " +
                "group by s.employee.empNo order by s.employee.empNo asc", Pair.class);
        List<Pair> salaries = q.getResultList();
        Assert.assertEquals(31L, salaries.size());
        Assert.assertEquals(Double.valueOf(1281612d), salaries.get(0).getSecond());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void test008_SelectHaving() {
        TypedQuery<Pair> q = em.createQuery(
                "select new org.apache.commons.math3.util.Pair(s.employee.empNo, sum(s.salary) as sal) from salary s " +
                "group by s.employee.empNo having sum(s.salary) > 1281612 order by sal desc", Pair.class);
        List<Pair> salaries = q.getResultList();
        Assert.assertEquals(4L, salaries.size());
        Assert.assertEquals(Double.valueOf(1604309d), salaries.get(0).getSecond());
    }

    @Test
    public void test009_SelectHint() {
        TypedQuery<Department> q = em.createQuery(" SELECT d from department d " +
                    "where d.deptName = :deptName", Department.class);
        ((org.hibernate.query.Query<Department>)q).addQueryHint(
                String.valueOf(new PhoenixDialect.SecondaryIndexHint(Department.class, "D_I0").build()));
        q.setParameter("deptName", "Finance");
        Department department = q.getSingleResult();
        Assert.assertEquals("Finance", department.getDeptName());
    }

    @Test
    public void test010_SelectLimit() {
        TypedQuery<Department> q = em.createQuery("select d from department d order by d.deptNo asc", Department.class);
        List<Department> departments = q.setMaxResults(2).getResultList();
        Assert.assertEquals(2L, departments.size());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(i + 1, departments.get(i).getDeptNo());
        }
    }

    @Test
    public void test011_SelectFirstResultQuery() {
        TypedQuery<Employee> q = em.createQuery("select e from employee e order by e.empNo asc", Employee.class);
        List<Employee> employees = q.setFirstResult(5).getResultList();
        Assert.assertEquals(10006, employees.get(0).getEmpNo());
    }

    @Test
    public void test012_SelectPagedQuery() {
        TypedQuery<Employee> q = em.createQuery("select e from employee e order by e.empNo asc", Employee.class);
        List<Employee> employees = q.setMaxResults(5).setFirstResult(5).getResultList();
        Assert.assertEquals(5L, employees.size());
        Assert.assertEquals(10006, employees.get(0).getEmpNo());
    }

    @Test
    public void test100_FunctionPercentileCont() {
        TypedQuery<Double> q = em.createQuery("select percentile_cont_asc(0.90, s.salary) from salary s", Double.class);
        Double percentile = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(89264d), percentile);

        q = em.createQuery("select percentile_cont_desc(0.90, s.salary) from salary s", Double.class);
        percentile = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(44238d), percentile);
    }

    @Test
    public void test101_FunctionPercentileDisc() {
        TypedQuery<Double> q = em.createQuery("select percentile_disc_asc(0.90, s.salary) from salary s", Double.class);
        Double percentile = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(89204d), percentile);

        q = em.createQuery("select percentile_disc_desc(0.90, s.salary) from salary s", Double.class);
        percentile = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(44276d), percentile);
    }

    @Test
    public void test102_FunctionPercentRank() {
        TypedQuery<Double> q = em.createQuery("select percent_rank_asc(0.90, s.salary) from salary s", Double.class);
        Double percentile = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(0d), percentile);

        q = em.createQuery("select percent_rank_desc(0.90, s.salary) from salary s", Double.class);
        percentile = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(1d), percentile);
    }

    @Test
    public void test103_FunctionStdDevPop() {
        TypedQuery<Double> q = em.createQuery("select stddev_pop(s.salary) from salary s", Double.class);
        Double stddev = q.getSingleResult();
        Assert.assertTrue(Math.abs(stddev.doubleValue() - 16522.5459d) < 0.001d);
    }

    @Test
    public void test104_FunctionStdDevSamp() {
        TypedQuery<Double> q = em.createQuery("select stddev_samp(s.salary) from salary s", Double.class);
        Double stddev = q.getSingleResult();
        Assert.assertTrue(Math.abs(stddev.doubleValue() - 16546.8975d) < 0.001d);
    }

    @Test
    public void test105_FunctionUpper() {
        TypedQuery<String> q = em.createQuery("select upper('x') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("X", result);
    }

    @Test
    public void test106_FunctionLower() {
        TypedQuery<String> q = em.createQuery("select lower('X') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("x", result);
    }

    @Test
    public void test107_FunctionReverse() {
        TypedQuery<String> q = em.createQuery("select reverse('abc') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("cba", result);
    }

    @Test
    public void test108_FunctionSubstr() {
        TypedQuery<String> q = em.createQuery("select substr('abcdef', 2, 3) from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("bcd", result);
    }

    @Test
    public void test109_FunctionInstr() {
        TypedQuery<Integer> q = em.createQuery("select instr('abcdef', 'bc') from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(2), result);
    }

    @Test
    public void test110_FunctionTrim() {
        TypedQuery<String> q = em.createQuery("select trim(' abcdef ') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("abcdef", result);
    }

    @Test
    public void test111_FunctionLTrim() {
        TypedQuery<String> q = em.createQuery("select ltrim(' abcdef ') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("abcdef ", result);
    }

    @Test
    public void test112_FunctionRTrim() {
        TypedQuery<String> q = em.createQuery("select rtrim(' abcdef ') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals(" abcdef", result);
    }

    @Test
    public void test112_FunctionLPad() {
        TypedQuery<String> q = em.createQuery("select lpad('abcdef', 8) from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("  abcdef", result);
    }

    @Test
    public void test113_FunctionLength() {
        TypedQuery<Integer> q = em.createQuery("select length('abcdef') from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf("abcdef".length()), result);
    }

    @Test
    public void test114_FunctionRegexpSubstr() {
        TypedQuery<String> q = em.createQuery("select regexp_substr('na1-appsrv35-sj35', '[^-]+') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("na1", result);
    }

    @Test
    public void test115_FunctionRegexpReplace() {
        TypedQuery<String> q = em.createQuery("select regexp_replace('abc123ABC', '[0-9]+', '#') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("abc#ABC", result);
    }

    @Test
    public void test116_FunctionToCharDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        TypedQuery<String> q = em.createQuery("select to_char(current_date(), 'yyyy-MM-dd') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals(sdf.format(new Date()), result);
    }

    @Test
    public void test117_FunctionToCharNumber() {
        TypedQuery<String> q = em.createQuery("select to_char(23.5678, '#0.0') from dual d", String.class);
        String result = q.getSingleResult();
        Assert.assertEquals("23.6", result.replace(",", "."));
    }

    @Test
    public void test118_FunctionToDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        TypedQuery<Date> q = em.createQuery("select to_date('2017-02-23', 'yyyy-MM-dd', :tz) from dual d", Date.class);
        q.setParameter("tz", TimeZone.getDefault().getID());
        Date result = q.getSingleResult();
        Assert.assertEquals("2017-02-23", sdf.format(result));
    }

    @Test
    public void test119_FunctionToTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        TypedQuery<Date> q = em.createQuery("select to_time('2005-10-01 14:03:22.559') from dual d", Date.class);
        Date result = q.getSingleResult();
        Assert.assertEquals("14:03:22", sdf.format(result));
    }

    @Test
    public void test120_FunctionToTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        TypedQuery<Date> q = em.createQuery(
                "select to_timestamp('2005-10-01 14:03:22.559', 'yyyy-MM-dd HH:mm:ss.SSS') from dual d", Date.class);
        Date result = q.getSingleResult();
        Assert.assertEquals("2005-10-01 14:03:22.559", sdf.format(result));
    }

    @Test
    public void test121_FunctionConvertTz() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        TypedQuery<Date> q = em.createQuery("select CONVERT_TZ(to_date('2010-01-01', 'yyyy-MM-dd', 'UTC'), " +
                        "'UTC', 'Asia/Tokyo') from dual d", Date.class);
        Date result = q.getSingleResult();
        Assert.assertEquals("2010-01-01 09:00:00.000", sdf.format(result));
    }

    @Test
    public void test122_FunctionTimezoneOffset() {
        TypedQuery<Integer> q = em.createQuery("select TIMEZONE_OFFSET('Indian/Cocos', to_date('2010-01-01', " +
                "'yyyy-MM-dd', 'UTC')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(390), result);
    }

    @Test
    public void test123_FunctionCurrentDate() {
        TypedQuery<Date> q = em.createQuery("select current_date() from dual d", Date.class);
        Date now = q.getSingleResult();
        Assert.assertTrue(Math.abs(System.currentTimeMillis() - now.getTime()) < 300000L);
    }

    @Test
    public void test124_FunctionCurrentTime() {
        TypedQuery<Date> q = em.createQuery("select current_time() from dual d", Date.class);
        Date now = q.getSingleResult();
        Assert.assertTrue(Math.abs(System.currentTimeMillis() - now.getTime()) < 300000L);
    }

    @Test
    public void test125_FunctionNow() {
        TypedQuery<Date> q = em.createQuery("select now() from dual d", Date.class);
        Date now = q.getSingleResult();
        Assert.assertTrue(Math.abs(System.currentTimeMillis() - now.getTime()) < 300000L);
    }

    @Test
    public void test126_FunctionYear() {
        TypedQuery<Integer> q = em.createQuery("select year(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(2017), result);
    }

    @Test
    public void test127_FunctionMonth() {
        TypedQuery<Integer> q = em.createQuery("select month(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(06), result);
    }

    @Test
    public void test128_FunctionWeek() {
        TypedQuery<Integer> q = em.createQuery("select week(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(24), result);
    }

    @Test
    public void test129_FunctionDayOfYear() {
        TypedQuery<Integer> q = em.createQuery("select dayofyear(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(168), result);
    }

    @Test
    public void test130_FunctionDayOfMonth() {
        TypedQuery<Integer> q = em.createQuery("select dayofmonth(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(17), result);
    }

    @Test
    public void test131_FunctionDayOfWeek() {
        TypedQuery<Integer> q = em.createQuery("select dayofweek(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(6), result);
    }

    @Test
    public void test132_FunctionHour() {
        TypedQuery<Integer> q = em.createQuery("select hour(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(12), result);
    }

    @Test
    public void test133_FunctionMinute() {
        TypedQuery<Integer> q = em.createQuery("select minute(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(54), result);
    }

    @Test
    public void test134_FunctionSecond() {
        TypedQuery<Integer> q = em.createQuery("select second(to_date('2017-06-17 12:54:23')) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(23), result);
    }

    @Test
    public void test135_FunctionMd5() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] expected = md.digest("abcdefg".getBytes());
        TypedQuery<byte[]> q = em.createQuery("select md5('abcdefg') from dual d", byte[].class);
        byte[] result = q.getSingleResult();
        Assert.assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void test136_FunctionRand() {
        TypedQuery<Double> q = em.createQuery("select rand() from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertNotNull(result);
    }

    @Test
    public void test137_FunctionAbs() {
        TypedQuery<Double> q = em.createQuery("select abs(-3.4) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.abs(-3.4d)), result);
    }

    @Test
    public void test138_FunctionSqrt() {
        TypedQuery<Double> q = em.createQuery("select sqrt(2) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.sqrt(2)), result);
    }

    @Test
    public void test139_FunctionCbrt() {
        TypedQuery<Double> q = em.createQuery("select cbrt(2) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.cbrt(2)), result);
    }

    @Test
    public void test140_FunctionExp() {
        TypedQuery<Double> q = em.createQuery("select exp(1.1) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.exp(1.1d)), result);
    }

    @Test
    public void test141_FunctionPower() {
        TypedQuery<Double> q = em.createQuery("select power(3,2) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.pow(3, 2)), result);
    }

    @Test
    public void test142_FunctionLn() {
        TypedQuery<Double> q = em.createQuery("select ln(3) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.log(3)), result);
    }

    @Test
    public void test143_FunctionLog() {
        TypedQuery<Double> q = em.createQuery("select log(3) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.log10(3)), result);
    }

    @Test
    public void test144_FunctionRound() {
        TypedQuery<Double> q = em.createQuery("select round(3.334) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.round(3.334d)), result);
    }

    @Test
    public void test145_FunctionCeil() {
        TypedQuery<Double> q = em.createQuery("select ceil(3.334) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.ceil(3.334d)), result);
    }

    @Test
    public void test146_FunctionFloor() {
        TypedQuery<Double> q = em.createQuery("select floor(3.334) from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Math.floor(3.334d)), result);
    }

    @Test
    public void test147_FunctionToNumber() {
        TypedQuery<Double> q = em.createQuery("select to_number('3.334') from dual d", Double.class);
        Double result = q.getSingleResult();
        Assert.assertEquals(Double.valueOf(Double.parseDouble("3.334")), result);
    }

    @Test
    public void test148_FunctionSign() {
        TypedQuery<Integer> q = em.createQuery("select sign(-3.334) from dual d", Integer.class);
        Integer result = q.getSingleResult();
        Assert.assertEquals(Integer.valueOf(-1), result);
    }

    @Test
    public void test201_Insert() throws Exception {
        em.getTransaction().begin();

        Employee e = new Employee();
        e.setEmpNo(99999);
        e.setFirstName("John");
        e.setLastName("Doe");
        e.setBirthDate(DATE_FORMATTER.parse("2008-04-12"));
        e.setGender(Gender.MALE);
        e.setHireDate(DATE_FORMATTER.parse("2016-01-30"));
        e.setSalaries(null);
        em.persist(e);

        em.getTransaction().commit();

        TypedQuery<Employee> q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 99999);
        Employee inserted = q.getSingleResult();
        Assert.assertEquals(e, inserted);
    }

    @Test(expected=NoResultException.class)
    public void test301_Delete() {
        TypedQuery<Employee> q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 10003);
        Employee e = q.getSingleResult();

        em.getTransaction().begin();
        em.remove(e);
        em.getTransaction().commit();

        q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 10003);
        e = q.getSingleResult();
        Assert.fail("Employee 10003 was not deleted");
    }

    @Test
    public void test401_Update() {
        TypedQuery<Employee> q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 10001);
        Employee e = q.getSingleResult();

        em.getTransaction().begin();

        e.setFirstName("Johny");
        em.persist(e);

        em.getTransaction().commit();

        q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 10001);
        Employee updated = q.getSingleResult();
        Assert.assertEquals(e.getFirstName(), updated.getFirstName());
    }

    @Test
    public void test501_Sequence() {
        Parameter p = new Parameter();
        p.setValue("x");
        em.getTransaction().begin();
        em.persist(p);
        em.getTransaction().commit();

        p = new Parameter();
        p.setValue("y");
        em.getTransaction().begin();
        em.persist(p);
        em.getTransaction().commit();

        TypedQuery<Parameter> q = em.createQuery("select p from parameter p where p.id = :id", Parameter.class);
        q.setParameter("id", 2L);
        p = q.getSingleResult();
        Assert.assertEquals(p.getValue(), "y");
    }

    private <T extends TimeRange> T findActiveEntity(Collection<T> range) {
        if (range != null) {
            final long now = System.currentTimeMillis();
            for (T t : range) {
                if (t.getFromDate().getTime() <= now && (t.getToDate() == null || now < t.getToDate().getTime())) {
                    return t;
                }
            }
        }
        return null;
    }
}
