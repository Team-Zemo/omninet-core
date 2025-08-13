package org.zemo.omninet.notes.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zemo.omninet.notes.dto.NotesDto;
import org.zemo.omninet.notes.dto.NotesResponse;
import org.zemo.omninet.notes.entity.Category;
import org.zemo.omninet.notes.entity.FileDetails;
import org.zemo.omninet.notes.entity.Notes;
import org.zemo.omninet.notes.exception.ResourceNotFoundException;
import org.zemo.omninet.notes.repository.CategoryRepo;
import org.zemo.omninet.notes.repository.FileRepo;
import org.zemo.omninet.notes.repository.NotesRepo;
import org.zemo.omninet.notes.service.NotesService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Service
public class NotesServiceImpl implements NotesService {

    @Autowired
    private NotesRepo notesRepo;

    @Autowired
    private ModelMapper mapper;

    @Autowired
    private CategoryRepo categoryRepo;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Autowired
    private FileRepo fileRepo;


    @Override
    public Boolean saveNote(String Notes, MultipartFile file) throws ResourceNotFoundException, IOException {

        ObjectMapper ob = new ObjectMapper();
        NotesDto notesDto = ob.readValue(Notes, NotesDto.class);
        //internal
        notesDto.setIsDeleted(false);
        notesDto.setDeletedAt(null);
        // if id is given then a request for update
        if (!ObjectUtils.isEmpty(notesDto.getId())) {
            updateNotes(notesDto, file);

        }


        // validation can be done  -- > bad mai


        // category exist which we are adding  not doing anything exception fak rhe
        Category category = checkCategoryExist(notesDto.getCategory().getId());


        //mapping and save
        Notes notes = mapper.map(notesDto, Notes.class);


        // related to files
        FileDetails fileDetails = saveFileDetails(file);

        if (!ObjectUtils.isEmpty(fileDetails)) {
            notes.setFileDetails(fileDetails);
        } else {
            if (ObjectUtils.isEmpty(notesDto.getId())) { // create request ho to null krdo
                notes.setFileDetails(null);
            }
        }


        Notes saveNotes = notesRepo.save(notes);


        return !ObjectUtils.isEmpty(saveNotes);

    }

    private void updateNotes(NotesDto notesDto, MultipartFile file) throws ResourceNotFoundException {
        Notes existNotes = notesRepo.findById(notesDto.getId()).orElseThrow(() -> new ResourceNotFoundException("invalid notes id for update"));

        // agr file nhi di to purani wali lgao or baki chize same rahegi
        if (ObjectUtils.isEmpty(file)) {
            notesDto.setFileDetails(mapper.map(existNotes.getFileDetails(), NotesDto.FileDto.class));
        }

    }

    private FileDetails saveFileDetails(MultipartFile file) throws IOException {
        if (!ObjectUtils.isEmpty(file) && !file.isEmpty()) {

            String originalFilename = file.getOriginalFilename();


            List<String> extensionsAllowed = Arrays.asList("pdf", "doc", "xls", "xlsx", "jpg", "png", "docx", "txt");
            if (!extensionsAllowed.contains(FilenameUtils.getExtension(originalFilename))) {
                throw new IllegalArgumentException("File extension not supported");
            }

            FileDetails fileDetails = new FileDetails();
            fileDetails.setOriginalFileName(originalFilename);
            fileDetails.setDisplayFileName(getDisplayName(originalFilename));

            String randomString = UUID.randomUUID().toString();
            // using apache common io here
            //add dependencies
            String extension = FilenameUtils.getExtension(originalFilename);
            String uploadFileName = randomString + "." + extension;  // jsdskfjf.pdf

            fileDetails.setUploadFileName(uploadFileName);
            fileDetails.setSize(file.getSize());


            File saveFile = new File(uploadPath);
            if (!saveFile.exists()) saveFile.mkdirs();
            String storePath = uploadPath.concat(uploadFileName);

            fileDetails.setPath(storePath);


            // uploading done here actual
            long upload = Files.copy(file.getInputStream(), Paths.get(storePath));
            if (upload != 0) {
                FileDetails finalSaved = fileRepo.save(fileDetails);
                return finalSaved;
            }
        }
        return null;
    }

