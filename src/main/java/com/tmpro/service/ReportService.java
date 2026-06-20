package com.tmpro.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.tmpro.model.Player;
import com.tmpro.model.Statistic;
import com.tmpro.model.Team;
import com.tmpro.repository.StatisticRepository;
import com.tmpro.repository.TeamRepository;
import com.tmpro.security.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("null")
public class ReportService {

    // CLEAN PREMIUM STYLE
    private static final DeviceRgb BRAND_BG = new DeviceRgb(255, 255, 255); // Fondo blanco
    private static final DeviceRgb BRAND_ACCENT = new DeviceRgb(52, 211, 153); // Verde Neón (Cabeceras tabla)
    private static final DeviceRgb BRAND_ACCENT_DIM = new DeviceRgb(5, 150, 105);
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(15, 23, 42); // Texto principal oscuro
    private static final DeviceRgb TEXT_MUTED = new DeviceRgb(71, 85, 105); // Texto gris
    private static final DeviceRgb TEXT_LIGHT = new DeviceRgb(15, 23, 42); // En Clean Premium, los textos sobre fondos claros son oscuros. (Nota: cabeceras irán con texto oscuro sobre verde)
    private static final SolidBorder BORDER = new SolidBorder(new DeviceRgb(226, 232, 240), 0.8f);

    @Autowired
    private PlayerService playerService;

    @Autowired
    private StatisticRepository statisticRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AccessControlService accessControl;

    @Transactional(readOnly = true)
    public byte[] generatePlayerReport(Long playerId) {
        Player player = playerService.findByIdWithTeam(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));
        if (player.getTeam() != null) {
            accessControl.assertCanViewTeam(player.getTeam().getId());
        }

        List<Statistic> statistics = statisticRepository.findByPlayerIdWithPlayer(player.getId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.setMargins(36, 36, 36, 36);
            addBrandHeader(
                    document,
                    "Informe de jugador",
                    player.getTeam() != null
                            ? "Jugador: " + player.getName() + " · " + player.getPosition() + " · Dorsal "
                                    + player.getDorsal() + " · " + player.getTeam().getName()
                            : "Jugador: " + player.getName() + " · " + player.getPosition() + " · Dorsal "
                                    + player.getDorsal());

            Table table = new Table(UnitValue.createPercentArray(new float[] { 3, 1, 1, 1, 1, 1, 1, 1 }))
                    .useAllAvailableWidth();
            table.addHeaderCell(headerCell("Partido"));
            table.addHeaderCell(headerCell("Min."));
            table.addHeaderCell(headerCell("G"));
            table.addHeaderCell(headerCell("A"));
            table.addHeaderCell(headerCell("Tir (Pta)"));
            table.addHeaderCell(headerCell("Pas. (%)"));
            table.addHeaderCell(headerCell("Duel. (%)"));
            table.addHeaderCell(headerCell("Rob/Par"));

            if (statistics.isEmpty()) {
                table.addCell(
                        new Cell(1, 8).add(new Paragraph("Sin registros de rendimiento.").setFontColor(TEXT_MUTED))
                                .setBorder(BORDER).setPadding(10).setTextAlignment(TextAlignment.CENTER));
            } else {
                for (Statistic stat : statistics) {
                    table.addCell(bodyCell(stat.getMatch(), TextAlignment.LEFT));
                    table.addCell(bodyCell(String.valueOf(stat.getMinutesPlayed()), TextAlignment.CENTER));
                    table.addCell(bodyCell(String.valueOf(stat.getGoals()), TextAlignment.CENTER));
                    table.addCell(bodyCell(String.valueOf(stat.getAssists()), TextAlignment.CENTER));
                    table.addCell(bodyCell(formatFraction(stat.getShotsOnTarget(), stat.getShotsTotal()), TextAlignment.CENTER));
                    table.addCell(bodyCell(formatFraction(stat.getPassesCompleted(), stat.getPassesTotal()), TextAlignment.CENTER));
                    table.addCell(bodyCell(formatFraction(stat.getDuelsWon(), stat.getDuelsTotal()), TextAlignment.CENTER));
                    String defStats = stat.getInterceptions() > 0 ? String.valueOf(stat.getInterceptions()) : (stat.getSaves() > 0 ? String.valueOf(stat.getSaves()) : "-");
                    table.addCell(bodyCell(defStats, TextAlignment.CENTER));
                }
            }

            document.add(sectionTitle("Detalle por partido"));
            document.add(table);
            document.add(footerLine());
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el informe", e);
        }

