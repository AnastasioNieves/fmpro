package com.tmpro.config;

import com.tmpro.model.Player;
import com.tmpro.model.Team;
import com.tmpro.repository.PlayerRepository;
import com.tmpro.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_TEAM = "FC Prueba";

    private static final String[][] SQUAD = {
            {"1", "Portero", "Álvaro Mendoza"},
            {"13", "Portero", "Pablo Herrera"},
            {"2", "Defensa", "Diego Salinas"},
            {"3", "Defensa", "Hugo Ramírez"},
            {"4", "Defensa", "Iván Torres"},
            {"5", "Defensa", "Marcos Gil"},
            {"12", "Defensa", "Sergio León"},
            {"14", "Defensa", "Rubén Castro"},
            {"6", "Centrocampista", "Andrés Vega"},
            {"8", "Centrocampista", "Jorge Navarro"},
            {"10", "Centrocampista", "Luis Morales"},
            {"15", "Centrocampista", "Miguel Rojas"},
            {"16", "Centrocampista", "Raúl Paredes"},
            {"18", "Centrocampista", "Tomás Aguirre"},
            {"7", "Delantero", "Carlos Ibáñez"},
            {"9", "Delantero", "Fernando Luna"},
            {"11", "Delantero", "Javier Romero"},
            {"17", "Delantero", "Nicolás Duarte"},
            {"19", "Delantero", "Óscar Fuentes"},
            {"20", "Delantero", "Pedro Arias"},
            {"21", "Delantero", "Ricardo Soler"},
            {"22", "Delantero", "Víctor Campos"},
    };

    public void seed(TeamRepository teamRepository, PlayerRepository playerRepository, String ownerUserId) {
        Team team = teamRepository.findByName(DEMO_TEAM);
        if (team == null) {
            team = new Team();
            team.setName(DEMO_TEAM);
            team.setCoach("Entrenador Demo");
            team.setOwnerUserId(ownerUserId);
            team = teamRepository.save(team);
            log.info("Equipo de prueba creado: {}", DEMO_TEAM);
        } else if (team.getOwnerUserId() == null && ownerUserId != null) {
            team.setOwnerUserId(ownerUserId);
            teamRepository.save(team);
        }

        if (playerRepository.countByTeamId(team.getId()) >= SQUAD.length) {
            return;
        }

        Set<String> existingDorsals = playerRepository.findByTeamId(team.getId()).stream()
                .map(Player::getDorsal)
                .collect(Collectors.toSet());

        int created = 0;
        for (String[] row : SQUAD) {
            String dorsal = row[0];
            String position = row[1];
            String name = row[2];

            if (existingDorsals.contains(dorsal)) {
                continue;
            }

            Player player = new Player();
            player.setDorsal(dorsal);
            player.setPosition(position);
            player.setName(name);
            player.setTeamId(team.getId());
            
            if ("Portero".equalsIgnoreCase(position)) {
                player.setPhotoUrl("/uploads/players/gk.png");
            } else if ("Defensa".equalsIgnoreCase(position)) {
                player.setPhotoUrl("/uploads/players/def.png");
            } else if ("Centrocampista".equalsIgnoreCase(position)) {
                player.setPhotoUrl("/uploads/players/mid.png");
            } else {
                player.setPhotoUrl("/uploads/players/fwd.png");
            }

            playerRepository.save(player);
            created++;
        }

        if (created > 0) {
            log.info("Plantilla de prueba: {} jugadores añadidos a {}", created, DEMO_TEAM);
        }
    }
}
