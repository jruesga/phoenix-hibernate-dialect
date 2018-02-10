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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity(name="department")
@Table(name="D", schema="T",
    indexes= {@Index(name="D_I0", columnList="DEPT_NAME", unique=true)})
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name="DEPT_NO", nullable=false)
    private int deptNo;

    @Column(name="DEPT_NAME", length=40)
    private String deptName;

    public Department() {
    }

    public int getDeptNo() {
        return deptNo;
    }

    public void setDeptNo(int deptNo) {
        this.deptNo = deptNo;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deptName == null) ? 0 : deptName.hashCode());
        result = prime * result + deptNo;
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
        Department other = (Department) obj;
        if (deptName == null) {
            if (other.deptName != null)
                return false;
        } else if (!deptName.equals(other.deptName))
            return false;
        if (deptNo != other.deptNo)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Department [deptNo=" + deptNo + ", deptName=" + deptName + "]";
    }

}
