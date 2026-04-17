package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    @ManyToOne
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @Column(name = "score_a")
    private int scoreA = 0;

    @Column(name = "score_b")
    private int scoreB = 0;
    @Column(name = "penalty_score_a")
    private int penaltyScoreA = 0;

    @Column(name = "penalty_score_b")
    private int penaltyScoreB = 0;

    @Column(length = 20)
    private String status = "PENDING"; 

    @Column(length = 20)
    private String stage = "GROUP"; 

    // 3 trường lưu lịch thi đấu Excel
    @Column(name = "match_date", length = 20)
    private String matchDate;

    @Column(name = "match_time", length = 20)
    private String matchTime;

    @Column(length = 50)
    private String field;

    public Match() {}

    // --- GETTER & SETTER ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Team getTeamA() { return teamA; }
    public void setTeamA(Team teamA) { this.teamA = teamA; }

    public Team getTeamB() { return teamB; }
    public void setTeamB(Team teamB) { this.teamB = teamB; }

    public int getScoreA() { return scoreA; }
    public void setScoreA(int scoreA) { this.scoreA = scoreA; }

    public int getScoreB() { return scoreB; }
    public void setScoreB(int scoreB) { this.scoreB = scoreB; }

    // --- GETTER & SETTER CHO PENALTY ---
    public int getPenaltyScoreA() { return penaltyScoreA; }
    public void setPenaltyScoreA(int penaltyScoreA) { this.penaltyScoreA = penaltyScoreA; }

    public int getPenaltyScoreB() { return penaltyScoreB; }
    public void setPenaltyScoreB(int penaltyScoreB) { this.penaltyScoreB = penaltyScoreB; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getMatchDate() { return matchDate; }
    public void setMatchDate(String matchDate) { this.matchDate = matchDate; }

    public String getMatchTime() { return matchTime; }
    public void setMatchTime(String matchTime) { this.matchTime = matchTime; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
}