package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequestDTO {
    @NotBlank @Size(max=255)
    private String title;
    private String description;
    @NotBlank
    private String content;
    private String[] tags;
    @Size(max=500)
    private String filePath;
    @NotBlank
    private String postType;
    private String[] keywords;
}
