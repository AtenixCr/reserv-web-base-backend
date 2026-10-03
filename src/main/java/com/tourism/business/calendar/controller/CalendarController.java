package com.tourism.business.calendar.controller;
import com.tourism.business.calendar.dto.TaskDto.*;
import com.tourism.business.calendar.service.CalendarService;
import com.tourism.business.auth.repository.UserRepository.Account;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
public class CalendarController {
    private final CalendarService service;
    public CalendarController(CalendarService service){this.service=service;}
    @GetMapping("/calendar") public Object month(@RequestParam(required=false) String month){return service.month(month);}
    @GetMapping("/tasks") public Object list(@RequestParam LocalDate date,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.tasks(date,page,size);}
    @GetMapping("/tasks/{id}") public Object detail(@PathVariable long id){return service.detail(id);}
    @PostMapping("/tasks") public Object create(@Valid @RequestBody Save body,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return ResponseEntity.status(201).body(service.save(null,body,key,actor.id));}
    @PatchMapping("/tasks/{id}") public Object update(@PathVariable long id,@Valid @RequestBody Save body,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return service.save(id,body,key,actor.id);}
    @DeleteMapping("/tasks/{id}") public Object delete(@PathVariable long id,@Valid @RequestBody Delete body,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return service.delete(id,body,key,actor.id);}
}
