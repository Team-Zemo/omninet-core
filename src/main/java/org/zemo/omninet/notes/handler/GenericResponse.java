package org.zemo.omninet.notes.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenericResponse {

    private String status;  // success or fail ->

    private String message;  // custom message

    private Object data;

    private HttpStatus responseStatus;

    public ResponseEntity<?> create() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status);
        map.put("message", message);
        if (!ObjectUtils.isEmpty(data)) map.put("data", data);

        return new ResponseEntity<>(map, responseStatus);
    }
}
