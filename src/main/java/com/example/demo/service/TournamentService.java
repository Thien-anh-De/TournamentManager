package com.example.demo.service;

import com.example.demo.model.Match;
import com.example.demo.model.Team;
import com.example.demo.model.Player;
import com.example.demo.repository.MatchRepository;
import com.example.demo.repository.TeamRepository;
import com.example.demo.repository.PlayerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Random;

@Service
public class TournamentService {

    @Autowired private TeamRepository teamRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private PlayerRepository playerRepository;

    // =========================
    // 1. BẢNG XẾP HẠNG
    // =========================
    public List<Team> getRanking() {
        Sort sort = Sort.by(Sort.Direction.ASC, "groupName")
                .and(Sort.by(Sort.Direction.DESC, "points", "goalDifference", "goalsFor"));
        return teamRepository.findAll(sort);
    }

    // =========================
    // 2. UPDATE MATCH (ĐÃ THÊM PENALTY)
    // =========================
    @Transactional
    public void updateMatchResult(String matchId, int scoreA, int scoreB, int penA, int penB) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy trận!"));
        if ("FINISHED".equals(match.getStatus())) throw new RuntimeException("Trận đã có kết quả!");

        match.setScoreA(scoreA); match.setScoreB(scoreB);
        match.setPenaltyScoreA(penA); match.setPenaltyScoreB(penB); // Lưu điểm Pen
        match.setStatus("FINISHED");
        Team A = match.getTeamA(); Team B = match.getTeamB();

