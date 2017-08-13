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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.commons.math3.util.Pair;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ruesga.phoenix.jpa.JpaEntityManager;
import com.ruesga.phoenix.jpa.entities.Department;
import com.ruesga.phoenix.jpa.entities.DepartmentEmployee;
import com.ruesga.phoenix.jpa.entities.Employee;
import com.ruesga.phoenix.jpa.entities.Gender;
import com.ruesga.phoenix.jpa.entities.Salary;
import com.ruesga.phoenix.jpa.entities.TimeRange;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PhoenixDialectTest {

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

        // create the database
        em.getTransaction().begin();
        BufferedReader is = new BufferedReader(new InputStreamReader(
                PhoenixDialectTest.class.getResourceAsStream("/create_database.sql")));
        String line = null;
        while((line = is.readLine()) != null) {
            if (line.trim().isEmpty() || line.startsWith("--")) {
                continue;
            }
            em.createNativeQuery(line).executeUpdate();
        }
        em.getTransaction().commit();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (em != null) {
            em.close();
        }
        JpaEntityManager.getInstance().close();
    }

    @Test
    public void test001_SelectAll() throws Exception {
        TypedQuery<Employee> q = em.createQuery("select e from employee e", Employee.class);
        List<Employee> e = q.getResultList();
        Assert.assertEquals(e.size(), 31);
    }

    @Test
    public void test002_SelectRowKey() throws Exception {
        TypedQuery<Employee> q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 10002);
        Employee e = q.getSingleResult();
        Assert.assertNotNull(e);
        Assert.assertEquals(e.getFirstName(), "Bezalel");
    }

    @Test
    public void test003_SelectLazyLoad() throws Exception {
        TypedQuery<Employee> q = em.createQuery("select e from employee e where e.empNo = :empNo", Employee.class);
        q.setParameter("empNo", 10010);
        Employee e = q.getSingleResult();

        Set<DepartmentEmployee> departments = e.getDepartments();
        Assert.assertNotNull(departments);
        Assert.assertEquals(departments.size(), 2);
        Department department = findActiveEntity(departments).getDepartment();
        Assert.assertNotNull(department);
        Assert.assertEquals(department.getDeptName(), "Finance");
    }

    @Test
    public void test004_SelectJoin() throws Exception {
        TypedQuery<Salary> q = em.createQuery("select s from salary s inner join s.employee as e " +
                "where e.empNo = :empNo", Salary.class);
        q.setParameter("empNo", 10001);
        List<Salary> salaries = q.getResultList();
        Assert.assertEquals(salaries.size(), 17);
        Salary salary = findActiveEntity(salaries);
        Assert.assertNotNull(salary);
        Assert.assertEquals(salary.getSalary(), Double.valueOf(88958d));
    }

    @Test
    public void test005_SelectCount() throws Exception {
        TypedQuery<Long> q = em.createQuery("select count(d.deptNo) from department d", Long.class);
        long count = q.getSingleResult();
        Assert.assertEquals(count, 3L);
    }

    @Test
    public void test006_SelectOrderBy() throws Exception {
        TypedQuery<Department> q = em.createQuery(
                "select d from department d order by d.deptName desc", Department.class);
        List<Department> departments = q.getResultList();
        Assert.assertEquals(departments.size(), 3L);
        Assert.assertEquals(departments.get(2).getDeptName(), "Finance");
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void test007_SelectGroupBy() throws Exception {
        TypedQuery<Pair> q = em.createQuery(
                "select new org.apache.commons.math3.util.Pair(s.employee.empNo, sum(s.salary)) from salary s " +
                "group by s.employee.empNo order by s.employee.empNo asc", Pair.class);
        List<Pair> salaries = q.getResultList();
        Assert.assertEquals(salaries.size(), 31L);
        Assert.assertEquals(salaries.get(0).getSecond(), Double.valueOf(1281612d));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void test008_SelectHaving() throws Exception {
        TypedQuery<Pair> q = em.createQuery(
                "select new org.apache.commons.math3.util.Pair(s.employee.empNo, sum(s.salary) as sal) from salary s " +
                "group by s.employee.empNo having sum(s.salary) > 1281612 order by sal desc", Pair.class);
        List<Pair> salaries = q.getResultList();
        Assert.assertEquals(salaries.size(), 4L);
        Assert.assertEquals(salaries.get(0).getSecond(), Double.valueOf(1604309d));
    }

    @Test
    public void test009_SelectHint() throws Exception {
        TypedQuery<Department> q = em.createQuery(" SELECT d from department d " +
                    "where d.deptName = :deptName", Department.class);
        ((org.hibernate.query.Query<Department>)q).addQueryHint(
                String.valueOf(new PhoenixDialect.SecondaryIndexHint(Department.class, "D_I0").build()));
        q.setParameter("deptName", "Finance");
        Department department = q.getSingleResult();
        Assert.assertEquals(department.getDeptName(), "Finance");
    }

    @Test
    public void test010_SelectLimit() throws Exception {
        TypedQuery<Department> q = em.createQuery("select d from department d order by d.deptNo asc", Department.class);
        List<Department> departments = q.setMaxResults(2).getResultList();
        Assert.assertEquals(departments.size(), 2L);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(departments.get(i).getDeptNo(), i + 1);
        }
    }

    @Test
    public void test011_SelectFirstResultQuery() throws Exception {
        TypedQuery<Employee> q = em.createQuery("select e from employee e order by e.empNo asc", Employee.class);
        List<Employee> employees = q.setFirstResult(5).getResultList();
        Assert.assertEquals(employees.get(0).getEmpNo(), 10006);
    }

    @Test
    public void test012_SelectPagedQuery() throws Exception {
        TypedQuery<Employee> q = em.createQuery("select e from employee e order by e.empNo asc", Employee.class);
        List<Employee> employees = q.setMaxResults(5).setFirstResult(5).getResultList();
        Assert.assertEquals(employees.size(), 5L);
        Assert.assertEquals(employees.get(0).getEmpNo(), 10006);
    }

    @Test
    public void test101_Insert() throws Exception {
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
    public void test102_Delete() {
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
    public void test103_Update() {
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
