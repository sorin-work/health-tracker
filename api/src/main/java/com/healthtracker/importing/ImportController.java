package com.healthtracker.importing;

import com.healthtracker.common.ApiException;
import com.healthtracker.security.AuthUser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final VitalImportService vitalImportService;

    public ImportController(VitalImportService vitalImportService) {
        this.vitalImportService = vitalImportService;
    }

    /** Upload a CSV of readings. With {@code dryRun=true} nothing is saved; the result shows what would be. */
    @PostMapping("/vitals")
    public ImportResult vitals(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) throws IOException {
        if (file.isEmpty()) {
            throw ApiException.badRequest("The file is empty");
        }
        return vitalImportService.importCsv(user.id(), new String(file.getBytes(), StandardCharsets.UTF_8), dryRun);
    }
}
