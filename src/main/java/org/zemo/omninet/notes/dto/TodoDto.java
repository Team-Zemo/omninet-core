package org.zemo.omninet.notes.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TodoDto {

    private Integer id;

    private String title;

    private StatusDto status;

    private String createdBy;

    private Date createdDate;

    private String updatedBy;

    private Date updatedDate;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatusDto {
        private Integer id;
        private String name;

    }

}
