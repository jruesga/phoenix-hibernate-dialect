package com.ruesga.phoenix.jpa.entities;

import javax.persistence.*;
import java.io.Serializable;

@Entity(name="versioned")
@Table(name="VE", schema="T")
public class VersionedEntity implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "ID", nullable = false)
  private int id;

  @Version
  @Column(name = "VERSION")
  private int version;

  @Column(name = "FLD")
  private String field;

  public VersionedEntity() {
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id;
    result = prime * result + version;
    result = prime * result + ((field == null) ? 0 : field.hashCode());
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
    VersionedEntity other = (VersionedEntity) obj;
    if (id != other.id) {
      return false;
    } else if (version != other.version) {
      return false;
    } else if (field == null && other.field != null) {
      return false;
    } else if (field != null && !field.equals(other.field)) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public String toString() {
    return "VersionedEntity [id=" + id + ", version=" + version + ", field=" + field + "]";
  }
}
