package com.tourism.business.business.controller;

import com.tourism.business.business.dto.ParkServiceDto.*;
import com.tourism.business.business.service.ParkServiceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/services")
public class ParkServiceController {

    private final ParkServiceService service;

    public ParkServiceController(ParkServiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminService> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminService create(@Valid @RequestBody Save body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    public AdminService update(
        @PathVariable("id") long id,
        @Valid @RequestBody Save body
    ) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(
        @PathVariable("id") long id,
        @RequestParam("version") String version
    ) {
        service.disable(id, version);
    }
}