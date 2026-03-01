package net.protsenko.refrigeration.engineer.service.processor;

import org.springframework.web.multipart.MultipartFile;

public interface FileProcessor {
    RawContent process(MultipartFile file) throws Exception;
}
