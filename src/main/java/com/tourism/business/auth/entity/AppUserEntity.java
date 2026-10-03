package com.tourism.business.auth.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;
@Entity
@Immutable
@Table(name="AppUser",schema="dbo")
public class AppUserEntity {
    @Id @Column(name="id") private Long id;
    @Nationalized @Column(name="username",length=80,nullable=false) private String username;
    @Column(name="passwordHash",length=100,nullable=false) private String passwordHash;
    @Nationalized @Column(name="name",length=120,nullable=false) private String name;
    @Column(name="role",length=5,nullable=false) private String role;
    @Column(name="active",nullable=false) private boolean active;
    public Long getId() {return id;}
    public String getUsername() {return username;}
    public String getPasswordHash() {return passwordHash;}
    public String getName() {return name;}
    public String getRole() {return role;}
    public boolean isActive() {return active;}
}
