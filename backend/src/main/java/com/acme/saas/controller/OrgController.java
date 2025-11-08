package com.acme.saas.controller;

import com.acme.saas.domain.Org;
import com.acme.saas.service.OrgService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {

    record CreateOrgRequest(@NotBlank String slug, @NotBlank String name) {}

    private final OrgService orgService;
    public OrgController(OrgService orgService) { this.orgService = orgService; }

    @PostMapping
    public ResponseEntity<Org> create(@RequestBody CreateOrgRequest req) {
        Org org = orgService.createOrg(req.slug(), req.name());
        return ResponseEntity.ok(org);
    }

    @GetMapping("/whoami")
    public Map<String, String> whoami(@RequestHeader(value="X-Tenant-Id", required=false) String tenant) {
        return Map.of("tenant", tenant == null ? "public" : tenant);
    }
}
