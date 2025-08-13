package org.zemo.omninet.notes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotesRequest {

    private String title;

    private String description;


    private NotesDto.CategoryDto category;
}
