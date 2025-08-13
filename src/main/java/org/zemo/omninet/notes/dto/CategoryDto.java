package org.zemo.omninet.notes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {


    private Integer id;

    //    @NotBlank
//    @Min(value = 1)
//    @Max(value = 20)
    private String name;

    //    @NotBlank
//    @Min(value = 1)
//    @Max(value = 100)
    private String description;

    //    @NotNull
    private Boolean isActive;

    private String createdBy;

    private Date createdDate;

    private String updatedBy;

    private Date updatedDate;

}
