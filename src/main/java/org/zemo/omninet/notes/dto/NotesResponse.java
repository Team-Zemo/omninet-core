package org.zemo.omninet.notes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotesResponse {

    private List<NotesDto> notes;

    private Integer pageNo;

    private Integer pageSize;

    private Long totalNotesCount;

    private Integer totalPagesCount;

    private boolean isFirst;

    private boolean isLast;
}
