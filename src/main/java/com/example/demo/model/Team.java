package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "matches_played")
    private int matchesPlayed = 0;

    @Column(name = "goals_for")
    private int goalsFor = 0;

    @Column(name = "goal_difference")
    private int goalDifference = 0;

    private int points = 0;

    @Column(name = "group_name", length = 20)
    private String groupName;

    public Team() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }

    public int getGoalsFor() { return goalsFor; }
    public void setGoalsFor(int goalsFor) { this.goalsFor = goalsFor; }

    public int getGoalDifference() { return goalDifference; }
    public void setGoalDifference(int goalDifference) { this.goalDifference = goalDifference; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}