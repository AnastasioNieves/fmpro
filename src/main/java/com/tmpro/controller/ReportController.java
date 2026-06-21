package com.tmpro.controller;

import com.tmpro.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/players/{id}")
    public ResponseEntity<byte[]> getPlayerReport(@PathVariable String id) {
        byte[] pdfContent = reportService.generatePlayerReport(id);
        return pdfResponse(pdfContent, "informe-jugador-" + id + ".pdf");
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<byte[]> getTeamReport(@PathVariable String id) {
        byte[] pdfContent = reportService.generateTeamReport(id);
        return pdfResponse(pdfContent, "informe-equipo-" + id + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdfContent, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }
}
