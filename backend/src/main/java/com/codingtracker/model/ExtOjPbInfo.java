package com.codingtracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "extoj_pb_info",
    uniqueConstraints = @UniqueConstraint(columnNames = {"pid", "ojName"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtOjPbInfo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OJPlatform ojName;

    @Column(length = 200, nullable = false)
    private String pid;

    @Column(length = 1024)
    private String name;

    @Column(length = 64)
    private String type;

    private Double points;

    @Column(length = 256)
    private String url;

    @ManyToMany(fetch = FetchType.LAZY,
        cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "problem_tags",
        joinColumns = @JoinColumn(name = "problem_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}
