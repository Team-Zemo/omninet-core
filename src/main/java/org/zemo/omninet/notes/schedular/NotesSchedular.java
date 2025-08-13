package org.zemo.omninet.notes.schedular;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zemo.omninet.notes.entity.Notes;
import org.zemo.omninet.notes.repository.NotesRepo;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NotesSchedular {


    private final NotesRepo notesRepo;

    public NotesSchedular(NotesRepo notesRepo) {
        this.notesRepo = notesRepo;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteNotesSchedular() {
        // 09 / 08/ 2025   - 7days =->
        LocalDateTime cutOffDate = LocalDateTime.now().minusDays(7);

        List<Notes> deleteNotes = notesRepo.findAllByIsDeletedAndDeletedAtBefore(true, cutOffDate);

        notesRepo.deleteAll(deleteNotes);  // delete permanent
    }
}
