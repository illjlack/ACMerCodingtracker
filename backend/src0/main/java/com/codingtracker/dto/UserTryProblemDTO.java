package com.codingtracker.dto;

import com.codingtracker.model.ExtOjPbInfo;
import com.codingtracker.model.UserTryProblem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class UserTryProblemDTO {
    private String username;
    private Long problemId;
    private String ojName;
    private String pid;
    private String name;
    private String type;
    private Double points;
    private String url;
    private Set<String> tags;
    private String result;
    private LocalDateTime attemptTime;

    public UserTryProblemDTO(UserTryProblem userTryProblem, String username) {
        this.username = username;
        this.result = userTryProblem.getResult().name();
        this.attemptTime = userTryProblem.getAttemptTime();

        ExtOjPbInfo problem = userTryProblem.getExtOjPbInfo();
        this.problemId = problem.getId();
        this.ojName = problem.getOjName().name();
        this.pid = problem.getPid();
        this.name = problem.getName();
        this.type = problem.getType();
        this.points = problem.getPoints();
        this.url = problem.getUrl();
        this.tags = problem.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toSet());
    }
}

