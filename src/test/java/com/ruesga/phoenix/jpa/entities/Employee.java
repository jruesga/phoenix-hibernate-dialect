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
package com.ruesga.phoenix.jpa.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity(name="employee")
@Table(name="E", schema="T",
        indexes= {@Index(name="E_I0", columnList="GENDER", unique=true)})
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name="EMP_NO", nullable=false)
    private int empNo;

    @Column(name="BIRTH_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date birthDate;

    @Column(name="FIRST_NAME", length=255)
    private String firstName;

    @Column(name="LAST_NAME", length=255)
    private String lastName;

    @Column(name="GENDER")
    @Enumerated(EnumType.ORDINAL)
    private Gender gender;

    @Column(name="HIRE_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date hireDate;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMP_NO", nullable=false)
    private Set<DepartmentEmployee> departments;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMP_NO", nullable=false)
    private Set<Salary> salaries;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMP_NO", nullable=false)
    private Set<Title> titles;

    public Employee() {
    }

    public int getEmpNo() {
        return empNo;
    }

    public void setEmpNo(int empNo) {
        this.empNo = empNo;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    public Set<DepartmentEmployee> getDepartments() {
        return departments;
    }

    public void setDepartments(Set<DepartmentEmployee> departments) {
        this.departments = departments;
    }

    public Set<Salary> getSalaries() {
        return salaries;
    }

    public void setSalaries(Set<Salary> salaries) {
        this.salaries = salaries;
    }

    public Set<Title> getTitles() {
        return titles;
    }

    public void setTitles(Set<Title> titles) {
        this.titles = titles;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((birthDate == null) ? 0 : birthDate.hashCode());
        result = prime * result + empNo;
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + ((gender == null) ? 0 : gender.hashCode());
        result = prime * result + ((hireDate == null) ? 0 : hireDate.hashCode());
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Employee other = (Employee) obj;
        if (birthDate == null) {
            if (other.birthDate != null)
                return false;
        } else if (!birthDate.equals(other.birthDate))
            return false;
        if (empNo != other.empNo)
            return false;
        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else if (!firstName.equals(other.firstName))
            return false;
        if (gender != other.gender)
            return false;
        if (hireDate == null) {
            if (other.hireDate != null)
                return false;
        } else if (!hireDate.equals(other.hireDate))
            return false;
        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else if (!lastName.equals(other.lastName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Employee [empNo=" + empNo + ", birthDate=" + birthDate + ", firstName=" + firstName + ", lastName="
                + lastName + ", gender=" + gender + ", hireDate=" + hireDate + "]";
    }
}
