package com.example.demo.repository;

import com.example.demo.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    // MỚI: Lấy TẤT CẢ cầu thủ ghi ít nhất 1 bàn trong TOÀN GIẢI (xếp từ cao xuống thấp)
    List<Player> findByGoalsGreaterThanOrderByGoalsDesc(int goals);

    // Lấy danh sách cầu thủ thuộc về một Đội bóng
    List<Player> findByTeam_Id(String teamId); 
}