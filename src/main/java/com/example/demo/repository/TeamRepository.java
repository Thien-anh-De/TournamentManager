package com.example.demo.repository;

import com.example.demo.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface TeamRepository extends JpaRepository<Team, String> {
    List<Team> findByGroupName(String groupName);

}