    private String getDisplayName(String originalFilename) {
        // java_programming_tutorial.pdf
        // java_pro.pdf


        // using apache common io here
        //add dependencies
        String extension = FilenameUtils.getExtension(originalFilename);
        String fileName = FilenameUtils.removeExtension(originalFilename);

        if (fileName.length() > 8) {
            fileName = fileName.substring(0, 8);
        }
        fileName = fileName + "." + extension;
        return fileName;

    }

    private Category checkCategoryExist(Integer id) throws ResourceNotFoundException {
        return categoryRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("category is invalid"));
    }


    @Override
    public List<NotesDto> getAllNotes() {
        return notesRepo.findAll().stream()
                .map(notes -> mapper.map(notes, NotesDto.class)).toList();
    }


    @Override
    public byte[] downloadFile(FileDetails fileDetails) throws ResourceNotFoundException, IOException {
        // logic
        InputStream io = new FileInputStream(fileDetails.getPath());

        return IOUtils.toByteArray(io);

    }

    @Override
    public FileDetails getFileDetails(Integer id) throws ResourceNotFoundException {
        return fileRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("file is not available"));

    }

    @Override
    public NotesResponse getAllNotesByUser(String userID, Integer pageNo, Integer pageSize) {


        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Notes> notes = notesRepo.findByCreatedByAndIsDeletedFalse(userID, pageable);

        List<NotesDto> notesDto = notes.get().map(n -> mapper.map(n, NotesDto.class)).toList();

        NotesResponse notesResponse = NotesResponse.builder()
                .notes(notesDto)
                .pageNo(notes.getNumber())
                .pageSize(notes.getSize())
                .totalNotesCount(notes.getTotalElements())
                .totalPagesCount(notes.getTotalPages())
                .isFirst(notes.isFirst())
                .isLast(notes.isLast())
                .build();

        return notesResponse;


    }

    @Override
    public NotesResponse getAllNotesBySearch(String userID, Integer pageNo, Integer pageSize, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Notes> notes = notesRepo.searchNotes(keyword, userID, pageable);

        List<NotesDto> notesDto = notes.get().map(n -> mapper.map(n, NotesDto.class)).toList();

        NotesResponse notesResponse = NotesResponse.builder()
                .notes(notesDto)
                .pageNo(notes.getNumber())
                .pageSize(notes.getSize())
                .totalNotesCount(notes.getTotalElements())
                .totalPagesCount(notes.getTotalPages())
                .isFirst(notes.isFirst())
                .isLast(notes.isLast())
                .build();

        return notesResponse;
    }

    @Override
    public void softDeleteNotes(Integer id) throws ResourceNotFoundException {
        Notes notes = notesRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notes id invalid "));
        notes.setIsDeleted(true);
        notes.setDeletedAt(LocalDateTime.now());
        notesRepo.save(notes);
    }

    @Override
    public void restoreNotes(Integer id) throws ResourceNotFoundException {
        Notes notes = notesRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notes id invalid "));
        notes.setIsDeleted(false);
        notes.setDeletedAt(null);
        notesRepo.save(notes);
    }

    @Override
    public List<NotesDto> getUserRecycleBInNotes(String userID) {

        List<NotesDto> notes = notesRepo
                .findByCreatedByAndIsDeletedTrue(userID).
                stream().map(n -> mapper.map(n, NotesDto.class))
                .toList();

        return notes;
    }

    @Override
    public void hardDeleteNotes(Integer id) throws ResourceNotFoundException {
        Notes notes = notesRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("notes not found"));

        if (notes.getIsDeleted()) {
            notesRepo.delete(notes);
        } else {
            throw new IllegalArgumentException("Sorry u can't delete directly");
        }
    }

    @Override
    public void emptyRecycleBin(String userID) {

        List<Notes> recycleNotes = notesRepo.findByCreatedByAndIsDeletedTrue(userID);

        if (!CollectionUtils.isEmpty(recycleNotes)) {
            notesRepo.deleteAll(recycleNotes);
        }
    }

    @Override
    public boolean copyNotes(Integer id) throws ResourceNotFoundException {
        Notes notes = notesRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("notes not found"));

        Notes copy = Notes.builder()
                .title(notes.getTitle())
                .description(notes.getDescription())
                .category(notes.getCategory())
                .isDeleted(false)
                .fileDetails(null)
                .build();

        // check user validation


        return !ObjectUtils.isEmpty(notesRepo.save(copy));
    }


}
