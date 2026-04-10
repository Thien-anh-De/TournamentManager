package com.example.demo.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty; // Thêm thư viện này

@Entity
@Table(name = "players")
public class Player {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false)
    private String name;

    private int number;

    private int goals = 0;

    @Column(name = "yellow_cards")
    private int yellowCards = 0;

    @Column(name = "red_cards")
    private int redCards = 0;

    // THÊM: Phản lưới nhà
    @Column(name = "own_goals")
    private int ownGoals = 0;

    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnore // THÊM CÁI NÀY ĐỂ WEB KHÔNG BỊ TREO KHI LOAD CẦU THỦ
    private Team team;

    // ===== GETTER SETTER =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public int getGoals() { return goals; }
    public void setGoals(int goals) { this.goals = goals; }

    public int getYellowCards() { return yellowCards; }
    public void setYellowCards(int yellowCards) { this.yellowCards = yellowCards; }

    public int getRedCards() { return redCards; }
    public void setRedCards(int redCards) { this.redCards = redCards; }

    public int getOwnGoals() { return ownGoals; }
    public void setOwnGoals(int ownGoals) { this.ownGoals = ownGoals; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    // ==========================================
    // MỚI: HÀM TRẢ VỀ TÊN ĐỘI BÓNG CHO FRONTEND
    // ==========================================
    @JsonProperty("teamName")
    public String getTeamName() {
        return this.team != null ? this.team.getName() : "Cầu thủ tự do";
    }
}