package com.tmpro.service;

import com.tmpro.model.Player;
import com.tmpro.repository.PlayerRepository;
import com.tmpro.security.AccessControlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

@Service
@SuppressWarnings("null")
public class PlayerPhotoService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final PlayerRepository playerRepository;
    private final AccessControlService accessControl;
    private final Path playersDir;

    public PlayerPhotoService(
            PlayerRepository playerRepository,
            AccessControlService accessControl,
            @Value("${fmpro.uploads-dir:uploads}") String uploadsDir
    ) {
        this.playerRepository = playerRepository;
        this.accessControl = accessControl;
        this.playersDir = Paths.get(uploadsDir, "players").toAbsolutePath().normalize();
    }

    public String savePhoto(Long playerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Formato no válido. Usa JPG, PNG o WebP.");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));
        if (player.getTeam() != null) {
            accessControl.assertCanManageTeam(player.getTeam().getId());
        }

        String extension = switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        try {
            Files.createDirectories(playersDir);
            deleteExistingFiles(playerId);
            Path target = playersDir.resolve(playerId + extension);
            file.transferTo(target);
            String publicUrl = "/uploads/players/" + playerId + extension;
            player.setPhotoUrl(publicUrl);
            playerRepository.save(player);
            return publicUrl;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la foto", e);
        }
    }

    private void deleteExistingFiles(Long playerId) throws IOException {
        for (String ext : new String[]{".jpg", ".jpeg", ".png", ".webp"}) {
            Path candidate = playersDir.resolve(playerId + ext);
            if (Files.exists(candidate)) {
                Files.delete(candidate);
            }
        }
    }
}

