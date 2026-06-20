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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("null")
public class ReportService {

    private static final DeviceRgb BRAND_BG = new DeviceRgb(6, 14, 11);
    private static final DeviceRgb BRAND_ACCENT = new DeviceRgb(52, 211, 153);
    private static final DeviceRgb BRAND_ACCENT_DIM = new DeviceRgb(5, 150, 105);
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(15, 23, 42);
    private static final DeviceRgb TEXT_MUTED = new DeviceRgb(71, 85, 105);
    private static final DeviceRgb TEXT_LIGHT = new DeviceRgb(255, 255, 255);
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

            int totalGoals = 0;
            int totalAssists = 0;
            int totalMinutes = 0;

            Table table = new Table(UnitValue.createPercentArray(new float[] { 4, 1, 1, 1 }))
                    .useAllAvailableWidth();
            table.addHeaderCell(headerCell("Partido"));
            table.addHeaderCell(headerCell("Goles"));
            table.addHeaderCell(headerCell("Asist."));
            table.addHeaderCell(headerCell("Min."));

            if (statistics.isEmpty()) {
                table.addCell(
                        new Cell(1, 4).add(new Paragraph("Sin registros de rendimiento.").setFontColor(TEXT_MUTED))
                                .setBorder(BORDER).setPadding(10));
            } else {
                for (Statistic stat : statistics) {
                    table.addCell(bodyCell(stat.getMatch(), TextAlignment.LEFT));
                    table.addCell(bodyCell(String.valueOf(stat.getGoals()), TextAlignment.CENTER));
                    table.addCell(bodyCell(String.valueOf(stat.getAssists()), TextAlignment.CENTER));
                    table.addCell(bodyCell(String.valueOf(stat.getMinutesPlayed()), TextAlignment.CENTER));
                    totalGoals += stat.getGoals();
                    totalAssists += stat.getAssists();
                    totalMinutes += stat.getMinutesPlayed();
                }
            }

            document.add(sectionTitle("Detalle por partido"));
            document.add(table);
            document.add(summaryLine(String.format(
                    "Totales: %d goles · %d asistencias · %d minutos",
                    totalGoals, totalAssists, totalMinutes)));
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

            Table table = new Table(UnitValue.createPercentArray(new float[] { 3, 2, 1, 1, 1, 1 }))
                    .useAllAvailableWidth();
            table.addHeaderCell(headerCell("Jugador"));
            table.addHeaderCell(headerCell("Posición"));
            table.addHeaderCell(headerCell("Part."));
            table.addHeaderCell(headerCell("Goles"));
            table.addHeaderCell(headerCell("Asist."));
            table.addHeaderCell(headerCell("Min."));

            document.add(sectionTitle("Resumen por jugador"));

            if (aggregates.isEmpty()) {
                table.addCell(
                        new Cell(1, 6).add(new Paragraph("Sin estadísticas para este equipo.").setFontColor(TEXT_MUTED))
                                .setBorder(BORDER).setPadding(10));
                document.add(table);
            } else {
                int teamGoals = 0;
                int teamAssists = 0;
                int teamMinutes = 0;
                for (PlayerAggregate agg : aggregates.values()) {
                    table.addCell(bodyCell(agg.player.getName(), TextAlignment.LEFT));
                    table.addCell(bodyCell(agg.player.getPosition(), TextAlignment.LEFT));
                    table.addCell(bodyCell(String.valueOf(agg.matches), TextAlignment.CENTER));
                    table.addCell(bodyCell(String.valueOf(agg.goals), TextAlignment.CENTER));
                    table.addCell(bodyCell(String.valueOf(agg.assists), TextAlignment.CENTER));
                    table.addCell(bodyCell(String.valueOf(agg.minutes), TextAlignment.CENTER));
                    teamGoals += agg.goals;
                    teamAssists += agg.assists;
                    teamMinutes += agg.minutes;
                }
                document.add(table);
                document.add(summaryLine(String.format(
                        "Totales del equipo: %d goles · %d asistencias · %d minutos",
                        teamGoals, teamAssists, teamMinutes)));
                document.add(footerLine());
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el informe", e);
        }

        return outputStream.toByteArray();
    }



    private static Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setBold()
                        .setFontSize(10)
                        .setFontColor(TEXT_LIGHT))
                .setBackgroundColor(BRAND_ACCENT)
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private static Cell bodyCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(10)
                        .setFontColor(TEXT_DARK))
                .setPadding(8)
                .setBorder(new SolidBorder(new DeviceRgb(230, 230, 230), 0.5f))
                .setTextAlignment(alignment);
    }

    private static Paragraph sectionTitle(String text) {
        return new Paragraph(text)
                .setFontSize(11)
                .setBold()
                .setFontColor(BRAND_ACCENT_DIM)
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
                        .setFontSize(18)
                        .setBold()
                        .setFontColor(TEXT_LIGHT)
                        .setMargin(0));

        textCell.add(
                new Paragraph(subtitle)
                        .setFontSize(10)
                        .setFontColor(new DeviceRgb(180, 200, 200))
                        .setMarginTop(4));

        header.addCell(logoCell);
        header.addCell(textCell);

        document.add(header);
    }

    private Image loadBrandLogo() {
        try (InputStream is = ReportService.class.getResourceAsStream("C:\\Users\\tasio\\Desktop\\fmpro-main\\frontend\\public\\logo-primary.webp")) {
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

        private PlayerAggregate(Player player) {
            this.player = player;
        }
    }
}

