package com.tourism.business.business.controller;
import com.tourism.business.business.dto.SettingsDto.*;
import com.tourism.business.business.service.SettingsService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
public class SettingsController {
    private final SettingsService service;
    public SettingsController(SettingsService service) { this.service=service; }
    @GetMapping("/settings") Snapshot read() { return service.read(); }
    @PutMapping("/settings") Snapshot save(@Valid @RequestBody Settings body) { return service.save(body); }
    @PostMapping("/rate-plans") Snapshot publish(@Valid @RequestBody Prices body) { return service.publish(body); }
    @PutMapping("/exchange-rates") Snapshot exchanges(@Valid @RequestBody Exchanges body) { return service.exchanges(body); }
    @GetMapping("/public/business") Map<String,Object> business() { return service.publicBusiness(); }
    @GetMapping("/dashboard") Map<String,Object> dashboard() { return service.dashboard(); }
}
