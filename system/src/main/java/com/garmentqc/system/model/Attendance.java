// package com.garmentqc.system.model;

// import java.time.Instant;
// import java.time.LocalDate;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.FetchType;
// import jakarta.persistence.JoinColumn;
// import jakarta.persistence.ManyToOne;
// import jakarta.persistence.Table;
// import jakarta.persistence.UniqueConstraint;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// @Entity
// @Table(name = "attendances", uniqueConstraints = {
//         @UniqueConstraint(name = "uk_attendance_user_work_date", columnNames = { "user_id", "work_date" })
// })
// @Getter
// @Setter
// @NoArgsConstructor
// public class Attendance extends BaseEntity {

//     @ManyToOne(fetch = FetchType.LAZY, optional = false)
//     @JoinColumn(name = "user_id", nullable = false)
//     private User user;

//     @Column(name = "work_date", nullable = false)
//     private LocalDate workDate;

//     @Column(name = "clock_in_at")
//     private Instant clockInAt;

//     @Column(name = "clock_out_at")
//     private Instant clockOutAt;

//     @Column(name = "clock_in_latitude", precision = 10, scale = 7)
//     private Double clockInLatitude;

//     @Column(name = "clock_in_longitude", precision = 10, scale = 7)
//     private Double clockInLongitude;

//     @Column(name = "clock_in_accuracy_meters")
//     private Double clockInAccuracyMeters;

//     @Column(name = "clock_out_latitude", precision = 10, scale = 7)
//     private Double clockOutLatitude;

//     @Column(name = "clock_out_longitude", precision = 10, scale = 7)
//     private Double clockOutLongitude;

//     @Column(name = "clock_out_accuracy_meters")
//     private Double clockOutAccuracyMeters;

//     @Column(name = "break_minutes", nullable = false)
//     private Integer breakMinutes = 0;

//     @Column(name = "target_pcs_per_hour", nullable = false)
//     private Integer targetPcsPerHour = 40;
// }
