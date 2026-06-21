package com.tmpro.controller;

import com.tmpro.model.Player;
import com.tmpro.model.PlayerDTO;
import com.tmpro.service.PlayerPhotoService;
import com.tmpro.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerPhotoService playerPhotoService;

    @PostMapping
    public ResponseEntity<PlayerDTO> createPlayer(@RequestBody PlayerDTO playerDTO) {
        Player newPlayer = playerService.createPlayer(toEntity(playerDTO));
        PlayerDTO response = toDto(newPlayer);
        if (response.getTeam_id() == null) {
            response.setTeam_id(playerDTO.getTeam_id());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PlayerDTO>> getAllPlayers() {
        return ResponseEntity.ok(toDtoList(playerService.getAllPlayers()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String id) {
        if (playerService.deletePlayer(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerDTO> updatePlayer(@PathVariable String id, @RequestBody PlayerDTO playerDTO) {
        return playerService.updatePlayer(id, toEntity(playerDTO))
                .map(player -> ResponseEntity.ok(toDto(player)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<PlayerDTO> uploadPhoto(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file
    ) {
        String photoUrl = playerPhotoService.savePhoto(id, file);
        return playerService.findById(id)
                .map(player -> {
                    PlayerDTO dto = toDto(player);
                    dto.setPhotoUrl(photoUrl);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Player toEntity(PlayerDTO dto) {
        Player player = new Player();
        player.setName(dto.getName());
        player.setDorsal(dto.getDorsal());
        player.setPosition(dto.getPosition());

        if (dto.getTeam_id() == null || dto.getTeam_id().isBlank()) {
            throw new IllegalArgumentException("team_id es obligatorio");
        }

        player.setTeamId(dto.getTeam_id().trim());
        return player;
    }

    private PlayerDTO toDto(Player player) {
        PlayerDTO dto = new PlayerDTO();
        dto.setId(player.getId());
        dto.setName(player.getName());
        dto.setDorsal(player.getDorsal());
        dto.setPosition(player.getPosition());
        if (player.getTeamId() != null) {
            dto.setTeam_id(player.getTeamId());
        }
        dto.setPhotoUrl(player.getPhotoUrl());
        return dto;
    }

    private List<PlayerDTO> toDtoList(List<Player> players) {
        List<PlayerDTO> list = new ArrayList<>();
        for (Player player : players) {
            list.add(toDto(player));
        }
        return list;
    }
}
