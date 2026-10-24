// package com.garmentqc.system.model;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.Table;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// @Entity
// @Table(name = "attendance_sites")
// @Getter
// @Setter
// @NoArgsConstructor
// public class AttendanceSite extends BaseEntity {

//     @Column(nullable = false, length = 100)
//     private String name;

//     @Column(nullable = false, precision = 10, scale = 7)
//     private Double latitude;

//     @Column(nullable = false, precision = 10, scale = 7)
//     private Double longtitude;

//     @Column(name = "radius_meters", nullable = false)
//     private Integer radiusMeters = 100;

//     @Column(nullable = false)
//     private boolean active = true;

// }
