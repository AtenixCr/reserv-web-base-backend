package com.tourism.business.finance.controller;
import com.tourism.business.finance.dto.FinanceDto.*;
import com.tourism.business.finance.service.FinanceService;
import com.tourism.business.reports.service.ClosureReports;
import com.tourism.business.auth.repository.UserRepository.Account;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1")
public class FinanceController {
    private final FinanceService finance;private final ClosureReports reports;
    public FinanceController(FinanceService finance,ClosureReports reports){this.finance=finance;this.reports=reports;}
    @GetMapping("/finance/summary") public Object summary(@RequestParam LocalDate date){return finance.summary(date);}
    @GetMapping("/finance/admissions/{id}/refundable") public Object refundable(@PathVariable long id){return finance.refundable(id);}
    @PostMapping("/expenses") public Object expense(@Valid @RequestBody Expense body,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return ResponseEntity.status(201).body(finance.expense(body,key,actor.id));}
    @PostMapping("/refunds") public Object refund(@Valid @RequestBody Refund body,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return ResponseEntity.status(201).body(finance.refund(body,key,actor.id));}
    @PostMapping("/adjustments/quotes") public Object quote(@Valid @RequestBody Compensation body){return finance.compensationQuote(body);}
    @PostMapping("/adjustments") public Object compensate(@Valid @RequestBody Compensation body,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return ResponseEntity.status(201).body(finance.compensate(body,key,actor.id));}
    @PostMapping("/cash-sessions/{id}/close") public Object close(@PathVariable long id,@Valid @RequestBody Close body,@RequestHeader(value="Idempotency-Key",required=false) String key,@AuthenticationPrincipal Account actor){return ResponseEntity.status(201).body(finance.close(id,body,key,actor.id));}
    @GetMapping("/closures/{id}") public Object closure(@PathVariable long id){return finance.closure(id);}
    @GetMapping("/closures/{id}/report") public ResponseEntity<byte[]> report(@PathVariable long id,@RequestParam String format,@AuthenticationPrincipal Account actor){var content=reports.generate(id,format);finance.exported(id,format,actor.id);return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"closure-"+id+"."+format+"\"").contentType(MediaType.parseMediaType(format.equals("pdf")?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(content);}
    @PostMapping("/closures/{id}/email") public Object retry(@PathVariable long id){return finance.retryEmail(id);}
}
