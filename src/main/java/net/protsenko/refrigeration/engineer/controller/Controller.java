package net.protsenko.refrigeration.engineer.controller;

import lombok.RequiredArgsConstructor;
import net.protsenko.refrigeration.engineer.domain.model.ChamberSpecList;
import net.protsenko.refrigeration.engineer.service.IntakeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/spec")
@RequiredArgsConstructor
public class Controller {
    private final IntakeService intakeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChamberSpecList upload(
            @RequestPart("spec") MultipartFile spec,
            @RequestPart(value = "layout", required = false) MultipartFile layout,
            @RequestPart(value = "turnover", required = false) MultipartFile turnover
    ) {
        return intakeService.upload(spec, layout, turnover);
    }
}
