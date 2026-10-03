package com.tourism.business.audit.controller;
import com.tourism.business.audit.service.ActivityService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/activity")
public class ActivityController {
    private final ActivityService service;
    public ActivityController(ActivityService service){this.service=service;}
    @GetMapping public Object list(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) Long actorId,@RequestParam(defaultValue="") String type,@RequestParam(defaultValue="") String entity,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(from,to,actorId,type,entity,page,size);}
    @GetMapping("/options") public Object options(){return service.options();}
    @GetMapping("/{group}/entries") public Object entries(@PathVariable String group,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.entries(group,page,size);}
}