        return outputStream.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] generateTeamReport(Long teamId) {
        accessControl.assertCanViewTeam(teamId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));

        List<Statistic> statistics = statisticRepository.findByTeamIdWithPlayer(teamId);
        Map<Long, PlayerAggregate> aggregates = new LinkedHashMap<>();

        for (Statistic stat : statistics) {
            if (stat.getPlayer() == null) {
                continue;
            }
            Long playerId = stat.getPlayer().getId();
            PlayerAggregate agg = aggregates.computeIfAbsent(playerId, id -> new PlayerAggregate(stat.getPlayer()));
            agg.goals += stat.getGoals();
            agg.assists += stat.getAssists();
            agg.minutes += stat.getMinutesPlayed();
            agg.matches++;
            agg.shotsOnTarget += stat.getShotsOnTarget();
            agg.shotsTotal += stat.getShotsTotal();
            agg.passesCompleted += stat.getPassesCompleted();
            agg.passesTotal += stat.getPassesTotal();
            agg.duelsWon += stat.getDuelsWon();
            agg.duelsTotal += stat.getDuelsTotal();
            agg.interceptions += stat.getInterceptions();
            agg.saves += stat.getSaves();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.setMargins(36, 36, 36, 36);
            addBrandHeader(
                    document,
                    "Informe de equipo",
                    "Equipo: " + team.getName() + " · Entrenador: "
                            + (team.getCoach() != null ? team.getCoach() : "—"));

            document.add(sectionTitle("Análisis de Rendimiento por Posiciones"));

            if (aggregates.isEmpty()) {
                Table table = new Table(1).useAllAvailableWidth();
                table.addCell(
                        new Cell().add(new Paragraph("Sin estadísticas para este equipo.").setFontColor(TEXT_MUTED))
                                .setBorder(BORDER).setPadding(10));
                document.add(table);
            } else {
                
                // Agrupación por posiciones
                List<PlayerAggregate> porteros = new ArrayList<>();
                List<PlayerAggregate> defensas = new ArrayList<>();
                List<PlayerAggregate> medios = new ArrayList<>();
                List<PlayerAggregate> delanteros = new ArrayList<>();
                List<PlayerAggregate> otros = new ArrayList<>();

                for (PlayerAggregate agg : aggregates.values()) {
                    String pos = agg.player.getPosition() != null ? agg.player.getPosition().toLowerCase() : "";
                    if (pos.contains("por")) porteros.add(agg);
                    else if (pos.contains("med") || pos.contains("centro") || pos.contains("piv") || pos.contains("int") || pos.contains("mco") || pos.contains("mp")) medios.add(agg);
                    else if (pos.contains("def") || pos.contains("lat") || pos.contains("central") || pos.equals("df") || pos.equals("ct") || pos.equals("cb")) defensas.add(agg);
                    else if (pos.contains("del") || pos.contains("ext") || pos.contains("pun") || pos.contains("dc")) delanteros.add(agg);
                    else otros.add(agg);
                }

                addCategoryTable(document, "Porteros", porteros, true, false, false, false);
                addCategoryTable(document, "Defensas", defensas, false, true, false, false);
                addCategoryTable(document, "Centrocampistas", medios, false, false, true, false);
                addCategoryTable(document, "Delanteros", delanteros, false, false, false, true);
                addCategoryTable(document, "Otros", otros, false, false, false, false);

            }

            document.add(footerLine());
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el informe", e);
        }

        return outputStream.toByteArray();
    }

    private void addCategoryTable(Document document, String title, List<PlayerAggregate> players, boolean isGK, boolean isDEF, boolean isMID, boolean isFWD) {
        if (players.isEmpty()) return;

        document.add(new Paragraph(title)
                .setFontSize(14)
                .setBold()
                .setFontColor(TEXT_DARK)
                .setMarginTop(16)
                .setMarginBottom(6));

        Table table = new Table(UnitValue.createPercentArray(new float[] { 3, 1, 1, 1, 1, 1, 1, 1, 1 }))
                .useAllAvailableWidth();
        
        table.addHeaderCell(headerCell("Jugador"));
        table.addHeaderCell(headerCell("Min."));
        
        if (isFWD || isMID || isDEF || (!isGK && !isDEF && !isMID && !isFWD)) table.addHeaderCell(headerCell("G")); else table.addHeaderCell(headerCell(""));
        if (isFWD || isMID || (!isGK && !isDEF && !isMID && !isFWD)) table.addHeaderCell(headerCell("A")); else table.addHeaderCell(headerCell(""));
        if (isFWD || isMID) table.addHeaderCell(headerCell("Tir (Pta)")); else table.addHeaderCell(headerCell(""));
        if (isMID || isDEF || isGK) table.addHeaderCell(headerCell("Pas (%)")); else table.addHeaderCell(headerCell(""));
        if (isMID || isDEF) table.addHeaderCell(headerCell("Duel (%)")); else table.addHeaderCell(headerCell(""));
        if (isDEF || isMID) table.addHeaderCell(headerCell("Robos")); else table.addHeaderCell(headerCell(""));
        if (isGK) table.addHeaderCell(headerCell("Paradas")); else table.addHeaderCell(headerCell(""));

        for (PlayerAggregate agg : players) {
            table.addCell(bodyCell(agg.player.getName(), TextAlignment.LEFT));
            table.addCell(bodyCell(String.valueOf(agg.minutes), TextAlignment.CENTER));
            
            if (isFWD || isMID || isDEF || (!isGK && !isDEF && !isMID && !isFWD)) table.addCell(bodyCell(String.valueOf(agg.goals), TextAlignment.CENTER)); else table.addCell(bodyCell("-", TextAlignment.CENTER));
            if (isFWD || isMID || (!isGK && !isDEF && !isMID && !isFWD)) table.addCell(bodyCell(String.valueOf(agg.assists), TextAlignment.CENTER)); else table.addCell(bodyCell("-", TextAlignment.CENTER));
            if (isFWD || isMID) table.addCell(bodyCell(formatFraction(agg.shotsOnTarget, agg.shotsTotal), TextAlignment.CENTER)); else table.addCell(bodyCell("-", TextAlignment.CENTER));
            if (isMID || isDEF || isGK) table.addCell(bodyCell(formatFraction(agg.passesCompleted, agg.passesTotal), TextAlignment.CENTER)); else table.addCell(bodyCell("-", TextAlignment.CENTER));
            if (isMID || isDEF) table.addCell(bodyCell(formatFraction(agg.duelsWon, agg.duelsTotal), TextAlignment.CENTER)); else table.addCell(bodyCell("-", TextAlignment.CENTER));
            if (isDEF || isMID) table.addCell(bodyCell(String.valueOf(agg.interceptions), TextAlignment.CENTER)); else table.addCell(bodyCell("-", TextAlignment.CENTER));
            if (isGK) table.addCell(bodyCell(String.valueOf(agg.saves), TextAlignment.CENTER)); else table.addCell(bodyCell("-", TextAlignment.CENTER));
        }

        document.add(table);
    }

    private static String formatFraction(int part, int total) {
        if (total == 0) return "-";
        int percent = Math.round(((float) part / total) * 100);
        return String.format("%d/%d (%d%%)", part, total, percent);
    }

    private static Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(new DeviceRgb(0, 0, 0))) // Negro puro sobre el verde para Clean Premium
                .setBackgroundColor(BRAND_ACCENT)
                .setPadding(6)
                .setBorder(new SolidBorder(new DeviceRgb(200, 220, 200), 0.5f))
                .setTextAlignment(TextAlignment.CENTER);
    }

    private static Cell bodyCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK))
                .setPadding(6)
                .setBorder(new SolidBorder(new DeviceRgb(230, 230, 230), 0.5f))
                .setTextAlignment(alignment);
    }

    private static Paragraph sectionTitle(String text) {
        return new Paragraph(text)
                .setFontSize(16)
                .setBold()
                .setFontColor(TEXT_DARK)
                .setMarginTop(14)
                .setMarginBottom(8);
    }

    private static Paragraph summaryLine(String text) {
        return new Paragraph(text)
                .setFontSize(11)
                .setBold()
                .setFontColor(TEXT_DARK)
                .setMarginTop(10)
                .setMarginBottom(0);
    }

    private static Paragraph footerLine() {
        return new Paragraph("Generado: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()))
                .setFontSize(9)
                .setFontColor(TEXT_MUTED)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(14)
                .setMarginBottom(0);
    }

    private void addBrandHeader(Document document, String title, String subtitle) {

        Table header = new Table(UnitValue.createPercentArray(new float[] { 1, 6 }))
                .useAllAvailableWidth()
                .setBackgroundColor(BRAND_BG)
                .setBorderRadius(new BorderRadius(12))
                .setPadding(12)
                .setMarginBottom(20);

        // LOGO
        Cell logoCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(BRAND_BG);

        Image logo = loadBrandLogo();
        if (logo != null) {
            logo.setWidth(45).setHeight(45);
            logoCell.add(logo);
        }

        // TEXTO
        Cell textCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(BRAND_BG)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        textCell.add(
                new Paragraph(title)
                        .setFontSize(22)
                        .setBold()
                        .setFontColor(TEXT_DARK)
                        .setMargin(0));

        textCell.add(
                new Paragraph(subtitle)
                        .setFontSize(11)
                        .setFontColor(TEXT_MUTED)
                        .setMarginTop(4));

        header.addCell(logoCell);
        header.addCell(textCell);

        document.add(header);
    }

    private Image loadBrandLogo() {
        try (InputStream is = ReportService.class.getResourceAsStream("/logo-primary.webp")) {
            if (is == null) {
                return null;
            }
            byte[] bytes = is.readAllBytes();
            ImageData data = ImageDataFactory.create(bytes);
            return new Image(data);
        } catch (Exception e) {
            return null;
        }
    }

    private static class PlayerAggregate {
        private final Player player;
        private int goals;
        private int assists;
        private int minutes;
        private int matches;
        private int shotsOnTarget;
        private int shotsTotal;
        private int passesCompleted;
        private int passesTotal;
        private int duelsWon;
        private int duelsTotal;
        private int interceptions;
        private int saves;

        private PlayerAggregate(Player player) {
            this.player = player;
        }
    }
}
