package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePostRequestDTO {
    @NotBlank @Size(max=255)
    private String title;
    @Size(max=5000) private String description;
    @NotBlank @Size(max=100000)
    private String content;
    @Size(max=20) private String[] tags;
    @Size(max=500)
    private String filePath;
    @NotBlank
    private String postType;
    @Size(max=10) private String[] keywords;
}
