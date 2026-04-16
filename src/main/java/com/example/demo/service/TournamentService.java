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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TournamentService {

    @Autowired private TeamRepository teamRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private PlayerRepository playerRepository;

    // ==========================================
    // HÀM MỚI: TRỌNG TÀI SO SÁNH CÔNG BẰNG (PPG)
    // Dùng để so sánh các đội KHÁC BẢNG có số trận thi đấu không bằng nhau
    // ==========================================
    private int compareTeamsFairly(Team a, Team b) {
        // 1. So sánh Điểm trung bình mỗi trận
        double ppgA = a.getMatchesPlayed() > 0 ? (double) a.getPoints() / a.getMatchesPlayed() : 0;
        double ppgB = b.getMatchesPlayed() > 0 ? (double) b.getPoints() / b.getMatchesPlayed() : 0;
        if (ppgA != ppgB) return Double.compare(ppgB, ppgA); // Xếp giảm dần

        // 2. So sánh Hiệu số trung bình mỗi trận
        double gdpgA = a.getMatchesPlayed() > 0 ? (double) a.getGoalDifference() / a.getMatchesPlayed() : 0;
        double gdpgB = b.getMatchesPlayed() > 0 ? (double) b.getGoalDifference() / b.getMatchesPlayed() : 0;
        if (gdpgA != gdpgB) return Double.compare(gdpgB, gdpgA);

        // 3. So sánh Số bàn thắng trung bình mỗi trận
        double gfpgA = a.getMatchesPlayed() > 0 ? (double) a.getGoalsFor() / a.getMatchesPlayed() : 0;
        double gfpgB = b.getMatchesPlayed() > 0 ? (double) b.getGoalsFor() / b.getMatchesPlayed() : 0;
        return Double.compare(gfpgB, gfpgA);
    }

    // =========================
    // 1. BẢNG XẾP HẠNG
    // =========================
    public List<Team> getRanking() {
        Sort sort = Sort.by(Sort.Direction.ASC, "groupName")
                .and(Sort.by(Sort.Direction.DESC, "points", "goalDifference", "goalsFor"));
        return teamRepository.findAll(sort);
    }

    // =========================
    // 2. UPDATE MATCH
    // =========================
    @Transactional
    public void updateMatchResult(String matchId, int scoreA, int scoreB, int penA, int penB) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy trận!"));
        if ("FINISHED".equals(match.getStatus())) {
            throw new RuntimeException("Trận đã có kết quả!");
        }

        match.setScoreA(scoreA);
        match.setScoreB(scoreB);
        match.setPenaltyScoreA(penA);
        match.setPenaltyScoreB(penB); 
        match.setStatus("FINISHED");
        
        Team A = match.getTeamA(); 
        Team B = match.getTeamB();

        // Chỉ cộng điểm bảng xếp hạng nếu là vòng bảng
        if ("GROUP".equals(match.getStage())) {
            A.setMatchesPlayed(A.getMatchesPlayed() + 1);
            B.setMatchesPlayed(B.getMatchesPlayed() + 1);
            A.setGoalsFor(A.getGoalsFor() + scoreA);
            B.setGoalsFor(B.getGoalsFor() + scoreB);
            
            int diff = scoreA - scoreB;
            A.setGoalDifference(A.getGoalDifference() + diff);
            B.setGoalDifference(B.getGoalDifference() - diff);

            if (scoreA > scoreB) {
                A.setPoints(A.getPoints() + 3);
            } else if (scoreA < scoreB) {
                B.setPoints(B.getPoints() + 3);
            } else {
                A.setPoints(A.getPoints() + 1);
                B.setPoints(B.getPoints() + 1);
            }
            teamRepository.save(A);
            teamRepository.save(B);
        }
        matchRepository.save(match);
        autoAdvanceNextRound(); 
    }

    private Team getWinner(Match m) {
        if (m.getScoreA() > m.getScoreB()) return m.getTeamA();
        if (m.getScoreB() > m.getScoreA()) return m.getTeamB();
        return m.getPenaltyScoreA() > m.getPenaltyScoreB() ? m.getTeamA() : m.getTeamB();
    }

    // =========================
    // 3. AUTO NEXT ROUND
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
                if (w.size() >= 2) {
                    saveKnockoutMatch("FINAL", w.get(0), w.get(1), "FINAL");
                }
            }
        }
    }

    private void saveKnockoutMatch(String id, Team a, Team b, String stage) {
        Match m = new Match();
        m.setId(id);
        m.setTeamA(a);
        m.setTeamB(b);
        m.setStage(stage);
        matchRepository.save(m);
    }

    // =========================
    // 4. CHIA BẢNG
    // =========================
    @Transactional
    public void autoGenerateGroupsFromDB(int numGroups) {
        List<Team> teams = teamRepository.findAll();
        if (teams.isEmpty()) throw new RuntimeException("Chưa có đội!");
        
        matchRepository.deleteAll(); 
        for (Team t : teams) {
            t.setPoints(0); t.setMatchesPlayed(0); t.setGoalsFor(0); t.setGoalDifference(0);
        }
        
        Collections.shuffle(teams);
        for (int i = 0; i < teams.size(); i++) {
            // Đảm bảo chia nhóm chính xác, nếu numGroups = 1 thì tất cả vào Bảng A
            teams.get(i).setGroupName("Bảng " + (char) ('A' + (i % numGroups)));
        }
        teamRepository.saveAll(teams);
    }

    // =========================
    // 5. THUẬT TOÁN SINH LỊCH (QUEUE X THỜI GIAN THỰC TẾ)
    // =========================
    @Transactional
    public void generateSchedule() {
        matchRepository.deleteAll();
        List<Team> allTeams = teamRepository.findAll();
        Map<String, List<Team>> groups = allTeams.stream()
                .filter(t -> t.getGroupName() != null)
                .collect(Collectors.groupingBy(Team::getGroupName));
        
        Map<Integer, List<Match>> matchesByRound = new HashMap<>();

        // Bước 1: Sinh ra toàn bộ số trận cần thiết (Vòng lặp Round-Robin)
        for (List<Team> groupTeams : groups.values()) {
            List<Team> teams = new ArrayList<>(groupTeams);
            if (teams.size() < 2) continue;
            
            if (teams.size() % 2 != 0) { 
                Team bye = new Team(); bye.setId("BYE"); teams.add(bye); 
            } 

            int numTeams = teams.size();
            int numRounds = numTeams - 1;

            for (int round = 0; round < numRounds; round++) {
                for (int i = 0; i < numTeams / 2; i++) {
                    Team t1 = teams.get(i);
                    Team t2 = teams.get(numTeams - 1 - i);
                    
                    if (t1.getId() != null && !t1.getId().equals("BYE") && 
                        t2.getId() != null && !t2.getId().equals("BYE")) {
                        
                        Match m = new Match(); 
                        m.setTeamA(t1); m.setTeamB(t2); m.setStatus("PENDING"); m.setStage("GROUP");
                        
                        matchesByRound.computeIfAbsent(round + 1, k -> new ArrayList<>()).add(m);
                    }
                }
                teams.add(1, teams.remove(numTeams - 1)); 
            }
        }

        // Bước 2: Nạp toàn bộ vào Hàng đợi (Queue)
        List<Match> pendingQueue = new ArrayList<>();
        int absoluteMaxRounds = matchesByRound.keySet().stream().max(Integer::compareTo).orElse(0);
        for (int r = 1; r <= absoluteMaxRounds; r++) {
            if (matchesByRound.containsKey(r)) {
                pendingQueue.addAll(matchesByRound.get(r));
            }
        }

        // Bước 3: Rải lịch theo Ràng buộc (Chỉ đá T7/CN, Mỗi sân có 4 slot/ngày, Không đá 2 trận/ngày)
        String[] fields = {"Sân 1", "Sân 2", "Sân 3", "Sân 4"};
        String[] timeSlots = {"8h30", "10h00", "14h00", "15h30"};
        
        LocalDate currentDate = LocalDate.of(2026, 3, 7); // Mặc định Khai mạc Thứ 7
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d/M/yyyy");
        int matchIdCounter = 1;

        while (!pendingQueue.isEmpty()) {
            Set<String> teamsPlayedToday = new HashSet<>(); 
            int fIdx = 0;
            int tIdx = 0;
            
            Iterator<Match> iterator = pendingQueue.iterator();
            while (iterator.hasNext()) {
                Match m = iterator.next();
                
                if (!teamsPlayedToday.contains(m.getTeamA().getId()) && 
                    !teamsPlayedToday.contains(m.getTeamB().getId())) {
                    
                    m.setId("M" + (matchIdCounter++)); 
                    m.setMatchDate(currentDate.format(fmt));
                    m.setMatchTime(timeSlots[tIdx]); 
                    m.setField(fields[fIdx]);
                    matchRepository.save(m);
                    
                    teamsPlayedToday.add(m.getTeamA().getId());
                    teamsPlayedToday.add(m.getTeamB().getId());
                    
                    iterator.remove();
                    
                    fIdx++;
                    if (fIdx >= fields.length) { 
                        fIdx = 0; 
                        tIdx++; 
                    }
                    if (tIdx >= timeSlots.length) {
                        break;
                    }
                }
            }
            
            // Bước 4: Chuyển sang Ngày cuối tuần tiếp theo
            if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                currentDate = currentDate.plusDays(1); // T7 nhảy sang CN
            } else {
                currentDate = currentDate.plusDays(6); // CN nhảy sang T7 tuần sau
            }
        }
    }

    // =========================
    // 6. LỌC 8 ĐỘI VÀO KNOCKOUT (HỖ TRỢ 1, 2, 3, 4, 5 BẢNG)
    // =========================
    public List<Team> getTop8ForQuarter() {
        Map<String, List<Team>> groups = teamRepository.findAll().stream()
                .filter(t -> t.getGroupName() != null)
                .collect(Collectors.groupingBy(Team::getGroupName));

        int numGroups = groups.size();
        List<Team> qualifiedTeams = new ArrayList<>();

        // Bước 1: Sắp xếp nội bộ từng Bảng
        for (List<Team> groupTeams : groups.values()) {
            groupTeams.sort((a, b) -> {
                if (b.getPoints() != a.getPoints()) return b.getPoints() - a.getPoints();
                if (b.getGoalDifference() != a.getGoalDifference()) return b.getGoalDifference() - a.getGoalDifference();
                return b.getGoalsFor() - a.getGoalsFor();
            });
        }

        // Bước 2: Bốc đủ 8 đội theo Thể thức
        if (numGroups == 1) {
            // Thể thức 1 Bảng (League)
            for (List<Team> groupTeams : groups.values()) {
                qualifiedTeams.addAll(groupTeams.stream().limit(8).collect(Collectors.toList()));
            }

        } else if (numGroups == 4) {
            for (List<Team> groupTeams : groups.values()) {
                qualifiedTeams.addAll(groupTeams.stream().limit(2).collect(Collectors.toList()));
            }
            
        } else if (numGroups == 2) {
            for (List<Team> groupTeams : groups.values()) {
                qualifiedTeams.addAll(groupTeams.stream().limit(4).collect(Collectors.toList()));
            }
            
        } else if (numGroups == 3) {
            // Lấy 3 Nhất + 3 Nhì + 2 Ba xuất sắc nhất (So sánh chéo bằng Hệ số)
            List<Team> thirdPlaceTeams = new ArrayList<>();
            for (List<Team> groupTeams : groups.values()) {
                if (groupTeams.size() >= 1) qualifiedTeams.add(groupTeams.get(0)); 
                if (groupTeams.size() >= 2) qualifiedTeams.add(groupTeams.get(1)); 
                if (groupTeams.size() >= 3) thirdPlaceTeams.add(groupTeams.get(2)); 
            }
            thirdPlaceTeams.sort(this::compareTeamsFairly);
            qualifiedTeams.addAll(thirdPlaceTeams.stream().limit(2).collect(Collectors.toList()));

        } else if (numGroups == 5) {
            // Lấy 5 Nhất + 3 Nhì xuất sắc nhất (So sánh chéo bằng Hệ số)
            List<Team> secondPlaceTeams = new ArrayList<>();
            for (List<Team> groupTeams : groups.values()) {
                if (groupTeams.size() >= 1) qualifiedTeams.add(groupTeams.get(0)); 
                if (groupTeams.size() >= 2) secondPlaceTeams.add(groupTeams.get(1)); 
            }
            secondPlaceTeams.sort(this::compareTeamsFairly);
            qualifiedTeams.addAll(secondPlaceTeams.stream().limit(3).collect(Collectors.toList()));
            
        } else {
            throw new RuntimeException("Lỗi: Số bảng hiện tại là " + numGroups + ". Hệ thống hỗ trợ 1, 2, 3, 4 hoặc 5 bảng để bốc ra 8 đội Tứ kết!");
        }

        return qualifiedTeams;
    }

    // =========================
    // 7. KNOCKOUT GENERATORS (HẠT GIỐNG TOÀN GIẢI)
    // =========================
    @Transactional
    public void generateQuarterFinals() {
        if (!matchRepository.findByStage("QUARTER").isEmpty()) {
            throw new RuntimeException("Lịch Tứ kết đã tồn tại!");
        }

        List<Team> top8 = getTop8ForQuarter();
        if (top8.size() < 8) {
            throw new RuntimeException("Lỗi: Không đủ 8 đội để xếp Tứ kết! Vui lòng đảm bảo hệ thống có đủ ít nhất 8 đội.");
        }

        // BƯỚC ĐỘT PHÁ: Xếp hạng hạt giống toàn giải dựa trên HỆ SỐ TRUNG BÌNH (PPG)
        top8.sort(this::compareTeamsFairly);

        // Bắt cặp tự động theo Hạt giống (Seed 1 vs Seed 8, Seed 2 vs Seed 7...)
        saveKnockoutMatch("QF1", top8.get(0), top8.get(7), "QUARTER");
        saveKnockoutMatch("QF2", top8.get(1), top8.get(6), "QUARTER");
        saveKnockoutMatch("QF3", top8.get(2), top8.get(5), "QUARTER");
        saveKnockoutMatch("QF4", top8.get(3), top8.get(4), "QUARTER");
    }

    @Transactional
    public void generateSemiFinals() {
        if (!matchRepository.findByStage("SEMI").isEmpty()) throw new RuntimeException("Lịch Bán kết đã tồn tại!");
        
        List<Match> quarters = matchRepository.findByStage("QUARTER");
        if (quarters.isEmpty() || !quarters.stream().allMatch(m -> "FINISHED".equals(m.getStatus()))) {
            throw new RuntimeException("Chưa đá xong Tứ kết!");
        }
        
        List<Team> w = quarters.stream().map(this::getWinner).collect(Collectors.toList());
        if (w.size() >= 4) {
            saveKnockoutMatch("SF1", w.get(0), w.get(1), "SEMI");
            saveKnockoutMatch("SF2", w.get(2), w.get(3), "SEMI");
        }
    }

    @Transactional
    public void generateFinal() {
        if (!matchRepository.findByStage("FINAL").isEmpty()) throw new RuntimeException("Lịch Chung kết đã tồn tại!");
        
        List<Match> semis = matchRepository.findByStage("SEMI");
        if (semis.isEmpty() || !semis.stream().allMatch(m -> "FINISHED".equals(m.getStatus()))) {
            throw new RuntimeException("Chưa đá xong Bán kết!");
        }
        
        List<Team> w = semis.stream().map(this::getWinner).collect(Collectors.toList());
        if (w.size() >= 2) saveKnockoutMatch("FINAL", w.get(0), w.get(1), "FINAL");
    }

    // =========================
    // 8. CHAMPION
    // =========================
    public Team getChampion() {
        List<Match> finals = matchRepository.findByStage("FINAL");
        if (!finals.isEmpty() && "FINISHED".equals(finals.get(0).getStatus())) return getWinner(finals.get(0));
        return null;
    }

    // =========================
    // 9. TOP SCORER
    // =========================
    public List<Player> getTopScorers() {
        return playerRepository.findAll().stream()
                .filter(p -> p.getGoals() > 0)
                .sorted((p1, p2) -> {
                    if (p1.getGoals() != p2.getGoals()) return p2.getGoals() - p1.getGoals();
                    Team t1 = p1.getTeam(), t2 = p2.getTeam();
                    if (t1 != null && t2 != null && !t1.getId().equals(t2.getId())) {
                        // So sánh thành tích đội (cũng áp dụng hệ số trung bình cho công bằng)
                        return compareTeamsFairly(t1, t2);
                    }
                    if (p1.getRedCards() != p2.getRedCards()) return p1.getRedCards() - p2.getRedCards();
                    if (p1.getYellowCards() != p2.getYellowCards()) return p1.getYellowCards() - p2.getYellowCards();
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

            int scoreA = rand.nextInt(4);
            int scoreB = rand.nextInt(4);
            updateMatchResult(m.getId(), scoreA, scoreB, 0, 0);

            assignRandomGoals(m.getTeamA().getId(), scoreA, rand);
            assignRandomGoals(m.getTeamB().getId(), scoreB, rand);
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

        int numYellow = rand.nextInt(4);
        for (int i = 0; i < numYellow; i++) {
            Player p = allPlayers.get(rand.nextInt(allPlayers.size()));
            p.setYellowCards(p.getYellowCards() + 1);
            playerRepository.save(p);
        }

        if (rand.nextInt(100) < 15) { 
            Player p = allPlayers.get(rand.nextInt(allPlayers.size()));
            p.setRedCards(p.getRedCards() + 1);
            playerRepository.save(p);
        }
    }
}