        // Chỉ cộng điểm bảng xếp hạng nếu là vòng bảng
        if ("GROUP".equals(match.getStage())) {
            A.setMatchesPlayed(A.getMatchesPlayed() + 1); B.setMatchesPlayed(B.getMatchesPlayed() + 1);
            A.setGoalsFor(A.getGoalsFor() + scoreA); B.setGoalsFor(B.getGoalsFor() + scoreB);
            int diff = scoreA - scoreB;
            A.setGoalDifference(A.getGoalDifference() + diff); B.setGoalDifference(B.getGoalDifference() - diff);

            if (scoreA > scoreB) A.setPoints(A.getPoints() + 3);
            else if (scoreA < scoreB) B.setPoints(B.getPoints() + 3);
            else { A.setPoints(A.getPoints() + 1); B.setPoints(B.getPoints() + 1); }
            teamRepository.save(A); teamRepository.save(B);
        }
        matchRepository.save(match);
        autoAdvanceNextRound(); // Auto check lên vòng
    }

    // --- HÀM MỚI: TÌM ĐỘI CHIẾN THẮNG ---
    private Team getWinner(Match m) {
        if (m.getScoreA() > m.getScoreB()) return m.getTeamA();
        if (m.getScoreB() > m.getScoreA()) return m.getTeamB();
        // Nếu hòa bàn thắng thì xét Penalty
        return m.getPenaltyScoreA() > m.getPenaltyScoreB() ? m.getTeamA() : m.getTeamB();
    }

    // =========================
    // 3. AUTO NEXT ROUND (ĐÃ SỬA ĐỂ DÙNG HÀM GET WINNER MỚI)
    // =========================
    @Transactional
    public void autoAdvanceNextRound() {
        List<Match> quarters = matchRepository.findByStage("QUARTER");
        if (!quarters.isEmpty() && quarters.stream().allMatch(m -> "FINISHED".equals(m.getStatus()))) {
            if (matchRepository.findByStage("SEMI").isEmpty()) {
                List<Team> w = quarters.stream().map(this::getWinner).collect(Collectors.toList());
                if (w.size() >= 4) {
                    saveKnockoutMatch("SF1", w.get(0), w.get(1), "SEMI");
                    saveKnockoutMatch("SF2", w.get(2), w.get(3), "SEMI");
                }
            }
        }
        List<Match> semis = matchRepository.findByStage("SEMI");
        if (!semis.isEmpty() && semis.stream().allMatch(m -> "FINISHED".equals(m.getStatus()))) {
            if (matchRepository.findByStage("FINAL").isEmpty()) {
                List<Team> w = semis.stream().map(this::getWinner).collect(Collectors.toList());
                if (w.size() >= 2) saveKnockoutMatch("FINAL", w.get(0), w.get(1), "FINAL");
            }
        }
    }

    private void saveKnockoutMatch(String id, Team a, Team b, String stage) {
        Match m = new Match(); m.setId(id); m.setTeamA(a); m.setTeamB(b); m.setStage(stage); matchRepository.save(m);
    }

    // =========================
    // 4. CHIA BẢNG
    // =========================
    @Transactional
    public void autoGenerateGroupsFromDB(int numGroups) {
        List<Team> teams = teamRepository.findAll();
        if (teams.isEmpty()) throw new RuntimeException("Chưa có đội!");
        matchRepository.deleteAll(); // Xóa lịch cũ
        for (Team t : teams) {
            t.setPoints(0); t.setMatchesPlayed(0); t.setGoalsFor(0); t.setGoalDifference(0);
        }
        Collections.shuffle(teams);
        for (int i = 0; i < teams.size(); i++) {
            teams.get(i).setGroupName("Bảng " + (char) ('A' + (i % numGroups)));
        }
        teamRepository.saveAll(teams);
    }

    // =========================
    // 5. SINH LỊCH (ROUND-ROBIN TỐI ƯU DYNAMIC)
    // =========================
    @Transactional
    public void generateSchedule() {
        matchRepository.deleteAll();
        List<Team> allTeams = teamRepository.findAll();
        Map<String, List<Team>> groups = allTeams.stream().filter(t -> t.getGroupName() != null).collect(Collectors.groupingBy(Team::getGroupName));
        Map<Integer, List<Match>> matchesByRound = new HashMap<>();
        int maxRounds = 0;

        for (List<Team> groupTeams : groups.values()) {
            List<Team> teams = new ArrayList<>(groupTeams);
            if (teams.size() < 2) continue;
            if (teams.size() % 2 != 0) { Team bye = new Team(); bye.setId("BYE"); teams.add(bye); } 

            int numTeams = teams.size(), numRounds = numTeams - 1;
            maxRounds = Math.max(maxRounds, numRounds);

            for (int round = 0; round < numRounds; round++) {
                for (int i = 0; i < numTeams / 2; i++) {
                    Team t1 = teams.get(i), t2 = teams.get(numTeams - 1 - i);
                    if (t1.getId() != null && !t1.getId().equals("BYE") && t2.getId() != null && !t2.getId().equals("BYE")) {
                        Match m = new Match(); m.setTeamA(t1); m.setTeamB(t2); m.setStatus("PENDING"); m.setStage("GROUP");
                        matchesByRound.computeIfAbsent(round + 1, k -> new ArrayList<>()).add(m);
                    }
                }
                teams.add(1, teams.remove(numTeams - 1)); 
            }
        }

        String[] fields = {"Sân 1", "Sân 2", "Sân 3", "Sân 4"};
        String[] timeSlots = {"8h30", "10h00", "14h00", "15h30"};
        LocalDate baseDate = LocalDate.of(2026, 3, 1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d/M/yyyy");
        int matchIdCounter = 1;

        for (int r = 1; r <= maxRounds; r++) {
            List<Match> roundMatches = matchesByRound.getOrDefault(r, new ArrayList<>());
            String dateStr = baseDate.plusWeeks(r - 1).format(fmt);
            int fIdx = 0, tIdx = 0;

            for (Match m : roundMatches) {
                m.setId("M" + (matchIdCounter++)); m.setMatchDate(dateStr);
                m.setMatchTime(timeSlots[tIdx]); m.setField(fields[fIdx]);
                matchRepository.save(m);
                
                fIdx++;
                if (fIdx >= fields.length) { fIdx = 0; tIdx = (tIdx + 1) % timeSlots.length; }
            }
        }
    }

    // =========================
    // 6. TOP 2
    // =========================
    public List<Team> getTopTeams() {
        return teamRepository.findAll().stream().collect(Collectors.groupingBy(Team::getGroupName)).values().stream()
                .flatMap(list -> list.stream().sorted((a, b) -> {
                    if (b.getPoints() != a.getPoints()) return b.getPoints() - a.getPoints();
                    if (b.getGoalDifference() != a.getGoalDifference()) return b.getGoalDifference() - a.getGoalDifference();
                    return b.getGoalsFor() - a.getGoalsFor();
                }).limit(2)).collect(Collectors.toList());
    }

    // =========================
    // 7. TỨ KẾT (Cần 4 bảng)
    // =========================
    @Transactional
    public void generateQuarterFinals() {
        Map<String, List<Team>> grouped = getTopTeams().stream().collect(Collectors.groupingBy(Team::getGroupName));
        List<Team> A = grouped.get("Bảng A"), B = grouped.get("Bảng B"), C = grouped.get("Bảng C"), D = grouped.get("Bảng D");

        if (A == null || B == null || C == null || D == null || A.size() < 2 || B.size() < 2 || C.size() < 2 || D.size() < 2) {
            throw new RuntimeException("Tính năng bốc thăm Tứ kết hiện tại yêu cầu mô hình chính xác 4 bảng (A, B, C, D)!");
        }

        saveKnockoutMatch("QF1", A.get(0), B.get(1), "QUARTER");
        saveKnockoutMatch("QF2", B.get(0), A.get(1), "QUARTER");
        saveKnockoutMatch("QF3", C.get(0), D.get(1), "QUARTER");
        saveKnockoutMatch("QF4", D.get(0), C.get(1), "QUARTER");
    }

    // =========================
    // 7.1 MỚI: TẠO BÁN KẾT (CHẠY THEO NÚT BẤM)
    // =========================
    @Transactional
    public void generateSemiFinals() {
        if (!matchRepository.findByStage("SEMI").isEmpty()) {
            throw new RuntimeException("Lịch Bán kết đã tồn tại!");
        }
        List<Match> quarters = matchRepository.findByStage("QUARTER");
        if (quarters.isEmpty() || !quarters.stream().allMatch(m -> "FINISHED".equals(m.getStatus()))) {
            throw new RuntimeException("Chưa đá xong Tứ kết! Hãy nhập đủ điểm vòng Tứ kết trước.");
        }
        
        List<Team> w = quarters.stream().map(this::getWinner).collect(Collectors.toList());
        if (w.size() >= 4) {
            saveKnockoutMatch("SF1", w.get(0), w.get(1), "SEMI");
            saveKnockoutMatch("SF2", w.get(2), w.get(3), "SEMI");
        }
    }

    // =========================
    // 7.2 MỚI: TẠO CHUNG KẾT (CHẠY THEO NÚT BẤM)
    // =========================
    @Transactional
    public void generateFinal() {
        if (!matchRepository.findByStage("FINAL").isEmpty()) {
            throw new RuntimeException("Lịch Chung kết đã tồn tại!");
        }
        List<Match> semis = matchRepository.findByStage("SEMI");
        if (semis.isEmpty() || !semis.stream().allMatch(m -> "FINISHED".equals(m.getStatus()))) {
            throw new RuntimeException("Chưa đá xong Bán kết! Hãy nhập đủ điểm vòng Bán kết trước.");
        }
        
        List<Team> w = semis.stream().map(this::getWinner).collect(Collectors.toList());
        if (w.size() >= 2) {
            saveKnockoutMatch("FINAL", w.get(0), w.get(1), "FINAL");
        }
    }

    // =========================
    // 8. CHAMPION & 9. TOP SCORER
    // =========================
    public Team getChampion() {
        List<Match> finals = matchRepository.findByStage("FINAL");
        return (!finals.isEmpty() && "FINISHED".equals(finals.get(0).getStatus())) 
               ? getWinner(finals.get(0)) : null;
    }

    public List<Player> getTopScorers() {
        return playerRepository.findAll().stream()
                .filter(p -> p.getGoals() > 0)
                .sorted((p1, p2) -> {
                    // 1. Ưu tiên tuyệt đối: Số bàn thắng cá nhân (Giảm dần)
                    if (p1.getGoals() != p2.getGoals()) {
                        return p2.getGoals() - p1.getGoals();
                    }
                    
                    Team t1 = p1.getTeam();
                    Team t2 = p2.getTeam();
                    
                    // 2. Tiêu chí phụ mức 1: Thành tích của Đội bóng (Chỉ xét nếu khác đội)
                    if (t1 != null && t2 != null && !t1.getId().equals(t2.getId())) {
                        if (t1.getPoints() != t2.getPoints()) return t2.getPoints() - t1.getPoints();
                        if (t1.getGoalDifference() != t2.getGoalDifference()) return t2.getGoalDifference() - t1.getGoalDifference();
                        if (t1.getGoalsFor() != t2.getGoalsFor()) return t2.getGoalsFor() - t1.getGoalsFor();
                    }
                    
                    // 3. Tiêu chí phụ mức 2: Chỉ số Fair-play cá nhân
                    if (p1.getRedCards() != p2.getRedCards()) return p1.getRedCards() - p2.getRedCards();
                    if (p1.getYellowCards() != p2.getYellowCards()) return p1.getYellowCards() - p2.getYellowCards();
                    
                    // 4. Alphabet
                    return p1.getName().compareToIgnoreCase(p2.getName());
                })
                .collect(Collectors.toList());
    }

    // =========================
    // 10. GIẢ LẬP KẾT QUẢ VÒNG BẢNG (AUTO DEMO)
    // =========================
    @Transactional
    public void autoMockGroupStage() {
        List<Match> groupMatches = matchRepository.findByStage("GROUP");
        if (groupMatches.isEmpty()) throw new RuntimeException("Chưa có lịch thi đấu để giả lập!");

        Random rand = new Random();
        for (Match m : groupMatches) {
            if ("FINISHED".equals(m.getStatus())) continue;

            int scoreA = rand.nextInt(4); // 0-3 bàn
            int scoreB = rand.nextInt(4);

            // Mặc định vòng bảng không có penalty, truyền 0, 0
            updateMatchResult(m.getId(), scoreA, scoreB, 0, 0);

            // Random người ghi bàn
            assignRandomGoals(m.getTeamA().getId(), scoreA, rand);
            assignRandomGoals(m.getTeamB().getId(), scoreB, rand);

            // Random thẻ phạt (Vàng & Đỏ)
            assignRandomCards(m.getTeamA().getId(), m.getTeamB().getId(), rand);
        }
    }

    private void assignRandomGoals(String teamId, int goals, Random rand) {
        if (goals <= 0) return;
        List<Player> players = playerRepository.findByTeam_Id(teamId);
        if (players.isEmpty()) return;
        for (int i = 0; i < goals; i++) {
            Player p = players.get(rand.nextInt(players.size()));
            p.setGoals(p.getGoals() + 1);
            playerRepository.save(p);
        }
    }

    private void assignRandomCards(String teamAId, String teamBId, Random rand) {
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(playerRepository.findByTeam_Id(teamAId));
        allPlayers.addAll(playerRepository.findByTeam_Id(teamBId));
        if (allPlayers.isEmpty()) return;

        // Random thẻ Vàng (0-3 thẻ mỗi trận)
        int numYellow = rand.nextInt(4);
        for (int i = 0; i < numYellow; i++) {
            Player p = allPlayers.get(rand.nextInt(allPlayers.size()));
            p.setYellowCards(p.getYellowCards() + 1);
            playerRepository.save(p);
        }

        // Random thẻ Đỏ (15% cơ hội có 1 thẻ đỏ)
        if (rand.nextInt(100) < 15) { 
            Player p = allPlayers.get(rand.nextInt(allPlayers.size()));
            p.setRedCards(p.getRedCards() + 1);
            playerRepository.save(p);
        }
    }
}