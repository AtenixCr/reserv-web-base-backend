package com.tourism.business.admissions.controller;
import com.tourism.business.admissions.dto.AdmissionDto.*;
import com.tourism.business.admissions.service.AdmissionService;
import com.tourism.business.finance.service.CashSessionService;
import com.tourism.business.auth.repository.UserRepository.Account;
import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
public class AdmissionController {
    private final AdmissionService admissions;private final CashSessionService cash;
    public AdmissionController(AdmissionService admissions,CashSessionService cash){this.admissions=admissions;this.cash=cash;}
    @GetMapping("/cash-sessions/current") public Object current(@AuthenticationPrincipal Account actor){return cash.current("ADMIN".equals(actor.role));}
    @PostMapping("/cash-sessions") public Object open(@Valid @RequestBody Open request,@AuthenticationPrincipal Account actor){return cash.open(request.openingFloat(),actor.id);}
    @GetMapping("/admissions") public Object list(@RequestParam LocalDate date,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return admissions.list(date,search,page,size);}
    @GetMapping("/admissions/{id}") public Object detail(@PathVariable long id){return admissions.detail(id);}
    @PostMapping("/admissions/quotes") public Object quote(@Valid @RequestBody Request request){return admissions.quote(request);}
    @PostMapping("/admissions") public Object create(@Valid @RequestBody Request request,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return ResponseEntity.status(201).body(admissions.create(request,key,actor.id));}
    @PatchMapping("/admissions/{id}") public Object contact(@PathVariable long id,@Valid @RequestBody Contact request){return admissions.contact(id,request);}
    @PostMapping("/admissions/{id}/correction-quotes") public Object correctionQuote(@PathVariable long id,@Valid @RequestBody Correction request){return admissions.correctionQuote(id,request);}
    @PostMapping("/admissions/{id}/corrections") public Object correction(@PathVariable long id,@Valid @RequestBody Correction request,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return ResponseEntity.status(201).body(admissions.correct(id,request,key,actor.id));}
}
