package com.example.demo.controller;

import com.example.demo.model.Match;
import com.example.demo.model.Team;
import com.example.demo.model.Player;
import com.example.demo.repository.MatchRepository;
import com.example.demo.repository.PlayerRepository;
import com.example.demo.repository.TeamRepository;
import com.example.demo.service.TournamentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournament")
@CrossOrigin(origins = "*")
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    // 1. BẢNG XẾP HẠNG
    @GetMapping("/ranking")
    public List<Team> getRanking() {
        return tournamentService.getRanking();
    }

    // 2. UPDATE MATCH
    @PostMapping("/match/{matchId}/update")
    public ResponseEntity<String> updateScore(
            @PathVariable String matchId,
            @RequestParam int scoreA,
            @RequestParam int scoreB,
            @RequestParam(defaultValue = "0") int penA,
            @RequestParam(defaultValue = "0") int penB) {
        try {
            tournamentService.updateMatchResult(matchId, scoreA, scoreB, penA, penB);
            return ResponseEntity.ok("Cập nhật kết quả thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 3. CHIA BẢNG
    @PostMapping("/auto-group-db")
    public ResponseEntity<String> autoGroupDB(@RequestParam int numGroups) {
        try {
            tournamentService.autoGenerateGroupsFromDB(numGroups);
            return ResponseEntity.ok("Chia bảng thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 4. SINH LỊCH (ROUND-ROBIN)
    @PostMapping("/generate-schedule")
    public ResponseEntity<String> generateSchedule() {
        try {
            tournamentService.generateSchedule();
            return ResponseEntity.ok("Đã tạo lịch thi đấu!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 5. VÒNG LOẠI TRỰC TIẾP 
    @PostMapping("/generate-quarterfinals")
    public ResponseEntity<String> generateQuarter() {
        try {
            tournamentService.generateQuarterFinals();
            return ResponseEntity.ok("Đã tạo Tứ kết thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/generate-semifinals")
    public ResponseEntity<String> generateSemi() {
        try {
            tournamentService.generateSemiFinals(); 
            return ResponseEntity.ok("Đã tạo Bán kết thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/generate-final")
    public ResponseEntity<String> generateFinal() {
        try {
            tournamentService.generateFinal();
            return ResponseEntity.ok("Đã tạo Chung kết thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 6. LẤY MATCH THEO STAGE
    @GetMapping("/matches")
    public List<Match> getMatchesByStage(@RequestParam String stage) {
        return matchRepository.findByStage(stage);
    }

    // 7. CHAMPION
    @GetMapping("/champion")
    public Team getChampion() {
        return tournamentService.getChampion();
    }


    // 8. TOP SCORER TOÀN GIẢI 
    @GetMapping("/top-scorers")
    public List<Player> topScorers() {
        return tournamentService.getTopScorers();
    }

    // 9. LẤY TẤT CẢ ĐỘI BÓNG
    @GetMapping("/teams")
    public List<Team> getAllTeams() {
        return teamRepository.findAll(Sort.by("name"));
    }

    // 10. LẤY CẦU THỦ THEO ĐỘI
    @GetMapping("/teams/{teamId}/players")
    public List<Player> getPlayersByTeam(@PathVariable String teamId) {
        return playerRepository.findByTeam_Id(teamId);
    }

    // 11. UPDATE PLAYER 
    @PostMapping("/player/update")
    public ResponseEntity<String> updatePlayer(
            @RequestParam String playerId,
            @RequestParam int goals,
            @RequestParam int ownGoals,
            @RequestParam int yellow,
            @RequestParam int red) {

        try {
            Player p = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cầu thủ!"));

            p.setGoals(p.getGoals() + goals);
            p.setOwnGoals(p.getOwnGoals() + ownGoals); 
            p.setYellowCards(p.getYellowCards() + yellow);
            p.setRedCards(p.getRedCards() + red);

            playerRepository.save(p);

            return ResponseEntity.ok("Cập nhật cầu thủ thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
    
    // 12. AUTO MOCK DATA
    @PostMapping("/mock-group-stage")
    public ResponseEntity<String> mockGroupStage() {
        try {
            tournamentService.autoMockGroupStage();
            return ResponseEntity.ok("Đã giả lập xong kết quả toàn bộ Vòng Bảng!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}