package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.service.OULinesImport;
import io.github.liamtkehoe.nhl_pickem.service.OULinesService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final OULinesService lines;

    public AdminController(OULinesService lines) {
        this.lines = lines;
    }

    public record ImportResult(int lines) {}

    @PutMapping("/lines")
    public ImportResult importLines(@RequestBody OULinesImport request) {
        return new ImportResult(lines.importLines(request));
    }
}
