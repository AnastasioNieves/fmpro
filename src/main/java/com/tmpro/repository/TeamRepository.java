package com.tmpro.repository;

import com.tmpro.model.Team;
import com.tmpro.model.TeamDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {


    Team findByName(String name);

    List<Team> findByOwnerUserId(Long ownerUserId);

    List<Team> findByIdIn(List<Long> ids);

    @Query("SELECT new com.tmpro.model.TeamDTO(t.id, t.name, t.coach) FROM Team t")
    List<TeamDTO> findAllAsDTO();
}
